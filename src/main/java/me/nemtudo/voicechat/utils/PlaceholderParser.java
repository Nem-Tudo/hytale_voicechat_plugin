package me.nemtudo.voicechat.utils;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderParser {

    private final Map<String, String> placeholders = new HashMap<>();

    public PlaceholderParser add(String key, String value) {
        placeholders.put(key, value != null ? value : "");
        return this;
    }

    public String parse(String text) {
        if (text == null) return "";

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}