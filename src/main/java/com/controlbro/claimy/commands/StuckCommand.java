package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.util.Optional;

public class StuckCommand implements CommandExecutor {
    private final ClaimyPlugin plugin;

    public StuckCommand(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(player.getLocation());
        if (townOptional.isEmpty()) {
            player.sendMessage("You are not inside a claim.");
            return true;
        }
        Location location = findOutside(player.getLocation());
        if (location == null) {
            player.sendMessage("No safe location found.");
            return true;
        }
        player.teleport(location);
        MessageUtil.send(plugin, player, "stuck-teleported");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        return true;
    }

    private Location findOutside(Location location) {
        World world = location.getWorld();
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();
        int blockX = location.getBlockX();
        int blockZ = location.getBlockZ();
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int outsideX = blockX - minX < maxX - blockX ? minX - 1 : maxX + 1;
        int outsideZ = blockZ - minZ < maxZ - blockZ ? minZ - 1 : maxZ + 1;
        Location candidate = new Location(world, outsideX + 0.5, location.getY(), blockZ + 0.5);
        if (world.getChunkAt(candidate).isLoaded()) {
            candidate.setY(world.getHighestBlockYAt(candidate) + 1);
            return candidate;
        }
        Location candidateZ = new Location(world, blockX + 0.5, location.getY(), outsideZ + 0.5);
        candidateZ.setY(world.getHighestBlockYAt(candidateZ) + 1);
        return candidateZ;
    }
}
