package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class FixHeartsCommand implements CommandExecutor, TabCompleter {
   private final BlissGems plugin;

   public FixHeartsCommand(BlissGems plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("blissgems.admin") && !sender.hasPermission("blissgems.fixhearts")) {
         sender.sendMessage("§cYou don't have permission to use this command.");
         return true;
      }

      if (args.length == 0) {
         if (!(sender instanceof Player target)) {
            sender.sendMessage("§cUsage: /fixhearts <player>");
            return true;
         }
      } else {
         target = Bukkit.getPlayer(args[0]);
         if (target == null) {
            sender.sendMessage("§cPlayer §e" + args[0] + "§c not found or offline.");
            return true;
         }
      }

      AttributeInstance attr = target.getAttribute(Attributes.maxHealth());
      if (attr == null) {
         sender.sendMessage("§cCould not access max-health attribute for " + target.getName() + ".");
         return true;
      }

      this.plugin
         .getLogger()
         .info("[fixhearts] " + target.getName() + " baseValue=" + attr.getBaseValue() + " value=" + attr.getValue() + " health=" + target.getHealth());

      for (AttributeModifier m : attr.getModifiers()) {
         Object keyDesc = "n/a";

         try {
            NamespacedKey k = m.getKey();
            if (k != null) {
               keyDesc = k.toString();
            }
         } catch (Throwable var11) {
         }

         this.plugin
            .getLogger()
            .info(
               "[fixhearts]   modifier name="
                  + m.getName()
                  + " key="
                  + (String)keyDesc
                  + " uuid="
                  + m.getUniqueId()
                  + " amount="
                  + m.getAmount()
                  + " op="
                  + m.getOperation()
            );
      }

      if (this.plugin.getLifeAbilities() != null) {
         this.plugin.getLifeAbilities().cleanup(target);
      }

      if (this.plugin.getSoulManager() != null) {
         this.plugin.getSoulManager().cleanup(target);
      }

      int removed = 0;

      for (AttributeModifier m : new ArrayList(attr.getModifiers())) {
         attr.removeModifier(m);
         removed++;
      }

      double defaultBase = attr.getDefaultValue();
      attr.setBaseValue(defaultBase);
      target.setHealth(attr.getValue());
      target.sendMessage("§d§l❤ §fYour hearts have been restored.");
      if (sender != target) {
         sender.sendMessage("§aFixed hearts for §l" + target.getName() + "§a (removed " + removed + " modifier(s)).");
      }

      this.plugin.getLogger().info("[fixhearts] " + sender.getName() + " reset hearts for " + target.getName() + " (removed " + removed + " modifier(s))");
      return true;
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         String partial = args[0].toLowerCase();
         return Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
      } else {
         return new ArrayList<>();
      }
   }
}
