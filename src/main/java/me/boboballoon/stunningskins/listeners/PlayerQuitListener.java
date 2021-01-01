package me.boboballoon.stunningskins.listeners;

import me.boboballoon.stunningskins.utils.SkinUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SkinUtil.SKINNED_PLAYERS.remove(event.getPlayer().getUniqueId());
    }
}
