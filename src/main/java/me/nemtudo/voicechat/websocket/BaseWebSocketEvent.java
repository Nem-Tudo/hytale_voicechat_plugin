package me.nemtudo.voicechat.websocket;

import me.nemtudo.voicechat.VoiceChat;
import com.google.gson.Gson;
import org.json.JSONObject;

public abstract class BaseWebSocketEvent<T> {

    protected final VoiceChat plugin;
    protected final Gson gson;
    private final Class<T> dataClass;

    public BaseWebSocketEvent(VoiceChat plugin, Class<T> dataClass) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.dataClass = dataClass;
    }

    public final void execute(Object args) {
        if (!(args instanceof JSONObject jsonObject)) {
            plugin.getLogger().atWarning().log("Invalid args type for " + getEventName());
            return;
        }

        try {
            T data = gson.fromJson(jsonObject.toString(), dataClass);

            if (data != null) {
                onEvent(data);
            } else {
                plugin.getLogger().atWarning().log("Failed to parse data for " + getEventName());
            }
        } catch (Exception e) {
            plugin.getLogger().atSevere().log("Error in " + getEventName() + ": " + e.getMessage());
        }
    }

    protected abstract void onEvent(T data);

    public abstract String getEventName();

    public Boolean isOnce() {
        return false;
    }
}