package com.controlbro.claimy.model;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.Optional;

public class Town {
    private final UUID id;
    private final String name;
    private String displayName;
    private final UUID owner;
    private final Set<UUID> residents = new HashSet<>();
    private final Set<UUID> assistants = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<ChunkKey> chunks = new HashSet<>();
    private final Set<ChunkKey> outpostChunks = new HashSet<>();
    private final Map<Integer, Region> plots = new HashMap<>();
    private final Map<Integer, UUID> plotOwners = new HashMap<>();
    private final Map<Integer, String> plotColors = new HashMap<>();
    private final Map<TownFlag, Boolean> flags = new EnumMap<>(TownFlag.class);
    private final Map<UUID, EnumSet<ResidentPermission>> residentPermissions = new HashMap<>();
    private String mapColor;
    private TownBuildMode buildMode = TownBuildMode.OPEN_TOWN;
    private int chunkLimit;
    private UUID nationId;
    private final Set<UUID> deniedTowns = new HashSet<>();
    private boolean nationJoinNotified;

    public Town(UUID id, String name, String displayName, UUID owner, int chunkLimit) {
        this.id = id;
        this.name = name;
        this.displayName = displayName == null ? name : displayName;
        this.owner = owner;
        this.chunkLimit = chunkLimit;
        this.residents.add(owner);
        for (TownFlag flag : TownFlag.values()) {
            flags.put(flag, flag.getDefaultValue());
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName == null ? name : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? name : displayName;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<UUID> getResidents() {
        return residents;
    }

    public Set<UUID> getAssistants() {
        return assistants;
    }

    public Set<String> getAllies() {
        return allies;
    }

    public Set<ChunkKey> getChunks() {
        return chunks;
    }

    public Set<ChunkKey> getOutpostChunks() {
        return outpostChunks;
    }

    public Map<Integer, Region> getPlots() {
        return plots;
    }

    public Map<Integer, UUID> getPlotOwners() {
        return plotOwners;
    }

    public Map<Integer, String> getPlotColors() {
        return plotColors;
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

    public boolean isAssistant(UUID uuid) {
        return assistants.contains(uuid);
    }

    public int getChunkLimit() {
        return chunkLimit;
    }

    public Optional<UUID> getNationId() {
        return Optional.ofNullable(nationId);
    }

    public void setNationId(UUID nationId) {
        this.nationId = nationId;
    }

    public Set<UUID> getDeniedTowns() {
        return deniedTowns;
    }

    public boolean isNationJoinNotified() {
        return nationJoinNotified;
    }

    public void setNationJoinNotified(boolean nationJoinNotified) {
        this.nationJoinNotified = nationJoinNotified;
    }

    public String getMapColor() {
        return mapColor;
    }

    public void setMapColor(String mapColor) {
        this.mapColor = mapColor;
    }

    public TownBuildMode getBuildMode() {
        return buildMode;
    }

    public void setBuildMode(TownBuildMode buildMode) {
        this.buildMode = buildMode;
    }

    public boolean isFlagEnabled(TownFlag flag) {
        return flags.getOrDefault(flag, false);
    }

    public void setFlag(TownFlag flag, boolean value) {
        flags.put(flag, value);
    }

    public void addAssistant(UUID uuid) {
        assistants.add(uuid);
    }

    public void removeAssistant(UUID uuid) {
        assistants.remove(uuid);
    }

    public void addOutpostChunk(ChunkKey chunkKey) {
        outpostChunks.add(chunkKey);
    }

    public void removeOutpostChunk(ChunkKey chunkKey) {
        outpostChunks.remove(chunkKey);
    }

    public Optional<UUID> getPlotOwner(int plotId) {
        return Optional.ofNullable(plotOwners.get(plotId));
    }

    public void claimPlot(int plotId, UUID ownerId) {
        plotOwners.put(plotId, ownerId);
    }

    public void unclaimPlot(int plotId) {
        plotOwners.remove(plotId);
    }

    public void removePlotsOwnedBy(UUID ownerId) {
        plotOwners.entrySet().removeIf(entry -> entry.getValue().equals(ownerId));
    }
}
