package me.boboballoon.stunningskins.utils;

import org.bukkit.ChatColor;

public final class TextUtil {
    private TextUtil() {}

    /**
     * A util method used to add color codes to a string
     *
     * @param message the string
     * @return the string with the applied color codes
     */
    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
