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
        for (ChunkKey key : town.getChunks()) {
            if (!key.getWorld().equals(world.getName())) {
                continue;
            }
            if (Math.abs(key.getX() - playerChunkX) > 4 || Math.abs(key.getZ() - playerChunkZ) > 4) {
                continue;
            }
            int minX = key.getX() << 4;
            int minZ = key.getZ() << 4;
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            int maxY = world.getMaxHeight();
            for (int x = minX; x <= maxX; x++) {
                spawnColumn(world, x, minZ, maxY, player);
                spawnColumn(world, x, maxZ, maxY, player);
            }
            for (int z = minZ; z <= maxZ; z++) {
                spawnColumn(world, minX, z, maxY, player);
                spawnColumn(world, maxX, z, maxY, player);
            }
        }
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
