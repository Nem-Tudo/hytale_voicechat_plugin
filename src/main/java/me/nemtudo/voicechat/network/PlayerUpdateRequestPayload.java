package me.nemtudo.voicechat.network;

import me.nemtudo.voicechat.model.PlayerState;

import java.util.List;

/**
 * Request payload for updating player data to API
 */
public class PlayerUpdateRequestPayload {

    public final int playerCount;
    public final List<PlayerState> players;

    public PlayerUpdateRequestPayload(int playerCount, List<PlayerState> players) {
        this.playerCount = playerCount;
        this.players = players;
    }
}