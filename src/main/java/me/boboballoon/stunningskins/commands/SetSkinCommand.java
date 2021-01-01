package me.boboballoon.stunningskins.commands;

import me.boboballoon.stunningskins.utils.SkinUtil;
import me.boboballoon.stunningskins.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSkinCommand implements CommandExecutor {

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

        if (args.length != 1) {
            player.sendMessage(TextUtil.format("&r&cYou have entered improper arguments to execute this command!"));
            return false;
        }

        String target = args[0];

        Player targeted = Bukkit.getPlayerExact(target);
        if (targeted != null) {
            this.changeSkin(SkinUtil.changeSkin(player, targeted), player);
            return true;
        }

        this.changeSkin(SkinUtil.changeSkin(player, target), player);
        return true;
    }

    private void changeSkin(boolean value, Player player) {
        if (value) {
            player.sendMessage(TextUtil.format("&r&aYour skin has been changed!"));
        } else {
            player.sendMessage(TextUtil.format("&r&cAn internal error has occurred!"));
        }
    }
}
