package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TownAdminCommand implements CommandExecutor {
    private final ClaimyPlugin plugin;

    public TownAdminCommand(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("claimy.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/townadmin reload | /townadmin mall setplot <id> | /townadmin mall clear <id>");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getTownManager().reload();
            plugin.getMallManager().load();
            plugin.getTownGui().reload();
            sender.sendMessage("Claimy reloaded.");
            return true;
        }
        if (args[0].equalsIgnoreCase("mall")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("/townadmin mall setplot <id> | /townadmin mall clear <id>");
                return true;
            }
            String action = args[1];
            int id;
            try {
                id = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("Invalid plot id.");
                return true;
            }
            if (action.equalsIgnoreCase("setplot")) {
                plugin.getMallManager().definePlot(id, player.getLocation().getChunk());
                sender.sendMessage("Mall plot " + id + " set to current chunk.");
                return true;
            }
            if (action.equalsIgnoreCase("clear")) {
                plugin.getMallManager().clearPlotOwner(id);
                sender.sendMessage("Mall plot " + id + " cleared.");
                return true;
            }
        }
        return true;
    }
}
