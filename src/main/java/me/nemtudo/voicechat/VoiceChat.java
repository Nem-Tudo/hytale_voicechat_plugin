package me.nemtudo.voicechat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import me.nemtudo.voicechat.utils.VersionComparator;
import me.nemtudo.voicechat.utils.VersionStatus;
import me.nemtudo.voicechat.utils.VoiceChatConfig;

import javax.annotation.Nonnull;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VoiceChat Plugin
 * Sincroniza jogadores (posição / mundo) com API externa
 * Otimizado para múltiplos mundos e 500+ players
 */
public class VoiceChat extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long FORCE_UPDATE_INTERVAL_MINUTES = 3;

    public final Config<VoiceChatConfig> config;

    private final Gson gson = new GsonBuilder().create();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Snapshot coletado no tick atual (thread-safe)
    private final Map<String, PlayerState> collectedStates = new ConcurrentHashMap<>();

    // Último snapshot enviado
    private final Map<String, PlayerState> previousStates = new HashMap<>();

    //private long lastPlayerUpdateTime = 0;

    private ScheduledFuture<?> trackingTask;
    private ScheduledFuture<?> forceUpdateTask;

    // Version check
    private volatile String latestStableVersion = null;
    public volatile String downloadPluginURL = null;
    private volatile boolean versionMismatch = false;

    public VoiceChat(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = this.withConfig("VoiceChat", VoiceChatConfig.CODEC);
        LOGGER.atInfo().log("VoiceChat loaded! By ozb (@nemtudo)");
    }

    // ────────────────────────────────────────────────
    // Plugin lifecycle
    // ────────────────────────────────────────────────

    @Override
    protected void setup() {
        config.save();

        LOGGER.atInfo().log("Server ID: " + config.get().getServerId());
        LOGGER.atInfo().log("API Base URL: " + config.get().getApiBaseUrl());

        getCommandRegistry().registerCommand(new VoiceChatCommand(this));

        checkPluginVersion();
        startPlayerTracking();
        startForcePlayerUpdateTimer();
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> consolidateAndSendIfNeeded(true),
                2,
                TimeUnit.SECONDS
        );

        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerJoin);
    }

    @Override
    protected void shutdown() {
        if (trackingTask != null) trackingTask.cancel(false);
        if (forceUpdateTask != null) forceUpdateTask.cancel(false);
        LOGGER.atInfo().log("VoiceChat disabled");
    }

    private void checkPluginVersion() {
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                String pluginName = getName();
                String currentVersion = getManifest().getVersion().toString();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                config.get().getApiBaseUrl()
                                        + "/plugins/"
                                        + pluginName
                                        + "/versions"
                        ))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() != 200) {
                    LOGGER.atWarning().log(
                            "Failed to check plugin version (HTTP " + response.statusCode() + ")"
                    );
                    return;
                }

                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                latestStableVersion = json.get("latestStableVersion").getAsString();
                downloadPluginURL = json.get("downloadPluginURL").getAsString();

                VersionStatus status = VersionComparator.compare(currentVersion, latestStableVersion);

                switch (status) {

                    case SAME_VERSION -> {
                        LOGGER.atInfo().log("VoiceChat is on the most up-to-date version possible :)");
                        LOGGER.atInfo().log("Current version : " + currentVersion + " | Latest stable : " + latestStableVersion);
                    }

                    case BEHIND_LAST_PATCH -> {
                        LOGGER.atWarning().log("VoiceChat is slightly outdated (patch version behind).");
                        LOGGER.atWarning().log("Current version : " + currentVersion + " | Latest stable : " + latestStableVersion);
                        LOGGER.atWarning().log("Lasted stable Download Link: " + downloadPluginURL);
                    }

                    case BEHIND_MAJOR -> {
                        versionMismatch = true;

                        LOGGER.atSevere().log("==================================================");
                        LOGGER.atSevere().log(" VoiceChat version MAJOR mismatch detected!");
                        LOGGER.atSevere().log(" Current version               : " + currentVersion);
                        LOGGER.atSevere().log(" Latest stable                 : " + latestStableVersion);
                        LOGGER.atSevere().log(" Lasted stable Download Link   : " + downloadPluginURL);
                        LOGGER.atSevere().log(" Please update the plugin as soon as possible.");
                        LOGGER.atSevere().log("==================================================");
                    }

                    case AHEAD_LAST_PATCH -> {
                        LOGGER.atWarning().log("VoiceChat is running a newer PATCH version than the latest stable.");
                        LOGGER.atWarning().log("This is usually safe, but unexpected issues may occur.");
                        LOGGER.atWarning().log("Current version : " + currentVersion + " | Latest stable : " + latestStableVersion);
                        LOGGER.atWarning().log("Lasted stable Download Link: " + downloadPluginURL);
                    }

                    case AHEAD_MAJOR -> {
                        LOGGER.atWarning().log("==================================================");
                        LOGGER.atWarning().log(" VoiceChat is running a NEWER MAJOR version!");
                        LOGGER.atWarning().log(" This build may be unstable or incompatible.");
                        LOGGER.atWarning().log(" Current version : " + currentVersion);
                        LOGGER.atWarning().log(" Latest stable   : " + latestStableVersion);
                        LOGGER.atWarning().log(" Lasted stable Download Link: " + downloadPluginURL);
                        LOGGER.atWarning().log("==================================================");
                    }
                }


            } catch (Exception e) {
                LOGGER.atSevere().log(
                        "Failed to check VoiceChat plugin version: " + e.getMessage()
                );
            }
        });
    }


    // ────────────────────────────────────────────────
    // Player join messages
    // ────────────────────────────────────────────────

    private void onPlayerJoin(PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        Player player = event.getPlayer();

        if (player == null) return;

        if (config.get().getAnnounceVoiceChatOnJoin()) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                playerRef.sendMessage(
                        Message.raw(config.get().getAnnounceVoiceChatOnJoinMessage())
                                .bold(true)
                                .color(Color.GREEN)
                );
            }, 7, TimeUnit.SECONDS);
        }

        if (versionMismatch && player.hasPermission("nemtudo.voicechat.warns.differentVersion")) {

            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                playerRef.sendMessage(Message.raw("Hey admin! VoiceChat is outdated on this server!").color(Color.ORANGE).bold(true));
                playerRef.sendMessage(Message.raw("Current version: " + getManifest().getVersion().toString() + " | Latest stable: " + latestStableVersion).color(Color.YELLOW));
                playerRef.sendMessage(Message.raw("Download here: " + downloadPluginURL).color(Color.YELLOW).link(downloadPluginURL));
            }, 8, TimeUnit.SECONDS);
        }
    }

    // ────────────────────────────────────────────────
    // Tracking logic (FASE 1)
    // ────────────────────────────────────────────────

    private void startPlayerTracking() {
        trackingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::collectFromAllWorlds,
                5, 1, TimeUnit.SECONDS
        );
    }

    private void collectFromAllWorlds() {
        Universe universe = Universe.get();
        Collection<World> worlds = universe.getWorlds().values();
        if (worlds.isEmpty()) return;

        collectedStates.clear();

        AtomicInteger remaining = new AtomicInteger(worlds.size());

        for (World world : worlds) {
            world.execute(() -> {
                collectFromWorld(world);

                if (remaining.decrementAndGet() == 0) {
                    HytaleServer.SCHEDULED_EXECUTOR.execute(() -> consolidateAndSendIfNeeded(false));
                }
            });
        }
    }

    private void collectFromWorld(World world) {
        UUID worldUuid = world.getWorldConfig().getUuid();
        Universe universe = Universe.get();

        for (PlayerRef player : universe.getPlayers()) {

            if (player.getWorldUuid() == null ||
                    !player.getWorldUuid().equals(worldUuid)) {
                continue;
            }

            PlayerState state = new PlayerState(
                    player.getUuid().toString(),
                    player.getUsername()
            );

            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                TransformComponent transform = store.getComponent(
                        ref,
                        TransformComponent.getComponentType()
                );

                if (transform != null) {
                    Vector3d pos = transform.getPosition();
                    state.position = new Position(
                            pos.x, pos.y, pos.z,
                            worldUuid.toString()
                    );
                }
            }

            collectedStates.put(state.uuid, state);
        }
    }

    // ────────────────────────────────────────────────
    // Diff + envio (FASE 2)
    // ────────────────────────────────────────────────

    private void consolidateAndSendIfNeeded(boolean force) {
        Map<String, PlayerState> snapshot = new HashMap<>(collectedStates);

        if (!force) {
            boolean hasChanges = snapshot.size() != previousStates.size();

            if (!hasChanges) {
                for (var entry : snapshot.entrySet()) {
                    PlayerState prev = previousStates.get(entry.getKey());
                    if (prev == null || !prev.equals(entry.getValue())) {
                        hasChanges = true;
                        break;
                    }
                }
            }

            if (!hasChanges) return;
        }

        sendPlayerUpdate(snapshot, snapshot.size());
        previousStates.clear();
        previousStates.putAll(snapshot);
        //lastPlayerUpdateTime = System.currentTimeMillis();
    }

    // ────────────────────────────────────────────────
    // Force update timer
    // ────────────────────────────────────────────────

    private void startForcePlayerUpdateTimer() {
        forceUpdateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    //long delta = System.currentTimeMillis() - lastPlayerUpdateTime;
                    //if (delta >= TimeUnit.MINUTES.toMillis(FORCE_UPDATE_INTERVAL_MINUTES)) {
                    consolidateAndSendIfNeeded(true); //really force EVERY INTERVAL_MINUTES
                    //}
                },
                FORCE_UPDATE_INTERVAL_MINUTES,
                FORCE_UPDATE_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    // ────────────────────────────────────────────────
    // HTTP
    // ────────────────────────────────────────────────

    private void sendPlayerUpdate(Map<String, PlayerState> states, int count) {
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                ApiRequest payload = new ApiRequest();
                payload.playerCount = count;
                payload.players = new ArrayList<>(states.values());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                config.get().getApiBaseUrl()
                                        + "/servers/"
                                        + config.get().getServerId()
                                        + "/players"
                        ))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + config.get().getServerToken())
                        .timeout(Duration.ofSeconds(5))
                        .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() >= 400) {
                    LOGGER.atSevere().log(
                            "API error " + response.statusCode() + ": " + response.body()
                    );
                }

            } catch (Exception e) {
                LOGGER.atSevere().log("Failed to send player update", e);
            }
        });
    }

    // ────────────────────────────────────────────────
    // DTOs
    // ────────────────────────────────────────────────

    private static class ApiRequest {
        public int playerCount;
        public List<PlayerState> players;
    }

    private static class PlayerState {
        public String uuid;
        public String name;
        public Position position;

        // TODO Reserved for future voice settings (proximity, volume, etc)
        public Map<String, Object> settings = new HashMap<>();


        public PlayerState(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PlayerState other)) return false;
            if (!name.equals(other.name)) return false;
            if (position == null && other.position == null) return true;
            if (position == null || other.position == null) return false;
            return position.equals(other.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, name, position);
        }
    }

    private static class Position {
        public double x, y, z;
        public String world;

        public Position(double x, double y, double z, String world) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Position p)) return false;
            return Math.abs(x - p.x) < 0.01 &&
                    Math.abs(y - p.y) < 0.01 &&
                    Math.abs(z - p.z) < 0.01 &&
                    world.equals(p.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, world);
        }
    }
}
