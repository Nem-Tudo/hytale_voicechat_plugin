package me.nemtudo.voicechat.websocket.events;

public record VoiceJoinData(
        String playerId,
        String playerName
){}