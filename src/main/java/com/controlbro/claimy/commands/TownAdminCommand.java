package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Region;
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
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/townadmin reload | /townadmin mall setplot <id> | /townadmin mall clear <id>");
            sender.sendMessage("Use a golden shovel to select mall region corners.");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getTownManager().reload();
            plugin.getMallManager().load();
            plugin.getTownGui().reload();
            sender.sendMessage("Claimy reloaded.");
            if (sender instanceof Player player) {
                playSuccess(player);
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("mall")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("/townadmin mall setplot <id> | /townadmin mall clear <id>");
                sender.sendMessage("Use a golden shovel to select mall region corners.");
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
                Region region = plugin.getMallManager().buildSelection(player.getUniqueId()).orElse(null);
                if (region == null) {
                    sender.sendMessage("Select two corners with the golden shovel first.");
                    return true;
                }
                plugin.getMallManager().definePlot(id, region);
                sender.sendMessage("Mall plot " + id + " set to selected region.");
                playSuccess(player);
                return true;
            }
            if (action.equalsIgnoreCase("clear")) {
                plugin.getMallManager().clearPlotOwner(id);
                sender.sendMessage("Mall plot " + id + " cleared.");
                playSuccess(player);
                return true;
            }
        }
        return true;
    }

    private void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }
}
