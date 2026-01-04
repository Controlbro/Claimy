package com.controlbro.claimy.model;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.EnumSet;

public class Town {
    private final String name;
    private final UUID owner;
    private final Set<UUID> residents = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<ChunkKey> chunks = new HashSet<>();
    private final Map<TownFlag, Boolean> flags = new EnumMap<>(TownFlag.class);
    private final Map<UUID, EnumSet<ResidentPermission>> residentPermissions = new HashMap<>();
    private String mapColor;
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

    public Map<UUID, EnumSet<ResidentPermission>> getResidentPermissionOverrides() {
        return residentPermissions;
    }

    public boolean isResidentPermissionEnabled(UUID uuid, ResidentPermission permission) {
        EnumSet<ResidentPermission> permissions = residentPermissions.get(uuid);
        if (permissions == null) {
            return true;
        }
        return permissions.contains(permission);
    }

    public void setResidentPermission(UUID uuid, ResidentPermission permission, boolean value) {
        EnumSet<ResidentPermission> permissions = residentPermissions.getOrDefault(uuid, EnumSet.allOf(ResidentPermission.class));
        if (value) {
            permissions.add(permission);
        } else {
            permissions.remove(permission);
        }
        if (permissions.size() == ResidentPermission.values().length) {
            residentPermissions.remove(uuid);
        } else {
            residentPermissions.put(uuid, permissions);
        }
    }

    public void setResidentPermissions(UUID uuid, EnumSet<ResidentPermission> permissions) {
        if (permissions == null) {
            residentPermissions.remove(uuid);
            return;
        }
        if (permissions.size() == ResidentPermission.values().length) {
            residentPermissions.remove(uuid);
        } else {
            residentPermissions.put(uuid, EnumSet.copyOf(permissions));
        }
    }

    public void clearResidentPermissions(UUID uuid) {
        residentPermissions.remove(uuid);
    }

    public boolean isResident(UUID uuid) {
        return residents.contains(uuid);
    }

    public int getChunkLimit() {
        return chunkLimit;
    }

    public String getMapColor() {
        return mapColor;
    }

    public void setMapColor(String mapColor) {
        this.mapColor = mapColor;
    }

    public boolean isFlagEnabled(TownFlag flag) {
        return flags.getOrDefault(flag, false);
    }

    public void setFlag(TownFlag flag, boolean value) {
        flags.put(flag, value);
    }
}
