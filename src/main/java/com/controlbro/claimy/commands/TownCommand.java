package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.ChunkKey;
import com.controlbro.claimy.model.ResidentPermission;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.model.TownFlag;
import com.controlbro.claimy.util.MapColorUtil;
import com.controlbro.claimy.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

public class TownCommand implements CommandExecutor {
    private final ClaimyPlugin plugin;

    public TownCommand(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            plugin.getTownGui().openMain(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "kick" -> handleKick(player, args);
            case "flag" -> handleFlag(player, args);
            case "border" -> handleBorder(player, args);
            case "help" -> handleHelp(player);
            case "ally" -> handleAlly(player, args);
            case "unally" -> handleUnally(player, args);
            case "claim" -> handleClaim(player, args);
            case "resident" -> handleResidentPermission(player, args);
            case "color" -> handleColor(player, args);
            default -> plugin.getTownGui().openMain(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/town create <name>");
            return;
        }
        if (plugin.getTownManager().getTown(player.getUniqueId()).isPresent()) {
            player.sendMessage("You are already in a town.");
            return;
        }
        String name = args[1];
        if (plugin.getTownManager().getTown(name).isPresent()) {
            player.sendMessage("Town already exists.");
            return;
        }
        Town town = plugin.getTownManager().createTown(name, player.getUniqueId());
        if (plugin.getTownManager().isChunkClaimed(player.getLocation().getChunk())
                || plugin.getTownManager().isChunkWithinBuffer(player.getLocation().getChunk(), town)) {
            player.sendMessage("Cannot create a town within 1 chunk of another town.");
            plugin.getTownManager().deleteTown(town);
            return;
        }
        plugin.getTownManager().claimChunk(town, player.getLocation().getChunk());
        MessageUtil.send(plugin, player, "town-created", "town", town.getName());
        playSuccess(player);
    }

    private void handleDelete(Player player, String[] args) {
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId()) && !player.hasPermission("claimy.admin")) {
            player.sendMessage("Only the owner can delete the town.");
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage("Type /town delete confirm to delete your town.");
            return;
        }
        plugin.getTownManager().deleteTown(town);
        MessageUtil.send(plugin, player, "town-deleted");
        playSuccess(player);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/town invite <player>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("Only the owner can invite.");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("Player not found.");
            return;
        }
        plugin.getTownManager().invitePlayer(target.getUniqueId(), town);
        MessageUtil.send(plugin, player, "invite-sent", "player", target.getName());
        MessageUtil.send(plugin, target, "invite-received", "town", town.getName());
        playSuccess(player);
    }

    private void handleAccept(Player player, String[] args) {
        String townName = args.length >= 2 ? args[1] : null;
        if (plugin.getTownManager().getTown(player.getUniqueId()).isPresent()) {
            player.sendMessage("You are already in a town.");
            return;
        }
        Optional<String> invite = plugin.getTownManager().getInvite(player.getUniqueId());
        if (invite.isEmpty()) {
            player.sendMessage("You have no invites.");
            return;
        }
        if (townName != null && !invite.get().equalsIgnoreCase(townName)) {
            player.sendMessage("You do not have an invite to that town.");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(invite.get());
        if (townOptional.isEmpty()) {
            player.sendMessage("Town no longer exists.");
            return;
        }
        if (!plugin.getTownManager().addResident(townOptional.get(), player.getUniqueId())) {
            player.sendMessage("You are already in a town.");
            return;
        }
        plugin.getTownManager().clearInvite(player.getUniqueId());
        MessageUtil.send(plugin, player, "joined-town", "town", townOptional.get().getName());
        playSuccess(player);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/town kick <player>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("Only the owner can kick.");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("Player not found.");
            return;
        }
        if (town.getOwner().equals(target.getUniqueId())) {
            player.sendMessage("You cannot kick the owner.");
            return;
        }
        if (plugin.getTownManager().removeResident(town, target.getUniqueId())) {
            MessageUtil.send(plugin, player, "kicked", "player", target.getName());
            target.sendMessage("You were removed from the town.");
            playSuccess(player);
        }
    }

    private void handleFlag(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("/town flag <flag> <true|false>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("Only the owner can change flags.");
            return;
        }
        TownFlag flag;
        try {
            flag = TownFlag.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            player.sendMessage("Unknown flag.");
            return;
        }
        boolean value = Boolean.parseBoolean(args[2]);
        town.setFlag(flag, value);
        plugin.getTownManager().save();
        MessageUtil.send(plugin, player, "town-flag-updated", "flag", flag.name(), "value", String.valueOf(value));
        playSuccess(player);
    }

    private void handleBorder(Player player, String[] args) {
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("stay")) {
            boolean enabled = plugin.getTownGui().toggleBorderStay(player, townOptional.get());
            player.sendMessage(enabled ? "Border stay enabled." : "Border stay disabled.");
            playSuccess(player);
            return;
        }
        plugin.getTownGui().showBorder(player, townOptional.get());
        playSuccess(player);
    }

    private void handleHelp(Player player) {
        player.sendMessage(MessageUtil.color("&6Town Commands:"));
        player.sendMessage(MessageUtil.color("&e/town &7- Open town menu"));
        player.sendMessage(MessageUtil.color("&e/town create <name> &7- Create a town"));
        player.sendMessage(MessageUtil.color("&e/town delete &7- Delete your town"));
        player.sendMessage(MessageUtil.color("&e/town invite <player> &7- Invite a resident"));
        player.sendMessage(MessageUtil.color("&e/town accept <town> &7- Accept a town invite"));
        player.sendMessage(MessageUtil.color("&e/town kick <player> &7- Remove a resident"));
        player.sendMessage(MessageUtil.color("&e/town ally <town> &7- Request an alliance"));
        player.sendMessage(MessageUtil.color("&e/town ally accept <town> &7- Accept an alliance request"));
        player.sendMessage(MessageUtil.color("&e/town ally deny <town> &7- Deny an alliance request"));
        player.sendMessage(MessageUtil.color("&e/town unally <town> &7- Remove an ally"));
        player.sendMessage(MessageUtil.color("&e/town flag <flag> <true|false> &7- Set flags"));
        player.sendMessage(MessageUtil.color("&e/town resident <player> <permission> <true|false> &7- Set resident permissions"));
        player.sendMessage(MessageUtil.color("&e/town border &7- Show borders"));
        player.sendMessage(MessageUtil.color("&e/town border stay &7- Toggle persistent borders"));
        player.sendMessage(MessageUtil.color("&e/town claim &7- Claim the chunk you are standing in"));
        player.sendMessage(MessageUtil.color("&e/town claim auto &7- Toggle auto-claim"));
        player.sendMessage(MessageUtil.color("&e/town color <color> &7- Set town map color"));
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/town ally <town>");
            return;
        }
        if (args[1].equalsIgnoreCase("accept")) {
            handleAllyAccept(player, args);
            return;
        }
        if (args[1].equalsIgnoreCase("deny")) {
            handleAllyDeny(player, args);
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("Only the owner can ally.");
            return;
        }
        Optional<Town> allyOptional = plugin.getTownManager().getTown(args[1]);
        if (allyOptional.isEmpty()) {
            player.sendMessage("Town not found.");
            return;
        }
        Town ally = allyOptional.get();
        if (plugin.getTownManager().requestAlly(town, ally)) {
            player.sendMessage("Alliance request sent to " + ally.getName() + ".");
            notifyTownOwner(ally, "Your town has an alliance request from " + town.getName()
                    + ". Use /town ally accept " + town.getName() + " to accept.");
            playSuccess(player);
        } else {
            player.sendMessage("Unable to send alliance request.");
        }
    }

    private void handleAllyAccept(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("/town ally accept <town>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId()) && !player.hasPermission("claimy.admin")) {
            player.sendMessage("Only the owner can accept allies.");
            return;
        }
        Optional<Town> allyOptional = plugin.getTownManager().getTown(args[2]);
        if (allyOptional.isEmpty()) {
            player.sendMessage("Town not found.");
            return;
        }
        Town ally = allyOptional.get();
        if (plugin.getTownManager().acceptAllyRequest(town, ally)) {
            player.sendMessage("Town allied with " + ally.getName() + ".");
            notifyTownOwner(ally, "Your town is now allied with " + town.getName() + ".");
            playSuccess(player);
        } else {
            player.sendMessage("No pending ally request from that town.");
        }
    }

    private void handleAllyDeny(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("/town ally deny <town>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId()) && !player.hasPermission("claimy.admin")) {
            player.sendMessage("Only the owner can deny allies.");
            return;
        }
        Optional<Town> allyOptional = plugin.getTownManager().getTown(args[2]);
        if (allyOptional.isEmpty()) {
            player.sendMessage("Town not found.");
            return;
        }
        Town ally = allyOptional.get();
        if (plugin.getTownManager().denyAllyRequest(town, ally)) {
            player.sendMessage("Alliance request denied.");
            notifyTownOwner(ally, "Your alliance request to " + town.getName() + " was denied.");
            playSuccess(player);
        } else {
            player.sendMessage("No pending ally request from that town.");
        }
    }

    private void handleUnally(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/town unally <town>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("Only the owner can unally.");
            return;
        }
        Optional<Town> allyOptional = plugin.getTownManager().getTown(args[1]);
        if (allyOptional.isEmpty()) {
            player.sendMessage("Town not found.");
            return;
        }
        Town ally = allyOptional.get();
        plugin.getTownManager().removeAlly(town, ally);
        plugin.getTownManager().removeAlly(ally, town);
        player.sendMessage("Town unallied with " + ally.getName());
        playSuccess(player);
    }

    private void handleClaim(Player player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("auto")) {
            if (plugin.getTownManager().getTown(player.getUniqueId()).isEmpty()) {
                MessageUtil.send(plugin, player, "no-town");
                return;
            }
            boolean enabled = plugin.getTownManager().toggleAutoClaim(player.getUniqueId());
            player.sendMessage(enabled ? "Auto-claim enabled." : "Auto-claim disabled.");
            playSuccess(player);
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (plugin.getMallManager().isInMall(player.getLocation())) {
            player.sendMessage("You cannot claim a town chunk inside the mall.");
            return;
        }
        if (plugin.getTownManager().isChunkClaimed(player.getLocation().getChunk())) {
            player.sendMessage("Chunk already claimed.");
            return;
        }
        if (plugin.getTownManager().isChunkWithinBuffer(player.getLocation().getChunk(), town)) {
            player.sendMessage("You must leave a 1 chunk buffer between towns.");
            return;
        }
        if (plugin.getTownManager().claimChunk(town, player.getLocation().getChunk())) {
            player.sendMessage("Chunk claimed.");
            playSuccess(player);
            plugin.getTownGui().showClaimBorder(player, town, new ChunkKey(
                    player.getWorld().getName(),
                    player.getLocation().getChunk().getX(),
                    player.getLocation().getChunk().getZ()
            ));
        } else {
            player.sendMessage("You have reached your chunk limit.");
        }
    }

    private void handleResidentPermission(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("/town resident <player> <permission> <true|false>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId()) && !player.hasPermission("claimy.admin")) {
            player.sendMessage("Only the owner can change resident permissions.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null || !town.isResident(target.getUniqueId())) {
            player.sendMessage("That player is not a resident.");
            return;
        }
        ResidentPermission permission;
        try {
            permission = ResidentPermission.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            player.sendMessage("Unknown permission.");
            return;
        }
        if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
            player.sendMessage("/town resident <player> <permission> <true|false>");
            return;
        }
        boolean value = Boolean.parseBoolean(args[3]);
        town.setResidentPermission(target.getUniqueId(), permission, value);
        plugin.getTownManager().save();
        player.sendMessage("Resident permission updated.");
        playSuccess(player);
    }

    private void handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/town color <color>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId()) && !player.hasPermission("claimy.admin")) {
            player.sendMessage("Only the owner can change map colors.");
            return;
        }
        Optional<String> normalized = MapColorUtil.normalizeColorName(args[1]);
        if (normalized.isEmpty()) {
            player.sendMessage("Invalid color name.");
            return;
        }
        town.setMapColor(normalized.get());
        plugin.getTownManager().save();
        plugin.getMapIntegration().refreshAll();
        player.sendMessage("Town map color set to " + normalized.get() + ".");
        playSuccess(player);
    }

    private void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }

    private void notifyTownOwner(Town town, String message) {
        Player owner = Bukkit.getPlayer(town.getOwner());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(message);
        }
    }
}
