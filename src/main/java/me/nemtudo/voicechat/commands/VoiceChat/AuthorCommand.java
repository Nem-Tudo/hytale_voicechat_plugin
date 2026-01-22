package me.nemtudo.voicechat.commands.VoiceChat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import me.nemtudo.voicechat.VoiceChat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class AuthorCommand extends AbstractCommand {

    private final VoiceChat plugin;

    public AuthorCommand(VoiceChat plugin) {
        super("author", "View the VoiceChat developer", false);
        this.plugin = plugin;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        Color pink = Color.decode("#FF69B4");
        Color purple = Color.decode("#DA70D6");
        Color blue = Color.decode("#1E90FF");

        context.sender().sendMessage(Message.raw("Omg, you want to see MY info?! Okay").color(Color.GREEN).bold(true));
        context.sender().sendMessage(Message.raw("- Country: Brazil receba pae").color(blue).bold(true));
        context.sender().sendMessage(Message.raw("- Name: Nem Tudo").color(blue).bold(true));
        context.sender().sendMessage(Message.raw("- Hytale Nickname: ozb").color(blue).bold(true));
        context.sender().sendMessage(Message.raw("- Instagram: @_nemtudo_").color(purple).bold(true).link("https://instagram.com/_nemtudo_"));
        context.sender().sendMessage(Message.raw("- Discord: discord.gg/nemtudo").color(Color.BLUE).bold(true).link("https://discord.gg/nemtudo"));
        context.sender().sendMessage(Message.raw("- Twitter/X: @NemTudo_").color(purple).bold(true).link("https://x.com/NemTudo"));
        context.sender().sendMessage(Message.raw("I think nobody is going to use this command lol").color(Color.WHITE).italic(true).link("https://instagram.com/_nemtudo_"));

        return CompletableFuture.completedFuture(null);
    }


    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}