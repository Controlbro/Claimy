package com.controlbro.claimy.gui;

import com.controlbro.claimy.model.ChunkKey;
import com.controlbro.claimy.model.Town;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TownBorderRenderer {
    private TownBorderRenderer() {
    }

    public static void render(Player player, Town town) {
        World world = player.getWorld();
        int playerChunkX = player.getLocation().getChunk().getX();
        int playerChunkZ = player.getLocation().getChunk().getZ();
        String worldName = world.getName();
        for (ChunkKey key : town.getChunks()) {
            if (!key.getWorld().equals(worldName)) {
                continue;
            }
            if (Math.abs(key.getX() - playerChunkX) > 4 || Math.abs(key.getZ() - playerChunkZ) > 4) {
                continue;
            }
            renderEdges(player, world, town, key);
        }
    }

    public static void renderChunk(Player player, Town town, ChunkKey key) {
        World world = player.getWorld();
        if (!key.getWorld().equals(world.getName())) {
            return;
        }
        renderEdges(player, world, town, key);
    }

    private static void renderEdges(Player player, World world, Town town, ChunkKey key) {
        int minX = key.getX() << 4;
        int minZ = key.getZ() << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int maxY = world.getMaxHeight();
        String worldName = key.getWorld();
        if (!isClaimed(town, worldName, key.getX(), key.getZ() - 1)) {
            for (int x = minX; x <= maxX; x++) {
                spawnColumn(world, x, minZ, maxY, player);
            }
        }
        if (!isClaimed(town, worldName, key.getX(), key.getZ() + 1)) {
            for (int x = minX; x <= maxX; x++) {
                spawnColumn(world, x, maxZ, maxY, player);
            }
        }
        if (!isClaimed(town, worldName, key.getX() - 1, key.getZ())) {
            for (int z = minZ; z <= maxZ; z++) {
                spawnColumn(world, minX, z, maxY, player);
            }
        }
        if (!isClaimed(town, worldName, key.getX() + 1, key.getZ())) {
            for (int z = minZ; z <= maxZ; z++) {
                spawnColumn(world, maxX, z, maxY, player);
            }
        }
    }

    private static boolean isClaimed(Town town, String world, int x, int z) {
        return town.getChunks().contains(new ChunkKey(world, x, z));
    }

    private static void spawnColumn(World world, int x, int z, int maxY, Player player) {
        int y = world.getHighestBlockYAt(x, z) + 1;
        int step = 4;
        for (int currentY = y; currentY <= maxY; currentY += step) {
            Location location = new Location(world, x + 0.5, currentY, z + 0.5);
            player.spawnParticle(Particle.DUST, location, 4, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(org.bukkit.Color.LIME, 1.5f));
        }
    }
}
