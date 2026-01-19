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

                    // Server Secret Key - For code generations
                    .append(new KeyedCodec<String>("ServerSecretKey", Codec.STRING),
                            (config, value, info) -> config.serverSecretKey = value,
                            (config, info) -> config.serverSecretKey)
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

                    // API Base URL - URL base da API
                    .append(new KeyedCodec<String>("ApiBaseUrl", Codec.STRING),
                            (config, value, info) -> config.apiBaseUrl = value,
                            (config, info) -> config.apiBaseUrl)
                    .add()
                    // Base URL - URL base da voicechat
                    .append(new KeyedCodec<String>("BaseUrl", Codec.STRING),
                            (config, value, info) -> config.baseUrl = value,
                            (config, info) -> config.baseUrl)
                    .add()
                    .build();

    // Valores padrão
    private String _comment = "Get your credentials here: https://hytaleweb.nemtudo.me/addserver";
    private String serverId = "ABCDEF";
    private String serverSecretKey = "ABCDE";
    private String serverToken = "secret_server_token";
    private boolean announceVoiceChatOnJoin = true;
    private String apiBaseUrl = "https://apihytale.nemtudo.me";
    private String baseUrl = "https://voice.nemtudo.me";

    public VoiceChatConfig() {
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerSecretKey() {
        return serverSecretKey;
    }

    public String getServerToken() {
        return serverToken;
    }

    public boolean getAnnounceVoiceChatOnJoin() {
        return announceVoiceChatOnJoin;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}