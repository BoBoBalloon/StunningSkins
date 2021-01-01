package me.boboballoon.stunningskins.utils;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import me.boboballoon.stunningskins.StunningSkins;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_16_R3.PacketPlayOutRespawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkinUtil {
    public static Map<UUID, Property> SKINNED_PLAYERS = new HashMap<>();

    /**
     * A method used to set the skin of a player using another players username as a method of retrieving a skin
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
            e.printStackTrace(); //remove
            return false;
        }

        JsonObject player;
        try {
            player = getJson(url).getAsJsonObject();
        } catch (IOException e) { // | ParseException e
            e.printStackTrace(); //remove
            return false;
        }

        return changeSkin(target, convert(player.get("id").getAsString()));
    }

    /**
     * A method used to set the skin of a player using another players uuid as a method of retrieving a skin
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
            e.printStackTrace(); //remove
            return false;
        }

        Gson gson = new GsonBuilder().create();
        JsonElement properties;
        try {
            properties = getJson(url, gson).getAsJsonObject().get("properties");
        } catch (IOException e) {
            e.printStackTrace(); //remove
            return false;
        }

        Property skinData = gson.fromJson(String.valueOf(properties), Property[].class)[0];

        EntityPlayer player = ((CraftPlayer) target).getHandle();
        GameProfile profile = player.getProfile();
        PropertyMap propertyMap = profile.getProperties();

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
     * A method used to set the skin of a player using another player as a method of retrieving a skin
     *
     * @param target the player whose skin you're trying to change
     * @param skin   the player whose skin you're trying to set the targets skin to
     * @return a boolean that is true when the players skin was set successfully, false when an internal error occurred
     */
    public static boolean changeSkin(Player target, Player skin) {
        EntityPlayer player = ((CraftPlayer) target).getHandle();
        EntityPlayer targeted = ((CraftPlayer) skin).getHandle();
        PropertyMap propertyMap = player.getProfile().getProperties();

        Property oldSkinData = propertyMap.get("textures").iterator().next();
        Property skinData = targeted.getProfile().getProperties().get("textures").iterator().next();

        if (!SKINNED_PLAYERS.containsKey(target.getUniqueId())) {
            SKINNED_PLAYERS.put(target.getUniqueId(), oldSkinData);
        }
        propertyMap.remove("textures", oldSkinData);
        propertyMap.put("textures", skinData);

        reloadPlayer(target);

        return true;
    }

    /**
     * A method used to restore the skin of a player who has already changed their skin
     *
     * @param target the player who you're trying to restore their original skin
     * @return a boolean that is true when the players skin was restored successfully, false when an internal error occurred
     */
    public static boolean unSkinPlayer(Player target) {
        UUID uuid = target.getUniqueId();
        if (!SKINNED_PLAYERS.containsKey(uuid)) {
            return false;
        }

        EntityPlayer player = ((CraftPlayer)target).getHandle();
        PropertyMap propertyMap = player.getProfile().getProperties();

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
        for (Player current : Bukkit.getOnlinePlayers()) {
            current.hidePlayer(plugin, player);
            current.showPlayer(plugin, player);
        }
        EntityPlayer craftPlayer = ((CraftPlayer) player).getHandle();

        PacketPlayOutPlayerInfo removeInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, craftPlayer);
        PacketPlayOutPlayerInfo addInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, craftPlayer);
        Location location = player.getLocation().clone();

        craftPlayer.playerConnection.sendPacket(removeInfo);
        craftPlayer.playerConnection.sendPacket(addInfo);

        PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(craftPlayer.world.getDimensionManager(), craftPlayer.getWorld().getDimensionKey(), craftPlayer.getWorldServer().getSeed(), craftPlayer.playerInteractManager.getGameMode(), craftPlayer.playerInteractManager.getGameMode(), false, false, true);

        player.teleport(new Location(Bukkit.getWorld("world"), 0, 1000, 0));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            craftPlayer.playerConnection.sendPacket(respawn);
            player.teleport(location);
        }, 2L);
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
