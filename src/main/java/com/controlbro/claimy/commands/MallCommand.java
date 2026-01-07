package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Region;
import com.controlbro.claimy.util.MapColorUtil;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class MallCommand implements CommandExecutor {
    private final ClaimyPlugin plugin;

    public MallCommand(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendPrefixed(plugin, sender, "Players only.");
            return true;
        }
        if (args.length == 0) {
            openAuto(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "createplot" -> handleCreatePlot(player, args);
            case "clearplot" -> handleClearPlot(player, args);
            case "setowner" -> handleSetOwner(player, args);
            case "unclaim" -> handleUnclaim(player, args);
            case "claim" -> handleClaim(player, args);
            case "config" -> handleConfig(player, args);
            case "color" -> handleColor(player, args);
            case "employee" -> handleEmployee(player, args);
            default -> sendUsage(player);
        }
        return true;
    }

    private void openAuto(Player player) {
        Optional<Integer> ownedPlot = plugin.getMallManager().getPlotOwnedBy(player.getUniqueId());
        if (ownedPlot.isPresent()) {
            plugin.getMallGui().openConfig(player, ownedPlot.get());
            playSuccess(player);
            return;
        }
        Optional<Integer> plotAt = plugin.getMallManager().getPlotAt(player.getLocation());
        if (plotAt.isPresent() && plugin.getMallManager().isPlotEmployee(plotAt.get(), player.getUniqueId())) {
            plugin.getMallGui().openEmployee(player, plotAt.get());
            playSuccess(player);
            return;
        }
        sendUsage(player);
    }

    private void sendUsage(Player player) {
        MessageUtil.sendPrefixed(plugin, player, "/mall claim <id> | /mall config <id> | /mall color <color> | /mall employee");
        if (player.hasPermission("claimy.admin")) {
            MessageUtil.sendPrefixed(plugin, player,
                    "/mall createplot <id> | /mall clearplot <id> | /mall setowner <id> <player> | /mall unclaim <player>");
        }
    }

    private boolean requireAdmin(Player player) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        MessageUtil.sendPrefixed(plugin, player, "No permission.");
        return false;
    }

    private void handleCreatePlot(Player player, String[] args) {
        if (!requireAdmin(player)) {
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendPrefixed(plugin, player, "/mall createplot <id>");
            MessageUtil.sendPrefixed(plugin, player, "Use a golden shovel to select mall region corners.");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            MessageUtil.sendPrefixed(plugin, player, "Invalid plot id.");
            return;
        }
        Region region = plugin.getMallManager().buildSelection(player.getUniqueId()).orElse(null);
        if (region == null) {
            MessageUtil.sendPrefixed(plugin, player, "Select two corners with the golden shovel first.");
            return;
        }
        plugin.getMallManager().definePlot(id, region);
        plugin.getMallManager().clearSelection(player.getUniqueId());
        MessageUtil.sendPrefixed(plugin, player, "Mall plot " + id + " created.");
        playSuccess(player);
    }

    private void handleClearPlot(Player player, String[] args) {
        if (!requireAdmin(player)) {
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendPrefixed(plugin, player, "/mall clearplot <id>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            MessageUtil.sendPrefixed(plugin, player, "Invalid plot id.");
            return;
        }
        plugin.getMallManager().clearPlotOwner(id);
        MessageUtil.sendPrefixed(plugin, player, "Mall plot " + id + " cleared.");
        playSuccess(player);
    }

    private void handleSetOwner(Player player, String[] args) {
        if (!requireAdmin(player)) {
            return;
        }
        if (args.length < 3) {
            MessageUtil.sendPrefixed(plugin, player, "/mall setowner <id> <player>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            MessageUtil.sendPrefixed(plugin, player, "Invalid plot id.");
            return;
        }
        OfflinePlayer target = player.getServer().getOfflinePlayer(args[2]);
        if (target.getName() == null) {
            MessageUtil.sendPrefixed(plugin, player, "Player not found.");
            return;
        }
        if (!plugin.getMallManager().setPlotOwner(id, target.getUniqueId())) {
            MessageUtil.sendPrefixed(plugin, player, "Mall plot not found.");
            return;
        }
        MessageUtil.sendPrefixed(plugin, player, "Mall plot " + id + " owner set to " + target.getName() + ".");
        playSuccess(player);
    }

    private void handleUnclaim(Player player, String[] args) {
        if (!requireAdmin(player)) {
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendPrefixed(plugin, player, "/mall unclaim <player>");
            return;
        }
        OfflinePlayer target = player.getServer().getOfflinePlayer(args[1]);
        if (target.getName() == null) {
            MessageUtil.sendPrefixed(plugin, player, "Player not found.");
            return;
        }
        int cleared = plugin.getMallManager().clearPlotsOwnedBy(target.getUniqueId());
        int removed = plugin.getMallManager().removeEmployeeFromAllPlots(target.getUniqueId());
        MessageUtil.sendPrefixed(plugin, player,
                "Cleared " + cleared + " mall plots and removed " + removed + " employee entries.");
        playSuccess(player);
    }

    private void handleClaim(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendPrefixed(plugin, player, "/mall claim <id>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            MessageUtil.sendPrefixed(plugin, player, "Invalid plot id.");
            return;
        }
        if (plugin.getConfig().getBoolean("settings.mall.require-town-for-mall-claim")) {
            if (plugin.getTownManager().getTown(player.getUniqueId()).isEmpty()) {
                MessageUtil.sendPrefixed(plugin, player, "You must be in a town to claim a mall plot.");
                return;
            }
        }
        boolean claimed = plugin.getMallManager().claimPlot(id, player.getUniqueId());
        if (claimed) {
            MessageUtil.sendPrefixed(plugin, player, "Mall plot " + id + " claimed.");
            playSuccess(player);
        } else {
            MessageUtil.sendPrefixed(plugin, player, "Mall plot cannot be claimed.");
        }
    }

    private void handleConfig(Player player, String[] args) {
        Optional<Integer> plotId = Optional.empty();
        if (args.length >= 2) {
            try {
                plotId = Optional.of(Integer.parseInt(args[1]));
            } catch (NumberFormatException ex) {
                MessageUtil.sendPrefixed(plugin, player, "Invalid plot id.");
                return;
            }
        } else {
            plotId = plugin.getMallManager().getPlotAt(player.getLocation());
        }
        if (plotId.isEmpty()) {
            MessageUtil.sendPrefixed(plugin, player, "You must be in a mall plot or specify an id.");
            return;
        }
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(plotId.get());
        if (owner.isPresent() && owner.get().equals(player.getUniqueId())) {
            plugin.getMallGui().openConfig(player, plotId.get());
            playSuccess(player);
            return;
        }
        if (plugin.getMallManager().isPlotEmployee(plotId.get(), player.getUniqueId())) {
            plugin.getMallGui().openEmployee(player, plotId.get());
            playSuccess(player);
            return;
        }
        MessageUtil.sendPrefixed(plugin, player, "You do not have access to that mall plot.");
    }

    private void handleColor(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendPrefixed(plugin, player, "/mall color <color>");
            return;
        }
        Optional<Integer> plotId = plugin.getMallManager().getPlotAt(player.getLocation());
        if (plotId.isEmpty()) {
            MessageUtil.sendPrefixed(plugin, player, "You must be standing in your mall plot.");
            return;
        }
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(plotId.get());
        if (owner.isEmpty() || !owner.get().equals(player.getUniqueId())) {
            MessageUtil.sendPrefixed(plugin, player, "You do not own this mall plot.");
            return;
        }
        String colorInput = args[1];
        Optional<String> normalized = MapColorUtil.normalizeColorName(colorInput);
        if (normalized.isEmpty()) {
            MessageUtil.sendPrefixed(plugin, player, "Invalid color name.");
            return;
        }
        plugin.getMallManager().setPlotColor(plotId.get(), normalized.get());
        MessageUtil.sendPrefixed(plugin, player, "Mall plot color set to " + normalized.get() + ".");
        playSuccess(player);
    }

    private void handleEmployee(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendPrefixed(plugin, player, "/mall employee <add|remove|accept|deny|quit> <player>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("accept")) {
            handleEmployeeAccept(player);
            return;
        }
        if (action.equals("deny")) {
            handleEmployeeDeny(player);
            return;
        }
        if (action.equals("quit")) {
            handleEmployeeQuit(player);
            return;
        }
        if (args.length < 3) {
            MessageUtil.sendPrefixed(plugin, player, "/mall employee <add|remove> <player>");
            return;
        }
        Optional<Integer> plotId = plugin.getMallManager().getPlotAt(player.getLocation());
        if (plotId.isEmpty()) {
            MessageUtil.sendPrefixed(plugin, player, "You must be standing in your mall plot.");
            return;
        }
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(plotId.get());
        if (owner.isEmpty() || !owner.get().equals(player.getUniqueId())) {
            MessageUtil.sendPrefixed(plugin, player, "You do not own this mall plot.");
            return;
        }
        String targetName = args[2];
        OfflinePlayer target = player.getServer().getOfflinePlayer(targetName);
        if (target.getName() == null) {
            MessageUtil.sendPrefixed(plugin, player, "Player not found.");
            return;
        }
        UUID targetId = target.getUniqueId();
        boolean updated;
        if (action.equals("add")) {
            updated = plugin.getMallManager().requestEmployee(plotId.get(), targetId);
            if (updated && target.isOnline()) {
                MessageUtil.sendPrefixed(plugin, target.getPlayer(), "You have been invited to join mall plot "
                        + plotId.get() + ". Use /mall employee accept to accept.");
            }
        } else if (action.equals("remove")) {
            updated = plugin.getMallManager().removeEmployee(plotId.get(), targetId);
        } else {
            MessageUtil.sendPrefixed(plugin, player, "/mall employee <add|remove> <player>");
            return;
        }
        if (updated) {
            if (action.equals("add")) {
                MessageUtil.sendPrefixed(plugin, player, "Employee request sent.");
            } else {
                MessageUtil.sendPrefixed(plugin, player, "Mall employees updated.");
            }
            playSuccess(player);
        } else {
            MessageUtil.sendPrefixed(plugin, player, "Unable to update employees.");
        }
    }

    private void handleEmployeeAccept(Player player) {
        Optional<Integer> request = plugin.getMallManager().getEmployeeRequest(player.getUniqueId());
        if (request.isEmpty()) {
            MessageUtil.sendPrefixed(plugin, player, "You have no pending employee requests.");
            return;
        }
        if (plugin.getMallManager().acceptEmployeeRequest(player.getUniqueId())) {
            MessageUtil.sendPrefixed(plugin, player, "Mall employee request accepted.");
            playSuccess(player);
        } else {
            MessageUtil.sendPrefixed(plugin, player, "Unable to accept employee request.");
        }
    }

    private void handleEmployeeDeny(Player player) {
        Optional<Integer> request = plugin.getMallManager().getEmployeeRequest(player.getUniqueId());
        if (request.isEmpty()) {
            MessageUtil.sendPrefixed(plugin, player, "You have no pending employee requests.");
            return;
        }
        plugin.getMallManager().denyEmployeeRequest(player.getUniqueId());
        MessageUtil.sendPrefixed(plugin, player, "Mall employee request denied.");
        playSuccess(player);
    }

    private void handleEmployeeQuit(Player player) {
        int removed = plugin.getMallManager().removeEmployeeFromAllPlots(player.getUniqueId());
        if (removed > 0) {
            MessageUtil.sendPrefixed(plugin, player, "You have left your mall store.");
            playSuccess(player);
        } else {
            MessageUtil.sendPrefixed(plugin, player, "You are not an employee of any mall plot.");
        }
    }

    private void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }
}
