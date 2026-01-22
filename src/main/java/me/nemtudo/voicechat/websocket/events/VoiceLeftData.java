package me.nemtudo.voicechat.websocket.events;

public record VoiceLeftData(
        String playerId,
        String playerName
){}