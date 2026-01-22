package me.nemtudo.voicechat.websocket;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.websocket.events.*;

public class WebSocketEventRegistry {

    private final VoiceChat plugin;
    private final Socket socket;

    public WebSocketEventRegistry(VoiceChat plugin, Socket socket) {
        this.plugin = plugin;
        this.socket = socket;
    }

    public void registerAllEvents() {
        registerEvent(new ConnectionEvent(plugin));
        registerEvent(new VoiceJoinEvent(plugin));
        registerEvent(new VoiceLeftEvent(plugin));
        registerEvent(new PluginVersionUpdateEvent(plugin));

        plugin.getLogger().atInfo().log("WebSocket events registered successfully");
    }

    private void registerEvent(BaseWebSocketEvent<?> event) {
        String eventName = event.getEventName();
        Boolean isOnce = event.isOnce();

        Emitter.Listener listener = args -> {
            if (args != null && args.length > 0) {
                event.execute(args[0]);
            } else {
                plugin.getLogger().atWarning().log("Event '" + eventName + "' received without data");
            }
        };

        if (isOnce != null && isOnce) {
            socket.once(eventName, listener);
            plugin.getLogger().atInfo().log("Registered event (once): " + eventName);
        } else {
            socket.on(eventName, listener);
            plugin.getLogger().atInfo().log("Registered event: " + eventName);
        }
    }
}