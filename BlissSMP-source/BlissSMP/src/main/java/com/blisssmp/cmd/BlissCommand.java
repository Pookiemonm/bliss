
package com.blisssmp.cmd;

import com.blisssmp.BlissSMP;
import com.blisssmp.core.GemManager;
import com.blisssmp.core.GemManager.Gem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BlissCommand implements CommandExecutor, TabCompleter {

    private final BlissSMP plugin;

    public BlissCommand(BlissSMP plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "BlissSMP commands:");
            sender.sendMessage(ChatColor.GRAY + "/" + label + " gem list");
            sender.sendMessage(ChatColor.GRAY + "/" + label + " gem give <player> <GEM>");
            sender.sendMessage(ChatColor.GRAY + "/" + label + " prog <1|2|3|4>");
            return true;
        }

        if (args[0].equalsIgnoreCase("gem")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.GRAY + "Usage: /" + label + " gem <list|give>");
                return true;
            }
            if (args[1].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatColor.AQUA + "Gems: " + String.join(", ", plugin.gems().gemNames()));
                return true;
            }
            if (args[1].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("bliss.admin")) {
                    sender.sendMessage(ChatColor.RED + "You need bliss.admin.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.GRAY + "Usage: /" + label + " gem give <player> <GEM>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                try {
                    Gem g = Gem.valueOf(args[3].toUpperCase(Locale.ROOT));
                    plugin.gems().giveGem(target, g);
                    sender.sendMessage(ChatColor.GREEN + "Gave " + g.name() + " to " + target.getName());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Unknown gem. Use: " + String.join(", ", plugin.gems().gemNames()));
                }
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("prog")) {
            if (!sender.hasPermission("bliss.admin")) {
                sender.sendMessage(ChatColor.RED + "You need bliss.admin.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.GRAY + "Usage: /" + label + " prog <1|2|3|4>");
                return true;
            }
            try {
                int s = Integer.parseInt(args[1]);
                plugin.progs().setStage(s);
                sender.sendMessage(ChatColor.GREEN + "Prog set to " + s);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Not a number. Use 1..4.");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(Arrays.asList("help","gem","prog"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("gem")) {
            out.addAll(Arrays.asList("list","give"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("gem") && args[1].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("gem") && args[1].equalsIgnoreCase("give")) {
            out.addAll(plugin.gems().gemNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("prog")) {
            out.addAll(Arrays.asList("1","2","3","4"));
        }
        return out;
    }
}
