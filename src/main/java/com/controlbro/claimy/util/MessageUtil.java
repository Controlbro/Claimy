package com.controlbro.claimy.util;

import com.controlbro.claimy.ClaimyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageUtil {
    private MessageUtil() {
    }

    public static void send(ClaimyPlugin plugin, CommandSender sender, String key, String... replacements) {
        String message = plugin.getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        sender.sendMessage(color(prefix + message));
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
