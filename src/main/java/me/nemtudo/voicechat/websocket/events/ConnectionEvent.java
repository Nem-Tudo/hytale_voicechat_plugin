package me.nemtudo.voicechat.websocket.events;

import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.utils.ConsoleColors;
import me.nemtudo.voicechat.websocket.BaseWebSocketEvent;

public class ConnectionEvent extends BaseWebSocketEvent<ConnectionData> {

    volatile boolean messageSent = false;

    public ConnectionEvent(VoiceChat plugin) {
        super(plugin, ConnectionData.class);
    }

    @Override
    protected void onEvent(ConnectionData data) {
        if (data.user() != null) {
            if (!messageSent) {
                messageSent = true;
                plugin.getLogger().atInfo().log(ConsoleColors.success("Server connected: " + data.user().username() + " (ID: " + data.user().id() + ")"));
            }
            plugin.getPlayerTrackingService().forceUpdate();
        }
    }

    @Override
    public String getEventName() {
        return "connected";
    }
}