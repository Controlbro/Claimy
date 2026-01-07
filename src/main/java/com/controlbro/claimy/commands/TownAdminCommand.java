package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

public class TownAdminCommand implements CommandExecutor {
    private final ClaimyPlugin plugin;

    public TownAdminCommand(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("claimy.admin")) {
            MessageUtil.sendPrefixed(plugin, sender, "No permission.");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendPrefixed(plugin, sender, "Use /townadmin reload.");
            MessageUtil.sendPrefixed(plugin, sender, "Mall admin commands moved to /mall.");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getTownManager().reload();
            plugin.getMallManager().load();
            plugin.getTownGui().reload();
            MessageUtil.sendPrefixed(plugin, sender, "Claimy reloaded.");
            if (sender instanceof Player player) {
                playSuccess(player);
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("mall") || args[0].equalsIgnoreCase("mallunclaim")) {
            MessageUtil.sendPrefixed(plugin, sender, "Mall admin commands moved to /mall.");
            return true;
        }
        return true;
    }

    private void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }
}
