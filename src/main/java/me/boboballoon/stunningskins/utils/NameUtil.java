package me.boboballoon.stunningskins.utils;

import com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

        reloadPlayer(target);

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

        reloadPlayer(target);

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

    private static void reloadPlayer(Player player) {
        Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(player);
        Object onlinePlayer = ReflectionUtil.executeMethod(craftPlayer, "getHandle");

        Class<?> packetPlayOutPlayerInfo = ReflectionUtil.getClass("net.minecraft.server.{NMS}.PacketPlayOutPlayerInfo");
        Class<?> enumPlayerInfoAction = ReflectionUtil.getClass("net.minecraft.server.{NMS}.PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

        Object removePlayerField;
        try {
            removePlayerField = ReflectionUtil.getField(enumPlayerInfoAction, "REMOVE_PLAYER").get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        Object playerArray = Array.newInstance(onlinePlayer.getClass(), 1);
        Array.set(playerArray, 0, onlinePlayer);

        Object subtract = ReflectionUtil.newInstanceFromClass(packetPlayOutPlayerInfo, removePlayerField, playerArray);

        Object addPlayerField;
        try {
            addPlayerField = ReflectionUtil.getField(enumPlayerInfoAction, "ADD_PLAYER").get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        Object add = ReflectionUtil.newInstanceFromClass(packetPlayOutPlayerInfo, addPlayerField, playerArray);

        Class<?> packetClass = ReflectionUtil.getClass("net.minecraft.server.{NMS}.Packet");
        Class<?> playerConnectionClass = ReflectionUtil.getClass("net.minecraft.server.{NMS}.PlayerConnection");

        Method sendPacket = ReflectionUtil.getMethod(playerConnectionClass, "sendPacket", packetClass);

        for (Player online : Bukkit.getOnlinePlayers()) {
            Object craftPlayerTarget = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(online);
            Object onlinePlayerTarget = ReflectionUtil.executeMethod(craftPlayerTarget, "getHandle");

            Object playerConnection = ReflectionUtil.getFieldFromObject(onlinePlayerTarget, "playerConnection");

            ReflectionUtil.executeMethod(playerConnection, sendPacket, subtract); //packetClass.cast(subtract)
            ReflectionUtil.executeMethod(playerConnection, sendPacket, add); //packetClass.cast(add)

            if (online == player) {
                continue;
            }

            Class<?> entityHuman = ReflectionUtil.getClass("net.minecraft.server.{NMS}.EntityHuman");
            Class<?> entitySpawnPacketClass = ReflectionUtil.getClass("net.minecraft.server.{NMS}.PacketPlayOutNamedEntitySpawn");

            Constructor<?> entitySpawnPacketClassConstructor = ReflectionUtil.getClassConstructor(entitySpawnPacketClass, entityHuman);

            Object spawn = ReflectionUtil.newInstanceFromClass(entitySpawnPacketClassConstructor, onlinePlayer);

            ReflectionUtil.executeMethod(playerConnection, sendPacket, spawn); //packetClass.cast(spawn)
        }
    }
}
