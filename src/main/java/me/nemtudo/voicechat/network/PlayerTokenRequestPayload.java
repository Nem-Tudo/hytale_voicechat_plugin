package me.nemtudo.voicechat.network;

import java.util.UUID;

/**
 * Request payload for token
 */
public class PlayerTokenRequestPayload {

    public final String playerUUID;
    public final String username;
    public final Boolean isDevVersion;

    public PlayerTokenRequestPayload(UUID playerUUID, String playerName, Boolean isDevVersion) {
        this.playerUUID = playerUUID.toString();
        this.username = playerName;
        this.isDevVersion = isDevVersion;
    }
}