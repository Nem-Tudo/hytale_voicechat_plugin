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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * VoiceChat Plugin - Sincroniza posições de jogadores com API externa
 */
public class VoiceChat extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long FORCE_UPDATE_INTERVAL_MINUTES = 3;

    public final Config<VoiceChatConfig> config;
    private final Gson gson = new GsonBuilder().create();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Armazena o estado anterior dos jogadores
    private final Map<String, PlayerState> previousStates = new HashMap<>();

    // Armazena os códigos de convite dos jogadores (UUID -> Código)
    private final Map<String, String> playerInviteCodes = new HashMap<>();

    // Última vez que enviamos atualização de jogadores
    private long lastPlayerUpdateTime = 0;
    private ScheduledFuture<Void> trackingTask;
    private ScheduledFuture<?> forceUpdateTask;

    public VoiceChat(@Nonnull JavaPluginInit init) {
        super(init);

        this.config = this.withConfig("VoiceChat", VoiceChatConfig.CODEC);

        LOGGER.atInfo().log("VoiceChat has been loaded! By ozb (@nemtudo)");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Loaded " + this.getName());

        config.save();

        LOGGER.atInfo().log("Server ID: " + config.get().getServerId());
        LOGGER.atInfo().log("API Base URL: " + config.get().getApiBaseUrl());
        LOGGER.atInfo().log("Base URL: " + config.get().getBaseUrl());

        getCommandRegistry().registerCommand(new VoiceChatCommand(this));

        startPlayerTracking();
        startForcePlayerUpdateTimer();

        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerJoin);
    }

    @Override
    protected void shutdown() {
        if (trackingTask != null && !trackingTask.isDone()) {
            trackingTask.cancel(false);
        }
        if (forceUpdateTask != null && !forceUpdateTask.isDone()) {
            forceUpdateTask.cancel(false);
        }
        LOGGER.atInfo().log("VoiceChat disabled!");
    }


    private void onPlayerJoin(PlayerConnectEvent event) {
        if (this.config.get().getAnnounceVoiceChatOnJoin()) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                event.getPlayerRef().sendMessage(
                        Message.raw("This server uses VoiceChat! Use the \"/voicechat\" command to talk to other players via voice!")
                                .bold(true)
                                .color(Color.green)
                );
            }, 7, TimeUnit.SECONDS);
        }
    }

    public String getOrCreateInviteCode(String playerUuid) {
        return playerInviteCodes.computeIfAbsent(playerUuid, uuid -> generateInviteCode());
    }

    private String generateInviteCode() {
        Random random = new Random();
        String code;
        do {
            code = String.format("%05d", random.nextInt(100000));
        } while (playerInviteCodes.containsValue(code));
        return code;
    }

    private void startPlayerTracking() {
        trackingTask = (ScheduledFuture<Void>) HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    try {
                        Universe universe = Universe.get();
                        Map<String, World> worlds = universe.getWorlds();
                        if (!worlds.isEmpty()) {
                            World world = worlds.values().iterator().next();
                            world.execute(this::checkAndSendPlayerUpdates);
                        }
                    } catch (Exception e) {
                        LOGGER.atSevere().log("Erro ao executar checkAndSendPlayerUpdates", e);
                    }
                },
                5, 1, TimeUnit.SECONDS
        );

        getTaskRegistry().registerTask(trackingTask);
        LOGGER.atInfo().log("Player tracking started");
    }

    private void startForcePlayerUpdateTimer() {
        forceUpdateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::forcePlayerUpdateIfNeeded,
                FORCE_UPDATE_INTERVAL_MINUTES,
                FORCE_UPDATE_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );

        LOGGER.atInfo().log("Force player update timer started (every " + FORCE_UPDATE_INTERVAL_MINUTES + " minutes if no recent update)");
    }

    private void checkAndSendPlayerUpdates() {
        Universe universe = Universe.get();
        List<PlayerRef> players = universe.getPlayers();

        Set<String> currentPlayerUuids = new HashSet<>();
        Map<String, PlayerState> currentStates = new HashMap<>();
        boolean hasChanges = false;

        for (PlayerRef player : players) {
            String uuid = player.getUuid().toString();
            currentPlayerUuids.add(uuid);

            String username = player.getUsername();
            String inviteCode = getOrCreateInviteCode(uuid);
            PlayerState currentState = new PlayerState(uuid, username, inviteCode);

            Ref<EntityStore> entityRef = player.getReference();
            if (entityRef != null && entityRef.isValid()) {
                Store<EntityStore> store = entityRef.getStore();
                TransformComponent transform = store.getComponent(
                        entityRef,
                        TransformComponent.getComponentType()
                );

                if (transform != null) {
                    Vector3d position = transform.getPosition();
                    String worldName = player.getWorldUuid() != null ?
                            player.getWorldUuid().toString() : "No World";

                    currentState.position = new Position(position.x, position.y, position.z, worldName);
                }
            }

            currentStates.put(uuid, currentState);

            PlayerState previousState = previousStates.get(uuid);
            if (previousState == null || !previousState.equals(currentState)) {
                hasChanges = true;
            }
        }

        Set<String> playersLeft = new HashSet<>(previousStates.keySet());
        playersLeft.removeAll(currentPlayerUuids);

        if (!playersLeft.isEmpty()) {
            hasChanges = true;
            for (String uuid : playersLeft) {
                playerInviteCodes.remove(uuid);
            }
        }

        if (hasChanges) {
            sendPlayerUpdate(currentStates, players.size());
            previousStates.clear();
            previousStates.putAll(currentStates);
            lastPlayerUpdateTime = System.currentTimeMillis();
            // LOGGER.atInfo().log("Player states updated and sent to API");
        }
    }

    /**
     * Verifica se já passou o tempo sem envio → força envio completo dos players
     */
    private void forcePlayerUpdateIfNeeded() {
        long timeSinceLast = System.currentTimeMillis() - lastPlayerUpdateTime;
        long threshold = TimeUnit.MINUTES.toMillis(FORCE_UPDATE_INTERVAL_MINUTES);

        if (timeSinceLast >= threshold || lastPlayerUpdateTime == 0) {
            //LOGGER.atInfo().log("Forçando envio completo de players (timeout de " + FORCE_UPDATE_INTERVAL_MINUTES + " min sem atualização)");
            forceSendAllPlayers();
        }
    }

    /**
     * Força o envio da lista completa de jogadores atuais
     */
    private void forceSendAllPlayers() {
        Universe universe = Universe.get();
        List<PlayerRef> players = universe.getPlayers();

        Map<String, PlayerState> currentStates = new HashMap<>();

        for (PlayerRef player : players) {
            String uuid = player.getUuid().toString();
            String username = player.getUsername();
            String inviteCode = getOrCreateInviteCode(uuid);
            PlayerState state = new PlayerState(uuid, username, inviteCode);

            Ref<EntityStore> entityRef = player.getReference();
            if (entityRef != null && entityRef.isValid()) {
                Store<EntityStore> store = entityRef.getStore();
                TransformComponent transform = store.getComponent(
                        entityRef,
                        TransformComponent.getComponentType()
                );

                if (transform != null) {
                    Vector3d pos = transform.getPosition();
                    String worldName = player.getWorldUuid() != null ?
                            player.getWorldUuid().toString() : "No World";
                    state.position = new Position(pos.x, pos.y, pos.z, worldName);
                }
            }

            currentStates.put(uuid, state);
        }

        sendPlayerUpdate(currentStates, players.size());

        // Atualiza o estado anterior para evitar envio duplicado logo em seguida
        previousStates.clear();
        previousStates.putAll(currentStates);
        lastPlayerUpdateTime = System.currentTimeMillis();
    }

    private void sendPlayerUpdate(Map<String, PlayerState> playerStates, int playerCount) {
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                String url = config.get().getApiBaseUrl() + "/servers/" + config.get().getServerId() + "/players";

                ApiRequest apiRequest = new ApiRequest();
                apiRequest.playerCount = playerCount;
                apiRequest.players = new ArrayList<>(playerStates.values());

                String jsonBody = gson.toJson(apiRequest);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + config.get().getServerToken())
                        .timeout(Duration.ofSeconds(5))
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    // LOGGER.atInfo().log("Successfully sent player data (forced or changed)");
                } else {
                    LOGGER.atSevere().log("API error " + status + " - " + response.body());
                    if (status == 401) {
                        LOGGER.atSevere().log("You need to setup a valid ServerToken in config file");

                        if (trackingTask != null && !trackingTask.isDone()) {
                            trackingTask.cancel(false);
                        }
                        if (forceUpdateTask != null && !forceUpdateTask.isDone()) {
                            forceUpdateTask.cancel(false);
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.atSevere().log("Failed to send player update", e);
            }
        });
    }

    // ────────────────────────────────────────────────
    //  Classes internas mantidas iguais
    // ────────────────────────────────────────────────

    private static class ApiRequest {
        public int playerCount;
        public List<PlayerState> players;
    }

    private static class PlayerState {
        public String uuid;
        public String name;
        public String inviteCode;
        public Map<String, Object> settings = new HashMap<>();
        public Position position;

        public PlayerState(String uuid, String name, String inviteCode) {
            this.uuid = uuid;
            this.name = name;
            this.inviteCode = inviteCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PlayerState)) return false;
            PlayerState other = (PlayerState) obj;
            if (!this.name.equals(other.name)) return false;
            if (!this.inviteCode.equals(other.inviteCode)) return false;
            if (this.position == null && other.position == null) return true;
            if (this.position == null || other.position == null) return false;
            return this.position.equals(other.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, name, inviteCode, position);
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
        public boolean equals(Object obj) {
            if (!(obj instanceof Position)) return false;
            Position other = (Position) obj;
            return Math.abs(this.x - other.x) < 0.01 &&
                    Math.abs(this.y - other.y) < 0.01 &&
                    Math.abs(this.z - other.z) < 0.01 &&
                    this.world.equals(other.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, world);
        }
    }
}