package me.nemtudo.voicechat.commands.VoiceChat.Connect;

import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.commands.VoiceChat.DownloadCommand;
import me.nemtudo.voicechat.commands.VoiceChat.ReloadCommand;
import me.nemtudo.voicechat.network.PlayerTokenRequestPayload;
import me.nemtudo.voicechat.utils.ApiRequestHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DevCommand extends AbstractCommand {

    private final VoiceChat plugin;
    private final ApiRequestHelper apiRequestHelper;
    private final HytaleLogger LOGGER;

    public DevCommand(VoiceChat plugin) {
        super("dev", "Get the connect voice chat link for a development version. Expect bugs!", false);
        this.plugin = plugin;
        this.LOGGER = plugin.getLogger();
        this.apiRequestHelper = plugin.getApiRequestHelper();
        this.addSubCommand(new ReloadCommand(this.plugin));
        this.addSubCommand(new DownloadCommand(this.plugin));
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("[Voice Chat] Only players can execute this command"));
            return CompletableFuture.completedFuture(null);
        }

        UUID playerUUID = context.sender().getUuid();
        String playerName = context.sender().getDisplayName();

        PlayerTokenRequestPayload requestPayload = new PlayerTokenRequestPayload(playerUUID, playerName, true);

        context.sender().sendMessage(Message.raw("[Voice Chat] Generating your link... Wait").color(Color.CYAN));

        apiRequestHelper.request(
                "POST",
                "/players/connect",
                requestPayload
        ).thenAccept(response -> {

            if (response.statusCode() == 401) {
                LOGGER.atSevere().log("API error 401 (Unauthorized): Invalid server token. ServerToken in /mods/NemTudo_VoiceChat/VoiceChat.json is invalid. Response: " + response.body());
                context.sender().sendMessage(Message.raw("[Voice Chat] Authentication error with the API. Please contact an administrator. ServerToken in /mods/NemTudo_VoiceChat/VoiceChat.json is invalid").color(Color.RED));
                return;
            }

            if (response.statusCode() >= 400) {
                LOGGER.atSevere().log("API error " + response.statusCode() + ": " + response.body());
                context.sender().sendMessage(Message.raw("[Voice Chat] An error occurred while generating your link: HTTP " + response.statusCode()).color(Color.RED).italic(true));
                return;
            }

            String responseBody = response.body();
            JsonObject bodyJson = plugin.gson.fromJson(responseBody, JsonObject.class);

            String playerToken = bodyJson.get("token").getAsString();

            String finalURL = plugin.config.get().getBaseUrl() + "/dev/connect?token=" + playerToken;

            context.sender().sendMessage(Message.raw("[Voice Chat] Click here to connect to Voice Chat:").link(finalURL).color(Color.GREEN).bold(true));
            context.sender().sendMessage(Message.raw(finalURL).link(finalURL).color(Color.GREEN));
            context.sender().sendMessage(Message.raw("[Voice Chat] This is a DEV version!!! Expect bugs!!! Use \"/voicechat\" for the stable version.").color(Color.ORANGE).bold(true));

        }).exceptionally(e -> {
            LOGGER.atSevere().log("Failed to generate player token " + e);
            return null;
        });

        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

}