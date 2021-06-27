package me.boboballoon.stunningskins.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import me.boboballoon.stunningskins.StunningSkins;
import me.boboballoon.stunningskins.listeners.PlayerDeathListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkinUtil {
    public static Map<UUID, Property> SKINNED_PLAYERS = new HashMap<>();

    private SkinUtil() {
    }

    /**
     * A method used to set the skin of a player using another players username as a method of retrieving a skin (always fire async)
     *
     * @param target   the player whose skin you're trying to change
     * @param username the username of the player whose skin you're trying to set the targets skin to
     * @return a boolean that is true when the players skin was set successfully, false when an internal error occurred
     */
    public static boolean changeSkin(Player target, String username) {
        URL url;
        try {
            url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username + "?at=" + (System.currentTimeMillis() / 1000));
        } catch (MalformedURLException e) {
            return false;
        }

        JsonObject player;
        try {
            player = getJson(url).getAsJsonObject();
        } catch (IOException e) {
            return false;
        }

        return changeSkin(target, convert(player.get("id").getAsString()));
    }

    /**
     * A method used to set the skin of a player using another players uuid as a method of retrieving a skin (always fire async)
     *
     * @param target the player whose skin you're trying to change
     * @param uuid   the uuid of the player whose skin you're trying to set the targets skin to
     * @return a boolean that is true when the players skin was set successfully, false when an internal error occurred
     */
    public static boolean changeSkin(Player target, UUID uuid) {
        URL url;
        try {
            url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString() + "?unsigned=false");
        } catch (MalformedURLException e) {
            return false;
        }

        Gson gson = new GsonBuilder().create();
        JsonElement properties;
        try {
            properties = getJson(url, gson).getAsJsonObject().get("properties");
        } catch (IOException e) {
            return false;
        }

        Property skinData = gson.fromJson(String.valueOf(properties), Property[].class)[0];

        Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(target);
        Object player = ReflectionUtil.executeMethod(craftPlayer, "getHandle");

        GameProfile playerProfile = (GameProfile) ReflectionUtil.executeMethod(player, "getProfile");
        PropertyMap propertyMap = playerProfile.getProperties();

        Property oldSkinData = propertyMap.get("textures").iterator().next();

        if (!SKINNED_PLAYERS.containsKey(target.getUniqueId())) {
            SKINNED_PLAYERS.put(target.getUniqueId(), oldSkinData);
        }
        propertyMap.remove("textures", oldSkinData);
        propertyMap.put("textures", skinData);

        reloadPlayer(target);

        return true;
    }

    /**
     * A method used to set the skin of a player using another player as a method of retrieving a skin (always fire async)
     *
     * @param target the player whose skin you're trying to change
     * @param skin   the player whose skin you're trying to set the targets skin to
     * @return a boolean that is true when the players skin was set successfully, false when an internal error occurred
     */
    public static boolean changeSkin(Player target, Player skin) {
        Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(target);
        Object player = ReflectionUtil.executeMethod(craftPlayer, "getHandle");
        GameProfile playerProfile = (GameProfile) ReflectionUtil.executeMethod(player, "getProfile");

        Object craftTargeted = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(skin);
        Object targeted = ReflectionUtil.executeMethod(craftTargeted, "getHandle");
        GameProfile targetProfile = (GameProfile) ReflectionUtil.executeMethod(targeted, "getProfile");

        PropertyMap propertyMap = playerProfile.getProperties();

        Property oldSkinData = propertyMap.get("textures").iterator().next();
        Property skinData = targetProfile.getProperties().get("textures").iterator().next();

        if (!SKINNED_PLAYERS.containsKey(target.getUniqueId())) {
            SKINNED_PLAYERS.put(target.getUniqueId(), oldSkinData);
        }
        propertyMap.remove("textures", oldSkinData);
        propertyMap.put("textures", skinData);

        reloadPlayer(target);

        return true;
    }

    /**
     * A method used to restore the skin of a player who has already changed their skin (always fire async)
     *
     * @param target the player who you're trying to restore their original skin
     * @return a boolean that is true when the players skin was restored successfully, false when an internal error occurred
     */
    public static boolean unSkinPlayer(Player target) {
        UUID uuid = target.getUniqueId();
        if (!SKINNED_PLAYERS.containsKey(uuid)) {
            return false;
        }

        Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(target);
        Object player = ReflectionUtil.executeMethod(craftPlayer, "getHandle");
        GameProfile playerProfile = (GameProfile) ReflectionUtil.executeMethod(player, "getProfile");
        PropertyMap propertyMap = playerProfile.getProperties();

        Property oldSkinData = propertyMap.get("textures").iterator().next();
        Property skinData = SKINNED_PLAYERS.get(uuid);

        propertyMap.remove("textures", oldSkinData);
        propertyMap.put("textures", skinData);

        SKINNED_PLAYERS.remove(uuid);

        reloadPlayer(target);

        return true;
    }

    private static JsonElement getJson(URL url) throws IOException {
        InputStream inputStream = url.openStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        Gson gson = new GsonBuilder().create();

        JsonObject returnValue = gson.fromJson(reader, JsonObject.class);

        inputStream.close();
        return returnValue;
    }

    private static JsonElement getJson(URL url, Gson gson) throws IOException {
        InputStream inputStream = url.openStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        JsonObject returnValue = gson.fromJson(reader, JsonObject.class);

        inputStream.close();
        return returnValue;
    }

    private static void reloadPlayer(Player player) {
        Plugin plugin = StunningSkins.getInstance();
        Bukkit.getScheduler().runTask(StunningSkins.getInstance(), () -> {
            for (Player current : Bukkit.getOnlinePlayers()) {
                current.hidePlayer(plugin, player);
                current.showPlayer(plugin, player);
            }

            Object craftPlayer = ReflectionUtil.getClass("org.bukkit.craftbukkit.{NMS}.entity.CraftPlayer").cast(player);
            Object onlinePlayer = ReflectionUtil.executeMethod(craftPlayer, "getHandle");
            Object playerConnection = ReflectionUtil.getFieldFromObject(onlinePlayer, "playerConnection");

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

            Method sendPacket = ReflectionUtil.getMethod(playerConnection.getClass(), "sendPacket", packetClass);

            ReflectionUtil.executeMethod(playerConnection, sendPacket, subtract); //packetClass.cast(subtract)
            ReflectionUtil.executeMethod(playerConnection, sendPacket, add); //packetClass.cast(add)

            Location location = player.getLocation().clone();
            PlayerDeathListener.WATCHED_PLAYERS.add(player.getUniqueId());
            player.setHealth(0);
            player.spigot().respawn();
            player.teleport(location);
        /*
        Object worldField = ReflectionUtil.getFieldFromObject(onlinePlayer, "world");
        Object dimensionManager = ReflectionUtil.executeMethod(worldField, "getDimensionManager");

        Object getWorldMethod = ReflectionUtil.executeMethod(onlinePlayer, "getWorld");
        Object getDimensionKeyMethod = ReflectionUtil.executeMethod(getWorldMethod, "getDimensionKey");

        Object getWorldServerMethod = ReflectionUtil.executeMethod(onlinePlayer, "getWorldServer");
        Object getSeedMethod = ReflectionUtil.executeMethod(getWorldServerMethod, "getSeed");

        Object playerInteractManagerField = ReflectionUtil.getFieldFromObject(onlinePlayer, "playerInteractManager");
        Object getGamemodeMethod = ReflectionUtil.executeMethod(playerInteractManagerField, "getGameMode");

        Constructor<?> respawnConstructor = ReflectionUtil.getClassConstructor("net.minecraft.server.{NMS}.PacketPlayOutRespawn", dimensionManager.getClass(), getDimensionKeyMethod.getClass(), long.class, getGamemodeMethod.getClass(), getGamemodeMethod.getClass(), boolean.class, boolean.class, boolean.class);

        Object respawn = ReflectionUtil.newInstanceFromClass(respawnConstructor, dimensionManager, getDimensionKeyMethod, getSeedMethod, getGamemodeMethod, getGamemodeMethod, false, false, true);

        ReflectionUtil.executeMethod(playerConnection, sendPacket, respawn);
         */
        });
    }

    /*
    Made by sothatsit (too lazy to make my own)
    https://www.spigotmc.org/threads/free-code-easily-convert-between-trimmed-and-full-uuids.165615/
     */
    private static UUID convert(String uuid) {
        if (uuid.length() == 32) {
            StringBuilder builder = new StringBuilder(36);
            builder.append(uuid, 0, 8);
            builder.append('-');
            builder.append(uuid, 8, 12);
            builder.append('-');
            builder.append(uuid, 12, 16);
            builder.append('-');
            builder.append(uuid, 16, 20);
            builder.append('-');
            builder.append(uuid, 20, 32);
            return UUID.fromString(builder.toString());
        } else {
            return UUID.fromString(uuid);
        }
    }
}
