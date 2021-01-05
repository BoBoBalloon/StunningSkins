package me.boboballoon.stunningskins.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_16_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NameUtil {
    public static Map<UUID, String> NICKED_PLAYERS = new HashMap<>();

    /**
     * A method to change a player's name (always fire async)
     *
     * @param target the player who you wish to change their name
     * @param name   the name that you wish to set the player's name to
     * @return a boolean that is true when the change was applied successfully and false when an internal error occurred
     */
    public static boolean changeName(Player target, String name) {
        if ((name.length() < 3 || name.length() > 16) || name.contains(" ")) {
            return false;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return false;
            }
        }

        EntityPlayer player = ((CraftPlayer) target).getHandle();

        PacketPlayOutPlayerInfo subtract = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player);

        if (!NICKED_PLAYERS.containsKey(target.getUniqueId())) {
            NICKED_PLAYERS.put(target.getUniqueId(), target.getName());
        }

        Class<GameProfile> gameProfile = GameProfile.class;
        Field field;
        try {
            field = gameProfile.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            return false;
        }

        GameProfile playerProfile = player.getProfile();

        field.setAccessible(true);
        try {
            field.set(playerProfile, name);
        } catch (IllegalAccessException e) {
            return false;
        }

        PacketPlayOutPlayerInfo add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, player);
        PacketPlayOutEntityDestroy remove = new PacketPlayOutEntityDestroy(player.getId());
        PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(player);

        reloadPlayer(target, subtract, add, remove, spawn);

        return true;
    }

    /**
     * A method to reset a player's name (always fire async)
     *
     * @param target the player who you wish to reset their name
     * @return a boolean that is true when the change was applied successfully and false when an internal error occurred
     */
    public static boolean unNamePlayer(Player target) {
        if (!NICKED_PLAYERS.containsKey(target.getUniqueId())) {
            return false;
        }

        EntityPlayer player = ((CraftPlayer) target).getHandle();

        PacketPlayOutPlayerInfo subtract = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player);

        Class<GameProfile> gameProfile = GameProfile.class;
        Field field;
        try {
            field = gameProfile.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            return false;
        }

        GameProfile playerProfile = player.getProfile();

        field.setAccessible(true);
        try {
            field.set(playerProfile, NICKED_PLAYERS.get(target.getUniqueId()));
        } catch (IllegalAccessException e) {
            return false;
        }

        PacketPlayOutPlayerInfo add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, player);
        PacketPlayOutEntityDestroy remove = new PacketPlayOutEntityDestroy(player.getId());
        PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(player);

        reloadPlayer(target, subtract, add, remove, spawn);

        NICKED_PLAYERS.remove(target.getUniqueId());

        return true;
    }

    /**
     * A method to reset a player's name unsafely (only execute when you have to and it does not matter if player is updated: e.g. server is shutting down and plugin is about to be disabled)
     *
     * @param target the player who you wish to reset their name
     */
    public static void unNamePlayerUnsafe(Player target) {
        if (!NICKED_PLAYERS.containsKey(target.getUniqueId())) {
            return;
        }

        EntityPlayer player = ((CraftPlayer) target).getHandle();

        Class<GameProfile> gameProfile = GameProfile.class;
        Field field;
        try {
            field = gameProfile.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            return;
        }

        GameProfile playerProfile = player.getProfile();

        field.setAccessible(true);
        try {
            field.set(playerProfile, NICKED_PLAYERS.get(target.getUniqueId()));
        } catch (IllegalAccessException e) {
            return;
        }

        NICKED_PLAYERS.remove(target.getUniqueId());
    }

    private static void reloadPlayer(Player target, PacketPlayOutPlayerInfo subtract, PacketPlayOutPlayerInfo add, PacketPlayOutEntityDestroy remove, PacketPlayOutNamedEntitySpawn spawn) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            EntityPlayer onlinePlayer = ((CraftPlayer) online).getHandle();
            onlinePlayer.playerConnection.sendPacket(subtract);
            onlinePlayer.playerConnection.sendPacket(add);
            if (online == target) {
                continue;
            }
            onlinePlayer.playerConnection.sendPacket(remove);
            onlinePlayer.playerConnection.sendPacket(spawn);
        }
    }
}
