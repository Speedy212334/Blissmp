package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.MythicWorldEventManager;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NewsCommand implements CommandExecutor {
   private final BlissGems plugin;

   public NewsCommand(BlissGems plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      sender.sendMessage("");
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l================== &e&lBLISSGEMS NEWS &6&l=================="));
      sender.sendMessage("");
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e✦ &6&lMYTHIC ARTIFACT RESPAWNS:"));
      MythicWorldEventManager mythicManager = this.plugin.getMythicWorldEventManager();
      if (mythicManager != null) {
         List<MythicWorldEventManager.BrokenMythicEntry> broken = mythicManager.getAllEntries();
         if (broken.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  • All mythic artifacts are intact and in player hands.");
         } else {
            for (MythicWorldEventManager.BrokenMythicEntry entry : broken) {
               if (entry.isPending()) {
                  sender.sendMessage(
                     ChatColor.translateAlternateColorCodes('&', "  &7• &f" + entry.displayName + "&7: &cRespawns in " + entry.getRemainingFormatted())
                  );
               } else {
                  sender.sendMessage(
                     ChatColor.translateAlternateColorCodes(
                        '&', "  &7• &f" + entry.displayName + "&7: &aReformed at X: " + entry.respawnX + ", Y: " + entry.respawnY + ", Z: " + entry.respawnZ
                     )
                  );
               }
            }
         }
      } else {
         sender.sendMessage(ChatColor.GRAY + "  • World event manager offline.");
      }

      sender.sendMessage("");
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b✦ &3&lSPAWN PEDESTAL:"));
      PedestalManager pedestal = this.plugin.getPedestalManager();
      Location main = pedestal != null ? pedestal.state().mainLoc() : null;
      if (main == null) {
         sender.sendMessage(ChatColor.GRAY + "  • Not set up on this server yet.");
      } else {
         sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Status: " + (pedestal.state().active() ? "&aOPEN" : "&cCLOSED")));
         sender.sendMessage(
            ChatColor.translateAlternateColorCodes('&', "  &7• Coordinates: &fX: " + main.getBlockX() + ", Y: " + main.getBlockY() + ", Z: " + main.getBlockZ())
         );
      }

      sender.sendMessage("");
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d✦ &5&lCOMBAT & WORLD RULES:"));
      int pearlCd = this.plugin.getEnchantLimiterManager() != null ? this.plugin.getEnchantLimiterManager().getEnderPearlCooldownSeconds() : 15;
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Ender Pearl Cooldown: &e" + pearlCd + "s"));
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Tempered Mace: &cMax Density II &7| &cNo Breach/WindBurst &7| &660s Combat CD"));
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Mythic Netherite Set: &aProt III &7| &d2x Durability (50% damage reduction)"));
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Merchants: &e1 Trade per item per player &7| &dShift+RClick for Soul Form"));
      sender.sendMessage("");
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l====================================================="));
      sender.sendMessage("");
      return true;
   }
}
