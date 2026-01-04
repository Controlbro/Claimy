package com.controlbro.claimy.economy;

import com.controlbro.claimy.ClaimyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BalanceStorage {
    private final ClaimyPlugin plugin;
    private final File file;
    private final Map<UUID, Double> balances = new HashMap<>();

    public BalanceStorage(ClaimyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
        load();
    }

    public synchronized void load() {
        balances.clear();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create balances.yml: " + e.getMessage());
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            balances.put(UUID.fromString(key), config.getDouble(key));
        }
    }

    public synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save balances.yml: " + e.getMessage());
        }
    }

    public synchronized double getBalance(UUID uuid) {
        if (!balances.containsKey(uuid)) {
            double starting = plugin.getConfig().getDouble("settings.starting-balance", 0.0);
            balances.put(uuid, starting);
            save();
        }
        return balances.getOrDefault(uuid, 0.0);
    }

    public synchronized void setBalance(UUID uuid, double amount) {
        balances.put(uuid, amount);
        save();
    }

    public synchronized void addBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public synchronized void subtractBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) - amount);
    }
}
