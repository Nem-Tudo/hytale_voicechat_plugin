package me.nemtudo.voicechat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class VoiceChatCommandDownload extends AbstractCommand {

    private final VoiceChat plugin;

    public VoiceChatCommandDownload(VoiceChat plugin) {
        super("download", "Get the plugin download link", false);
        this.plugin = plugin;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String downloadPluginURL = plugin.downloadPluginURL;
        context.sender().sendMessage(Message.raw(downloadPluginURL).link(downloadPluginURL).color(Color.green).bold(true));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}