package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.ResidentPermission;
import com.controlbro.claimy.model.Town;
import com.controlbro.claimy.model.TownFlag;
import com.controlbro.claimy.util.MapColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class TownTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "delete", "invite", "accept", "kick", "flag", "border", "help", "ally", "unally", "claim",
            "resident", "color", "assistant", "buildmode", "outpost", "plot"
    );

    private final ClaimyPlugin plugin;

    public TownTabCompleter(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, completions);
            return completions;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "invite", "kick" -> {
                    StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()), completions);
                }
                case "ally" -> {
                    List<String> suggestions = new ArrayList<>();
                    suggestions.add("accept");
                    suggestions.add("deny");
                    suggestions.addAll(plugin.getTownManager().getTownNames());
                    StringUtil.copyPartialMatches(args[1], suggestions, completions);
                }
                case "unally", "accept" -> {
                    StringUtil.copyPartialMatches(args[1], plugin.getTownManager().getTownNames(), completions);
                }
                case "flag" -> {
                    List<String> flags = Arrays.stream(TownFlag.values())
                            .map(flag -> flag.name().toLowerCase(Locale.ROOT))
                            .collect(Collectors.toList());
                    StringUtil.copyPartialMatches(args[1], flags, completions);
                }
                case "claim" -> {
                    StringUtil.copyPartialMatches(args[1], List.of("auto"), completions);
                }
                case "border" -> {
                    StringUtil.copyPartialMatches(args[1], List.of("stay"), completions);
                }
                case "delete" -> {
                    StringUtil.copyPartialMatches(args[1], List.of("confirm"), completions);
                }
                case "resident" -> {
                    if (sender instanceof Player player) {
                        Optional<Town> town = plugin.getTownManager().getTown(player.getUniqueId());
                        if (town.isPresent()) {
                            List<String> names = town.get().getResidents().stream()
                                    .map(Bukkit::getOfflinePlayer)
                                    .map(offline -> offline.getName() == null ? "" : offline.getName())
                                    .filter(name -> !name.isBlank())
                                    .collect(Collectors.toList());
                            StringUtil.copyPartialMatches(args[1], names, completions);
                        }
                    }
                }
                case "assistant" -> {
                    StringUtil.copyPartialMatches(args[1], List.of("add", "remove"), completions);
                }
                case "buildmode" -> {
                    StringUtil.copyPartialMatches(args[1], List.of("open", "plot"), completions);
                }
                case "outpost" -> {
                    StringUtil.copyPartialMatches(args[1], List.of("create", "claim"), completions);
                }
                case "plot" -> {
                    StringUtil.copyPartialMatches(args[1], List.of("create", "claim", "unclaim", "cancel"), completions);
                }
                case "color" -> {
                    StringUtil.copyPartialMatches(args[1], MapColorUtil.getNamedColors().keySet(), completions);
                }
                default -> {
                }
            }
            return completions;
        }
        if (args.length == 3) {
            if ("flag".equals(sub)) {
                StringUtil.copyPartialMatches(args[2], List.of("true", "false"), completions);
            }
            if ("ally".equals(sub) && (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny"))) {
                if (sender instanceof Player player) {
                    Optional<Town> townOptional = plugin.getTownManager().getTown(player.getUniqueId());
                    if (townOptional.isPresent()) {
                        StringUtil.copyPartialMatches(args[2], plugin.getTownManager().getAllyRequests(townOptional.get()), completions);
                    }
                }
            }
            if ("resident".equals(sub)) {
                List<String> permissions = Arrays.stream(ResidentPermission.values())
                        .map(permission -> permission.name().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[2], permissions, completions);
            }
            if ("assistant".equals(sub) && sender instanceof Player player) {
                Optional<Town> town = plugin.getTownManager().getTown(player.getUniqueId());
                if (town.isPresent()) {
                    List<String> names = town.get().getResidents().stream()
                            .map(Bukkit::getOfflinePlayer)
                            .map(offline -> offline.getName() == null ? "" : offline.getName())
                            .filter(name -> !name.isBlank())
                            .collect(Collectors.toList());
                    StringUtil.copyPartialMatches(args[2], names, completions);
                }
            }
        }
        if (args.length == 4 && "resident".equals(sub)) {
            StringUtil.copyPartialMatches(args[3], List.of("true", "false"), completions);
        }
        return completions;
    }
}
