package me.boboballoon.stunningskins.listeners;

import me.boboballoon.stunningskins.utils.NameUtil;
import me.boboballoon.stunningskins.utils.SkinUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SkinUtil.SKINNED_PLAYERS.remove(player.getUniqueId());
        NameUtil.unNamePlayerUnsafe(player);
    }
}
