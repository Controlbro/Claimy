package com.controlbro.claimy.util;

import java.awt.Color;
import net.md_5.bungee.api.ChatColor;

public final class ChatColorUtil {
    private ChatColorUtil() {
    }

    public static String colorize(int rgb, String message) {
        ChatColor color = ChatColor.of(new Color(rgb & 0xFFFFFF));
        return color + message;
    }
}
