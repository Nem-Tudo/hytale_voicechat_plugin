package me.nemtudo.voicechat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import me.nemtudo.voicechat.utils.GenerateCode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class VoiceChatCommandDev extends AbstractCommand {

    private final VoiceChat plugin;

    // Opção 1: Recebe o plugin no construtor (RECOMENDADO)
    public VoiceChatCommandDev(VoiceChat plugin) {
        super("dev", "Get the DEV voice chat link", false);
        this.plugin = plugin;
        this.addSubCommand(new VoiceChatCommandReload(this.plugin));
        this.addSubCommand(new VoiceChatCommandDownload(this.plugin));
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        // Obtém o token do servidor através do config
        String serverSecretKey = plugin.config.get().getServerSecretKey();
        String serverId = plugin.config.get().getServerId();

        // Gera o código usando UUID do jogador + token do servidor
        String playerCode = GenerateCode.createCode(
                context.sender().getUuid().toString(),
                serverSecretKey
        );

        // Envia a mensagem com o link

        String finalLink = plugin.config.get().getBaseUrl() + "/dev/" + serverId + "/" + playerCode;

        context.sender().sendMessage(Message.raw("Link (click): " + finalLink).link(finalLink).color(Color.green));
        context.sender().sendMessage(Message.raw("SERVER ID: " + serverId));
        context.sender().sendMessage(Message.raw("YOUR CODE: " + playerCode));
        context.sender().sendMessage(Message.raw("DEV VERSION! EXPECT BUGS").color(Color.orange));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

}