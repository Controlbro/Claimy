package com.controlbro.claimy.gui;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.ChunkKey;
import com.controlbro.claimy.model.ResidentPermission;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.model.TownFlag;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TownGui implements Listener {
    private final ClaimyPlugin plugin;
    private YamlConfiguration guiConfig;
    private final Map<UUID, Integer> borderTasks = new HashMap<>();
    private final Map<UUID, Map<Integer, UUID>> residentSlots = new HashMap<>();
    private final Map<UUID, ResidentPermissionContext> residentPermissionContexts = new HashMap<>();

    public TownGui(ClaimyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void openMain(Player player) {
        String title = MessageUtil.color(guiConfig.getString("menu.title", "Town"));
        int size = guiConfig.getInt("menu.size", 27);
        Inventory inventory = Bukkit.createInventory(player, size, title);
        ConfigurationSection items = guiConfig.getConfigurationSection("menu.items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                int slot = itemSection.getInt("slot");
                Material material = Material.matchMaterial(itemSection.getString("material", "STONE"));
                ItemStack stack = new ItemStack(material == null ? Material.STONE : material);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(MessageUtil.color(itemSection.getString("name", key)));
                List<String> lore = new ArrayList<>();
                for (String line : itemSection.getStringList("lore")) {
                    lore.add(MessageUtil.color(line));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
                inventory.setItem(slot, stack);
            }
        }
        player.openInventory(inventory);
    }

    public void openFlags(Player player, Town town) {
        String title = MessageUtil.color(guiConfig.getString("flags.title", "Flags"));
        int size = guiConfig.getInt("flags.size", 27);
        Inventory inventory = Bukkit.createInventory(player, size, title);
        int slot = 10;
        for (TownFlag flag : TownFlag.values()) {
            boolean enabled = town.isFlagEnabled(flag);
            ItemStack stack = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(MessageUtil.color("&e" + flag.name()));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.color("&7Current: " + enabled));
            lore.add(MessageUtil.color("&7Click to toggle"));
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inventory.setItem(slot, stack);
            slot++;
        }
        player.openInventory(inventory);
    }

    public void openResidents(Player player, Town town) {
        if (!town.getOwner().equals(player.getUniqueId()) && !player.hasPermission("claimy.admin")) {
            player.sendMessage("Only the town owner can manage residents.");
            return;
        }
        ConfigurationSection section = guiConfig.getConfigurationSection("residents");
        if (section == null) {
            player.sendMessage("Resident menu is missing from gui.yml.");
            return;
        }
        String title = MessageUtil.color(section.getString("title", "Residents"));
        int size = section.getInt("size", 54);
        Inventory inventory = Bukkit.createInventory(player, size, title);
        ConfigurationSection items = section.getConfigurationSection("items");
        HashSet<Integer> reservedSlots = new HashSet<>();
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                int slot = itemSection.getInt("slot");
                reservedSlots.add(slot);
                Material material = Material.matchMaterial(itemSection.getString("material", "STONE"));
                ItemStack stack = new ItemStack(material == null ? Material.STONE : material);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(MessageUtil.color(itemSection.getString("name", key)));
                List<String> lore = new ArrayList<>();
                for (String line : itemSection.getStringList("lore")) {
                    lore.add(MessageUtil.color(line));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
                inventory.setItem(slot, stack);
            }
        }
        Map<Integer, UUID> slots = new HashMap<>();
        List<UUID> residents = new ArrayList<>(town.getResidents());
        residents.sort((a, b) -> {
            String nameA = Optional.ofNullable(Bukkit.getOfflinePlayer(a).getName()).orElse(a.toString());
            String nameB = Optional.ofNullable(Bukkit.getOfflinePlayer(b).getName()).orElse(b.toString());
            return nameA.compareToIgnoreCase(nameB);
        });
        int slot = 0;
        for (UUID residentId : residents) {
            while (slot < size && reservedSlots.contains(slot)) {
                slot++;
            }
            if (slot >= size) {
                break;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(residentId));
            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(residentId).getName()).orElse("Unknown");
            meta.setDisplayName(MessageUtil.color("&e" + name));
            meta.setLore(List.of(
                    MessageUtil.color("&7Click to edit permissions.")
            ));
            head.setItemMeta(meta);
            inventory.setItem(slot, head);
            slots.put(slot, residentId);
            slot++;
        }
        residentSlots.put(player.getUniqueId(), slots);
        player.openInventory(inventory);
    }

    public void openResidentPermissions(Player player, Town town, UUID residentId) {
        if (!town.getOwner().equals(player.getUniqueId()) && !player.hasPermission("claimy.admin")) {
            player.sendMessage("Only the town owner can manage residents.");
            return;
        }
        ConfigurationSection section = guiConfig.getConfigurationSection("resident-permissions");
        if (section == null) {
            player.sendMessage("Resident permissions menu is missing from gui.yml.");
            return;
        }
        String residentName = Optional.ofNullable(Bukkit.getOfflinePlayer(residentId).getName()).orElse("Resident");
        String title = MessageUtil.color(section.getString("title", "Resident Permissions")
                .replace("{resident}", residentName));
        int size = section.getInt("size", 27);
        Inventory inventory = Bukkit.createInventory(player, size, title);
        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                int itemSlot = itemSection.getInt("slot");
                Material material = Material.matchMaterial(itemSection.getString("material", "STONE"));
                ItemStack stack = new ItemStack(material == null ? Material.STONE : material);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(MessageUtil.color(itemSection.getString("name", key)));
                List<String> lore = new ArrayList<>();
                Optional<ResidentPermission> permission = parsePermissionKey(key);
                String status = "N/A";
                if (permission.isPresent()) {
                    boolean allowed = town.isResidentPermissionEnabled(residentId, permission.get());
                    status = allowed ? "Allowed" : "Denied";
                }
                for (String line : itemSection.getStringList("lore")) {
                    lore.add(MessageUtil.color(line.replace("{status}", status)));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
                inventory.setItem(itemSlot, stack);
            }
        }
        residentPermissionContexts.put(player.getUniqueId(),
                new ResidentPermissionContext(town.getName(), residentId));
        player.openInventory(inventory);
    }

    public void showBorder(Player player, Town town) {
        if (!plugin.getConfig().getBoolean("settings.show-border-particles")) {
            return;
        }
        int repeats = 12;
        int interval = 10;
        int[] remaining = {repeats};
        int[] taskIdHolder = new int[1];
        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || remaining[0] <= 0) {
                plugin.getServer().getScheduler().cancelTask(taskIdHolder[0]);
                return;
            }
            TownBorderRenderer.render(player, town);
            remaining[0]--;
        }, 0L, interval);
        taskIdHolder[0] = taskId;
    }

    public void showClaimBorder(Player player, Town town, ChunkKey chunkKey) {
        if (!plugin.getConfig().getBoolean("settings.show-border-particles")) {
            return;
        }
        int repeats = 6;
        int interval = 10;
        int[] remaining = {repeats};
        int[] taskIdHolder = new int[1];
        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || remaining[0] <= 0) {
                plugin.getServer().getScheduler().cancelTask(taskIdHolder[0]);
                return;
            }
            TownBorderRenderer.renderChunk(player, town, chunkKey);
            remaining[0]--;
        }, 0L, interval);
        taskIdHolder[0] = taskId;
    }

    public boolean toggleBorderStay(Player player, Town town) {
        if (borderTasks.containsKey(player.getUniqueId())) {
            plugin.getServer().getScheduler().cancelTask(borderTasks.remove(player.getUniqueId()));
            return false;
        }
        int interval = 20;
        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline()) {
                plugin.getServer().getScheduler().cancelTask(borderTasks.remove(player.getUniqueId()));
                return;
            }
            TownBorderRenderer.render(player, town);
        }, 0L, interval);
        borderTasks.put(player.getUniqueId(), taskId);
        return true;
    }

    public void stopBorderStay(UUID playerId) {
        Integer taskId = borderTasks.remove(playerId);
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (title.equals(MessageUtil.color(guiConfig.getString("menu.title", "Town")))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            ConfigurationSection items = guiConfig.getConfigurationSection("menu.items");
            if (items == null) {
                return;
            }
            int slot = event.getSlot();
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                if (itemSection.getInt("slot") != slot) {
                    continue;
                }
                switch (key.toLowerCase(Locale.ROOT)) {
                    case "create" -> player.sendMessage("Use /town create <name>");
                    case "delete" -> player.sendMessage("Use /town delete");
                    case "invite" -> player.sendMessage("Use /town invite <player>");
                    case "kick" -> player.sendMessage("Use /town kick <player>");
                    case "border" -> plugin.getTownManager().getTown(player.getUniqueId())
                            .ifPresent(town -> showBorder(player, town));
                    case "flags" -> plugin.getTownManager().getTown(player.getUniqueId())
                            .ifPresent(town -> openFlags(player, town));
                    case "ally" -> player.sendMessage("Use /town ally <town>");
                    case "residents" -> plugin.getTownManager().getTown(player.getUniqueId())
                            .ifPresent(town -> openResidents(player, town));
                    default -> {
                    }
                }
                return;
            }
        }
        if (title.equals(MessageUtil.color(guiConfig.getString("residents.title", "Residents")))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            ConfigurationSection section = guiConfig.getConfigurationSection("residents");
            if (section == null) {
                return;
            }
            ConfigurationSection items = section.getConfigurationSection("items");
            int slot = event.getSlot();
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    ConfigurationSection itemSection = items.getConfigurationSection(key);
                    if (itemSection == null) {
                        continue;
                    }
                    if (itemSection.getInt("slot") == slot && key.equalsIgnoreCase("back")) {
                        openMain(player);
                        return;
                    }
                }
            }
            Map<Integer, UUID> slots = residentSlots.get(player.getUniqueId());
            if (slots == null) {
                return;
            }
            UUID residentId = slots.get(slot);
            if (residentId == null) {
                return;
            }
            plugin.getTownManager().getTown(player.getUniqueId())
                    .ifPresent(town -> openResidentPermissions(player, town, residentId));
        }
        ConfigurationSection residentPermissionsSection = guiConfig.getConfigurationSection("resident-permissions");
        String residentTitlePrefix = "Resident Permissions";
        if (residentPermissionsSection != null) {
            residentTitlePrefix = residentPermissionsSection.getString("title", "Resident Permissions")
                    .replace("{resident}", "");
        }
        if (title.startsWith(MessageUtil.color(residentTitlePrefix))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            ResidentPermissionContext context = residentPermissionContexts.get(player.getUniqueId());
            if (context == null) {
                return;
            }
            Optional<Town> townOptional = plugin.getTownManager().getTown(context.townName);
            if (townOptional.isEmpty()) {
                return;
            }
            Town town = townOptional.get();
            ConfigurationSection items = residentPermissionsSection != null
                    ? residentPermissionsSection.getConfigurationSection("items")
                    : null;
            if (items == null) {
                return;
            }
            int slot = event.getSlot();
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                if (itemSection.getInt("slot") != slot) {
                    continue;
                }
                if (key.equalsIgnoreCase("back")) {
                    openResidents(player, town);
                    return;
                }
                Optional<ResidentPermission> permission = parsePermissionKey(key);
                if (permission.isPresent()) {
                    boolean allowed = town.isResidentPermissionEnabled(context.residentId, permission.get());
                    town.setResidentPermission(context.residentId, permission.get(), !allowed);
                    plugin.getTownManager().save();
                    openResidentPermissions(player, town, context.residentId);
                }
                return;
            }
        }
        if (title.equals(MessageUtil.color(guiConfig.getString("flags.title", "Flags")))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
            if (townOptional.isEmpty()) {
                return;
            }
            Town town = townOptional.get();
            String displayName = event.getCurrentItem().getItemMeta() != null
                    ? event.getCurrentItem().getItemMeta().getDisplayName()
                    : "";
            String flagName = ChatColor.stripColor(displayName);
            try {
                TownFlag flag = TownFlag.valueOf(flagName.toUpperCase(Locale.ROOT));
                town.setFlag(flag, !town.isFlagEnabled(flag));
                plugin.getTownManager().save();
                openFlags(player, town);
            } catch (IllegalArgumentException ex) {
                // ignore
            }
        }
    }

    private Optional<ResidentPermission> parsePermissionKey(String key) {
        try {
            return Optional.of(ResidentPermission.valueOf(key.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private record ResidentPermissionContext(String townName, UUID residentId) {
    }
}
