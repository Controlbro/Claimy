package com.controlbro.claimy.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Nation {
    private final UUID id;
    private String name;
    private UUID owner;
    private UUID capitalTownId;
    private final Set<UUID> memberTowns = new HashSet<>();
    private final List<NationLogEntry> activityLog = new ArrayList<>();
    private final Set<UUID> allies = new HashSet<>();
    private final Set<UUID> enemies = new HashSet<>();

    public Nation(UUID id, String name, UUID owner, UUID capitalTownId) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.capitalTownId = capitalTownId;
        this.memberTowns.add(capitalTownId);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public UUID getCapitalTownId() {
        return capitalTownId;
    }

    public void setCapitalTownId(UUID capitalTownId) {
        this.capitalTownId = capitalTownId;
    }

    public Set<UUID> getMemberTowns() {
        return memberTowns;
    }

    public List<NationLogEntry> getActivityLog() {
        return activityLog;
    }

    public void addLogEntry(String message) {
        activityLog.add(new NationLogEntry(Instant.now().toEpochMilli(), message));
    }

    public Set<UUID> getAllies() {
        return allies;
    }

    public Set<UUID> getEnemies() {
        return enemies;
    }

    public record NationLogEntry(long timestamp, String message) {
    }
}
