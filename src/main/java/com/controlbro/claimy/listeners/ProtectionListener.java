package com.controlbro.claimy.listeners;

import org.bukkit.Chunk;
import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.ChunkKey;
import com.controlbro.claimy.model.Nation;
import com.controlbro.claimy.model.ResidentPermission;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.model.TownBuildMode;
import com.controlbro.claimy.model.TownFlag;
import com.controlbro.claimy.util.ActionBarUtil;
import com.controlbro.claimy.util.ChatColorUtil;
import com.controlbro.claimy.util.MapColorUtil;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;

import java.util.*;
import java.util.Objects;

public class ProtectionListener implements Listener {
    private final ClaimyPlugin plugin;
    private final Map<UUID, Long> warningCooldowns = new HashMap<>();
    private final Map<UUID, String> activeClaimKey = new HashMap<>();
    private final Map<UUID, String> activeClaimName = new HashMap<>();
    private final Map<UUID, Integer> activeClaimColor = new HashMap<>();
    private final Map<UUID, Integer> claimDisplayTasks = new HashMap<>();
    private final Map<UUID, String> activePlotKey = new HashMap<>();

    public ProtectionListener(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (canBuild(event.getPlayer(), event.getBlock())) {
            return;
        }
        event.setCancelled(true);
        MessageUtil.send(plugin, event.getPlayer(), "claim-denied");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (canBuild(event.getPlayer(), event.getBlock())) {
            warnIfOutside(event.getPlayer(), event.getBlock());
            return;
        }
        event.setCancelled(true);
        MessageUtil.send(plugin, event.getPlayer(), "claim-denied");
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlockClicked())) {
            event.setCancelled(true);
            MessageUtil.send(plugin, event.getPlayer(), "claim-denied");
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlockClicked())) {
            event.setCancelled(true);
            MessageUtil.send(plugin, event.getPlayer(), "claim-denied");
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (!block.getType().hasGravity()) {
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return;
        }
        Town town = townOptional.get();
        if (!town.isFlagEnabled(TownFlag.GRAVITY_BLOCKS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.GOLDEN_SHOVEL && event.getClickedBlock() != null) {
            if (player.hasPermission("claimy.admin")) {
                handleMallSelection(player, event.getClickedBlock(), event.getAction());
                event.setCancelled(true);
                return;
            }
            if (plugin.getTownManager().isPlotSelecting(player.getUniqueId())) {
                handleTownPlotSelection(player, event.getClickedBlock(), event.getAction());
                event.setCancelled(true);
                return;
            }
            handleClaimTool(player, event.getClickedBlock());
            event.setCancelled(true);
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        Material type = block.getType();
        if (isContainer(type)) {
            if (!canAccessContainer(player, block)) {
                event.setCancelled(true);
                MessageUtil.send(plugin, player, "claim-denied");
            }
            return;
        }
        if (type == Material.BEDROCK) {
            return;
        }
        if (isBed(type)) {
            if (!canUseBeds(player, block)) {
                event.setCancelled(true);
                MessageUtil.send(plugin, player, "claim-denied");
            }
        }
        if (isRedstoneControl(type)) {
            if (!canUseRedstone(player, block)) {
                event.setCancelled(true);
                MessageUtil.send(plugin, player, "claim-denied");
            }
        }
        if (isDoor(type)) {
            if (!canUseDoor(player, block)) {
                event.setCancelled(true);
                MessageUtil.send(plugin, player, "claim-denied");
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.getTownManager().isAutoClaiming(player.getUniqueId())) {
            // fall through to claim display updates
        } else if (!event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            attemptClaim(player, event.getTo().getChunk(), event.getTo(), false);
        }
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            updateClaimDisplay(player, event.getTo());
            updatePlotDisplay(player, event.getTo());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            updateClaimDisplay(player, player.getLocation());
            updatePlotDisplay(player, player.getLocation());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getMallManager().stopSelectionPreview(event.getPlayer().getUniqueId());
        plugin.getTownManager().stopPlotSelectionPreview(event.getPlayer().getUniqueId());
        plugin.getTownManager().setPlotSelectionMode(event.getPlayer().getUniqueId(), false);
        plugin.getTownGui().stopBorderStay(event.getPlayer().getUniqueId());
        stopClaimDisplay(event.getPlayer().getUniqueId(), event.getPlayer());
        stopPlotDisplay(event.getPlayer().getUniqueId(), event.getPlayer());
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (entity.getType() == EntityType.VILLAGER) {
            if (!canUseVillagers(player, entity)) {
                event.setCancelled(true);
            }
        }
        if (entity instanceof Animals) {
            if (!canUseDoorsVillagers(player, entity.getLocation().getBlock())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Tameable tameable && tameable.getOwner() != null) {
            if (plugin.getConfig().getBoolean("settings.protect-pets.enabled")
                    && plugin.getConfig().getStringList("settings.protect-pets.worlds").contains(entity.getWorld().getName())) {
                if (event.getDamager() instanceof Player player) {
                    if (!player.getUniqueId().equals(tameable.getOwner().getUniqueId()) && !player.hasPermission("claimy.admin")) {
                        event.setCancelled(true);
                        return;
                    }
                } else {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (entity instanceof Animals || entity instanceof Creeper || entity.getType() == EntityType.VILLAGER) {
            Player attacker = null;
            if (event.getDamager() instanceof Player player) {
                attacker = player;
            } else if (event.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof Player shooter) {
                attacker = shooter;
            }
            if (attacker != null && !attacker.hasPermission("claimy.admin")) {
                Optional<Town> townOptional = plugin.getTownManager().getTownAt(entity.getLocation());
                if (townOptional.isPresent() && !townOptional.get().isResident(attacker.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
        if (event.getDamager() instanceof Creeper) {
            if (plugin.getTownManager().getTownAt(entity.getLocation()).isPresent()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Enderman) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof FallingBlock) {
            if (plugin.getTownManager().getTownAt(event.getBlock().getLocation()).isPresent()
                    || plugin.getMallManager().isInMall(event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getBlock().getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        int seaLevel = event.getLocation().getWorld().getSeaLevel();
        if (plugin.getConfig().getBoolean("settings.disable-explosions-above-sea-level")
                && event.getLocation().getY() > seaLevel) {
            event.setCancelled(true);
            return;
        }
        event.blockList().clear();
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        for (var blockState : event.getBlocks()) {
            Block block = blockState.getBlock();
            if (!canBuild(player, block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (plugin.getTownManager().getTownAt(event.getToBlock().getLocation()).isPresent()
                && plugin.getTownManager().getTownAt(event.getBlock().getLocation()).isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isMovingAcrossClaims(block, block.getRelative(event.getDirection()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isMovingAcrossClaims(block, block.getRelative(event.getDirection()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!canBuild(event.getPlayer(), event.getEntity().getLocation().getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            if (!canBuild(player, event.getEntity().getLocation().getBlock())) {
                event.setCancelled(true);
            }
        }
    }

    private void handleClaimTool(Player player, Block block) {
        if (player.hasPermission("claimy.admin")) {
            return;
        }
        attemptClaim(player, block.getChunk(), block.getLocation(), true);
    }

    private boolean attemptClaim(Player player, Chunk chunk, Location location, boolean notify) {
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            if (notify) {
                MessageUtil.send(plugin, player, "no-town");
            }
            return false;
        }
        Town town = townOptional.get();
        if (plugin.getMallManager().isInMall(location)) {
            if (notify) {
                player.sendMessage("You cannot claim a town chunk inside the mall.");
            }
            return false;
        }
        if (plugin.getTownManager().isChunkClaimed(chunk)) {
            if (notify) {
                player.sendMessage("Chunk already claimed.");
            }
            return false;
        }
        if (plugin.getTownManager().isChunkWithinBuffer(chunk, town)) {
            if (notify) {
                player.sendMessage("You must leave a 1 chunk buffer between towns.");
            }
            return false;
        }
        if (plugin.getTownManager().claimChunk(town, chunk)) {
            if (notify) {
                player.sendMessage("Chunk claimed.");
            }
            playSuccess(player);
            plugin.getTownGui().showClaimBorder(player, town,
                    new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
            return true;
        }
        if (notify) {
            player.sendMessage("You have reached your chunk limit.");
        }
        return false;
    }

    private boolean canBuild(Player player, Block block) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        if (plugin.getMallManager().isInMall(block.getLocation())) {
            return plugin.getMallManager().isMallMember(block.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.getOwner().equals(player.getUniqueId()) || town.isAssistant(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (isDeniedByTown(town, playerTown)) {
            return false;
        }
        Optional<Integer> plotId = plugin.getTownManager().getPlotAt(town, block.getLocation());
        Optional<UUID> plotOwner = plotId.flatMap(town::getPlotOwner);
        if (plotOwner.isPresent()) {
            return plotOwner.get().equals(player.getUniqueId());
        }
        if (isNationTrustedResident(town, playerTown)) {
            return true;
        }
        if (town.isResident(player.getUniqueId())) {
            if (town.getBuildMode() == TownBuildMode.PLOT_ONLY) {
                return false;
            }
            return town.isResidentPermissionEnabled(player.getUniqueId(), ResidentPermission.BUILD);
        }
        if (isNationOwnerAllowed(town, player.getUniqueId())) {
            return true;
        }
        if (playerTown.isPresent()) {
            Town ownTown = playerTown.get();
            if (plugin.getTownManager().isTownAlly(town, ownTown)
                    && town.isFlagEnabled(TownFlag.ALLOW_ALLY_BUILD)
                    && town.getBuildMode() == TownBuildMode.OPEN_TOWN
                    && plotOwner.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean canAccessContainer(Player player, Block block) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        if (plugin.getMallManager().isInMall(block.getLocation())) {
            return plugin.getMallManager().isMallMember(block.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.getOwner().equals(player.getUniqueId()) || town.isAssistant(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (isDeniedByTown(town, playerTown)) {
            return false;
        }
        if (isNationTrustedResident(town, playerTown)) {
            return true;
        }
        if (town.isResident(player.getUniqueId())) {
            return town.isResidentPermissionEnabled(player.getUniqueId(), ResidentPermission.CONTAINERS);
        }
        if (isNationOwnerAllowed(town, player.getUniqueId())) {
            return true;
        }
        if (playerTown.isPresent()) {
            Town ownTown = playerTown.get();
            if (plugin.getTownManager().isTownAlly(town, ownTown) && town.isFlagEnabled(TownFlag.ALLOW_ALLY_CONTAINERS)) {
                return true;
            }
        }
        return false;
    }

    private boolean canUseDoorsVillagers(Player player, Block block) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        if (plugin.getMallManager().isInMall(block.getLocation())) {
            return plugin.getMallManager().isMallMember(block.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.getOwner().equals(player.getUniqueId()) || town.isAssistant(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (isDeniedByTown(town, playerTown)) {
            return false;
        }
        if (isNationTrustedResident(town, playerTown)) {
            return true;
        }
        if (town.isResident(player.getUniqueId())) {
            return town.isResidentPermissionEnabled(player.getUniqueId(), ResidentPermission.DOORS);
        }
        if (isNationOwnerAllowed(town, player.getUniqueId())) {
            return true;
        }
        if (playerTown.isPresent()) {
            Town ownTown = playerTown.get();
            if (plugin.getTownManager().isTownAlly(town, ownTown) && town.isFlagEnabled(TownFlag.ALLOW_ALLY_DOORS)) {
                return true;
            }
        }
        return false;
    }

    private boolean canUseBeds(Player player, Block block) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        if (plugin.getMallManager().isInMall(block.getLocation())) {
            return plugin.getMallManager().isMallMember(block.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.getOwner().equals(player.getUniqueId()) || town.isAssistant(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (isDeniedByTown(town, playerTown)) {
            return false;
        }
        if (town.isResident(player.getUniqueId())) {
            return town.isResidentPermissionEnabled(player.getUniqueId(), ResidentPermission.BEDS);
        }
        return isNationOwnerAllowed(town, player.getUniqueId());
    }

    private boolean canUseRedstone(Player player, Block block) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        if (plugin.getMallManager().isInMall(block.getLocation())) {
            return plugin.getMallManager().isMallMember(block.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.getOwner().equals(player.getUniqueId()) || town.isAssistant(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (isDeniedByTown(town, playerTown)) {
            return false;
        }
        if (isNationTrustedResident(town, playerTown)) {
            return true;
        }
        if (town.isResident(player.getUniqueId())) {
            return town.isResidentPermissionEnabled(player.getUniqueId(), ResidentPermission.REDSTONE);
        }
        if (isNationOwnerAllowed(town, player.getUniqueId())) {
            return true;
        }
        if (playerTown.isPresent()) {
            Town ownTown = playerTown.get();
            if (plugin.getTownManager().isTownAlly(town, ownTown) && town.isFlagEnabled(TownFlag.ALLOW_ALLY_REDSTONE)) {
                return true;
            }
        }
        return false;
    }

    private boolean canUseDoor(Player player, Block block) {
        return canUseDoorsVillagers(player, block);
    }

    private boolean canUseVillagers(Player player, Entity entity) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        if (plugin.getMallManager().isInMall(entity.getLocation())) {
            return plugin.getMallManager().isMallMember(entity.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(entity.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.getOwner().equals(player.getUniqueId()) || town.isAssistant(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (isDeniedByTown(town, playerTown)) {
            return false;
        }
        if (town.isResident(player.getUniqueId())) {
            return town.isResidentPermissionEnabled(player.getUniqueId(), ResidentPermission.VILLAGERS);
        }
        if (isNationOwnerAllowed(town, player.getUniqueId())) {
            return true;
        }
        if (playerTown.isPresent()) {
            Town ownTown = playerTown.get();
            if (plugin.getTownManager().isTownAlly(town, ownTown) && town.isFlagEnabled(TownFlag.ALLOW_ALLY_VILLAGERS)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDeniedByTown(Town town, Optional<Town> playerTown) {
        if (playerTown.isEmpty()) {
            return false;
        }
        Town residentTown = playerTown.get();
        if (residentTown.getId().equals(town.getId())) {
            return false;
        }
        return town.getDeniedTowns().contains(residentTown.getId());
    }

    private boolean isNationTrustedResident(Town town, Optional<Town> playerTown) {
        if (playerTown.isEmpty()) {
            return false;
        }
        Town residentTown = playerTown.get();
        if (residentTown.getId().equals(town.getId())) {
            return false;
        }
        Optional<UUID> townNation = town.getNationId();
        Optional<UUID> residentNation = residentTown.getNationId();
        return townNation.isPresent() && townNation.equals(residentNation);
    }

    private boolean isNationOwnerAllowed(Town town, UUID playerId) {
        Optional<UUID> nationId = town.getNationId();
        if (nationId.isEmpty()) {
            return false;
        }
        return plugin.getNationManager()
                .getNation(nationId.get())
                .map(nation -> nation.getOwner().equals(playerId))
                .orElse(false);
    }

    private boolean isContainer(Material type) {
        return type == Material.CHEST
                || type == Material.TRAPPED_CHEST
                || type == Material.BARREL
                || type == Material.SHULKER_BOX
                || type.name().endsWith("SHULKER_BOX")
                || type == Material.FURNACE
                || type == Material.BLAST_FURNACE
                || type == Material.SMOKER
                || type == Material.HOPPER
                || type == Material.DROPPER
                || type == Material.DISPENSER
                || type == Material.BREWING_STAND
                || type == Material.CRAFTER
                || type == Material.ENDER_CHEST;
    }

    private boolean isBed(Material type) {
        return type.name().endsWith("_BED");
    }

    private boolean isDoor(Material type) {
        return type.name().endsWith("_DOOR")
                || type == Material.IRON_DOOR
                || type.name().endsWith("_TRAPDOOR")
                || type.name().endsWith("_FENCE_GATE");
    }

    private boolean isRedstoneControl(Material type) {
        return type == Material.LEVER
                || type.name().endsWith("_BUTTON")
                || type == Material.REPEATER
                || type == Material.COMPARATOR;
    }

    private boolean isMovingAcrossClaims(Block source, Block destination) {
        Optional<Town> sourceTown = plugin.getTownManager().getTownAt(source.getLocation());
        Optional<Town> destTown = plugin.getTownManager().getTownAt(destination.getLocation());
        if (sourceTown.isPresent() && destTown.isPresent()) {
            return !sourceTown.get().getName().equalsIgnoreCase(destTown.get().getName());
        }
        return sourceTown.isPresent() || destTown.isPresent();
    }

    private void warnIfOutside(Player player, Block block) {
        if (plugin.getTownManager().getTown(player.getUniqueId()).isEmpty()) {
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isPresent()) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("settings.warning-cooldown-seconds") * 1000L;
        long last = warningCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldown) {
            return;
        }
        warningCooldowns.put(player.getUniqueId(), now);
        MessageUtil.send(plugin, player, "claim-warning");
        plugin.getTownManager().getTown(player.getUniqueId())
                .ifPresent(town -> plugin.getTownGui().showBorder(player, town));
    }

    private void handleMallSelection(Player player, Block block, Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK) {
            plugin.getMallManager().setPrimarySelection(player.getUniqueId(), block.getLocation());
            player.sendMessage("Mall region primary corner set.");
            playSuccess(player);
            plugin.getMallManager().startSelectionPreview(player);
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            plugin.getMallManager().setSecondarySelection(player.getUniqueId(), block.getLocation());
            player.sendMessage("Mall region secondary corner set.");
            playSuccess(player);
            plugin.getMallManager().startSelectionPreview(player);
        }
    }

    private void handleTownPlotSelection(Player player, Block block, Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK) {
            plugin.getTownManager().setPrimaryPlotSelection(player.getUniqueId(), block.getLocation());
            player.sendMessage("Plot primary corner set.");
            playSuccess(player);
            plugin.getTownManager().startPlotSelectionPreview(player);
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            plugin.getTownManager().setSecondaryPlotSelection(player.getUniqueId(), block.getLocation());
            player.sendMessage("Plot secondary corner set.");
            playSuccess(player);
            plugin.getTownManager().startPlotSelectionPreview(player);
        }
    }

    private void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }

    private void updateClaimDisplay(Player player, Location location) {
        Optional<ClaimDisplay> display = buildClaimDisplay(location);
        String nextKey = display.map(ClaimDisplay::key).orElse(null);
        String currentKey = activeClaimKey.get(player.getUniqueId());
        if (Objects.equals(currentKey, nextKey)) {
            return;
        }
        String previousName = activeClaimName.get(player.getUniqueId());
        Integer previousColor = activeClaimColor.get(player.getUniqueId());
        if (nextKey == null) {
            if (previousName != null) {
                sendClaimMessage(player, "Exiting " + previousName, previousColor);
                scheduleClaimClear(player.getUniqueId(), player, 60L);
            } else {
                stopClaimDisplay(player.getUniqueId(), player);
            }
            activeClaimKey.remove(player.getUniqueId());
            activeClaimName.remove(player.getUniqueId());
            activeClaimColor.remove(player.getUniqueId());
            return;
        }
        activeClaimKey.put(player.getUniqueId(), nextKey);
        activeClaimName.put(player.getUniqueId(), display.get().name());
        activeClaimColor.put(player.getUniqueId(), display.get().color());
        sendClaimMessage(player, "Entering " + display.get().name(), display.get().color());
        scheduleClaimClear(player.getUniqueId(), player, 600L);
        maybeSendNationInfo(player, location);
    }

    private void sendClaimMessage(Player player, String message, Integer color) {
        UUID playerId = player.getUniqueId();
        String colored = color == null ? message : ChatColorUtil.colorize(color, message);
        ActionBarUtil.send(player, colored);
    }

    private void scheduleClaimClear(UUID playerId, Player player, long delayTicks) {
        Integer taskId = claimDisplayTasks.remove(playerId);
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        int newTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (!player.isOnline()) {
                stopClaimDisplay(playerId, player);
                return;
            }
            ActionBarUtil.send(player, "");
            stopClaimDisplay(playerId, player);
        }, delayTicks);
        claimDisplayTasks.put(playerId, newTaskId);
    }

    private void stopClaimDisplay(UUID playerId, Player player) {
        Integer taskId = claimDisplayTasks.remove(playerId);
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        activeClaimKey.remove(playerId);
        activeClaimName.remove(playerId);
        activeClaimColor.remove(playerId);
        if (player.isOnline()) {
            ActionBarUtil.send(player, "");
        }
    }

    private void updatePlotDisplay(Player player, Location location) {
        Optional<PlotDisplay> display = buildPlotDisplay(location);
        String nextKey = display.map(PlotDisplay::key).orElse(null);
        String currentKey = activePlotKey.get(player.getUniqueId());
        if (Objects.equals(currentKey, nextKey)) {
            return;
        }
        if (nextKey == null) {
            activePlotKey.remove(player.getUniqueId());
            ActionBarUtil.send(player, "");
            return;
        }
        activePlotKey.put(player.getUniqueId(), nextKey);
        ActionBarUtil.send(player, display.get().message());
    }

    private void stopPlotDisplay(UUID playerId, Player player) {
        activePlotKey.remove(playerId);
        if (player.isOnline()) {
            ActionBarUtil.send(player, "");
        }
    }

    private Optional<ClaimDisplay> buildClaimDisplay(Location location) {
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(location);
        if (townOptional.isEmpty()) {
            return Optional.empty();
        }
        Town town = townOptional.get();
        ChunkKey chunkKey = new ChunkKey(location.getWorld().getName(), location.getChunk().getX(),
                location.getChunk().getZ());
        boolean isOutpost = town.getOutpostChunks().contains(chunkKey);
        String townName = town.getDisplayName() + (isOutpost ? " (Outpost)" : "");
        String color = town.getMapColor();
        if (color == null) {
            color = plugin.getConfig().getString("settings.squaremap.town-default-color", "#00FF00");
        }
        int rgb = MapColorUtil.parseColor(color).orElse(0x00FF00);
        String key = "town:" + town.getName().toLowerCase(Locale.ROOT) + ":" + town.getDisplayName()
                + ":outpost:" + isOutpost;
        return Optional.of(new ClaimDisplay(key, townName, rgb));
    }

    private record ClaimDisplay(String key, String name, int color) {
    }

    private Optional<PlotDisplay> buildPlotDisplay(Location location) {
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(location);
        if (townOptional.isEmpty()) {
            return Optional.empty();
        }
        Town town = townOptional.get();
        Optional<Integer> plotId = plugin.getTownManager().getPlotAt(town, location);
        if (plotId.isEmpty()) {
            return Optional.empty();
        }
        Optional<UUID> ownerId = town.getPlotOwner(plotId.get());
        String ownerName = ownerId
                .map(id -> Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse("Unknown"))
                .orElse("Unclaimed");
        String color = town.getPlotColors().get(plotId.get());
        if (color == null) {
            color = town.getMapColor();
        }
        if (color == null) {
            color = plugin.getConfig().getString("settings.squaremap.town-default-color", "#00FF00");
        }
        int rgb = MapColorUtil.parseColor(color).orElse(0x00FF00);
        String message = ChatColorUtil.colorize(rgb, "Plot: " + plotId.get() + " | Owner: " + ownerName);
        String key = "plot:" + town.getId() + ":" + plotId.get();
        return Optional.of(new PlotDisplay(key, message));
    }

    private void maybeSendNationInfo(Player player, Location location) {
        if (plugin.getPlayerDataManager().hasSeenNationInfo(player.getUniqueId())) {
            return;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (playerTown.isEmpty()) {
            return;
        }
        Optional<Town> claimTown = plugin.getTownManager().getTownAt(location);
        if (claimTown.isEmpty()) {
            return;
        }
        Optional<Nation> nation = plugin.getNationManager().getNationForTown(claimTown.get());
        if (nation.isEmpty()) {
            return;
        }
        player.sendMessage("You are in a nation-controlled area.");
        player.sendMessage("Towns in the same nation share build access.");
        plugin.getPlayerDataManager().setSeenNationInfo(player.getUniqueId());
    }

    private record PlotDisplay(String key, String message) {
    }
}
