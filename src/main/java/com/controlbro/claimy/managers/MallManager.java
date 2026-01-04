package com.controlbro.claimy.managers;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Region;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MallManager {
    private final ClaimyPlugin plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<Integer, Region> plots = new HashMap<>();
    private final Map<Integer, UUID> plotOwners = new HashMap<>();
    private final Map<UUID, Location> selectionPrimary = new HashMap<>();
    private final Map<UUID, Location> selectionSecondary = new HashMap<>();

    public MallManager(ClaimyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mall.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create mall.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        plots.clear();
        plotOwners.clear();
        ConfigurationSection plotsSection = config.getConfigurationSection("plots");
        if (plotsSection == null) {
            return;
        }
        for (String key : plotsSection.getKeys(false)) {
            int id = Integer.parseInt(key);
            ConfigurationSection section = plotsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String regionValue = section.getString("region");
            if (regionValue != null) {
                plots.put(id, Region.deserialize(regionValue));
            }
            String owner = section.getString("owner");
            if (owner != null && !owner.isBlank()) {
                plotOwners.put(id, UUID.fromString(owner));
            }
        }
    }

    public void save() {
        config = new YamlConfiguration();
        ConfigurationSection plotsSection = config.createSection("plots");
        for (Map.Entry<Integer, Region> entry : plots.entrySet()) {
            ConfigurationSection section = plotsSection.createSection(String.valueOf(entry.getKey()));
            section.set("region", entry.getValue().serialize());
            UUID owner = plotOwners.get(entry.getKey());
            if (owner != null) {
                section.set("owner", owner.toString());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save mall.yml: " + e.getMessage());
        }
    }

    public void definePlot(int id, Region region) {
        plots.put(id, region);
        save();
    }

    public Optional<Region> getPlotRegion(int id) {
        return Optional.ofNullable(plots.get(id));
    }

    public Optional<UUID> getPlotOwner(int id) {
        return Optional.ofNullable(plotOwners.get(id));
    }

    public boolean claimPlot(int id, UUID playerId) {
        if (!plots.containsKey(id)) {
            return false;
        }
        if (plotOwners.containsKey(id)) {
            return false;
        }
        plotOwners.put(id, playerId);
        save();
        return true;
    }

    public Optional<Integer> getPlotAt(Location location) {
        for (Map.Entry<Integer, Region> entry : plots.entrySet()) {
            if (entry.getValue().contains(location)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public boolean isInMall(Location location) {
        return getPlotAt(location).isPresent();
    }

    public boolean isMallOwner(Location location, UUID playerId) {
        Optional<Integer> plotId = getPlotAt(location);
        return plotId.isPresent() && playerId.equals(plotOwners.get(plotId.get()));
    }

    public void clearPlotOwner(int id) {
        plotOwners.remove(id);
        save();
    }

    public void setPrimarySelection(UUID playerId, Location location) {
        selectionPrimary.put(playerId, location);
    }

    public void setSecondarySelection(UUID playerId, Location location) {
        selectionSecondary.put(playerId, location);
    }

    public Optional<Location> getPrimarySelection(UUID playerId) {
        return Optional.ofNullable(selectionPrimary.get(playerId));
    }

    public Optional<Location> getSecondarySelection(UUID playerId) {
        return Optional.ofNullable(selectionSecondary.get(playerId));
    }

    public Optional<Region> buildSelection(UUID playerId) {
        Location primary = selectionPrimary.get(playerId);
        Location secondary = selectionSecondary.get(playerId);
        if (primary == null || secondary == null) {
            return Optional.empty();
        }
        if (!primary.getWorld().equals(secondary.getWorld())) {
            return Optional.empty();
        }
        return Optional.of(new Region(
                primary.getWorld().getName(),
                primary.getBlockX(),
                primary.getBlockY(),
                primary.getBlockZ(),
                secondary.getBlockX(),
                secondary.getBlockY(),
                secondary.getBlockZ()
        ));
    }
}
