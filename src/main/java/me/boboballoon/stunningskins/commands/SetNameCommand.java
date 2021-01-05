package me.boboballoon.stunningskins.commands;

import me.boboballoon.stunningskins.StunningSkins;
import me.boboballoon.stunningskins.utils.NameUtil;
import me.boboballoon.stunningskins.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetNameCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextUtil.format("&r&cOnly players can execute this command!"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("stunningskins.setname")) {
            player.sendMessage(TextUtil.format("&r&cYou do not have permission to execute this command!"));
            return false;
        }

        if (args.length != 1) {
            player.sendMessage(TextUtil.format("&r&cYou have entered improper arguments to execute this command!"));
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(StunningSkins.getInstance(), () -> this.changeName(NameUtil.changeName(player, args[0]), player));
        return true;
    }

    private void changeName(boolean value, Player player) {
        if (value) {
            player.sendMessage(TextUtil.format("&r&aYour name has been changed!"));
        } else {
            player.sendMessage(TextUtil.format("&r&cAn internal error has occurred!"));
        }
    }
}
