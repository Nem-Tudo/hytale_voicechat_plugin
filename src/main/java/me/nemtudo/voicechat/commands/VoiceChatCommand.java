package me.nemtudo.voicechat.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.commands.VoiceChat.*;
import me.nemtudo.voicechat.utils.ApiRequestHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class VoiceChatCommand extends AbstractCommand {

    private final VoiceChat plugin;

    public VoiceChatCommand(VoiceChat plugin) {
        super("voicechat", "Get the voice chat link", false);
        this.plugin = plugin;
        this.addSubCommand(new ConnectCommand(this.plugin));
        this.addSubCommand(new ReloadCommand(this.plugin));
        this.addSubCommand(new DownloadCommand(this.plugin));
        this.addSubCommand(new DevCommand(this.plugin));
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("[Voice Chat] Only players can execute this command"));
            return CompletableFuture.completedFuture(null);
        }

        String finalURL = plugin.config.get().getBaseUrl();

        context.sender().sendMessage(Message.raw("[Voice Chat] Click here to Voice Chat:").link(finalURL).color(Color.GREEN).bold(true));
        context.sender().sendMessage(Message.raw(finalURL).link(finalURL).color(Color.GREEN));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

}