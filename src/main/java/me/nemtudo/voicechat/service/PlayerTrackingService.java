package me.nemtudo.voicechat.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.model.PlayerState;
import me.nemtudo.voicechat.model.Position;
import me.nemtudo.voicechat.network.PlayerUpdateRequestPayload;
import me.nemtudo.voicechat.websocket.WebSocketManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for tracking player positions and states
 */
public class PlayerTrackingService {

    private final HytaleLogger LOGGER;

    private static final long TRACKING_INTERVAL_SECONDS = 1;
    private static final long FORCE_UPDATE_INTERVAL_MINUTES = 3;

    private final VoiceChat plugin;
    private final WebSocketManager wsManager;

    // Thread-safe snapshot of current player states
    private final Map<String, PlayerState> currentStates = new ConcurrentHashMap<>();

    // Last snapshot that was sent to API
    private final Map<String, PlayerState> previousStates = new HashMap<>();

    private ScheduledFuture<?> trackingTask;
    private ScheduledFuture<?> forceUpdateTask;

    public PlayerTrackingService(VoiceChat plugin) {
        this.plugin = plugin;
        this.wsManager = plugin.getWebsocketManager();
        this.LOGGER = plugin.getLogger();
    }

    public void startTracking() {
        startPeriodicTracking();
        startForceUpdateTimer();
    }

    public void shutdown() {
        if (trackingTask != null) {
            trackingTask.cancel(false);
        }
        if (forceUpdateTask != null) {
            forceUpdateTask.cancel(false);
        }
    }

    public void forceUpdate() {
        consolidateAndSend(true);
    }

    private void startPeriodicTracking() {
        trackingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::collectPlayerStatesFromAllWorlds,
                5,
                TRACKING_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void startForceUpdateTimer() {
        forceUpdateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> consolidateAndSend(true),
                FORCE_UPDATE_INTERVAL_MINUTES,
                FORCE_UPDATE_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private void collectPlayerStatesFromAllWorlds() {
        Universe universe = Universe.get();
        Collection<World> worlds = universe.getWorlds().values();

        if (worlds.isEmpty()) {
            return;
        }

        currentStates.clear();
        AtomicInteger remainingWorlds = new AtomicInteger(worlds.size());

        for (World world : worlds) {
            world.execute(() -> {
                collectPlayerStatesFromWorld(world);

                if (remainingWorlds.decrementAndGet() == 0) {
                    HytaleServer.SCHEDULED_EXECUTOR.execute(() -> consolidateAndSend(false));
                }
            });
        }
    }

    private void collectPlayerStatesFromWorld(World world) {
        UUID worldUuid = world.getWorldConfig().getUuid();
        Universe universe = Universe.get();

        for (PlayerRef playerRef : universe.getPlayers()) {
            if (!isPlayerInWorld(playerRef, worldUuid)) {
                continue;
            }

            PlayerState state = createPlayerState(playerRef, worldUuid);
            if (state != null) {
                currentStates.put(state.getUuid(), state);
            }
        }
    }

    private boolean isPlayerInWorld(PlayerRef playerRef, UUID worldUuid) {
        return playerRef.getWorldUuid() != null &&
                playerRef.getWorldUuid().equals(worldUuid);
    }

    private PlayerState createPlayerState(PlayerRef playerRef, UUID worldUuid) {
        PlayerState state = new PlayerState(
                playerRef.getUuid().toString(),
                playerRef.getUsername()
        );

        Position position = extractPlayerPosition(playerRef, worldUuid);
        if (position != null) {
            state.setPosition(position);
        }

        return state;
    }

    private Position extractPlayerPosition(PlayerRef playerRef, UUID worldUuid) {
        Ref<EntityStore> ref = playerRef.getReference();

        if (ref == null || !ref.isValid()) {
            return null;
        }

        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(
                ref,
                TransformComponent.getComponentType()
        );

        if (transform == null) {
            return null;
        }

        Vector3d pos = transform.getPosition();
        return new Position(pos.x, pos.y, pos.z, worldUuid.toString());
    }

    private void consolidateAndSend(boolean force) {
        Map<String, PlayerState> snapshot = new HashMap<>(currentStates);

        if (!force && !hasStateChanges(snapshot)) {
            return;
        }

        sendPlayerUpdate(snapshot);
        updatePreviousStates(snapshot);
    }

    private boolean hasStateChanges(Map<String, PlayerState> snapshot) {
        if (snapshot.size() != previousStates.size()) {
            return true;
        }

        for (Map.Entry<String, PlayerState> entry : snapshot.entrySet()) {
            PlayerState previousState = previousStates.get(entry.getKey());
            if (previousState == null || !previousState.equals(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    private void updatePreviousStates(Map<String, PlayerState> snapshot) {
        previousStates.clear();
        previousStates.putAll(snapshot);
    }

    private void sendPlayerUpdate(Map<String, PlayerState> states) {
        PlayerUpdateRequestPayload requestPayload = new PlayerUpdateRequestPayload(
                states.size(),
                new ArrayList<>(states.values())
        );

        wsManager.emit("server:players", plugin.gson.toJson(requestPayload));
    }
}