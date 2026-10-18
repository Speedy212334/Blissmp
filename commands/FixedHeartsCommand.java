package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class FixedHeartsCommand implements CommandExecutor, TabCompleter {
   private final BlissGems plugin;

   public FixedHeartsCommand(BlissGems plugin) {
      this.plugin = plugin;
   }

   public static int applyTenHearts(BlissGems plugin, Player target) {
      AttributeInstance attr = target.getAttribute(Attributes.maxHealth());
      if (attr == null) {
         return -1;
      }

      if (plugin.getLifeAbilities() != null) {
         plugin.getLifeAbilities().cleanup(target);
      }

      if (plugin.getSoulManager() != null) {
         plugin.getSoulManager().cleanup(target);
      }

      int removed = 0;
      boolean respectExternal = plugin.getConfig().getBoolean("fixed-hearts.respect-external-hearts", true);
      if (!respectExternal) {
         for (AttributeModifier m : new ArrayList(attr.getModifiers())) {
            attr.removeModifier(m);
            removed++;
         }

         attr.setBaseValue(attr.getDefaultValue());
      }

      if (target.getHealth() > attr.getValue()) {
         target.setHealth(attr.getValue());
      }

      return removed;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("blissgems.admin") && !sender.hasPermission("blissgems.fixedhearts")) {
         sender.sendMessage("§cYou don't have permission to use this command.");
         return true;
      }

      if (args.length >= 2 && args[0].equalsIgnoreCase("everyone")) {
         Boolean enabled = parseBoolean(args[1]);
         if (enabled == null) {
            sender.sendMessage("§cExpected §etrue §cor §efalse§c, got §e" + args[1] + "§c.");
            return true;
         }

         this.plugin.getConfigManager().setFixedHeartsEnabled(enabled);
         if (enabled) {
            int affected = 0;

            for (Player online : Bukkit.getOnlinePlayers()) {
               if (applyTenHearts(this.plugin, online) >= 0) {
                  affected++;
               }
            }

            sender.sendMessage("§d§l❤ §aFixed hearts §lENABLED§a. Everyone is locked to 10 hearts on join §7(reset " + affected + " online player(s)).");
            this.plugin.getLogger().info("[fixedhearts] " + sender.getName() + " ENABLED fixed hearts (reset " + affected + " online player(s))");
         } else {
            sender.sendMessage("§d§l❤ §eFixed hearts §lDISABLED§e. Players keep their current hearts on join.");
            this.plugin.getLogger().info("[fixedhearts] " + sender.getName() + " DISABLED fixed hearts");
         }

         return true;
      } else {
         sender.sendMessage("§cUsage: §e/fixedhearts everyone <true|false>");
         sender.sendMessage("§7Currently: " + (this.plugin.getConfigManager().isFixedHeartsEnabled() ? "§aenabled" : "§cdisabled"));
         return true;
      }
   }

   private static Boolean parseBoolean(String s) {
      if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("enable") || s.equalsIgnoreCase("enabled") || s.equalsIgnoreCase("yes")) {
         return Boolean.TRUE;
      } else {
         return !s.equalsIgnoreCase("false")
               && !s.equalsIgnoreCase("off")
               && !s.equalsIgnoreCase("disable")
               && !s.equalsIgnoreCase("disabled")
               && !s.equalsIgnoreCase("no")
            ? null
            : Boolean.FALSE;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         return Arrays.asList("everyone").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
      } else {
         return args.length == 2 && args[0].equalsIgnoreCase("everyone")
            ? Arrays.asList("true", "false").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList())
            : new ArrayList<>();
      }
   }
}
