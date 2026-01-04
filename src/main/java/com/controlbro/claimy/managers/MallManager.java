package com.controlbro.claimy.managers;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.ChunkKey;
import org.bukkit.Chunk;
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
    private final Map<Integer, ChunkKey> plots = new HashMap<>();
    private final Map<Integer, UUID> plotOwners = new HashMap<>();

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
            ChunkKey chunkKey = ChunkKey.fromString(section.getString("chunk"));
            plots.put(id, chunkKey);
            String owner = section.getString("owner");
            if (owner != null && !owner.isBlank()) {
                plotOwners.put(id, UUID.fromString(owner));
            }
        }
    }

    public void save() {
        config = new YamlConfiguration();
        ConfigurationSection plotsSection = config.createSection("plots");
        for (Map.Entry<Integer, ChunkKey> entry : plots.entrySet()) {
            ConfigurationSection section = plotsSection.createSection(String.valueOf(entry.getKey()));
            section.set("chunk", entry.getValue().asString());
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

    public void definePlot(int id, Chunk chunk) {
        plots.put(id, new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
        save();
    }

    public Optional<ChunkKey> getPlotChunk(int id) {
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
        ChunkKey key = new ChunkKey(location.getWorld().getName(), location.getChunk().getX(), location.getChunk().getZ());
        for (Map.Entry<Integer, ChunkKey> entry : plots.entrySet()) {
            if (entry.getValue().equals(key)) {
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
}
