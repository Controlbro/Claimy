package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.ChunkKey;
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
        Location location = findOutside(player.getLocation(), townOptional.get());
        if (location == null) {
            player.sendMessage("No safe location found.");
            return true;
        }
        player.teleport(location);
        MessageUtil.send(plugin, player, "stuck-teleported");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        return true;
    }

    private Location findOutside(Location location, Town town) {
        World world = location.getWorld();
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();
        int blockX = location.getBlockX();
        int blockZ = location.getBlockZ();
        Location best = null;
        double bestDistance = Double.MAX_VALUE;
        double baseY = location.getY();
        Location east = findOutsideInDirection(world, town, chunkX, chunkZ, blockX, blockZ, baseY, 1, 0);
        best = chooseClosest(location, best, east, bestDistance);
        if (best != null) {
            bestDistance = best.distanceSquared(location);
        }
        Location west = findOutsideInDirection(world, town, chunkX, chunkZ, blockX, blockZ, baseY, -1, 0);
        best = chooseClosest(location, best, west, bestDistance);
        if (best != null) {
            bestDistance = best.distanceSquared(location);
        }
        Location south = findOutsideInDirection(world, town, chunkX, chunkZ, blockX, blockZ, baseY, 0, 1);
        best = chooseClosest(location, best, south, bestDistance);
        if (best != null) {
            bestDistance = best.distanceSquared(location);
        }
        Location north = findOutsideInDirection(world, town, chunkX, chunkZ, blockX, blockZ, baseY, 0, -1);
        best = chooseClosest(location, best, north, bestDistance);
        return best;
    }

    private Location findOutsideInDirection(World world, Town town, int chunkX, int chunkZ, int blockX, int blockZ,
                                            double baseY, int stepX, int stepZ) {
        String worldName = world.getName();
        int currentX = chunkX;
        int currentZ = chunkZ;
        int safety = 0;
        while (town.getChunks().contains(new ChunkKey(worldName, currentX, currentZ))) {
            currentX += stepX;
            currentZ += stepZ;
            safety++;
            if (safety > 1024) {
                return null;
            }
        }
        int lastClaimedX = currentX - stepX;
        int lastClaimedZ = currentZ - stepZ;
        double outsideBlockX = blockX + 0.5;
        double outsideBlockZ = blockZ + 0.5;
        if (stepX != 0) {
            outsideBlockX = (lastClaimedX << 4) + (stepX > 0 ? 16 : -1) + 0.5;
            outsideBlockZ = blockZ + 0.5;
        } else if (stepZ != 0) {
            outsideBlockZ = (lastClaimedZ << 4) + (stepZ > 0 ? 16 : -1) + 0.5;
            outsideBlockX = blockX + 0.5;
        }
        Location candidate = new Location(world, outsideBlockX, baseY, outsideBlockZ);
        candidate.setY(world.getHighestBlockYAt(candidate) + 1);
        return candidate;
    }

    private Location chooseClosest(Location origin, Location currentBest, Location candidate, double bestDistance) {
        if (candidate == null) {
            return currentBest;
        }
        double distance = candidate.distanceSquared(origin);
        if (distance < bestDistance) {
            return candidate;
        }
        return currentBest;
    }
}
