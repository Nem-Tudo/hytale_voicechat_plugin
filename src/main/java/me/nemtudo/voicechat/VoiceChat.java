package me.nemtudo.voicechat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.Config;
import me.nemtudo.voicechat.commands.VoiceChatCommand;
import me.nemtudo.voicechat.listener.PlayerConnect.ConnectSocketEmit;
import me.nemtudo.voicechat.listener.PlayerDisconnect.DisconnectSocketEmit;
import me.nemtudo.voicechat.service.PlayerTrackingService;
import me.nemtudo.voicechat.service.VersionCheckService;
import me.nemtudo.voicechat.listener.PlayerConnect.WelcomeMessages;
import me.nemtudo.voicechat.utils.ApiRequestHelper;
import me.nemtudo.voicechat.utils.VoiceChatConfig;
import me.nemtudo.voicechat.websocket.WebSocketManager;

import javax.annotation.Nonnull;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * VoiceChat Plugin
 * Sincroniza jogadores (posição / mundo) com API externa
 * Otimizado para múltiplos mundos e 500+ players
 */
public class VoiceChat extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Configuration
    public final Config<VoiceChatConfig> config;

    // HTTP utilities
    public final Gson gson;
    public final HttpClient httpClient;
    public final String apiQuerySuffix;

    // WebSocket
    WebSocketManager wsManager = new WebSocketManager(this);

    // Services
    private ApiRequestHelper apiRequestHelper;
    private PlayerTrackingService playerTrackingService;
    private VersionCheckService versionCheckService;

    public VoiceChat(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = this.withConfig("VoiceChat", VoiceChatConfig.CODEC);
        this.gson = new GsonBuilder().create();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.apiQuerySuffix = "pluginVersion=" + getManifest().getVersion().toString() + "&pluginName=" + getName();

        LOGGER.atInfo().log("VoiceChat loaded! By ozb (@nemtudo)");
    }

    @Override
    protected void setup() {
        config.save();

        initializeWebsocket();
        initializeServices();
        registerCommands();
        registerListeners();
        scheduleInitialTasks();

        logConfiguration();
    }

    @Override
    protected void shutdown() {
        if (playerTrackingService != null) {
            playerTrackingService.shutdown();
        }
        wsManager.shutdown();
        LOGGER.atInfo().log("VoiceChat disabled");
    }

    private void initializeWebsocket() {
        wsManager.connect(new HashMap<>());
    }

    private void initializeServices() {
        this.apiRequestHelper = new ApiRequestHelper(this);
        this.playerTrackingService = new PlayerTrackingService(this);
        this.versionCheckService = new VersionCheckService(this);
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new VoiceChatCommand(this));
    }

    private void registerListeners() {
        getEventRegistry().register(PlayerConnectEvent.class, new WelcomeMessages(this, versionCheckService)::execute);
        getEventRegistry().register(PlayerConnectEvent.class, new ConnectSocketEmit(this)::execute);
        getEventRegistry().register(PlayerDisconnectEvent.class, new DisconnectSocketEmit(this)::execute);
    }

    private void scheduleInitialTasks() {
        versionCheckService.checkPluginVersion();
        playerTrackingService.startTracking();

        // Force initial update after 2 seconds
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> playerTrackingService.forceUpdate(),
                2,
                TimeUnit.SECONDS
        );
    }

    private void logConfiguration() {
        LOGGER.atInfo().log("Server ID: " + config.get().getServerId());
        LOGGER.atInfo().log("API Base URL: " + config.get().getApiBaseUrl());
    }

    // GETTERS

    public void broadcast(Message message) {
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            playerRef.sendMessage(message);
        }
    }

    public ApiRequestHelper getApiRequestHelper() {
        return apiRequestHelper;
    }

    public PlayerTrackingService getPlayerTrackingService() {
        return playerTrackingService;
    }

    public VersionCheckService getVersionCheckService() {
        return versionCheckService;
    }

    public WebSocketManager getWebsocketManager() {
        return wsManager;
    }

}