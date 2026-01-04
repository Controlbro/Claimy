package com.controlbro.claimy.model;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Town {
    private final String name;
    private final UUID owner;
    private final Set<UUID> residents = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<ChunkKey> chunks = new HashSet<>();
    private final Map<TownFlag, Boolean> flags = new EnumMap<>(TownFlag.class);
    private int chunkLimit;

    public Town(String name, UUID owner, int chunkLimit) {
        this.name = name;
        this.owner = owner;
        this.chunkLimit = chunkLimit;
        this.residents.add(owner);
        for (TownFlag flag : TownFlag.values()) {
            flags.put(flag, true);
        }
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<UUID> getResidents() {
        return residents;
    }

    public Set<String> getAllies() {
        return allies;
    }

    public Set<ChunkKey> getChunks() {
        return chunks;
    }

    public Map<TownFlag, Boolean> getFlags() {
        return flags;
    }

    public boolean isResident(UUID uuid) {
        return residents.contains(uuid);
    }

    public int getChunkLimit() {
        return chunkLimit;
    }

    public void addChunkLimit(int amount) {
        chunkLimit += amount;
    }

    public boolean isFlagEnabled(TownFlag flag) {
        return flags.getOrDefault(flag, false);
    }

    public void setFlag(TownFlag flag, boolean value) {
        flags.put(flag, value);
    }
}
