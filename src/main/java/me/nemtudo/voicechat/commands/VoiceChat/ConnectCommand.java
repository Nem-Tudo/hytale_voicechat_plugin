package me.nemtudo.voicechat.commands.VoiceChat;

import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.commands.VoiceChat.Connect.DevConnectCommand;
import me.nemtudo.voicechat.network.PlayerTokenRequestPayload;
import me.nemtudo.voicechat.utils.ApiRequestHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConnectCommand extends AbstractCommand {

    private final VoiceChat plugin;
    private final ApiRequestHelper apiRequestHelper;
    private final HytaleLogger LOGGER;

    public ConnectCommand(VoiceChat plugin) {
        super("connect", "Get the voice chat connect link.", false);
        this.plugin = plugin;
        this.LOGGER = plugin.getLogger();
        this.apiRequestHelper = plugin.getApiRequestHelper();
        this.addSubCommand(new DevConnectCommand(this.plugin));
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("[Voice Chat] Only players can execute this command"));
            return CompletableFuture.completedFuture(null);
        }
        generateAndSendConnectLinkToPlayer(context.sender());

        return CompletableFuture.completedFuture(null);
    }

    public void generateAndSendConnectLinkToPlayer(CommandSender sender) {
        UUID playerUUID = sender.getUuid();
        String playerName = sender.getDisplayName();

        PlayerTokenRequestPayload requestPayload = new PlayerTokenRequestPayload(playerUUID, playerName, false);

        sender.sendMessage(Message.raw("[Voice Chat] Generating your link... Wait").color(Color.CYAN));

        apiRequestHelper.request(
                "POST",
                "/players/connect",
                requestPayload
        ).thenAccept(response -> {

            if (response.statusCode() == 401) {
                LOGGER.atSevere().log("API error 401 (Unauthorized): Invalid server token. ServerToken in /mods/NemTudo_VoiceChat/VoiceChat.json is invalid. Response: " + response.body());
                sender.sendMessage(Message.raw("[Voice Chat] Authentication error with the API. Please contact an administrator. ServerToken in /mods/NemTudo_VoiceChat/VoiceChat.json is invalid").color(Color.RED));
                return;
            }

            if (response.statusCode() != 200) {
                LOGGER.atSevere().log("API error " + response.statusCode() + ": " + response.body());
                sender.sendMessage(Message.raw("[Voice Chat] An error occurred while generating your link: HTTP " + response.statusCode()).color(Color.RED).italic(true));
                return;
            }

            String responseBody = response.body();
            JsonObject bodyJson = plugin.gson.fromJson(responseBody, JsonObject.class);

            String playerToken = bodyJson.get("token").getAsString();

            String finalURL = plugin.config.get().getBaseUrl() + "/connect?code=" + playerToken;

            sender.sendMessage(Message.raw("[Voice Chat] Your code: " + playerToken).link(finalURL).color(Color.WHITE));
            sender.sendMessage(Message.raw("[Voice Chat] Click here to connect to Voice Chat:").link(finalURL).color(Color.GREEN).bold(true));
            sender.sendMessage(Message.raw(finalURL).link(finalURL).color(Color.GREEN));

        }).exceptionally(e -> {
            LOGGER.atSevere().log("Failed to generate player token " + e);
            return null;
        });
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

}