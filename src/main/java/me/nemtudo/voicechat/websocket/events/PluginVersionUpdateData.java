package me.nemtudo.voicechat.websocket.events;

public record PluginVersionUpdateData(
        boolean needBroadcast,
        String message
) {
}