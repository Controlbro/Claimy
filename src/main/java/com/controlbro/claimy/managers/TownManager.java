package com.controlbro.claimy.managers;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.gui.MallSelectionRenderer;
import com.controlbro.claimy.model.ChunkKey;
import com.controlbro.claimy.model.Region;
import com.controlbro.claimy.model.ResidentPermission;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.model.TownBuildMode;
import com.controlbro.claimy.model.TownFlag;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TownManager {
    private final ClaimyPlugin plugin;
    private final Map<String, Town> towns = new HashMap<>();
    private final Map<UUID, Town> townById = new HashMap<>();
    private final Map<UUID, String> playerTown = new HashMap<>();
    private final Map<UUID, Location> plotPrimarySelection = new HashMap<>();
    private final Map<UUID, Location> plotSecondarySelection = new HashMap<>();
    private final Map<UUID, Integer> plotSelectionTasks = new HashMap<>();
    private final Set<UUID> plotSelectionMode = new HashSet<>();
    private final Map<UUID, String> invites = new HashMap<>();
    private final Map<String, Set<String>> allyRequests = new HashMap<>();
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
        townById.clear();
        playerTown.clear();

        ConfigurationSection townsSection = config.getConfigurationSection("towns");
        if (townsSection == null) return;

        for (String name : townsSection.getKeys(false)) {
            ConfigurationSection section = townsSection.getConfigurationSection(name);
            if (section == null) continue;

            UUID id = null;
            String idValue = section.getString("id");
            if (idValue != null) {
                try {
                    id = UUID.fromString(idValue);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid town id for town " + name + ": " + idValue);
                }
            }
            if (id == null) id = UUID.randomUUID();

            UUID owner = UUID.fromString(section.getString("owner"));
            int limit = section.getInt("chunk-limit", plugin.getConfig().getInt("settings.max-chunks"));
            String displayName = section.getString("display-name", name);

            Town town = new Town(id, name, displayName, owner, limit);

            town.getResidents().clear();
            for (String resident : section.getStringList("residents")) {
                town.getResidents().add(UUID.fromString(resident));
            }

            town.getAllies().addAll(section.getStringList("allies"));

            for (String chunkString : section.getStringList("chunks")) {
                town.getChunks().add(ChunkKey.fromString(chunkString));
            }

            for (String assistant : section.getStringList("assistants")) {
                try {
                    UUID assistantId = UUID.fromString(assistant);
                    if (town.isResident(assistantId)) {
                        town.addAssistant(assistantId);
                    }
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid assistant uuid for town " + name + ": " + assistant);
                }
            }

            for (String chunkString : section.getStringList("outposts")) {
                town.addOutpostChunk(ChunkKey.fromString(chunkString));
            }

            ConfigurationSection plotsSection = section.getConfigurationSection("plots");
            if (plotsSection != null) {
                for (String key : plotsSection.getKeys(false)) {
                    int plotId;
                    try {
                        plotId = Integer.parseInt(key);
                    } catch (NumberFormatException ex) {
                        plugin.getLogger().warning("Invalid plot id for town " + name + ": " + key);
                        continue;
                    }

                    ConfigurationSection plotSection = plotsSection.getConfigurationSection(key);
                    if (plotSection == null) continue;

                    String regionValue = plotSection.getString("region");
                    if (regionValue != null) {
                        town.getPlots().put(plotId, Region.deserialize(regionValue));
                    }

                    String ownerValue = plotSection.getString("owner");
                    if (ownerValue != null) {
                        try {
                            town.getPlotOwners().put(plotId, UUID.fromString(ownerValue));
                        } catch (IllegalArgumentException ex) {
                            plugin.getLogger().warning("Invalid plot owner for town " + name + ": " + ownerValue);
                        }
                    }

                    String plotColor = plotSection.getString("color");
                    if (plotColor != null && !plotColor.isBlank()) {
                        town.getPlotColors().put(plotId, plotColor);
                    }
                }
            }

            String buildMode = section.getString("build-mode");
            if (buildMode != null) {
                try {
                    town.setBuildMode(TownBuildMode.valueOf(buildMode.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid build mode for town " + name + ": " + buildMode);
                }
            }

            ConfigurationSection flagSection = section.getConfigurationSection("flags");
            if (flagSection != null) {
                for (TownFlag flag : TownFlag.values()) {
                    town.setFlag(flag, flagSection.getBoolean(flag.name(), flag.getDefaultValue()));
                }
            } else {
                for (TownFlag flag : TownFlag.values()) {
                    town.setFlag(flag, flag.getDefaultValue());
                }
            }

            ConfigurationSection permissionSection = section.getConfigurationSection("resident-permissions");
            if (permissionSection != null) {
                for (String key : permissionSection.getKeys(false)) {
                    UUID residentId = UUID.fromString(key);
                    EnumSet<ResidentPermission> allowed = EnumSet.noneOf(ResidentPermission.class);
                    for (String perm : permissionSection.getStringList(key)) {
                        try {
                            allowed.add(ResidentPermission.valueOf(perm.toUpperCase(Locale.ROOT)));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    town.setResidentPermissions(residentId, allowed);
                }
            }

            town.setMapColor(section.getString("map-color"));

            String nationValue = section.getString("nation");
            if (nationValue != null) {
                try {
                    town.setNationId(UUID.fromString(nationValue));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid nation id for town " + name + ": " + nationValue);
                }
            }

            for (String deniedTown : section.getStringList("denied-towns")) {
                try {
                    town.getDeniedTowns().add(UUID.fromString(deniedTown));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid denied town id for town " + name + ": " + deniedTown);
                }
            }

            town.setNationJoinNotified(section.getBoolean("nation-join-notified", false));

            towns.put(name.toLowerCase(Locale.ROOT), town);
            townById.put(town.getId(), town);

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
            section.set("id", town.getId().toString());
            if (!town.getDisplayName().equals(town.getName())) {
                section.set("display-name", town.getDisplayName());
            }
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
            if (!town.getAssistants().isEmpty()) {
                List<String> assistants = new ArrayList<>();
                for (UUID assistant : town.getAssistants()) {
                    assistants.add(assistant.toString());
                }
                section.set("assistants", assistants);
            }
            if (!town.getOutpostChunks().isEmpty()) {
                List<String> outposts = new ArrayList<>();
                for (ChunkKey key : town.getOutpostChunks()) {
                    outposts.add(key.asString());
                }
                section.set("outposts", outposts);
            }
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
            if (!town.getPlots().isEmpty()) {
                ConfigurationSection plotsSection = section.createSection("plots");
                for (Map.Entry<Integer, Region> entry : town.getPlots().entrySet()) {
                    ConfigurationSection plotSection = plotsSection.createSection(String.valueOf(entry.getKey()));
                    plotSection.set("region", entry.getValue().serialize());
                    UUID owner = town.getPlotOwners().get(entry.getKey());
                    if (owner != null) {
                        plotSection.set("owner", owner.toString());
                    }
                    String plotColor = town.getPlotColors().get(entry.getKey());
                    if (plotColor != null) {
                        plotSection.set("color", plotColor);
                    }
                }
            }
            if (town.getBuildMode() != null) {
                section.set("build-mode", town.getBuildMode().name());
            }
            town.getNationId().ifPresent(nationId -> section.set("nation", nationId.toString()));
            if (!town.getDeniedTowns().isEmpty()) {
                List<String> deniedTowns = town.getDeniedTowns().stream()
                        .map(UUID::toString)
                        .toList();
                section.set("denied-towns", deniedTowns);
            }
            if (town.isNationJoinNotified()) {
                section.set("nation-join-notified", true);
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

    public Optional<Town> getTownById(UUID townId) {
        return Optional.ofNullable(townById.get(townId));
    }

    public Optional<Town> getTown(UUID playerId) {
        String townName = playerTown.get(playerId);
        return getTown(townName);
    }

    public Town createTown(String name, UUID owner) {
        Town town = new Town(UUID.randomUUID(), name, name, owner, plugin.getConfig().getInt("settings.max-chunks"));
        towns.put(name.toLowerCase(Locale.ROOT), town);
        townById.put(town.getId(), town);
        playerTown.put(owner, name.toLowerCase(Locale.ROOT));
        save();
        plugin.getMapIntegration().refreshAll();
        return town;
    }

    public void deleteTown(Town town) {
        towns.remove(town.getName().toLowerCase(Locale.ROOT));
        townById.remove(town.getId());
        plugin.getNationManager().handleTownDeleted(town);
        for (UUID resident : town.getResidents()) {
            playerTown.remove(resident);
            autoClaiming.remove(resident);
        }
        String townKey = town.getName().toLowerCase(Locale.ROOT);
        allyRequests.remove(townKey);
        for (Set<String> requests : allyRequests.values()) {
            requests.remove(townKey);
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
            town.removeAssistant(playerId);
            town.removePlotsOwnedBy(playerId);
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

    public Optional<Integer> getPlotAt(Town town, Location location) {
        for (Map.Entry<Integer, Region> entry : town.getPlots().entrySet()) {
            if (entry.getValue().contains(location)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void setPlotSelectionMode(UUID playerId, boolean enabled) {
        if (enabled) {
            plotSelectionMode.add(playerId);
        } else {
            plotSelectionMode.remove(playerId);
            clearPlotSelection(playerId);
        }
    }

    public boolean isPlotSelecting(UUID playerId) {
        return plotSelectionMode.contains(playerId);
    }

    public void setPrimaryPlotSelection(UUID playerId, Location location) {
        plotPrimarySelection.put(playerId, location);
    }

    public void setSecondaryPlotSelection(UUID playerId, Location location) {
        plotSecondarySelection.put(playerId, location);
    }

    public Optional<Location> getPrimaryPlotSelection(UUID playerId) {
        return Optional.ofNullable(plotPrimarySelection.get(playerId));
    }

    public Optional<Location> getSecondaryPlotSelection(UUID playerId) {
        return Optional.ofNullable(plotSecondarySelection.get(playerId));
    }

    public Optional<Region> buildPlotSelection(UUID playerId) {
        Location primary = plotPrimarySelection.get(playerId);
        Location secondary = plotSecondarySelection.get(playerId);
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

    public void startPlotSelectionPreview(Player player) {
        stopPlotSelectionPreview(player.getUniqueId());
        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline()) {
                stopPlotSelectionPreview(player.getUniqueId());
                return;
            }
            MallSelectionRenderer.render(player,
                    plotPrimarySelection.get(player.getUniqueId()),
                    plotSecondarySelection.get(player.getUniqueId()));
        }, 0L, 10L);
        plotSelectionTasks.put(player.getUniqueId(), taskId);
    }

    public void stopPlotSelectionPreview(UUID playerId) {
        Integer taskId = plotSelectionTasks.remove(playerId);
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }

    public void clearPlotSelection(UUID playerId) {
        plotPrimarySelection.remove(playerId);
        plotSecondarySelection.remove(playerId);
        stopPlotSelectionPreview(playerId);
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

    public boolean requestAlly(Town requester, Town target) {
        if (requester.getName().equalsIgnoreCase(target.getName())) {
            return false;
        }
        if (isTownAlly(requester, target)) {
            return false;
        }
        Set<String> requests = allyRequests.computeIfAbsent(target.getName().toLowerCase(Locale.ROOT), key -> new HashSet<>());
        return requests.add(requester.getName().toLowerCase(Locale.ROOT));
    }

    public boolean acceptAllyRequest(Town target, Town requester) {
        String targetKey = target.getName().toLowerCase(Locale.ROOT);
        Set<String> requests = allyRequests.get(targetKey);
        if (requests == null || !requests.remove(requester.getName().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (requests.isEmpty()) {
            allyRequests.remove(targetKey);
        }
        addAlly(target, requester);
        addAlly(requester, target);
        return true;
    }

    public boolean denyAllyRequest(Town target, Town requester) {
        String targetKey = target.getName().toLowerCase(Locale.ROOT);
        Set<String> requests = allyRequests.get(targetKey);
        if (requests == null || !requests.remove(requester.getName().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (requests.isEmpty()) {
            allyRequests.remove(targetKey);
        }
        return true;
    }

    public Set<String> getAllyRequests(Town town) {
        Set<String> requests = allyRequests.get(town.getName().toLowerCase(Locale.ROOT));
        if (requests == null) {
            return Set.of();
        }
        return new HashSet<>(requests);
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
