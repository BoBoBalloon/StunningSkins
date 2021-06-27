package me.boboballoon.stunningskins.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDeathListener implements Listener {
    public static final List<UUID> WATCHED_PLAYERS = new ArrayList<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (!WATCHED_PLAYERS.contains(uuid)) {
            return;
        }

        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        WATCHED_PLAYERS.remove(uuid);
    }
}
