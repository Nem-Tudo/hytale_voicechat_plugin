package me.nemtudo.voicechat.commands.VoiceChat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import me.nemtudo.voicechat.VoiceChat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class DevCommand extends AbstractCommand {

    private final VoiceChat plugin;

    public DevCommand(VoiceChat plugin) {
        super("dev", "Get the voice chat link for a development version. Expect bugs!", false);
        this.plugin = plugin;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("[Voice Chat] Only players can execute this command"));
            return CompletableFuture.completedFuture(null);
        }

        String finalURL = plugin.config.get().getBaseUrl() + "/dev";

        context.sender().sendMessage(Message.raw("[Voice Chat] Click here to Voice Chat:").link(finalURL).color(Color.GREEN).bold(true));
        context.sender().sendMessage(Message.raw(finalURL).link(finalURL).color(Color.GREEN));
        context.sender().sendMessage(Message.raw("[Voice Chat] This is a DEV version!!! Expect bugs!!! Use \"/voicechat\" for the stable version.").color(Color.ORANGE).bold(true));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}