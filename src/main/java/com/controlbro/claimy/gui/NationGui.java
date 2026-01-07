package com.controlbro.claimy.gui;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Nation;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.Bukkit;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class NationGui implements Listener {
    private final ClaimyPlugin plugin;
    private YamlConfiguration guiConfig;

    public NationGui(ClaimyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void openMain(Player player) {
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        Optional<Nation> nationOptional = townOptional.flatMap(plugin.getNationManager()::getNationForTown)
                .or(() -> plugin.getNationManager().getNationByOwner(player.getUniqueId()));
        if (nationOptional.isEmpty()) {
            player.sendMessage("You are not part of a nation.");
            return;
        }
        Nation nation = nationOptional.get();
        ConfigurationSection section = guiConfig.getConfigurationSection("nation-menu");
        if (section == null) {
            player.sendMessage("Nation menu is missing from gui.yml.");
            return;
        }
        String title = MessageUtil.color(section.getString("title", "Nation"));
        int size = section.getInt("size", 27);
        Inventory inventory = Bukkit.createInventory(player, size, title);
        ConfigurationSection items = section.getConfigurationSection("items");
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
                meta.setDisplayName(MessageUtil.color(replacePlaceholders(itemSection.getString("name", key), nation)));
                List<String> lore = new ArrayList<>();
                for (String line : itemSection.getStringList("lore")) {
                    if (line.contains("{towns}")) {
                        lore.addAll(buildTownLore(nation));
                        continue;
                    }
                    if (line.contains("{log}")) {
                        lore.addAll(buildLogLore(player, nation));
                        continue;
                    }
                    lore.add(MessageUtil.color(replacePlaceholders(line, nation)));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
                inventory.setItem(slot, stack);
            }
        }
        player.openInventory(inventory);
    }

    private List<String> buildTownLore(Nation nation) {
        List<String> lines = new ArrayList<>();
        for (var townId : nation.getMemberTowns()) {
            Optional<Town> townOptional = plugin.getTownManager().getTownById(townId);
            if (townOptional.isEmpty()) {
                continue;
            }
            String display = townOptional.get().getDisplayName();
            if (nation.getCapitalTownId().equals(townId)) {
                display = display + " (Capital)";
            }
            lines.add(MessageUtil.color("&7- " + display));
        }
        if (lines.isEmpty()) {
            lines.add(MessageUtil.color("&7No towns"));
        }
        return lines;
    }

    private List<String> buildLogLore(Player player, Nation nation) {
        if (!canViewLog(player, nation)) {
            return List.of(MessageUtil.color("&7Activity log is visible to mayors."));
        }
        List<String> lines = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
        List<Nation.NationLogEntry> log = nation.getActivityLog();
        int start = Math.max(0, log.size() - 5);
        for (int i = start; i < log.size(); i++) {
            Nation.NationLogEntry entry = log.get(i);
            String time = formatter.format(Instant.ofEpochMilli(entry.timestamp()));
            lines.add(MessageUtil.color("&7[" + time + "] " + entry.message()));
        }
        if (lines.isEmpty()) {
            lines.add(MessageUtil.color("&7No activity yet"));
        }
        return lines;
    }

    private String replacePlaceholders(String input, Nation nation) {
        if (input == null) {
            return "";
        }
        String capital = plugin.getTownManager().getTownById(nation.getCapitalTownId())
                .map(Town::getDisplayName)
                .orElse("Unknown");
        return input
                .replace("{nation}", nation.getName())
                .replace("{capital}", capital)
                .replace("{town-count}", String.valueOf(nation.getMemberTowns().size()));
    }

    private boolean canViewLog(Player player, Nation nation) {
        if (nation.getOwner().equals(player.getUniqueId())) {
            return true;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        return townOptional.map(town -> town.getOwner().equals(player.getUniqueId())).orElse(false);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ConfigurationSection section = guiConfig.getConfigurationSection("nation-menu");
        if (section == null) {
            return;
        }
        String title = MessageUtil.color(section.getString("title", "Nation"));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        String name = event.getCurrentItem().getItemMeta() == null
                ? ""
                : event.getCurrentItem().getItemMeta().getDisplayName().toLowerCase(Locale.ROOT);
        if (name.contains("invite")) {
            player.sendMessage("Use /nation invite <town> to invite a town.");
        } else if (name.contains("remove")) {
            player.sendMessage("Use /nation remove <town> to remove a town.");
        }
    }
}
