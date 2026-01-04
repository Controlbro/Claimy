package com.controlbro.claimy.model;

import java.util.Objects;

public final class ChunkKey {
    private final String world;
    private final int x;
    private final int z;

    public ChunkKey(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public String asString() {
        return world + ":" + x + ":" + z;
    }

    public static ChunkKey fromString(String value) {
        String[] parts = value.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid chunk key: " + value);
        }
        return new ChunkKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChunkKey chunkKey = (ChunkKey) o;
        return x == chunkKey.x && z == chunkKey.z && Objects.equals(world, chunkKey.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }
}
