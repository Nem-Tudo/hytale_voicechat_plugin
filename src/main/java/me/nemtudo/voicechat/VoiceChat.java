package me.nemtudo.voicechat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
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

    private long lastPlayerUpdateTime = 0;

    private ScheduledFuture<?> trackingTask;
    private ScheduledFuture<?> forceUpdateTask;

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

        startPlayerTracking();
        startForcePlayerUpdateTimer();

        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerJoin);
    }

    @Override
    protected void shutdown() {
        if (trackingTask != null) trackingTask.cancel(false);
        if (forceUpdateTask != null) forceUpdateTask.cancel(false);
        LOGGER.atInfo().log("VoiceChat disabled");
    }

    // ────────────────────────────────────────────────
    // Player join message
    // ────────────────────────────────────────────────

    private void onPlayerJoin(PlayerConnectEvent event) {
        if (!config.get().getAnnounceVoiceChatOnJoin()) return;

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            event.getPlayerRef().sendMessage(
                    Message.raw("This server uses VoiceChat! Use /voicechat to talk via voice.")
                            .bold(true)
                            .color(Color.GREEN)
            );
        }, 7, TimeUnit.SECONDS);
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
                    HytaleServer.SCHEDULED_EXECUTOR.execute(this::consolidateAndSendIfNeeded);
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

    private void consolidateAndSendIfNeeded() {
        Map<String, PlayerState> snapshot = new HashMap<>(collectedStates);

        boolean hasChanges = snapshot.size() != previousStates.size();

        if (!hasChanges) {
            for (Map.Entry<String, PlayerState> entry : snapshot.entrySet()) {
                PlayerState prev = previousStates.get(entry.getKey());
                if (prev == null || !prev.equals(entry.getValue())) {
                    hasChanges = true;
                    break;
                }
            }
        }

        if (!hasChanges) return;

        sendPlayerUpdate(snapshot, snapshot.size());

        previousStates.clear();
        previousStates.putAll(snapshot);
        lastPlayerUpdateTime = System.currentTimeMillis();
    }

    // ────────────────────────────────────────────────
    // Force update timer
    // ────────────────────────────────────────────────

    private void startForcePlayerUpdateTimer() {
        forceUpdateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    long delta = System.currentTimeMillis() - lastPlayerUpdateTime;
                    if (delta >= TimeUnit.MINUTES.toMillis(FORCE_UPDATE_INTERVAL_MINUTES)) {
                        consolidateAndSendIfNeeded();
                    }
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
        public Map<String, Object> settings = new HashMap<>();
        public Position position;

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
