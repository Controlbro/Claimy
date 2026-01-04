package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
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
            StringUtil.copyPartialMatches(args[0], List.of("claim"), completions);
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            List<String> ids = plugin.getMallManager().getPlotIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            StringUtil.copyPartialMatches(args[1], ids, completions);
        }
        return completions;
    }
}
