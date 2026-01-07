package com.controlbro.claimy.managers;

import com.controlbro.claimy.ClaimyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerDataManager {
    private final ClaimyPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public PlayerDataManager(ClaimyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create players.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save players.yml: " + e.getMessage());
        }
    }

    public long getLastBuildWarning(UUID playerId) {
        return config.getLong("players." + playerId + ".build-warning-last", 0L);
    }

    public void setLastBuildWarning(UUID playerId, long timestamp) {
        config.set("players." + playerId + ".build-warning-last", timestamp);
        save();
    }
}
