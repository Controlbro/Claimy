package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Nation;
import com.controlbro.claimy.model.Town;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

public class NationCommand implements CommandExecutor {
    private final ClaimyPlugin plugin;

    public NationCommand(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            plugin.getNationGui().openMain(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "capital" -> handleCapital(player, args);
            case "ally" -> handleAlly(player, args);
            case "enemy" -> handleEnemy(player, args);
            case "remove" -> handleRemove(player, args);
            default -> plugin.getNationGui().openMain(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/nation create <name>");
            return;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            player.sendMessage("You must be a town mayor to create a nation.");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("Only the town mayor can create a nation.");
            return;
        }
        if (town.getNationId().isPresent()) {
            player.sendMessage("Your town is already in a nation.");
            return;
        }
        String name = args[1];
        if (name.isBlank()) {
            player.sendMessage("Nation name cannot be blank.");
            return;
        }
        if (plugin.getNationManager().getNationByName(name).isPresent()) {
            player.sendMessage("A nation with that name already exists.");
            return;
        }
        Nation nation = plugin.getNationManager().createNation(name, player.getUniqueId(), town);
        town.setNationId(nation.getId());
        plugin.getTownManager().save();
        plugin.getMapIntegration().refreshAll();
        player.sendMessage("You have founded the nation " + nation.getName() + ".");
        player.sendMessage("Your capital is " + town.getDisplayName() + ".");
        player.sendMessage("Towns in a nation trust each other by default.");
        player.sendMessage("Town mayors always retain full control of their town.");
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/nation invite <town>");
            return;
        }
        Optional<Nation> nationOptional = plugin.getNationManager().getNationByOwner(player.getUniqueId());
        if (nationOptional.isEmpty()) {
            player.sendMessage("Only the nation owner can invite towns.");
            return;
        }
        Nation nation = nationOptional.get();
        Optional<Town> targetOptional = plugin.getTownManager().getTown(args[1]);
        if (targetOptional.isEmpty()) {
            player.sendMessage("Town not found.");
            return;
        }
        Town targetTown = targetOptional.get();
        if (targetTown.getNationId().isPresent()) {
            player.sendMessage("That town is already in a nation.");
            return;
        }
        plugin.getNationManager().inviteTown(nation, targetTown);
        player.sendMessage("Nation invite sent to " + targetTown.getDisplayName() + ".");
        Player mayor = Bukkit.getPlayer(targetTown.getOwner());
        if (mayor != null && mayor.isOnline()) {
            mayor.sendMessage("Your town has been invited to join the nation " + nation.getName() + ".");
            mayor.sendMessage("Use /nation accept or /nation deny.");
        }
    }

