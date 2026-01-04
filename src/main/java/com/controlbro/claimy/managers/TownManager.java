package com.controlbro.claimy.managers;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.ChunkKey;
import com.controlbro.claimy.model.ResidentPermission;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.model.TownFlag;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TownManager {
    private final ClaimyPlugin plugin;
    private final Map<String, Town> towns = new HashMap<>();
    private final Map<UUID, String> playerTown = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>();
    private final Set<UUID> autoClaiming = new HashSet<>();
    private final File file;
    private YamlConfiguration config;

    public TownManager(ClaimyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "towns.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create towns.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        towns.clear();
        playerTown.clear();
        ConfigurationSection townsSection = config.getConfigurationSection("towns");
        if (townsSection == null) {
            return;
        }
        for (String name : townsSection.getKeys(false)) {
            ConfigurationSection section = townsSection.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            UUID owner = UUID.fromString(section.getString("owner"));
            int limit = section.getInt("chunk-limit", plugin.getConfig().getInt("settings.max-chunks"));
            Town town = new Town(name, owner, limit);
            town.getResidents().clear();
            for (String resident : section.getStringList("residents")) {
                town.getResidents().add(UUID.fromString(resident));
            }
            town.getAllies().addAll(section.getStringList("allies"));
            for (String chunkString : section.getStringList("chunks")) {
                town.getChunks().add(ChunkKey.fromString(chunkString));
            }
            ConfigurationSection flagSection = section.getConfigurationSection("flags");
            if (flagSection != null) {
                for (TownFlag flag : TownFlag.values()) {
                    town.setFlag(flag, flagSection.getBoolean(flag.name(), true));
                }
            }
            ConfigurationSection permissionSection = section.getConfigurationSection("resident-permissions");
            if (permissionSection != null) {
                for (String key : permissionSection.getKeys(false)) {
                    UUID residentId = UUID.fromString(key);
                    List<String> permissions = permissionSection.getStringList(key);
                    EnumSet<ResidentPermission> allowed = EnumSet.noneOf(ResidentPermission.class);
                    for (String permissionName : permissions) {
                        try {
                            ResidentPermission permission = ResidentPermission.valueOf(permissionName.toUpperCase(Locale.ROOT));
                            allowed.add(permission);
                        } catch (IllegalArgumentException ex) {
                            // ignore invalid permissions
                        }
                    }
                    town.setResidentPermissions(residentId, allowed);
                }
            }
            town.setMapColor(section.getString("map-color"));
            towns.put(name.toLowerCase(Locale.ROOT), town);
            for (UUID resident : town.getResidents()) {
                playerTown.put(resident, name.toLowerCase(Locale.ROOT));
            }
        }
    }

    public void save() {
        config = new YamlConfiguration();
        ConfigurationSection townsSection = config.createSection("towns");
        for (Town town : towns.values()) {
            ConfigurationSection section = townsSection.createSection(town.getName());
            section.set("owner", town.getOwner().toString());
            section.set("chunk-limit", town.getChunkLimit());
            List<String> residents = new ArrayList<>();
            for (UUID uuid : town.getResidents()) {
                residents.add(uuid.toString());
            }
            section.set("residents", residents);
            section.set("allies", new ArrayList<>(town.getAllies()));
            List<String> chunkStrings = new ArrayList<>();
            for (ChunkKey key : town.getChunks()) {
                chunkStrings.add(key.asString());
            }
            section.set("chunks", chunkStrings);
            ConfigurationSection flags = section.createSection("flags");
            for (TownFlag flag : TownFlag.values()) {
                flags.set(flag.name(), town.isFlagEnabled(flag));
            }
            ConfigurationSection residentPermissions = section.createSection("resident-permissions");
            for (Map.Entry<UUID, EnumSet<ResidentPermission>> entry : town.getResidentPermissionOverrides().entrySet()) {
                List<String> permissions = entry.getValue().stream()
                        .map(ResidentPermission::name)
                        .toList();
                residentPermissions.set(entry.getKey().toString(), permissions);
            }
            if (town.getMapColor() != null) {
                section.set("map-color", town.getMapColor());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save towns.yml: " + e.getMessage());
        }
    }

    public Optional<Town> getTown(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(towns.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<Town> getTown(UUID playerId) {
        String townName = playerTown.get(playerId);
        return getTown(townName);
    }

    public Town createTown(String name, UUID owner) {
        Town town = new Town(name, owner, plugin.getConfig().getInt("settings.max-chunks"));
        towns.put(name.toLowerCase(Locale.ROOT), town);
        playerTown.put(owner, name.toLowerCase(Locale.ROOT));
        save();
        plugin.getMapIntegration().refreshAll();
        return town;
    }

    public void deleteTown(Town town) {
        towns.remove(town.getName().toLowerCase(Locale.ROOT));
        for (UUID resident : town.getResidents()) {
            playerTown.remove(resident);
            autoClaiming.remove(resident);
        }
        save();
        plugin.getMapIntegration().refreshAll();
    }

    public boolean addResident(Town town, UUID playerId) {
        String existingTown = playerTown.get(playerId);
        if (existingTown != null && !existingTown.equalsIgnoreCase(town.getName())) {
            return false;
        }
        if (town.getResidents().add(playerId)) {
            playerTown.put(playerId, town.getName().toLowerCase(Locale.ROOT));
            save();
            return true;
        }
        return false;
    }

    public boolean removeResident(Town town, UUID playerId) {
        if (town.getResidents().remove(playerId)) {
            playerTown.remove(playerId);
            autoClaiming.remove(playerId);
            town.clearResidentPermissions(playerId);
            save();
            return true;
        }
        return false;
    }

    public void invitePlayer(UUID playerId, Town town) {
        invites.put(playerId, town.getName().toLowerCase(Locale.ROOT));
    }

    public Optional<String> getInvite(UUID playerId) {
        return Optional.ofNullable(invites.get(playerId));
    }

    public void clearInvite(UUID playerId) {
        invites.remove(playerId);
    }

    public Optional<Town> getTownAt(Location location) {
        ChunkKey key = new ChunkKey(location.getWorld().getName(), location.getChunk().getX(), location.getChunk().getZ());
        for (Town town : towns.values()) {
            if (town.getChunks().contains(key)) {
                return Optional.of(town);
            }
        }
        return Optional.empty();
    }

    public boolean claimChunk(Town town, Chunk chunk) {
        if (town.getChunks().size() >= town.getChunkLimit()) {
            return false;
        }
        if (isChunkWithinBuffer(chunk, town)) {
            return false;
        }
        ChunkKey key = new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        boolean added = town.getChunks().add(key);
        if (added) {
            save();
            plugin.getMapIntegration().refreshAll();
        }
        return added;
    }

    public boolean isChunkClaimed(Chunk chunk) {
        ChunkKey key = new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        for (Town town : towns.values()) {
            if (town.getChunks().contains(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean isChunkWithinBuffer(Chunk chunk, Town requester) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        String world = chunk.getWorld().getName();
        for (Town town : towns.values()) {
            if (town.equals(requester)) {
                continue;
            }
            for (ChunkKey key : town.getChunks()) {
                if (!key.getWorld().equals(world)) {
                    continue;
                }
                if (Math.abs(key.getX() - chunkX) <= 1 && Math.abs(key.getZ() - chunkZ) <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isTownAlly(Town town, Town other) {
        return town.getAllies().contains(other.getName());
    }

    public void addAlly(Town town, Town ally) {
        town.getAllies().add(ally.getName());
        save();
    }

    public void removeAlly(Town town, Town ally) {
        town.getAllies().remove(ally.getName());
        save();
    }

    public void reload() {
        plugin.reloadConfig();
        load();
        plugin.getMapIntegration().refreshAll();
    }

    public boolean isAutoClaiming(UUID playerId) {
        return autoClaiming.contains(playerId);
    }

    public boolean toggleAutoClaim(UUID playerId) {
        if (autoClaiming.remove(playerId)) {
            return false;
        }
        autoClaiming.add(playerId);
        return true;
    }

    public void stopAutoClaim(UUID playerId) {
        autoClaiming.remove(playerId);
    }

    public Set<String> getTownNames() {
        Set<String> names = new HashSet<>();
        for (Town town : towns.values()) {
            names.add(town.getName());
        }
        return names;
    }

    public Collection<Town> getTowns() {
        return new ArrayList<>(towns.values());
    }
}
