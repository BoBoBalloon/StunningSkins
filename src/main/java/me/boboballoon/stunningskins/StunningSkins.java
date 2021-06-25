package me.boboballoon.stunningskins;

import me.boboballoon.stunningskins.commands.ResetNameCommand;
import me.boboballoon.stunningskins.commands.ResetSkinCommand;
import me.boboballoon.stunningskins.commands.SetNameCommand;
import me.boboballoon.stunningskins.commands.SetSkinCommand;
import me.boboballoon.stunningskins.listeners.PlayerQuitListener;
import me.boboballoon.stunningskins.utils.NameUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public final class StunningSkins extends JavaPlugin {

    private static Plugin instance;

    @Override
    public void onEnable() {
        instance = this;

        Bukkit.getPluginCommand("setskin").setExecutor(new SetSkinCommand());
        Bukkit.getPluginCommand("resetskin").setExecutor(new ResetSkinCommand());
        Bukkit.getPluginCommand("setname").setExecutor(new SetNameCommand());
        Bukkit.getPluginCommand("resetname").setExecutor(new ResetNameCommand());

        this.registerListeners(new PlayerQuitListener());
    }

    @Override
    public void onDisable() {
        for (Map.Entry<UUID, String> index : NameUtil.NICKED_PLAYERS.entrySet()) {
            Player player = Bukkit.getOfflinePlayer(index.getKey()).getPlayer();
            if (player == null) {
                continue;
            }
            NameUtil.unNamePlayerUnsafe(player);
        }
    }

    public static Plugin getInstance() {
        return instance;
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, this);
        }
    }
}
