package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.Nation;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.util.ChatColorUtil;
import com.controlbro.claimy.util.MapColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class NationChatCommand implements CommandExecutor {
    private final ClaimyPlugin plugin;

    public NationChatCommand(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("/nc <message>");
            player.sendMessage("/nc toggle");
            return true;
        }
        if (args[0].equalsIgnoreCase("toggle")) {
            boolean enabled = !plugin.getPlayerDataManager().isNationChatEnabled(player.getUniqueId());
            plugin.getPlayerDataManager().setNationChatEnabled(player.getUniqueId(), enabled);
            player.sendMessage(enabled ? "Nation chat enabled." : "Nation chat disabled.");
            return true;
        }
        Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
        Optional<Nation> nationOptional = townOptional.flatMap(plugin.getNationManager()::getNationForTown)
                .or(() -> plugin.getNationManager().getNationByOwner(player.getUniqueId()));
        if (nationOptional.isEmpty()) {
            player.sendMessage("You are not part of a nation.");
            return true;
        }
        Nation nation = nationOptional.get();
        String message = String.join(" ", args);
        int color = resolveNationColor(nation);
        String formatted = ChatColorUtil.colorize(color,
                "[" + nation.getName() + "] " + player.getName() + ": " + message);
        for (UUID recipientId : collectNationRecipients(nation)) {
            if (!plugin.getPlayerDataManager().isNationChatEnabled(recipientId)) {
                continue;
            }
            Player recipient = Bukkit.getPlayer(recipientId);
            if (recipient != null && recipient.isOnline()) {
                recipient.sendMessage(formatted);
            }
        }
        return true;
    }

    private Set<UUID> collectNationRecipients(Nation nation) {
        Set<UUID> recipients = new HashSet<>();
        recipients.add(nation.getOwner());
        for (UUID townId : nation.getMemberTowns()) {
            plugin.getTownManager().getTownById(townId).ifPresent(town -> {
                recipients.add(town.getOwner());
                recipients.addAll(town.getResidents());
            });
        }
        return recipients;
    }

    private int resolveNationColor(Nation nation) {
        Optional<Town> capitalTown = plugin.getTownManager().getTownById(nation.getCapitalTownId());
        String color = capitalTown.map(Town::getMapColor)
                .orElse(plugin.getConfig().getString("settings.squaremap.town-default-color", "#00FF00"));
        return MapColorUtil.parseColor(color).orElse(0x00FF00);
    }
}
