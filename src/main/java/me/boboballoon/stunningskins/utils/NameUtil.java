package me.boboballoon.stunningskins.utils;

import com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NameUtil {
    public static Map<UUID, String> NICKED_PLAYERS = new HashMap<>();

    private NameUtil() {}

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

        Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(target);
        Object player = ReflectionUtil.executeMethod(craftPlayer, "getHandle");

        Class<?> packetPlayOutPlayerInfo = ReflectionUtil.getClass("net.minecraft.server.{NMS}.PacketPlayOutPlayerInfo");
        Class<?> enumPlayerInfoAction = ReflectionUtil.getClass("net.minecraft.server.{NMS}.PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

        Object removePlayerField;
        try {
            removePlayerField = ReflectionUtil.getField(enumPlayerInfoAction, "REMOVE_PLAYER").get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }

        Object playerArray = Array.newInstance(player.getClass(), 1);
        Array.set(playerArray, 0, player);

        Object subtract = ReflectionUtil.newInstanceFromClass(packetPlayOutPlayerInfo, removePlayerField, playerArray);

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

        GameProfile playerProfile = (GameProfile) ReflectionUtil.executeMethod(player, "getProfile");

        field.setAccessible(true);
        try {
            field.set(playerProfile, name);
        } catch (IllegalAccessException e) {
            return false;
        }

        Object addPlayerField;
        try {
            addPlayerField = ReflectionUtil.getField(enumPlayerInfoAction, "ADD_PLAYER").get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }

        Object add = ReflectionUtil.newInstanceFromClass(packetPlayOutPlayerInfo, addPlayerField, playerArray);
        //PacketPlayOutPlayerInfo add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, player);

        Object playerID = ReflectionUtil.getFieldFromObject(player, "getId");
        Object remove = ReflectionUtil.newInstanceFromClass("net.minecraft.server.{NMS}.PacketPlayOutEntityDestroy", playerID);
        //PacketPlayOutEntityDestroy remove = new PacketPlayOutEntityDestroy(player.getId());

        Object spawn = ReflectionUtil.newInstanceFromClass("net.minecraft.server.{NMS}.PacketPlayOutNamedEntitySpawn", player);
        //PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(player);

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

        Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(target);
        Object player = ReflectionUtil.executeMethod(craftPlayer, "getHandle");

        Class<?> packetPlayOutPlayerInfo = ReflectionUtil.getClass("net.minecraft.server.{NMS}.PacketPlayOutPlayerInfo");
        Class<?> enumPlayerInfoAction = ReflectionUtil.getClass("net.minecraft.server.{NMS}.PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

        Object removePlayerField;
        try {
            removePlayerField = ReflectionUtil.getField(enumPlayerInfoAction, "REMOVE_PLAYER").get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }

        Object playerArray = Array.newInstance(player.getClass(), 1);
        Array.set(playerArray, 0, player);

        Object subtract = ReflectionUtil.newInstanceFromClass(packetPlayOutPlayerInfo, removePlayerField, playerArray);

        //PacketPlayOutPlayerInfo subtract = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player);

        Class<GameProfile> gameProfile = GameProfile.class;
        Field field;
        try {
            field = gameProfile.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            return false;
        }

        GameProfile playerProfile = (GameProfile) ReflectionUtil.executeMethod(player, "getProfile");

        field.setAccessible(true);
        try {
            field.set(playerProfile, NICKED_PLAYERS.get(target.getUniqueId()));
        } catch (IllegalAccessException e) {
            return false;
        }

        Object addPlayerField;
        try {
            addPlayerField = ReflectionUtil.getField(enumPlayerInfoAction, "ADD_PLAYER").get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }

        Object add = ReflectionUtil.newInstanceFromClass(packetPlayOutPlayerInfo, addPlayerField, playerArray);
        //PacketPlayOutPlayerInfo add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, player);

        Object playerID = ReflectionUtil.getFieldFromObject(player, "getId");
        Object remove = ReflectionUtil.newInstanceFromClass("net.minecraft.server.{NMS}.PacketPlayOutEntityDestroy", playerID);
        //PacketPlayOutEntityDestroy remove = new PacketPlayOutEntityDestroy(player.getId());

        Object spawn = ReflectionUtil.newInstanceFromClass("net.minecraft.server.{NMS}.PacketPlayOutNamedEntitySpawn", player);
        //PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(player);

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

        Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(target);
        Object player = ReflectionUtil.executeMethod(craftPlayer, "getHandle");

        Class<GameProfile> gameProfile = GameProfile.class;
        Field field;
        try {
            field = gameProfile.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            return;
        }

        GameProfile playerProfile = (GameProfile) ReflectionUtil.executeMethod(player, "getProfile");

        field.setAccessible(true);
        try {
            field.set(playerProfile, NICKED_PLAYERS.get(target.getUniqueId()));
        } catch (IllegalAccessException e) {
            return;
        }

        NICKED_PLAYERS.remove(target.getUniqueId());
    }

    private static void reloadPlayer(Player target, Object subtract, Object add, Object remove, Object spawn) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(online);
            Object onlinePlayer = ReflectionUtil.executeMethod(craftPlayer, "getHandle");

            Object playerConnection = ReflectionUtil.getFieldFromObject(onlinePlayer, "playerConnection");

            ReflectionUtil.executeMethod(playerConnection, "sendPacket", subtract);
            ReflectionUtil.executeMethod(playerConnection, "sendPacket", add);
            if (online == target) {
                continue;
            }
            ReflectionUtil.executeMethod(playerConnection, "sendPacket", remove);
            ReflectionUtil.executeMethod(playerConnection, "sendPacket", spawn);
        }
    }
}
