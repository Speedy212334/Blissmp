package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StatsCommand {
   private final BlissGems plugin;

   public StatsCommand(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void execute(Player player, String[] args) {
      if (args.length == 0) {
         this.showTopKillers(player);
      } else {
         switch (args[0].toLowerCase()) {
            case "top":
               this.showTopKillers(player);
               break;
            case "me":
            case "my":
            case "personal":
               this.showPersonalStats(player);
               break;
            case "gems":
            case "usage":
               this.showGemUsage(player);
               break;
            default:
               player.sendMessage("§c/bliss stats [top|me|gems]");
         }
      }
   }

   private void showTopKillers(Player player) {
      player.sendMessage("§6§m" + "═".repeat(50));
      player.sendMessage("§e§l\ud83c\udfc6 TOP KILLERS \ud83c\udfc6");
      player.sendMessage("§6§m" + "═".repeat(50));
      List<Entry<UUID, Integer>> topKillers = this.plugin.getStatsManager().getTopKillers(10);
      if (topKillers.isEmpty()) {
         player.sendMessage("§7No kills recorded yet.");
      } else {
         int rank = 1;

         for (Entry<UUID, Integer> entry : topKillers) {
            Player target = Bukkit.getPlayer(entry.getKey());
            String playerName = target != null ? target.getName() : "Unknown";

            String medal = switch (rank) {
               case 1 -> "\ud83e\udd47";
               case 2 -> "\ud83e\udd48";
               case 3 -> "\ud83e\udd49";
               default -> "  ";
            };
            player.sendMessage("§e" + medal + " §7#" + rank + " §f" + playerName + " §7- §f" + entry.getValue() + " §7kills");
            rank++;
         }
      }

      player.sendMessage("§6§m" + "═".repeat(50));
      player.sendMessage("§8Use §7/bliss stats me §8for your personal stats");
      player.sendMessage("§8Use §7/bliss stats gems §8for gem distribution");
   }

   private void showPersonalStats(Player player) {
      player.sendMessage("§6§m" + "═".repeat(50));
      player.sendMessage("§e§l\ud83d\udcca YOUR STATS \ud83d\udcca");
      player.sendMessage("§6§m" + "═".repeat(50));
      int kills = this.plugin.getStatsManager().getKills(player);
      int deaths = this.plugin.getStatsManager().getDeaths(player);
      long timePlayedMs = this.plugin.getStatsManager().getTimePlayed(player);
      GemType currentGem = this.plugin.getGemManager().getGemType(player);
      int energy = this.plugin.getEnergyManager().getEnergy(player);
      player.sendMessage("§7Kills: §f" + kills);
      player.sendMessage("§7Deaths: §f" + deaths);
      player.sendMessage("§7K/D Ratio: §f" + (deaths == 0 ? kills : String.format("%.2f", (double)kills / deaths)));
      player.sendMessage("§7Time Played: §f" + this.formatTime(timePlayedMs));
      player.sendMessage("§7Current Gem: §f" + (currentGem != null ? currentGem.getDisplayName() : "None"));
      player.sendMessage("§7Energy: §f" + energy + "§8/§f10");
      player.sendMessage("§6§m" + "═".repeat(50));
   }

   private void showGemUsage(Player player) {
      player.sendMessage("§6§m" + "═".repeat(50));
      player.sendMessage("§e§l\ud83d\udc8e GEM DISTRIBUTION \ud83d\udc8e");
      player.sendMessage("§6§m" + "═".repeat(50));
      Map<String, Integer> gemUsage = this.plugin.getStatsManager().getGemUsageStats();

      for (Entry<String, Integer> entry : gemUsage.entrySet()) {
         String gem = entry.getKey();
         int count = entry.getValue();

         String icon = switch (gem) {
            case "Fire Gem" -> "\ud83d\udd25";
            case "Speed Gem" -> "⚡";
            case "Wealth Gem" -> "\ud83d\udcb0";
            case "Astra Gem" -> "✨";
            case "Puff Gem" -> "\ud83d\udca8";
            case "Flux Gem" -> "⚡";
            case "Life Gem" -> "❤️";
            case "Strength Gem" -> "\ud83d\udcaa";
            default -> "  ";
         };
         player.sendMessage("§7" + icon + " " + gem + ": §f" + count + " §7player" + (count == 1 ? "" : "s"));
      }

      player.sendMessage("§6§m" + "═".repeat(50));
   }

   private String formatTime(long milliseconds) {
      long seconds = milliseconds / 1000L;
      long minutes = seconds / 60L;
      long hours = minutes / 60L;
      long days = hours / 24L;
      if (days > 0L) {
         return days + "d " + hours % 24L + "h";
      } else if (hours > 0L) {
         return hours + "h " + minutes % 60L + "m";
      } else {
         return minutes > 0L ? minutes + "m " + seconds % 60L + "s" : seconds + "s";
      }
   }
}
