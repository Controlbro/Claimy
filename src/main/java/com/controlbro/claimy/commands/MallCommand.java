package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.util.MapColorUtil;
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
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/mall claim <id> | /mall config <id> | /mall color <color> | /mall employee");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "claim" -> handleClaim(player, args);
            case "config" -> handleConfig(player, args);
            case "color" -> handleColor(player, args);
            case "employee" -> handleEmployee(player, args);
            default -> sender.sendMessage("/mall claim <id> | /mall config <id> | /mall color <color> | /mall employee");
        }
        return true;
    }

    private void handleClaim(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/mall claim <id>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid plot id.");
            return;
        }
        if (plugin.getConfig().getBoolean("settings.mall.require-town-for-mall-claim")) {
            if (plugin.getTownManager().getTown(player.getUniqueId()).isEmpty()) {
                player.sendMessage("You must be in a town to claim a mall plot.");
                return;
            }
        }
        boolean claimed = plugin.getMallManager().claimPlot(id, player.getUniqueId());
        if (claimed) {
            player.sendMessage("Mall plot " + id + " claimed.");
            playSuccess(player);
        } else {
            player.sendMessage("Mall plot cannot be claimed.");
        }
    }

    private void handleConfig(Player player, String[] args) {
        Optional<Integer> plotId = Optional.empty();
        if (args.length >= 2) {
            try {
                plotId = Optional.of(Integer.parseInt(args[1]));
            } catch (NumberFormatException ex) {
                player.sendMessage("Invalid plot id.");
                return;
            }
        } else {
            plotId = plugin.getMallManager().getPlotAt(player.getLocation());
        }
        if (plotId.isEmpty()) {
            player.sendMessage("You must be in a mall plot or specify an id.");
            return;
        }
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(plotId.get());
        if (owner.isEmpty() || !owner.get().equals(player.getUniqueId())) {
            player.sendMessage("You do not own that mall plot.");
            return;
        }
        plugin.getMallGui().openConfig(player, plotId.get());
        playSuccess(player);
    }

    private void handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/mall color <color>");
            return;
        }
        Optional<Integer> plotId = plugin.getMallManager().getPlotAt(player.getLocation());
        if (plotId.isEmpty()) {
            player.sendMessage("You must be standing in your mall plot.");
            return;
        }
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(plotId.get());
        if (owner.isEmpty() || !owner.get().equals(player.getUniqueId())) {
            player.sendMessage("You do not own this mall plot.");
            return;
        }
        String colorInput = args[1];
        Optional<String> normalized = MapColorUtil.normalizeColorName(colorInput);
        if (normalized.isEmpty()) {
            player.sendMessage("Invalid color name.");
            return;
        }
        plugin.getMallManager().setPlotColor(plotId.get(), normalized.get());
        player.sendMessage("Mall plot color set to " + normalized.get() + ".");
        playSuccess(player);
    }

    private void handleEmployee(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/mall employee <add|remove|accept|deny> <player>");
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
        if (args.length < 3) {
            player.sendMessage("/mall employee <add|remove> <player>");
            return;
        }
        Optional<Integer> plotId = plugin.getMallManager().getPlotAt(player.getLocation());
        if (plotId.isEmpty()) {
            player.sendMessage("You must be standing in your mall plot.");
            return;
        }
        Optional<UUID> owner = plugin.getMallManager().getPlotOwner(plotId.get());
        if (owner.isEmpty() || !owner.get().equals(player.getUniqueId())) {
            player.sendMessage("You do not own this mall plot.");
            return;
        }
        String targetName = args[2];
        OfflinePlayer target = player.getServer().getOfflinePlayer(targetName);
        if (target.getName() == null) {
            player.sendMessage("Player not found.");
            return;
        }
        UUID targetId = target.getUniqueId();
        boolean updated;
        if (action.equals("add")) {
            updated = plugin.getMallManager().requestEmployee(plotId.get(), targetId);
            if (updated && target.isOnline()) {
                target.getPlayer().sendMessage("You have been invited to join mall plot "
                        + plotId.get() + ". Use /mall employee accept to accept.");
            }
        } else if (action.equals("remove")) {
            updated = plugin.getMallManager().removeEmployee(plotId.get(), targetId);
        } else {
            player.sendMessage("/mall employee <add|remove> <player>");
            return;
        }
        if (updated) {
            if (action.equals("add")) {
                player.sendMessage("Employee request sent.");
            } else {
                player.sendMessage("Mall employees updated.");
            }
            playSuccess(player);
        } else {
            player.sendMessage("Unable to update employees.");
        }
    }

    private void handleEmployeeAccept(Player player) {
        Optional<Integer> request = plugin.getMallManager().getEmployeeRequest(player.getUniqueId());
        if (request.isEmpty()) {
            player.sendMessage("You have no pending employee requests.");
            return;
        }
        if (plugin.getMallManager().acceptEmployeeRequest(player.getUniqueId())) {
            player.sendMessage("Mall employee request accepted.");
            playSuccess(player);
        } else {
            player.sendMessage("Unable to accept employee request.");
        }
    }

    private void handleEmployeeDeny(Player player) {
        Optional<Integer> request = plugin.getMallManager().getEmployeeRequest(player.getUniqueId());
        if (request.isEmpty()) {
            player.sendMessage("You have no pending employee requests.");
            return;
        }
        plugin.getMallManager().denyEmployeeRequest(player.getUniqueId());
        player.sendMessage("Mall employee request denied.");
        playSuccess(player);
    }

    private void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }
}
