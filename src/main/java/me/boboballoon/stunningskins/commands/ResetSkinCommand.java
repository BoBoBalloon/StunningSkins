package me.boboballoon.stunningskins.commands;

import me.boboballoon.stunningskins.StunningSkins;
import me.boboballoon.stunningskins.utils.SkinUtil;
import me.boboballoon.stunningskins.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetSkinCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextUtil.format("&r&cOnly players can execute this command!"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("stunningskins.setskin")) {
            player.sendMessage(TextUtil.format("&r&cYou do not have permission to execute this command!"));
            return false;
        }

        if (args.length != 0) {
            player.sendMessage(TextUtil.format("&r&cYou have entered improper arguments to execute this command!"));
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(StunningSkins.getInstance(), () -> {
            if (SkinUtil.unSkinPlayer(player)) {
                player.sendMessage(TextUtil.format("&r&aYour skin has been reset!"));
            } else {
                player.sendMessage(TextUtil.format("&r&cYou are not currently disguised!"));
            }
        });

        return true;
    }
}
