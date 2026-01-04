package com.controlbro.claimy.gui;

import com.controlbro.claimy.ClaimyPlugin;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TownGui implements Listener {
    private final ClaimyPlugin plugin;
    private YamlConfiguration guiConfig;

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

    public void showBorder(Player player, Town town) {
        if (!plugin.getConfig().getBoolean("settings.show-border-particles")) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> TownBorderRenderer.render(player, town));
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
            String display = event.getCurrentItem().getItemMeta() != null
                    ? event.getCurrentItem().getItemMeta().getDisplayName()
                    : "";
            if (display.contains("Create")) {
                player.sendMessage("Use /town create <name>");
            } else if (display.contains("Delete")) {
                player.sendMessage("Use /town delete");
            } else if (display.contains("Invite")) {
                player.sendMessage("Use /town invite <player>");
            } else if (display.contains("Kick")) {
                player.sendMessage("Use /town kick <player>");
            } else if (display.contains("Border")) {
                Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
                townOptional.ifPresent(town -> showBorder(player, town));
            } else if (display.contains("Flags")) {
                Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
                townOptional.ifPresent(town -> openFlags(player, town));
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
}
