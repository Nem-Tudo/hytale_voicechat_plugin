package me.nemtudo.voicechat.model;

import java.util.Objects;

/**
 * Represents a 3D position in a world
 */
public class Position {

    private static final double POSITION_TOLERANCE = 0.01;

    public final double x;
    public final double y;
    public final double z;
    public final String world;

    public Position(double x, double y, double z, String world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Position other)) {
            return false;
        }

        return Math.abs(x - other.x) < POSITION_TOLERANCE &&
                Math.abs(y - other.y) < POSITION_TOLERANCE &&
                Math.abs(z - other.z) < POSITION_TOLERANCE &&
                world.equals(other.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, world);
    }
}