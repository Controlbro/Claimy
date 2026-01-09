package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StopVisitCommand implements CommandExecutor {
    private final ClaimyPlugin plugin;

    public StopVisitCommand(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendPrefixed(plugin, sender, "Players only.");
            return true;
        }
        if (plugin.getTownManager().stopVisit(player, true)) {
            MessageUtil.sendPrefixed(plugin, player, "Visit ended. You can now leave the town.");
        } else {
            MessageUtil.sendPrefixed(plugin, player, "You are not currently visiting a town.");
        }
        return true;
    }
}
