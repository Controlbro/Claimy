package com.controlbro.claimy.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MapColorUtil {
    private static final Map<String, Integer> NAMED_COLORS = new HashMap<>();

    static {
        NAMED_COLORS.put("black", 0x000000);
        NAMED_COLORS.put("dark_gray", 0x555555);
        NAMED_COLORS.put("gray", 0xAAAAAA);
        NAMED_COLORS.put("light_gray", 0xD3D3D3);
        NAMED_COLORS.put("white", 0xFFFFFF);
        NAMED_COLORS.put("red", 0xFF0000);
        NAMED_COLORS.put("dark_red", 0x8B0000);
        NAMED_COLORS.put("orange", 0xFFA500);
        NAMED_COLORS.put("yellow", 0xFFFF00);
        NAMED_COLORS.put("gold", 0xFFD700);
        NAMED_COLORS.put("green", 0x00AA00);
        NAMED_COLORS.put("dark_green", 0x006400);
        NAMED_COLORS.put("lime", 0x00FF00);
        NAMED_COLORS.put("blue", 0x0000FF);
        NAMED_COLORS.put("dark_blue", 0x00008B);
        NAMED_COLORS.put("aqua", 0x00FFFF);
        NAMED_COLORS.put("teal", 0x008080);
        NAMED_COLORS.put("purple", 0x800080);
        NAMED_COLORS.put("magenta", 0xFF00FF);
        NAMED_COLORS.put("pink", 0xFFC0CB);
        NAMED_COLORS.put("brown", 0x8B4513);
    }

    private MapColorUtil() {
    }

    public static Optional<Integer> parseColor(String colorInput) {
        if (colorInput == null || colorInput.isBlank()) {
            return Optional.empty();
        }
        String input = colorInput.trim().toLowerCase(Locale.ROOT);
        Integer named = NAMED_COLORS.get(input);
        if (named != null) {
            return Optional.of(named);
        }
        String normalized = normalizeHex(input);
        if (normalized == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(normalized, 16));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<String> normalizeColorName(String colorInput) {
        if (colorInput == null || colorInput.isBlank()) {
            return Optional.empty();
        }
        String input = colorInput.trim().toLowerCase(Locale.ROOT);
        if (NAMED_COLORS.containsKey(input)) {
            return Optional.of(input);
        }
        String normalized = normalizeHex(input);
        if (normalized == null) {
            return Optional.empty();
        }
        return Optional.of("#" + normalized.toUpperCase(Locale.ROOT));
    }

    public static Map<String, Integer> getNamedColors() {
        return Map.copyOf(NAMED_COLORS);
    }

    private static String normalizeHex(String input) {
        String hex = input;
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        if (hex.length() != 6) {
            return null;
        }
        for (char c : hex.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                return null;
            }
        }
        return hex;
    }
}
