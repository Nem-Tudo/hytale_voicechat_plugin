package me.nemtudo.voicechat.commands.VoiceChat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import me.nemtudo.voicechat.VoiceChat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class DownloadCommand extends AbstractCommand {

    private final VoiceChat plugin;

    public DownloadCommand(VoiceChat plugin) {
        super("download", "Get the plugin download link", false);
        this.plugin = plugin;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String downloadPluginURL = plugin.getVersionCheckService().getDownloadPluginURL();
        context.sender().sendMessage(Message.raw(downloadPluginURL).link(downloadPluginURL).color(Color.green).bold(true));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}