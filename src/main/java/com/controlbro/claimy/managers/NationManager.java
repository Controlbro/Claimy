package com.controlbro.claimy.managers;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Nation;
import com.controlbro.claimy.model.Town;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class NationManager {
    private final ClaimyPlugin plugin;
    private final Map<UUID, Nation> nations = new HashMap<>();
    private final Map<String, UUID> nationsByName = new HashMap<>();
    private final Map<UUID, UUID> invites = new HashMap<>();
    private final File file;
    private YamlConfiguration config;

    public NationManager(ClaimyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "nations.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create nations.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        nations.clear();
        nationsByName.clear();
        ConfigurationSection nationsSection = config.getConfigurationSection("nations");
        if (nationsSection == null) {
            return;
        }
        for (String idKey : nationsSection.getKeys(false)) {
            ConfigurationSection section = nationsSection.getConfigurationSection(idKey);
            if (section == null) {
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(idKey);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid nation id " + idKey);
                continue;
            }
            String name = section.getString("name");
            if (name == null) {
                continue;
            }
            UUID owner;
            UUID capital;
            try {
                owner = UUID.fromString(section.getString("owner"));
                capital = UUID.fromString(section.getString("capital"));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid owner/capital for nation " + idKey);
                continue;
            }
            Nation nation = new Nation(id, name, owner, capital);
            nation.getMemberTowns().clear();
            for (String townId : section.getStringList("towns")) {
                try {
                    nation.getMemberTowns().add(UUID.fromString(townId));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid town id in nation " + name + ": " + townId);
                }
            }
            if (nation.getMemberTowns().isEmpty()) {
                nation.getMemberTowns().add(capital);
            }
            nation.getActivityLog().clear();
            for (String entry : section.getStringList("activity-log")) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length < 2) {
                    continue;
                }
                try {
                    long timestamp = Long.parseLong(parts[0]);
                    nation.getActivityLog().add(new Nation.NationLogEntry(timestamp, parts[1]));
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
            for (String ally : section.getStringList("allies")) {
                try {
                    nation.getAllies().add(UUID.fromString(ally));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid ally nation id for " + name + ": " + ally);
                }
            }
            for (String enemy : section.getStringList("enemies")) {
                try {
                    nation.getEnemies().add(UUID.fromString(enemy));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid enemy nation id for " + name + ": " + enemy);
                }
            }
            nations.put(id, nation);
            nationsByName.put(name.toLowerCase(Locale.ROOT), id);
        }
    }

    public void save() {
        config = new YamlConfiguration();
        ConfigurationSection nationsSection = config.createSection("nations");
        for (Nation nation : nations.values()) {
            ConfigurationSection section = nationsSection.createSection(nation.getId().toString());
            section.set("name", nation.getName());
            section.set("owner", nation.getOwner().toString());
            section.set("capital", nation.getCapitalTownId().toString());
            List<String> towns = nation.getMemberTowns().stream()
                    .map(UUID::toString)
                    .toList();
            section.set("towns", towns);
            List<String> logEntries = nation.getActivityLog().stream()
                    .map(entry -> entry.timestamp() + "|" + entry.message())
                    .toList();
            section.set("activity-log", logEntries);
            if (!nation.getAllies().isEmpty()) {
                List<String> allies = nation.getAllies().stream().map(UUID::toString).toList();
                section.set("allies", allies);
            }
            if (!nation.getEnemies().isEmpty()) {
                List<String> enemies = nation.getEnemies().stream().map(UUID::toString).toList();
                section.set("enemies", enemies);
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save nations.yml: " + e.getMessage());
        }
    }

    public Optional<Nation> getNation(UUID nationId) {
        return Optional.ofNullable(nations.get(nationId));
    }

    public Optional<Nation> getNationByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        UUID id = nationsByName.get(name.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(nations.get(id));
    }

    public Optional<Nation> getNationByOwner(UUID ownerId) {
        return nations.values().stream()
                .filter(nation -> nation.getOwner().equals(ownerId))
                .findFirst();
    }

    public Optional<Nation> getNationForTown(Town town) {
        return town.getNationId().flatMap(this::getNation);
    }

    public Nation createNation(String name, UUID ownerId, Town capitalTown) {
        Nation nation = new Nation(UUID.randomUUID(), name, ownerId, capitalTown.getId());
        nation.addLogEntry("Nation created.");
        nations.put(nation.getId(), nation);
        nationsByName.put(name.toLowerCase(Locale.ROOT), nation.getId());
        save();
        return nation;
    }

    public void deleteNation(Nation nation) {
        nations.remove(nation.getId());
        nationsByName.remove(nation.getName().toLowerCase(Locale.ROOT));
        for (Nation other : nations.values()) {
            other.getAllies().remove(nation.getId());
            other.getEnemies().remove(nation.getId());
        }
        save();
    }

    public void addTown(Nation nation, Town town) {
        nation.getMemberTowns().add(town.getId());
        town.setNationId(nation.getId());
        nation.addLogEntry(town.getDisplayName() + " joined the nation.");
        save();
    }

    public void removeTown(Nation nation, Town town) {
        nation.getMemberTowns().remove(town.getId());
        town.setNationId(null);
        nation.addLogEntry(town.getDisplayName() + " left the nation.");
        if (nation.getCapitalTownId().equals(town.getId()) && !nation.getMemberTowns().isEmpty()) {
            UUID newCapital = nation.getMemberTowns().iterator().next();
            nation.setCapitalTownId(newCapital);
            String capitalName = plugin.getTownManager().getTownById(newCapital)
                    .map(Town::getDisplayName)
                    .orElse("a new town");
            nation.addLogEntry("Capital moved to " + capitalName + ".");
        }
        if (nation.getMemberTowns().isEmpty()) {
            deleteNation(nation);
        } else {
            save();
        }
    }

    public void setCapital(Nation nation, Town town) {
        nation.setCapitalTownId(town.getId());
        nation.addLogEntry("Capital moved to " + town.getDisplayName() + ".");
        save();
    }

    public void logTownDeny(Town town, Town denied) {
        getNationForTown(town).ifPresent(nation -> {
            nation.addLogEntry(town.getDisplayName() + " denied " + denied.getDisplayName() + ".");
            save();
        });
    }

    public void logTownAllow(Town town, Town allowed) {
        getNationForTown(town).ifPresent(nation -> {
            nation.addLogEntry(town.getDisplayName() + " allowed " + allowed.getDisplayName() + ".");
            save();
        });
    }

    public void inviteTown(Nation nation, Town town) {
        invites.put(town.getId(), nation.getId());
    }

    public Optional<Nation> getInviteForTown(Town town) {
        UUID nationId = invites.get(town.getId());
        return Optional.ofNullable(nations.get(nationId));
    }

    public void clearInvite(Town town) {
        invites.remove(town.getId());
    }

    public boolean setAlliance(Nation nation, Nation target) {
        if (nation.getId().equals(target.getId())) {
            return false;
        }
        nation.getEnemies().remove(target.getId());
        target.getEnemies().remove(nation.getId());
        nation.getAllies().add(target.getId());
        target.getAllies().add(nation.getId());
        nation.addLogEntry("Allied with " + target.getName() + ".");
        target.addLogEntry("Allied with " + nation.getName() + ".");
        save();
        return true;
    }

    public boolean setEnemy(Nation nation, Nation target) {
        if (nation.getId().equals(target.getId())) {
            return false;
        }
        nation.getAllies().remove(target.getId());
        target.getAllies().remove(nation.getId());
        nation.getEnemies().add(target.getId());
        target.getEnemies().add(nation.getId());
        nation.addLogEntry("Marked " + target.getName() + " as an enemy.");
        target.addLogEntry("Marked " + nation.getName() + " as an enemy.");
        save();
        return true;
    }

    public Set<String> getNationNames() {
        Set<String> names = new HashSet<>();
        for (Nation nation : nations.values()) {
            names.add(nation.getName());
        }
        return names;
    }

    public List<Nation> getNations() {
        return new ArrayList<>(nations.values());
    }

    public void handleTownDeleted(Town town) {
        getNationForTown(town).ifPresent(nation -> removeTown(nation, town));
    }
}
