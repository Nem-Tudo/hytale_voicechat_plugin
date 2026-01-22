package me.nemtudo.voicechat.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.commands.VoiceChat.*;
import me.nemtudo.voicechat.utils.ApiRequestHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class VoiceChatCommand extends AbstractCommand {

    private final VoiceChat plugin;

    ConnectCommand connectCommand;

    public VoiceChatCommand(VoiceChat plugin) {
        super("voicechat", "Get the voice chat link", false);
        this.plugin = plugin;

        this.connectCommand = new ConnectCommand(this.plugin);
        this.addSubCommand(connectCommand);

        this.addSubCommand(new ReloadCommand(this.plugin));
        this.addSubCommand(new DownloadCommand(this.plugin));
        this.addSubCommand(new DevCommand(this.plugin));
        this.addSubCommand(new AuthorCommand(this.plugin));
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

                    connectCommand.generateAndSendConnectLinkToPlayer(context.sender());
                });

        return CompletableFuture.completedFuture(null);
    }

    private void sendDefaultVoiceLink(CommandSender sender) {
        String finalURL = plugin.config.get().getBaseUrl();

        sender.sendMessage(Message.raw("[Voice Chat] Click here to Voice Chat:").link(finalURL).color(Color.GREEN).bold(true));
        sender.sendMessage(Message.raw(finalURL).link(finalURL).color(Color.GREEN));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}