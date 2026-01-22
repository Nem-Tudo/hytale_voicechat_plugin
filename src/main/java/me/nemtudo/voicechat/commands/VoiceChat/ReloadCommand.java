package me.nemtudo.voicechat.commands.VoiceChat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import me.nemtudo.voicechat.VoiceChat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class ReloadCommand extends AbstractCommand {

    private final VoiceChat plugin;

    public ReloadCommand(VoiceChat plugin) {
        super("reload", "Reload the VoiceChat plugin", false);
        this.plugin = plugin;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        context.sender().sendMessage(Message.raw("Reloading VoiceChat...").color(Color.orange).italic(true));

        String reloadCommand = "plugins reload " + plugin.getName();

        return CommandManager.get().handleCommand(ConsoleSender.INSTANCE, reloadCommand)
                .thenRun(() -> {
                    context.sender().sendMessage(Message.raw("Reloaded!").color(Color.green).bold(true));
                })
                .exceptionally(throwable -> {
                    context.sender().sendMessage(Message.raw("Failed to reload: " + throwable.getMessage())
                            .color(Color.red));
                    return null;
                });
    }
}