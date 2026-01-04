package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.model.TownFlag;
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
import java.util.stream.Collectors;

public class TownTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "delete", "invite", "accept", "kick", "flag", "border", "help", "ally", "unally", "claim"
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
                case "ally", "unally", "accept" -> {
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
                default -> {
                }
            }
            return completions;
        }
        if (args.length == 3 && "flag".equals(sub)) {
            StringUtil.copyPartialMatches(args[2], List.of("true", "false"), completions);
        }
        return completions;
    }
}
