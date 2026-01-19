package me.nemtudo.voicechat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class VoiceChatCommandReload extends AbstractCommand {

    private final VoiceChat plugin;

    public VoiceChatCommandReload(VoiceChat plugin) {
        super("reload", "Reload the VoiceChat plugin", false);
        this.plugin = plugin;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        context.sender().sendMessage(Message.raw("Reloading VoiceChat...").color(Color.orange).italic(true));

        String reloadCommand = "plugins reload " + plugin.getName();

        CommandManager.get().handleCommand(ConsoleSender.INSTANCE, reloadCommand);

        context.sender().sendMessage(Message.raw("Reloaded!").color(Color.green).bold(true));

        return CompletableFuture.completedFuture(null);
    }
}