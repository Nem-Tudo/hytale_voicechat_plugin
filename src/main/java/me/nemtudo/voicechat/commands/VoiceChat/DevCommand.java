package me.nemtudo.voicechat.commands.VoiceChat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.commands.VoiceChat.Connect.DevConnectCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class DevCommand extends AbstractCommand {

    private final VoiceChat plugin;
    DevConnectCommand devConnectCommand;

    public DevCommand(VoiceChat plugin) {
        super("dev", "Get the voice chat link for a development version. Expect bugs!", false);
        this.plugin = plugin;
        this.devConnectCommand = new DevConnectCommand(plugin);
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("[Voice Chat] Only players can execute this command"));
            return CompletableFuture.completedFuture(null);
        }

        plugin.getApiRequestHelper().request("GET", "/players/exists/" + context.sender().getUuid().toString(), null)
                .whenComplete((response, error) -> {
                    if (error != null) {
                        sendDefaultVoiceLink(context.sender());
                        return;
                    }

                    if (response.statusCode() != 200) {
                        sendDefaultVoiceLink(context.sender());
                        return;
                    }

                    Boolean playerExists = plugin.gson.fromJson(response.body(), Boolean.class);
                    if (playerExists) {
                        sendDefaultVoiceLink(context.sender());
                        return;
                    }

                    devConnectCommand.generateAndSendConnectLinkToPlayer(context.sender());
                });

        return CompletableFuture.completedFuture(null);
    }

    private void sendDefaultVoiceLink(CommandSender sender) {
        String finalURL = plugin.config.get().getBaseUrl() + "/dev";

        sender.sendMessage(Message.raw("[Voice Chat] Click here to Voice Chat:").link(finalURL).color(Color.GREEN).bold(true));
        sender.sendMessage(Message.raw(finalURL).link(finalURL).color(Color.GREEN));
        sender.sendMessage(Message.raw("[Voice Chat] This is a DEV version!!! Expect bugs!!! Use \"/voicechat\" for the stable version.").color(Color.ORANGE).bold(true));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}