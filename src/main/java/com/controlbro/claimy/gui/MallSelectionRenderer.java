package com.controlbro.claimy.gui;

import com.controlbro.claimy.model.Region;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class MallSelectionRenderer {
    private MallSelectionRenderer() {
    }

    public static void render(Player player, Location primary, Location secondary) {
        if (primary == null) {
            return;
        }
        if (secondary == null || !primary.getWorld().equals(secondary.getWorld())) {
            spawnColumn(player, primary.getWorld(), primary.getBlockX(), primary.getBlockY(), primary.getBlockZ(),
                    primary.getWorld().getMaxHeight());
            return;
        }
        Region region = new Region(
                primary.getWorld().getName(),
                primary.getBlockX(),
                primary.getBlockY(),
                primary.getBlockZ(),
                secondary.getBlockX(),
                secondary.getBlockY(),
                secondary.getBlockZ()
        );
        World world = primary.getWorld();
        int minX = region.getMinX();
        int maxX = region.getMaxX();
        int minZ = region.getMinZ();
        int maxZ = region.getMaxZ();
        int minY = region.getMinY();
        int maxY = region.getMaxY();
        for (int x = minX; x <= maxX; x++) {
            spawnColumn(player, world, x, minY, minZ, maxY);
            spawnColumn(player, world, x, minY, maxZ, maxY);
        }
        for (int z = minZ; z <= maxZ; z++) {
            spawnColumn(player, world, minX, minY, z, maxY);
            spawnColumn(player, world, maxX, minY, z, maxY);
        }
    }

    private static void spawnColumn(Player player, World world, int x, int startY, int z, int endY) {
        int step = 3;
        int maxY = Math.max(startY, endY);
        int minY = Math.min(startY, endY);
        for (int y = minY; y <= maxY; y += step) {
            Location location = new Location(world, x + 0.5, y + 0.5, z + 0.5);
            player.spawnParticle(Particle.DUST, location, 2, 0.15, 0.15, 0.15, 0,
                    new Particle.DustOptions(Color.AQUA, 1.2f));
        }
    }
}
