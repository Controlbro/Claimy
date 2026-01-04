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
            for (int x = minX; x <= maxX; x++) {
                spawn(world, x, minZ, player);
                spawn(world, x, maxZ, player);
            }
            for (int z = minZ; z <= maxZ; z++) {
                spawn(world, minX, z, player);
                spawn(world, maxX, z, player);
            }
        }
    }

    private static void spawn(World world, int x, int z, Player player) {
        int y = world.getHighestBlockYAt(x, z) + 1;
        Location location = new Location(world, x + 0.5, y, z + 0.5);
        player.spawnParticle(Particle.DUST, location, 8, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(org.bukkit.Color.LIME, 1.5f));
    }
}
