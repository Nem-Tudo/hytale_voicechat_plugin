package me.nemtudo.voicechat.listener;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.service.VersionCheckService;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles player join events and welcome messages
 */
public class PlayerJoinListener {

    private static final int WELCOME_MESSAGE_DELAY_SECONDS = 7;
    private static final int VERSION_WARNING_DELAY_SECONDS = 8;

    private final VoiceChat plugin;
    private final VersionCheckService versionCheckService;

    public PlayerJoinListener(VoiceChat plugin, VersionCheckService versionCheckService) {
        this.plugin = plugin;
        this.versionCheckService = versionCheckService;
    }

    public void execute(PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        scheduleWelcomeMessage(playerRef, player);
        scheduleVersionWarning(playerRef, player);
    }

    private void scheduleWelcomeMessage(PlayerRef playerRef, Player player) {
        if (!plugin.config.get().getAnnounceVoiceChatOnJoin()) {
            return;
        }

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            playerRef.sendMessage(
                    Message.raw(plugin.config.get().getAnnounceVoiceChatOnJoinMessage())
                            .bold(true)
                            .color(Color.GREEN)
            );
        }, WELCOME_MESSAGE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void scheduleVersionWarning(PlayerRef playerRef, Player player) {
        if (!shouldShowVersionWarning(player)) {
            return;
        }

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            sendVersionWarningMessages(playerRef);
        }, VERSION_WARNING_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private boolean shouldShowVersionWarning(Player player) {
        return versionCheckService.hasVersionMismatch() && player.hasPermission("nemtudo.voicechat.warns.differentVersion");
    }

    private void sendVersionWarningMessages(PlayerRef playerRef) {
        String currentVersion = plugin.getManifest().getVersion().toString();
        String latestVersion = versionCheckService.getLatestStableVersion();
        String downloadUrl = versionCheckService.getDownloadPluginURL();
        String latestAcceptableVersion = versionCheckService.getLatestAcceptableVersion();

        playerRef.sendMessage(Message.raw("Hey admin! Voice chat is outdated on this server and will stop working soon.").color(Color.RED).bold(true));

        playerRef.sendMessage(Message.raw("Current version: " + currentVersion + " | Latest stable: " + latestVersion).color(Color.YELLOW));

        playerRef.sendMessage(Message.raw("Download here: " + downloadUrl).color(Color.YELLOW).link(downloadUrl));
    }
}