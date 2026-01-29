package com.controlbro.claimy.gui;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.util.MapColorUtil;
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
    private final Map<UUID, Integer> openEmployeePlots = new HashMap<>();
    private final Map<UUID, Integer> openColorPlots = new HashMap<>();
    private final Map<UUID, Map<Integer, String>> colorSlots = new HashMap<>();
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
            MessageUtil.sendPrefixed(plugin, player, "Mall config GUI is missing from gui.yml.");
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
                    lore.add(MessageUtil.color(replaceMallConfigLine(plotId, line)));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
                inventory.setItem(slot, stack);
            }
        }
        openPlots.put(player.getUniqueId(), plotId);
        player.openInventory(inventory);
    }

    public void openEmployee(Player player, int plotId) {
        ConfigurationSection section = guiConfig.getConfigurationSection("mall-employee");
        if (section == null) {
            MessageUtil.sendPrefixed(plugin, player, "Mall employee GUI is missing from gui.yml.");
            return;
        }
        String title = MessageUtil.color(section.getString("title", "Mall Employee"));
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
                    lore.add(MessageUtil.color(line));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
                inventory.setItem(slot, stack);
            }
        }
        openEmployeePlots.put(player.getUniqueId(), plotId);
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
            if (!handleEmployeeInventoryClick(event, player)) {
                handleColorInventoryClick(event, player);
            }
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
                MessageUtil.sendPrefixed(plugin, player, "Type the player name to add as an employee (or type 'cancel').");
                return;
            }
            if (key.equalsIgnoreCase("color")) {
                openColors(player, plotId);
                return;
            }
            if (key.equalsIgnoreCase("remove-employee")) {
                pendingActions.put(player.getUniqueId(), new PendingEmployeeAction(plotId, EmployeeAction.REMOVE));
                player.closeInventory();
                MessageUtil.sendPrefixed(plugin, player, "Type the player name to remove from employees (or type 'cancel').");
                return;
            }
            if (key.equalsIgnoreCase("redstone-interact")) {
                boolean enabled = plugin.getMallManager().isPlotRedstoneInteractEnabled(plotId);
                plugin.getMallManager().setPlotRedstoneInteract(plotId, !enabled);
                MessageUtil.sendPrefixed(plugin, player, "Mall redstone interact set to " + (!enabled) + ".");
                openConfig(player, plotId);
                return;
            }
        }
    }

    private boolean handleEmployeeInventoryClick(InventoryClickEvent event, Player player) {
        ConfigurationSection section = guiConfig.getConfigurationSection("mall-employee");
        if (section == null) {
            return false;
        }
        String title = MessageUtil.color(section.getString("title", "Mall Employee"));
        if (!event.getView().getTitle().equals(title)) {
            return false;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return true;
        }
        Integer plotId = openEmployeePlots.get(player.getUniqueId());
        if (plotId == null) {
            return true;
        }
        ConfigurationSection items = section.getConfigurationSection("items");
        if (items == null) {
            return true;
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
            if (key.equalsIgnoreCase("quit-store")) {
                if (plugin.getMallManager().removeEmployee(plotId, player.getUniqueId())) {
                    MessageUtil.sendPrefixed(plugin, player, "You have left this mall store.");
                } else {
                    MessageUtil.sendPrefixed(plugin, player, "Unable to leave this store.");
                }
                return true;
            }
        }
        return true;
    }

    private void handleColorInventoryClick(InventoryClickEvent event, Player player) {
        ConfigurationSection section = guiConfig.getConfigurationSection("mall-colors");
        if (section == null) {
            return;
        }
        String title = MessageUtil.color(section.getString("title", "Mall Plot Colors"));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);
        Map<Integer, String> slots = colorSlots.get(player.getUniqueId());
        if (slots == null) {
            return;
        }
        String color = slots.get(event.getSlot());
        if (color == null) {
            return;
        }
        Integer plotId = openColorPlots.get(player.getUniqueId());
        if (plotId == null) {
            return;
        }
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(plotId);
        if (owner.isEmpty() || !owner.get().equals(player.getUniqueId())) {
            MessageUtil.sendPrefixed(plugin, player, "You do not own this mall plot.");
            return;
        }
        plugin.getMallManager().setPlotColor(plotId, color);
        MessageUtil.sendPrefixed(plugin, player, "Mall plot color set to " + color + ".");
        openColors(player, plotId);
    }

    public void openColors(Player player, int plotId) {
        ConfigurationSection section = guiConfig.getConfigurationSection("mall-colors");
        if (section == null) {
            MessageUtil.sendPrefixed(plugin, player, "Mall colors GUI is missing from gui.yml.");
            return;
        }
        String title = MessageUtil.color(section.getString("title", "Mall Plot Colors"));
        int size = section.getInt("size", 54);
        Inventory inventory = Bukkit.createInventory(player, size, title);
        List<String> colorNames = new ArrayList<>(MapColorUtil.getNamedColors().keySet());
        colorNames.sort(String.CASE_INSENSITIVE_ORDER);
        Map<Integer, String> slots = new HashMap<>();
        int slot = 0;
        for (String color : colorNames) {
            if (slot >= size) {
                break;
            }
            ItemStack stack = new ItemStack(MapColorUtil.getDyeMaterial(color));
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(MessageUtil.color("&e" + color));
            meta.setLore(List.of(MessageUtil.color("&7Click to set this color")));
            stack.setItemMeta(meta);
            inventory.setItem(slot, stack);
            slots.put(slot, color);
            slot++;
        }
        openColorPlots.put(player.getUniqueId(), plotId);
        colorSlots.put(player.getUniqueId(), slots);
        player.openInventory(inventory);
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
            MessageUtil.sendPrefixed(plugin, event.getPlayer(), "Mall employee update cancelled.");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> handleEmployeeAction(event.getPlayer(), action, message));
    }

    private void handleEmployeeAction(Player player, PendingEmployeeAction action, String playerName) {
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(action.plotId);
        if (owner.isEmpty() || !owner.get().equals(player.getUniqueId())) {
            MessageUtil.sendPrefixed(plugin, player, "You do not own that mall plot.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target.getName() == null) {
            MessageUtil.sendPrefixed(plugin, player, "Player not found.");
            return;
        }
        UUID targetId = target.getUniqueId();
        if (action.action == EmployeeAction.ADD) {
            if (plugin.getMallManager().requestEmployee(action.plotId, targetId)) {
                MessageUtil.sendPrefixed(plugin, player, "Employee request sent to " + target.getName() + ".");
                if (target.isOnline() && target.getPlayer() != null) {
                    MessageUtil.sendPrefixed(plugin, target.getPlayer(), "You have been invited to join mall plot "
                            + action.plotId + ". Use /mall employee accept to accept.");
                }
            } else {
                MessageUtil.sendPrefixed(plugin, player, "Unable to send that employee request.");
            }
        } else {
            if (plugin.getMallManager().removeEmployee(action.plotId, targetId)) {
                MessageUtil.sendPrefixed(plugin, player, "Removed " + target.getName() + " from employees.");
            } else {
                MessageUtil.sendPrefixed(plugin, player, "Unable to remove that employee.");
            }
        }
        openConfig(player, action.plotId);
    }

    private String replaceMallConfigLine(int plotId, String line) {
        if (line.contains("{employees}")) {
            line = line.replace("{employees}", formatEmployees(plotId));
        }
        if (line.contains("{redstone_interact}")) {
            boolean enabled = plugin.getMallManager().isPlotRedstoneInteractEnabled(plotId);
            line = line.replace("{redstone_interact}", enabled ? "&aEnabled" : "&cDisabled");
        }
        return line;
    }

    private String formatEmployees(int plotId) {
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
        return value;
    }

    private record PendingEmployeeAction(int plotId, EmployeeAction action) {
    }

    private enum EmployeeAction {
        ADD,
        REMOVE
    }
}
