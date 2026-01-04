package com.controlbro.claimy.listeners;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.model.TownFlag;
import com.controlbro.claimy.util.MessageUtil;
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
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;

import java.util.*;

public class ProtectionListener implements Listener {
    private final ClaimyPlugin plugin;
    private final Map<UUID, Long> warningCooldowns = new HashMap<>();

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
        if (event.getTo() == null || event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.getTownManager().isAutoClaiming(player.getUniqueId())) {
            return;
        }
        attemptClaim(player, event.getTo().getChunk(), event.getTo(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getMallManager().stopSelectionPreview(event.getPlayer().getUniqueId());
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
        if (entity instanceof Animals || entity instanceof Creeper) {
            if (event.getDamager() instanceof Player player) {
                if (!canUseDoorsVillagers(player, entity.getLocation().getBlock())) {
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
                playSuccess(player);
            }
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
            return plugin.getMallManager().isMallOwner(block.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.isResident(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (playerTown.isPresent()) {
            Town ownTown = playerTown.get();
            if (plugin.getTownManager().isTownAlly(town, ownTown) && town.isFlagEnabled(TownFlag.ALLOW_ALLY_BUILD)) {
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
            return plugin.getMallManager().isMallOwner(block.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.isResident(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
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
            return plugin.getMallManager().isMallOwner(block.getLocation(), player.getUniqueId());
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.isResident(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
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
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        return town.isResident(player.getUniqueId());
    }

    private boolean canUseRedstone(Player player, Block block) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(block.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.isResident(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (playerTown.isPresent()) {
            Town ownTown = playerTown.get();
            if (plugin.getTownManager().isTownAlly(town, ownTown) && town.isFlagEnabled(TownFlag.ALLOW_ALLY_REDSTONE)) {
                return true;
            }
        }
        return false;
    }

    private boolean canUseDoor(Player player, Block block) {
        if (!plugin.getConfig().getBoolean("settings.lock-doors-by-default")) {
            return true;
        }
        return canUseDoorsVillagers(player, block);
    }

    private boolean canUseVillagers(Player player, Entity entity) {
        if (player.hasPermission("claimy.admin")) {
            return true;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTownAt(entity.getLocation());
        if (townOptional.isEmpty()) {
            return true;
        }
        Town town = townOptional.get();
        if (town.isResident(player.getUniqueId())) {
            return true;
        }
        Optional<Town> playerTown = plugin.getTownManager().getTown(player.getUniqueId());
        if (playerTown.isPresent()) {
            Town ownTown = playerTown.get();
            if (plugin.getTownManager().isTownAlly(town, ownTown) && town.isFlagEnabled(TownFlag.ALLOW_ALLY_VILLAGERS)) {
                return true;
            }
        }
        return false;
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
        return type.name().endsWith("_DOOR") || type == Material.IRON_DOOR;
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

    private void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }
}
