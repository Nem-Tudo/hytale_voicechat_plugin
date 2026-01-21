package me.nemtudo.voicechat.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the state of a player including position and settings
 */
public class PlayerState {

    private final String uuid;
    private final String name;
    private Position position;

    // Reserved for future voice settings (proximity, volume, etc)
    private final Map<String, Object> settings;

    public PlayerState(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.settings = new HashMap<>();
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerState other)) {
            return false;
        }

        if (!name.equals(other.name)) {
            return false;
        }

        return Objects.equals(position, other.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, position);
    }
}