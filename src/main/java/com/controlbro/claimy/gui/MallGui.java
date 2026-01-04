package com.controlbro.claimy.gui;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MallGui implements Listener {
    private final ClaimyPlugin plugin;
    private YamlConfiguration guiConfig;
    private final Map<UUID, Integer> openPlots = new HashMap<>();
    private final Map<UUID, PendingEmployeeAction> pendingActions = new HashMap<>();

    public MallGui(ClaimyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void openConfig(Player player, int plotId) {
        ConfigurationSection section = guiConfig.getConfigurationSection("mall-config");
        if (section == null) {
            player.sendMessage("Mall config GUI is missing from gui.yml.");
            return;
        }
        String title = MessageUtil.color(section.getString("title", "Mall Plot"));
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
                meta.setDisplayName(MessageUtil.color(itemSection.getString("name", key)));
                List<String> lore = new ArrayList<>();
                for (String line : itemSection.getStringList("lore")) {
                    lore.add(MessageUtil.color(replaceEmployees(plotId, line)));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
                inventory.setItem(slot, stack);
            }
        }
        openPlots.put(player.getUniqueId(), plotId);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ConfigurationSection section = guiConfig.getConfigurationSection("mall-config");
        if (section == null) {
            return;
        }
        String title = MessageUtil.color(section.getString("title", "Mall Plot"));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        Integer plotId = openPlots.get(player.getUniqueId());
        if (plotId == null) {
            return;
        }
        ConfigurationSection items = section.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        int slot = event.getSlot();
        for (String key : items.getKeys(false)) {
            ConfigurationSection itemSection = items.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }
            int itemSlot = itemSection.getInt("slot");
            if (slot != itemSlot) {
                continue;
            }
            if (key.equalsIgnoreCase("add-employee")) {
                pendingActions.put(player.getUniqueId(), new PendingEmployeeAction(plotId, EmployeeAction.ADD));
                player.closeInventory();
                player.sendMessage("Type the player name to add as an employee (or type 'cancel').");
                return;
            }
            if (key.equalsIgnoreCase("remove-employee")) {
                pendingActions.put(player.getUniqueId(), new PendingEmployeeAction(plotId, EmployeeAction.REMOVE));
                player.closeInventory();
                player.sendMessage("Type the player name to remove from employees (or type 'cancel').");
                return;
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        PendingEmployeeAction action = pendingActions.remove(event.getPlayer().getUniqueId());
        if (action == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            event.getPlayer().sendMessage("Mall employee update cancelled.");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> handleEmployeeAction(event.getPlayer(), action, message));
    }

    private void handleEmployeeAction(Player player, PendingEmployeeAction action, String playerName) {
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(action.plotId);
        if (owner.isEmpty() || !owner.get().equals(player.getUniqueId())) {
            player.sendMessage("You do not own that mall plot.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target.getName() == null) {
            player.sendMessage("Player not found.");
            return;
        }
        UUID targetId = target.getUniqueId();
        if (action.action == EmployeeAction.ADD) {
            if (plugin.getMallManager().addEmployee(action.plotId, targetId)) {
                player.sendMessage("Added " + target.getName() + " as an employee.");
            } else {
                player.sendMessage("Unable to add that employee.");
            }
        } else {
            if (plugin.getMallManager().removeEmployee(action.plotId, targetId)) {
                player.sendMessage("Removed " + target.getName() + " from employees.");
            } else {
                player.sendMessage("Unable to remove that employee.");
            }
        }
        openConfig(player, action.plotId);
    }

    private String replaceEmployees(int plotId, String line) {
        if (!line.contains("{employees}")) {
            return line;
        }
        List<String> names = new ArrayList<>();
        for (UUID employeeId : plugin.getMallManager().getPlotEmployees(plotId)) {
            OfflinePlayer employee = Bukkit.getOfflinePlayer(employeeId);
            if (employee.getName() != null) {
                names.add(employee.getName());
            } else {
                names.add(employeeId.toString());
            }
        }
        String value = names.isEmpty() ? "None" : String.join(", ", names);
        return line.replace("{employees}", value);
    }

    private record PendingEmployeeAction(int plotId, EmployeeAction action) {
    }

    private enum EmployeeAction {
        ADD,
        REMOVE
    }
}