    private void handleAccept(Player player) {
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            player.sendMessage("You are not a town mayor.");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("Only the town mayor can accept nation invites.");
            return;
        }
        if (town.getNationId().isPresent()) {
            player.sendMessage("Your town is already in a nation.");
            return;
        }
        Optional<Nation> inviteOptional = plugin.getNationManager().getInviteForTown(town);
        if (inviteOptional.isEmpty()) {
            player.sendMessage("You do not have a nation invite.");
            return;
        }
        Nation nation = inviteOptional.get();
        plugin.getNationManager().clearInvite(town);
        plugin.getNationManager().addTown(nation, town);
        plugin.getTownManager().save();
        plugin.getMapIntegration().refreshAll();
        if (!town.isNationJoinNotified()) {
            sendNationJoinNotice(town, nation);
            town.setNationJoinNotified(true);
            plugin.getTownManager().save();
        }
        player.sendMessage("Your town has joined the nation " + nation.getName() + ".");
    }

    private void handleDeny(Player player) {
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        if (townOptional.isEmpty()) {
            player.sendMessage("You are not a town mayor.");
            return;
        }
        Town town = townOptional.get();
        if (!town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("Only the town mayor can deny nation invites.");
            return;
        }
        Optional<Nation> inviteOptional = plugin.getNationManager().getInviteForTown(town);
        if (inviteOptional.isEmpty()) {
            player.sendMessage("You do not have a nation invite.");
            return;
        }
        plugin.getNationManager().clearInvite(town);
        player.sendMessage("Nation invite denied.");
    }

    private void handleCapital(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/nation capital <town>");
            return;
        }
        Optional<Nation> nationOptional = plugin.getNationManager().getNationByOwner(player.getUniqueId());
        if (nationOptional.isEmpty()) {
            player.sendMessage("Only the nation owner can set the capital.");
            return;
        }
        Nation nation = nationOptional.get();
        Optional<Town> townOptional = plugin.getTownManager().getTown(args[1]);
        if (townOptional.isEmpty()) {
            player.sendMessage("Town not found.");
            return;
        }
        Town town = townOptional.get();
        if (!nation.getMemberTowns().contains(town.getId())) {
            player.sendMessage("That town is not part of your nation.");
            return;
        }
        plugin.getNationManager().setCapital(nation, town);
        plugin.getTownManager().save();
        plugin.getMapIntegration().refreshAll();
        player.sendMessage("Nation capital set to " + town.getDisplayName() + ".");
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/nation ally <nation>");
            return;
        }
        Optional<Nation> nationOptional = plugin.getNationManager().getNationByOwner(player.getUniqueId());
        if (nationOptional.isEmpty()) {
            player.sendMessage("Only the nation owner can set allies.");
            return;
        }
        Nation nation = nationOptional.get();
        Optional<Nation> targetOptional = plugin.getNationManager().getNationByName(args[1]);
        if (targetOptional.isEmpty()) {
            player.sendMessage("Nation not found.");
            return;
        }
        if (!plugin.getNationManager().setAlliance(nation, targetOptional.get())) {
            player.sendMessage("Unable to ally with that nation.");
            return;
        }
        plugin.getMapIntegration().refreshAll();
        player.sendMessage("Nation allied with " + targetOptional.get().getName() + ".");
    }

    private void handleEnemy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/nation enemy <nation>");
            return;
        }
        Optional<Nation> nationOptional = plugin.getNationManager().getNationByOwner(player.getUniqueId());
        if (nationOptional.isEmpty()) {
            player.sendMessage("Only the nation owner can set enemies.");
            return;
        }
        Nation nation = nationOptional.get();
        Optional<Nation> targetOptional = plugin.getNationManager().getNationByName(args[1]);
        if (targetOptional.isEmpty()) {
            player.sendMessage("Nation not found.");
            return;
        }
        if (!plugin.getNationManager().setEnemy(nation, targetOptional.get())) {
            player.sendMessage("Unable to mark that nation as an enemy.");
            return;
        }
        plugin.getMapIntegration().refreshAll();
        player.sendMessage("Nation marked " + targetOptional.get().getName() + " as an enemy.");
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/nation remove <town>");
            return;
        }
        Optional<Nation> nationOptional = plugin.getNationManager().getNationByOwner(player.getUniqueId());
        if (nationOptional.isEmpty()) {
            player.sendMessage("Only the nation owner can remove towns.");
            return;
        }
        Nation nation = nationOptional.get();
        Optional<Town> townOptional = plugin.getTownManager().getTown(args[1]);
        if (townOptional.isEmpty()) {
            player.sendMessage("Town not found.");
            return;
        }
        Town town = townOptional.get();
        if (!nation.getMemberTowns().contains(town.getId())) {
            player.sendMessage("That town is not part of your nation.");
            return;
        }
        plugin.getNationManager().removeTown(nation, town);
        plugin.getTownManager().save();
        plugin.getMapIntegration().refreshAll();
        player.sendMessage("Removed " + town.getDisplayName() + " from the nation.");
    }

    private void sendNationJoinNotice(Town town, Nation nation) {
        Player mayor = Bukkit.getPlayer(town.getOwner());
        if (mayor != null && mayor.isOnline()) {
            mayor.sendMessage("Your town has joined the nation " + nation.getName() + ".");
            mayor.sendMessage("Residents of other nation towns can now build here.");
            mayor.sendMessage("You may deny specific towns with:");
            mayor.sendMessage("/town deny <town>");
        }
        for (var assistantId : town.getAssistants()) {
            Player assistant = Bukkit.getPlayer(assistantId);
            if (assistant != null && assistant.isOnline()) {
                assistant.sendMessage("Your town has joined the nation " + nation.getName() + ".");
                assistant.sendMessage("Residents of other nation towns can now build here.");
                assistant.sendMessage("You may deny specific towns with:");
                assistant.sendMessage("/town deny <town>");
            }
        }
    }
}
