package me.nemtudo.voicechat.listener.PlayerConnect;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.nemtudo.voicechat.VoiceChat;

/**
 * Handles player connect event and socket emit
 */
public class ConnectSocketEmit {

    private final VoiceChat plugin;

    public ConnectSocketEmit(VoiceChat plugin) {
        this.plugin = plugin;
    }

    public void execute(PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        plugin.getWebsocketManager().emit("server:player_connect", playerRef.getUuid().toString());
    }
}