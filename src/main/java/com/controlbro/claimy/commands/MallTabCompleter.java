package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import com.controlbro.claimy.util.MapColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MallTabCompleter implements TabCompleter {
    private final ClaimyPlugin plugin;

    public MallTabCompleter(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("claim", "config", "color", "employee"), completions);
            return completions;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("claim") || sub.equals("config")) {
                List<String> ids = plugin.getMallManager().getPlotIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], ids, completions);
            } else if (sub.equals("color")) {
                StringUtil.copyPartialMatches(args[1], MapColorUtil.getNamedColors().keySet(), completions);
            } else if (sub.equals("employee")) {
                StringUtil.copyPartialMatches(args[1], List.of("add", "remove", "accept", "deny"), completions);
            }
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("employee") && args[1].equalsIgnoreCase("add")) {
            StringUtil.copyPartialMatches(args[2], Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()), completions);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("employee") && args[1].equalsIgnoreCase("remove")) {
            StringUtil.copyPartialMatches(args[2], Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()), completions);
        }
        return completions;
    }
}
