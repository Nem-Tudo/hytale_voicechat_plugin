package me.nemtudo.voicechat.service;

import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.utils.ApiRequestHelper;
import me.nemtudo.voicechat.utils.VersionComparator;
import me.nemtudo.voicechat.utils.VersionStatus;

/**
 * Service responsible for checking plugin version against API
 */
public class VersionCheckService {


    private final VoiceChat plugin;

    private final HytaleLogger LOGGER;

    private final ApiRequestHelper apiRequestHelper;

    private volatile String latestStableVersion;
    private volatile String latestAcceptableVersion;
    private volatile String downloadPluginURL;
    private volatile boolean versionMismatch = false;

    public VersionCheckService(VoiceChat plugin, ApiRequestHelper apiRequestHelper) {
        this.plugin = plugin;
        this.LOGGER = plugin.getLogger();
        this.apiRequestHelper = apiRequestHelper;
    }

    public void checkPluginVersion() {
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                performVersionCheck();
            } catch (Exception e) {
                LOGGER.atSevere().log("Failed to check VoiceChat plugin version: " + e.getMessage());
            }
        });
    }

    private void performVersionCheck() {
        String pluginName = plugin.getName();
        String currentVersion = plugin.getManifest().getVersion().toString();

        apiRequestHelper.request(
                "GET",
                "/plugins/" + pluginName + "/versions",
                null
        ).thenAccept(response -> {
            if (response.statusCode() != 200) {
                LOGGER.atWarning().log(
                        "Failed to check plugin version (HTTP " + response.statusCode() + ")"
                );
                return;
            }

            processVersionResponse(response.body(), currentVersion);

        }).exceptionally(e -> {
            LOGGER.atSevere().log("Failed to check VoiceChat plugin version: " + e.getMessage());
            return null;
        });
    }

    private void processVersionResponse(String responseBody, String currentVersion) {
        JsonObject json = plugin.gson.fromJson(responseBody, JsonObject.class);
        latestStableVersion = json.get("latestStableVersion").getAsString();
        downloadPluginURL = json.get("downloadPluginURL").getAsString();
        latestAcceptableVersion = json.get("latestAcceptableVersion").getAsString();

        VersionStatus status = VersionComparator.compare(currentVersion, latestStableVersion);
        handleVersionStatus(status, currentVersion);
    }

    private void handleVersionStatus(VersionStatus status, String currentVersion) {
        switch (status) {
            case SAME_VERSION -> logSameVersion(currentVersion);
            case BEHIND_LAST_PATCH -> logBehindPatch(currentVersion);
            case BEHIND_MAJOR -> logBehindMajor(currentVersion);
            case AHEAD_LAST_PATCH -> logAheadPatch(currentVersion);
            case AHEAD_MAJOR -> logAheadMajor(currentVersion);
        }
    }

    private void logSameVersion(String currentVersion) {
        LOGGER.atInfo().log("VoiceChat is on the most up-to-date version possible :)");
        LOGGER.atInfo().log("Current version : " + currentVersion + " | Latest stable : " + latestStableVersion);
    }

    private void logBehindPatch(String currentVersion) {
        LOGGER.atWarning().log("VoiceChat is slightly outdated (patch version behind).");
        LOGGER.atWarning().log("Current version : " + currentVersion + " | Latest stable : " + latestStableVersion);
        LOGGER.atWarning().log("Latest stable Download Link: " + downloadPluginURL);
    }

    private void logBehindMajor(String currentVersion) {
        versionMismatch = true;

        LOGGER.atSevere().log("==================================================");
        LOGGER.atSevere().log(" VoiceChat version MAJOR mismatch detected!");
        LOGGER.atSevere().log(" Current version               : " + currentVersion);
        LOGGER.atSevere().log(" Latest stable                 : " + latestStableVersion);
        LOGGER.atSevere().log(" Latest stable Download Link   : " + downloadPluginURL);
        LOGGER.atSevere().log(" Please update the plugin as soon as possible.");
        LOGGER.atSevere().log("==================================================");
    }

    private void logAheadPatch(String currentVersion) {
        LOGGER.atWarning().log("VoiceChat is running a newer PATCH version than the latest stable.");
        LOGGER.atWarning().log("This is usually safe, but unexpected issues may occur.");
        LOGGER.atWarning().log("Current version : " + currentVersion + " | Latest stable : " + latestStableVersion);
        LOGGER.atWarning().log("Latest stable Download Link: " + downloadPluginURL);
    }

    private void logAheadMajor(String currentVersion) {
        LOGGER.atWarning().log("==================================================");
        LOGGER.atWarning().log(" VoiceChat is running a NEWER MAJOR version!");
        LOGGER.atWarning().log(" This build may be unstable or incompatible.");
        LOGGER.atWarning().log(" Current version : " + currentVersion);
        LOGGER.atWarning().log(" Latest stable   : " + latestStableVersion);
        LOGGER.atWarning().log(" Latest stable Download Link: " + downloadPluginURL);
        LOGGER.atWarning().log("==================================================");
    }

    public boolean hasVersionMismatch() {
        return versionMismatch;
    }

    public String getLatestStableVersion() {
        return latestStableVersion;
    }

    public String getLatestAcceptableVersion(){
        return latestAcceptableVersion;
    }

    public String getDownloadPluginURL() {
        return downloadPluginURL;
    }
}