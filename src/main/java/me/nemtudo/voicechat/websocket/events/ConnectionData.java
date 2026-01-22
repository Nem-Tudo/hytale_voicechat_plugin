package me.nemtudo.voicechat.websocket.events;

public record ConnectionData(
        boolean authenticated,
        String clientType,
        int socketApiVersion,
        long time,
        User user
) {
    public record User(String id, String username) {}
}