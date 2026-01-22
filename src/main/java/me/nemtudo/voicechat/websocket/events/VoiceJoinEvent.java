package me.nemtudo.voicechat.websocket.events;

import com.hypixel.hytale.server.core.Message;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.utils.PlaceholderParser;
import me.nemtudo.voicechat.websocket.BaseWebSocketEvent;

import java.awt.*;

public class VoiceJoinEvent extends BaseWebSocketEvent<VoiceJoinData> {

    public VoiceJoinEvent(VoiceChat plugin) {
        super(plugin, VoiceJoinData.class);
    }

    @Override
    protected void onEvent(VoiceJoinData data) {
        if (plugin.config.get().getBroadcastPlayerEnterVoiceChatEnabled()) {
            String template = plugin.config.get().getBroadcastPlayerEnterVoiceChatMessage();
            String message = new PlaceholderParser()
                    .add("player.name", data.playerName())
                    .add("player.id", data.playerId())
                    .parse(template);

            plugin.broadcast(Message.raw(message).color(Color.GREEN));
        }
    }

    @Override
    public String getEventName() {
        return "voice:player_joined";
    }
}