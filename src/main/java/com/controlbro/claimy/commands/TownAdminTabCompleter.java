package com.controlbro.claimy.commands;

import com.controlbro.claimy.ClaimyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TownAdminTabCompleter implements TabCompleter {
    private final ClaimyPlugin plugin;

    public TownAdminTabCompleter(ClaimyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("reload", "mall", "mallunclaim"), completions);
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mall")) {
            StringUtil.copyPartialMatches(args[1], List.of("setplot", "clear"), completions);
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("mall") && args[1].equalsIgnoreCase("clear")) {
            List<String> ids = plugin.getMallManager().getPlotIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            StringUtil.copyPartialMatches(args[2], ids, completions);
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mallunclaim")) {
            StringUtil.copyPartialMatches(args[1], plugin.getServer().getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .collect(Collectors.toList()), completions);
            return completions;
        }
        return completions;
    }
}
