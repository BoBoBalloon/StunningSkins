package me.boboballoon.stunningskins;

import me.boboballoon.stunningskins.commands.ResetSkinCommand;
import me.boboballoon.stunningskins.commands.SetSkinCommand;
import me.boboballoon.stunningskins.listeners.PlayerQuitListener;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class StunningSkins extends JavaPlugin {

    private static Plugin instance;

    @Override
    public void onEnable() {
        instance = this;

        Bukkit.getPluginCommand("setskin").setExecutor(new SetSkinCommand());
        Bukkit.getPluginCommand("resetskin").setExecutor(new ResetSkinCommand());

        this.registerListeners(new PlayerQuitListener());
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
