package com.controlbro.claimy.managers;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.gui.MallSelectionRenderer;
import com.controlbro.claimy.model.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MallManager {
    private final ClaimyPlugin plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<Integer, Region> plots = new HashMap<>();
    private final Map<Integer, UUID> plotOwners = new HashMap<>();
    private final Map<Integer, Set<UUID>> plotEmployees = new HashMap<>();
    private final Map<Integer, String> plotColors = new HashMap<>();
    private final Map<UUID, Integer> employeeRequests = new HashMap<>();
    private final Map<UUID, Location> selectionPrimary = new HashMap<>();
    private final Map<UUID, Location> selectionSecondary = new HashMap<>();
    private final Map<UUID, Integer> selectionTasks = new HashMap<>();

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
        plotEmployees.clear();
        plotColors.clear();
        employeeRequests.clear();
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
            plotColors.put(id, section.getString("color"));
            List<String> employees = section.getStringList("employees");
            if (!employees.isEmpty()) {
                Set<UUID> employeeSet = new HashSet<>();
                for (String employeeId : employees) {
                    employeeSet.add(UUID.fromString(employeeId));
                }
                plotEmployees.put(id, employeeSet);
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
            Set<UUID> employees = plotEmployees.get(entry.getKey());
            if (employees != null && !employees.isEmpty()) {
                List<String> employeeList = new ArrayList<>();
                for (UUID employee : employees) {
                    employeeList.add(employee.toString());
                }
                section.set("employees", employeeList);
            }
            String color = plotColors.get(entry.getKey());
            if (color != null) {
                section.set("color", color);
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
        plugin.getMapIntegration().refreshAll();
    }

    public Optional<Region> getPlotRegion(int id) {
        return Optional.ofNullable(plots.get(id));
    }

    public Optional<UUID> getPlotOwner(int id) {
        return Optional.ofNullable(plotOwners.get(id));
    }

    public Optional<Integer> getPlotOwnedBy(UUID playerId) {
        for (Map.Entry<Integer, UUID> entry : plotOwners.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public boolean claimPlot(int id, UUID playerId) {
        if (!plots.containsKey(id)) {
            return false;
        }
        if (plotOwners.containsKey(id)) {
            return false;
        }
        if (isPlotOwner(playerId)) {
            return false;
        }
        if (isEmployee(playerId)) {
            return false;
        }
        plotOwners.put(id, playerId);
        save();
        plugin.getMapIntegration().refreshAll();
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

    public boolean isMallMember(Location location, UUID playerId) {
        Optional<Integer> plotId = getPlotAt(location);
        if (plotId.isEmpty()) {
            return false;
        }
        int id = plotId.get();
        return playerId.equals(plotOwners.get(id)) || isPlotEmployee(id, playerId);
    }

    public void clearPlotOwner(int id) {
        plotOwners.remove(id);
        plotEmployees.remove(id);
        plotColors.remove(id);
        employeeRequests.values().removeIf(value -> value == id);
        save();
        plugin.getMapIntegration().refreshAll();
    }

    public boolean setPlotOwner(int id, UUID ownerId) {
        if (!plots.containsKey(id)) {
            return false;
        }
        plotOwners.put(id, ownerId);
        plotEmployees.remove(id);
        employeeRequests.values().removeIf(value -> value == id);
        save();
        plugin.getMapIntegration().refreshAll();
        return true;
    }

    public int clearPlotsOwnedBy(UUID ownerId) {
        List<Integer> ownedPlots = new ArrayList<>();
        for (Map.Entry<Integer, UUID> entry : plotOwners.entrySet()) {
            if (entry.getValue().equals(ownerId)) {
                ownedPlots.add(entry.getKey());
            }
        }
        for (int plotId : ownedPlots) {
            plotOwners.remove(plotId);
            plotEmployees.remove(plotId);
            plotColors.remove(plotId);
            employeeRequests.values().removeIf(value -> value == plotId);
        }
        if (!ownedPlots.isEmpty()) {
            save();
            plugin.getMapIntegration().refreshAll();
        }
        return ownedPlots.size();
    }

    public int removeEmployeeFromAllPlots(UUID employeeId) {
        int removed = 0;
        for (Map.Entry<Integer, Set<UUID>> entry : plotEmployees.entrySet()) {
            if (entry.getValue().remove(employeeId)) {
                removed++;
            }
        }
        employeeRequests.remove(employeeId);
        if (removed > 0) {
            save();
        }
        return removed;
    }

    public boolean addEmployee(int id, UUID employeeId) {
        if (!plots.containsKey(id)) {
            return false;
        }
        if (plotOwners.containsKey(id) && plotOwners.get(id).equals(employeeId)) {
            return false;
        }
        if (isPlotOwner(employeeId)) {
            return false;
        }
        if (isPlotEmployee(id, employeeId)) {
            return false;
        }
        Set<UUID> employees = plotEmployees.computeIfAbsent(id, key -> new HashSet<>());
        if (employees.add(employeeId)) {
            save();
            return true;
        }
        return false;
    }

    public boolean requestEmployee(int id, UUID employeeId) {
        if (!plots.containsKey(id)) {
            return false;
        }
        if (plotOwners.containsKey(id) && plotOwners.get(id).equals(employeeId)) {
            return false;
        }
        if (isPlotOwner(employeeId)) {
            return false;
        }
        if (isPlotEmployee(id, employeeId)) {
            return false;
        }
        return employeeRequests.putIfAbsent(employeeId, id) == null;
    }

    public Optional<Integer> getEmployeeRequest(UUID playerId) {
        return Optional.ofNullable(employeeRequests.get(playerId));
    }

    public boolean acceptEmployeeRequest(UUID playerId) {
        Integer plotId = employeeRequests.remove(playerId);
        if (plotId == null) {
            return false;
        }
        if (!plots.containsKey(plotId)) {
            return false;
        }
        return addEmployee(plotId, playerId);
    }

    public boolean denyEmployeeRequest(UUID playerId) {
        return employeeRequests.remove(playerId) != null;
    }

    public boolean removeEmployee(int id, UUID employeeId) {
        Set<UUID> employees = plotEmployees.get(id);
        if (employees != null && employees.remove(employeeId)) {
            save();
            return true;
        }
        return false;
    }

    public boolean isEmployee(UUID playerId) {
        for (Set<UUID> employees : plotEmployees.values()) {
            if (employees.contains(playerId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPlotOwner(UUID playerId) {
        return plotOwners.containsValue(playerId);
    }

    public boolean isPlotEmployee(int id, UUID playerId) {
        Set<UUID> employees = plotEmployees.get(id);
        return employees != null && employees.contains(playerId);
    }

    public Set<UUID> getPlotEmployees(int id) {
        return new HashSet<>(plotEmployees.getOrDefault(id, Set.of()));
    }

    public Optional<String> getPlotColor(int id) {
        return Optional.ofNullable(plotColors.get(id));
    }

    public void setPlotColor(int id, String color) {
        plotColors.put(id, color);
        save();
        plugin.getMapIntegration().refreshAll();
    }

    public void setPrimarySelection(UUID playerId, Location location) {
        selectionPrimary.put(playerId, location);
    }

    public void setSecondarySelection(UUID playerId, Location location) {
        selectionSecondary.put(playerId, location);
    }

    public void clearSelection(UUID playerId) {
        selectionPrimary.remove(playerId);
        selectionSecondary.remove(playerId);
        stopSelectionPreview(playerId);
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

    public void startSelectionPreview(Player player) {
        stopSelectionPreview(player.getUniqueId());
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Location primary = selectionPrimary.get(player.getUniqueId());
            if (primary == null) {
                stopSelectionPreview(player.getUniqueId());
                return;
            }
            Location secondary = selectionSecondary.get(player.getUniqueId());
            MallSelectionRenderer.render(player, primary, secondary);
        }, 0L, 30L);
        selectionTasks.put(player.getUniqueId(), taskId);
    }

    public void stopSelectionPreview(UUID playerId) {
        Integer taskId = selectionTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public Set<Integer> getPlotIds() {
        return new HashSet<>(plots.keySet());
    }

    public Map<Integer, Region> getPlots() {
        return new HashMap<>(plots);
    }
}
