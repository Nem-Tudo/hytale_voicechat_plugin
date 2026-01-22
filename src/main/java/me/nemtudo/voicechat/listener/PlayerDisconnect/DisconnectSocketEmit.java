package me.nemtudo.voicechat.listener.PlayerDisconnect;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.nemtudo.voicechat.VoiceChat;

/**
 * Handles player connect event and socket emit
 */
public class DisconnectSocketEmit {

    private final VoiceChat plugin;

    public DisconnectSocketEmit(VoiceChat plugin) {
        this.plugin = plugin;
    }

    public void execute(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        plugin.getWebsocketManager().emit("server:player_disconnect", playerRef.getUuid().toString());
    }

}