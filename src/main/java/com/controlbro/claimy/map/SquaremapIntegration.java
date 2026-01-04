package com.controlbro.claimy.map;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.ChunkKey;
import com.controlbro.claimy.model.Region;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.util.MapColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.MultiPolygon;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SquaremapIntegration implements MapIntegration {
    private static final String TOWN_LAYER_KEY = "claimy_towns";
    private static final String MALL_LAYER_KEY = "claimy_mall";
    private final ClaimyPlugin plugin;

    public SquaremapIntegration(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void refreshAll() {
        if (!plugin.getConfig().getBoolean("settings.squaremap.enabled")) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            updateWorld(world);
        }
    }

    private void updateWorld(World world) {
        WorldIdentifier worldId = BukkitAdapter.worldIdentifier(world);
        Optional<xyz.jpenilla.squaremap.api.MapWorld> mapWorld = SquaremapProvider.get().getWorldIfEnabled(worldId);
        if (mapWorld.isEmpty()) {
            return;
        }
        updateTownLayer(mapWorld.get(), world);
        updateMallLayer(mapWorld.get(), world);
    }

    private void updateTownLayer(xyz.jpenilla.squaremap.api.MapWorld mapWorld, World world) {
        if (!plugin.getConfig().getBoolean("settings.squaremap.show-towns")) {
            unregisterLayer(mapWorld, TOWN_LAYER_KEY);
            return;
        }
        SimpleLayerProvider provider = createLayer(
                mapWorld,
                TOWN_LAYER_KEY,
                plugin.getConfig().getString("settings.squaremap.town-layer.name", "Towns"),
                plugin.getConfig().getBoolean("settings.squaremap.town-layer.default-hidden", false),
                plugin.getConfig().getInt("settings.squaremap.town-layer.z-index", 250),
                plugin.getConfig().getInt("settings.squaremap.town-layer.priority", 0),
                plugin.getConfig().getBoolean("settings.squaremap.town-layer.show-controls", true)
        );
        for (Town town : plugin.getTownManager().getTowns()) {
            List<MultiPolygon.MultiPolygonPart> parts = new ArrayList<>();
            for (ChunkKey chunk : town.getChunks()) {
                if (!chunk.getWorld().equals(world.getName())) {
                    continue;
                }
                parts.add(MultiPolygon.part(toSquarePoints(chunk.getX(), chunk.getZ())));
            }
            if (parts.isEmpty()) {
                continue;
            }
            MultiPolygon polygon = MultiPolygon.multiPolygon(parts);
            int color = resolveTownColor(town);
            polygon.markerOptions(buildOptions(town.getName(), color));
            provider.addMarker(Key.of("town_" + sanitizeKey(town.getName())), polygon);
        }
    }

    private void updateMallLayer(xyz.jpenilla.squaremap.api.MapWorld mapWorld, World world) {
        if (!plugin.getConfig().getBoolean("settings.squaremap.show-mall")) {
            unregisterLayer(mapWorld, MALL_LAYER_KEY);
            return;
        }
        SimpleLayerProvider provider = createLayer(
                mapWorld,
                MALL_LAYER_KEY,
                plugin.getConfig().getString("settings.squaremap.mall-layer.name", "Mall"),
                plugin.getConfig().getBoolean("settings.squaremap.mall-layer.default-hidden", false),
                plugin.getConfig().getInt("settings.squaremap.mall-layer.z-index", 260),
                plugin.getConfig().getInt("settings.squaremap.mall-layer.priority", 0),
                plugin.getConfig().getBoolean("settings.squaremap.mall-layer.show-controls", true)
        );
        for (var entry : plugin.getMallManager().getPlots().entrySet()) {
            int id = entry.getKey();
            Region region = entry.getValue();
            if (!region.getWorld().equals(world.getName())) {
                continue;
            }
            List<Point> points = List.of(
                    Point.of(region.getMinX(), region.getMinZ()),
                    Point.of(region.getMinX(), region.getMaxZ() + 1),
                    Point.of(region.getMaxX() + 1, region.getMaxZ() + 1),
                    Point.of(region.getMaxX() + 1, region.getMinZ())
            );
            MultiPolygon polygon = MultiPolygon.multiPolygon(List.of(MultiPolygon.part(points)));
            int color = resolveMallColor(id);
            polygon.markerOptions(buildOptions("Mall Plot " + id, color));
            provider.addMarker(Key.of("mall_" + id), polygon);
        }
    }

    private SimpleLayerProvider createLayer(
            xyz.jpenilla.squaremap.api.MapWorld mapWorld,
            String keyName,
            String name,
            boolean defaultHidden,
            int zIndex,
            int priority,
            boolean showControls
    ) {
        Key key = Key.of(keyName);
        unregisterLayer(mapWorld, keyName);
        SimpleLayerProvider provider = SimpleLayerProvider.builder(name)
                .defaultHidden(defaultHidden)
                .zIndex(zIndex)
                .layerPriority(priority)
                .showControls(showControls)
                .build();
        mapWorld.layerRegistry().register(key, provider);
        return provider;
    }

    private void unregisterLayer(xyz.jpenilla.squaremap.api.MapWorld mapWorld, String keyName) {
        Key key = Key.of(keyName);
        if (mapWorld.layerRegistry().hasEntry(key)) {
            mapWorld.layerRegistry().unregister(key);
        }
    }

    private List<Point> toSquarePoints(int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        return List.of(
                Point.of(minX, minZ),
                Point.of(minX, maxZ),
                Point.of(maxX, maxZ),
                Point.of(maxX, minZ)
        );
    }

    private MarkerOptions buildOptions(String label, int color) {
        return MarkerOptions.builder()
                .clickTooltip(label)
                .hoverTooltip(label)
                .fill(true)
                .fillColor(color)
                .fillOpacity(plugin.getConfig().getDouble("settings.squaremap.fill-opacity", 0.35))
                .stroke(true)
                .strokeColor(color)
                .strokeOpacity(plugin.getConfig().getDouble("settings.squaremap.stroke-opacity", 0.8))
                .strokeWeight(plugin.getConfig().getInt("settings.squaremap.stroke-weight", 2))
                .build();
    }

    private String sanitizeKey(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
    }

    private int resolveTownColor(Town town) {
        String color = town.getMapColor();
        if (color == null) {
            color = plugin.getConfig().getString("settings.squaremap.town-default-color", "#00FF00");
        }
        return MapColorUtil.parseColor(color).orElse(0x00FF00);
    }

    private int resolveMallColor(int plotId) {
        String color = plugin.getMallManager().getPlotColor(plotId)
                .orElse(plugin.getConfig().getString("settings.squaremap.mall-default-color", "#FFAA00"));
        return MapColorUtil.parseColor(color).orElse(0xFFAA00);
    }
}
