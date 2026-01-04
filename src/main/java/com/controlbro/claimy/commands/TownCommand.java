package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.model.TownFlag;
import com.controlbro.claimy.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
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
            case "delete" -> handleDelete(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "kick" -> handleKick(player, args);
            case "flag" -> handleFlag(player, args);
            case "border" -> handleBorder(player);
            case "buy" -> handleBuy(player, args);
            case "ally" -> handleAlly(player, args);
            case "unally" -> handleUnally(player, args);
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
        plugin.getTownManager().claimChunk(town, player.getLocation().getChunk());
        MessageUtil.send(plugin, player, "town-created", "town", town.getName());
    }

    private void handleDelete(Player player) {
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
        plugin.getTownManager().deleteTown(town);
        MessageUtil.send(plugin, player, "town-deleted");
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
    }

    private void handleAccept(Player player, String[] args) {
        String townName = args.length >= 2 ? args[1] : null;
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
        plugin.getTownManager().addResident(townOptional.get(), player.getUniqueId());
        plugin.getTownManager().clearInvite(player.getUniqueId());
        MessageUtil.send(plugin, player, "joined-town", "town", townOptional.get().getName());
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
    }

    private void handleBorder(Player player) {
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        plugin.getTownGui().showBorder(player, townOptional.get());
    }

    private void handleBuy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/town buy <amount>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            MessageUtil.send(plugin, player, "no-town");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid amount.");
            return;
        }
        if (amount <= 0) {
            player.sendMessage("Amount must be positive.");
            return;
        }
        Economy economy = plugin.getEconomy();
        double cost = amount * plugin.getConfig().getDouble("settings.chunk-cost");
        if (!economy.has(player, cost)) {
            MessageUtil.send(plugin, player, "not-enough-money", "cost", String.valueOf(cost), "count", String.valueOf(amount));
            return;
        }
        economy.withdrawPlayer(player, cost);
        townOptional.get().addChunkLimit(amount);
        plugin.getTownManager().save();
        MessageUtil.send(plugin, player, "chunk-bought", "count", String.valueOf(amount), "cost", String.valueOf(cost));
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/town ally <town>");
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
        plugin.getTownManager().addAlly(town, ally);
        plugin.getTownManager().addAlly(ally, town);
        player.sendMessage("Town allied with " + ally.getName());
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
    }
}
