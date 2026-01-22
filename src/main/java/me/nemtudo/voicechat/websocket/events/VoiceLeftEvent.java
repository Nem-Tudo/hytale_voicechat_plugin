package me.nemtudo.voicechat.websocket.events;

import com.hypixel.hytale.server.core.Message;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.utils.PlaceholderParser;
import me.nemtudo.voicechat.websocket.BaseWebSocketEvent;

import java.awt.*;

public class VoiceLeftEvent extends BaseWebSocketEvent<VoiceLeftData> {

    public VoiceLeftEvent(VoiceChat plugin) {
        super(plugin, VoiceLeftData.class);
    }

    @Override
    protected void onEvent(VoiceLeftData data) {
        if (plugin.config.get().getBroadcastPlayerLeaveVoiceChatEnabled()) {
            String template = plugin.config.get().getBroadcastPlayerLeaveVoiceChatMessage();
            String message = new PlaceholderParser()
                    .add("player.name", data.playerName())
                    .add("player.id", data.playerId())
                    .parse(template);

            plugin.broadcast(Message.raw(message).color(Color.ORANGE));
        }
    }

    @Override
    public String getEventName() {
        return "voice:player_left";
    }
}