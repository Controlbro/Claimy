package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        if (args.length < 2 || !args[0].equalsIgnoreCase("claim")) {
            sender.sendMessage("/mall claim <id>");
            return true;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid plot id.");
            return true;
        }
        if (plugin.getConfig().getBoolean("settings.mall.require-town-for-mall-claim")) {
            if (plugin.getTownManager().getTown(player.getUniqueId()).isEmpty()) {
                player.sendMessage("You must be in a town to claim a mall plot.");
                return true;
            }
        }
        boolean claimed = plugin.getMallManager().claimPlot(id, player.getUniqueId());
        if (claimed) {
            player.sendMessage("Mall plot " + id + " claimed.");
        } else {
            player.sendMessage("Mall plot cannot be claimed.");
        }
        return true;
    }
}
