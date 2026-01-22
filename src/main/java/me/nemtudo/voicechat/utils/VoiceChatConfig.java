package me.nemtudo.voicechat.utils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Configuração do plugin VoiceChat
 */
public class VoiceChatConfig {

    public static final BuilderCodec<VoiceChatConfig> CODEC =
            BuilderCodec.builder(VoiceChatConfig.class, VoiceChatConfig::new)
                    // Comment
                    .append(new KeyedCodec<String>("_Comment", Codec.STRING),
                            (config, value, info) -> config._comment = value,
                            (config, info) -> config._comment)
                    .add()
                    // Server ID - Identificador único deste servidor
                    .append(new KeyedCodec<String>("ServerId", Codec.STRING),
                            (config, value, info) -> config.serverId = value,
                            (config, info) -> config.serverId)
                    .add()

                    // Server Token - Token de autenticação
                    .append(new KeyedCodec<String>("ServerToken", Codec.STRING),
                            (config, value, info) -> config.serverToken = value,
                            (config, info) -> config.serverToken)
                    .add()

                    // If plugin will send a message to players that join
                    .append(new KeyedCodec<Boolean>("AnnounceVoiceChatOnJoin", Codec.BOOLEAN),
                            (config, value, info) -> config.announceVoiceChatOnJoin = value,
                            (config, info) -> config.announceVoiceChatOnJoin)
                    .add()

                    // message to players that join
                    .append(new KeyedCodec<String>("AnnounceVoiceChatOnJoinMessage", Codec.STRING),
                            (config, value, info) -> config.announceVoiceChatOnJoinMessage = value,
                            (config, info) -> config.announceVoiceChatOnJoinMessage)
                    .add()

                    // If plugin will broadcast when a player enters voice chat
                    .append(new KeyedCodec<Boolean>("BroadcastPlayerEnterVoiceChatEnabled", Codec.BOOLEAN),
                            (config, value, info) -> config.broadcastPlayerEnterVoiceChatEnabled = value,
                            (config, info) -> config.broadcastPlayerEnterVoiceChatEnabled)
                    .add()

                    // Message broadcasted when a player enters voice chat
                    .append(new KeyedCodec<String>("BroadcastPlayerEnterVoiceChatMessage", Codec.STRING),
                            (config, value, info) -> config.broadcastPlayerEnterVoiceChatMessage = value,
                            (config, info) -> config.broadcastPlayerEnterVoiceChatMessage)
                    .add()

                    // If plugin will broadcast when a player leaves voice chat
                    .append(new KeyedCodec<Boolean>("BroadcastPlayerLeaveVoiceChatEnabled", Codec.BOOLEAN),
                            (config, value, info) -> config.broadcastPlayerLeaveVoiceChatEnabled = value,
                            (config, info) -> config.broadcastPlayerLeaveVoiceChatEnabled)
                    .add()

                    // Message broadcasted when a player leaves voice chat
                    .append(new KeyedCodec<String>("BroadcastPlayerLeaveVoiceChatMessage", Codec.STRING),
                            (config, value, info) -> config.broadcastPlayerLeaveVoiceChatMessage = value,
                            (config, info) -> config.broadcastPlayerLeaveVoiceChatMessage)
                    .add()

                    // If plugin will log websocket information to console
                    .append(new KeyedCodec<Boolean>("LogWebsocketInfoInConsole", Codec.BOOLEAN),
                            (config, value, info) -> config.logWebsocketInfoInConsole = value,
                            (config, info) -> config.logWebsocketInfoInConsole)
                    .add()

                    // Base URL - URL base da voicechat
                    .append(new KeyedCodec<String>("BaseUrl", Codec.STRING),
                            (config, value, info) -> config.baseUrl = value,
                            (config, info) -> config.baseUrl)
                    .add()

                    // API Base URL - URL base da API
                    .append(new KeyedCodec<String>("ApiBaseUrl", Codec.STRING),
                            (config, value, info) -> config.apiBaseUrl = value,
                            (config, info) -> config.apiBaseUrl)
                    .add()

                    // Websocket Base URL - URL base do Websocket
                    .append(new KeyedCodec<String>("WebsocketBaseUrl", Codec.STRING),
                            (config, value, info) -> config.websocketBaseUrl = value,
                            (config, info) -> config.websocketBaseUrl)
                    .add()
                    .build();

    // Valores padrão
    private String _comment = "Get ServerId and ServerToken here: https://hytaleweb.nemtudo.me/addserver";
    private String serverId = "ABCDEF";
    private String serverToken = "secret_server_token";

    private boolean announceVoiceChatOnJoin = true;
    private String announceVoiceChatOnJoinMessage = "This server uses VoiceChat! Use /voicechat to talk via voice.";

    private boolean broadcastPlayerEnterVoiceChatEnabled = true;
    private String broadcastPlayerEnterVoiceChatMessage = "[VoiceChat] {player.name} entered the voice chat.";

    private boolean broadcastPlayerLeaveVoiceChatEnabled = false;
    private String broadcastPlayerLeaveVoiceChatMessage = "[VoiceChat] {player.name} left the voice chat.";

    private boolean logWebsocketInfoInConsole = false;

    private String baseUrl = "https://voice.nemtudo.me";
    private String apiBaseUrl = "https://apihytale.nemtudo.me";
    private String websocketBaseUrl = "wss://apihytale.nemtudo.me";

    public VoiceChatConfig() {
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerToken() {
        return serverToken;
    }

    public boolean getAnnounceVoiceChatOnJoin() {
        return announceVoiceChatOnJoin;
    }

    public String getAnnounceVoiceChatOnJoinMessage() {
        return announceVoiceChatOnJoinMessage;
    }

    public boolean getBroadcastPlayerEnterVoiceChatEnabled() {
        return broadcastPlayerEnterVoiceChatEnabled;
    }

    public String getBroadcastPlayerEnterVoiceChatMessage() {
        return broadcastPlayerEnterVoiceChatMessage;
    }

    public boolean getBroadcastPlayerLeaveVoiceChatEnabled() {
        return broadcastPlayerLeaveVoiceChatEnabled;
    }

    public String getBroadcastPlayerLeaveVoiceChatMessage() {
        return broadcastPlayerLeaveVoiceChatMessage;
    }

    public boolean getLogWebsocketInfoInConsole() {
        return logWebsocketInfoInConsole;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getWebsocketBaseUrl() {
        return websocketBaseUrl;
    }
}