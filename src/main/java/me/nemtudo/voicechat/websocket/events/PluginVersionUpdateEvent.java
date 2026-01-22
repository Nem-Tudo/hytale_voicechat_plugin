package me.nemtudo.voicechat.websocket.events;

import com.hypixel.hytale.server.core.Message;
import me.nemtudo.voicechat.VoiceChat;
import me.nemtudo.voicechat.utils.ConsoleColors;
import me.nemtudo.voicechat.websocket.BaseWebSocketEvent;

import java.awt.*;

public class PluginVersionUpdateEvent extends BaseWebSocketEvent<PluginVersionUpdateData> {

    public PluginVersionUpdateEvent(VoiceChat plugin) {
        super(plugin, PluginVersionUpdateData.class);
    }

    @Override
    protected void onEvent(PluginVersionUpdateData data) {
        plugin.getVersionCheckService().checkPluginVersion();
        plugin.getLogger().atInfo().log(ConsoleColors.success("[Voice Chat] An update has just been released!"));
        if (data.needBroadcast()) {
            plugin.broadcast(Message.raw("[Voice Chat] An update has just been released!").color(Color.GREEN));
        }
    }

    @Override
    public String getEventName() {
        return "plugin:version_update";
    }
}