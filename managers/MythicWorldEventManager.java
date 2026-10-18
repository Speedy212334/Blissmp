package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class MythicWorldEventManager {
   private final BlissGems plugin;
   private final File dataFile;
   private YamlConfiguration config;
   private final Map<String, MythicWorldEventManager.BrokenMythicEntry> entries = new LinkedHashMap<>();
   private BukkitTask tickerTask;

   public MythicWorldEventManager(BlissGems plugin) {
      this.plugin = plugin;
      this.dataFile = new File(plugin.getDataFolder(), "broken_mythics.yml");
      this.loadData();
      this.startTicker();
   }

   public void loadData() {
      this.entries.clear();
      if (!this.dataFile.exists()) {
         try {
            this.dataFile.createNewFile();
         } catch (IOException e) {
            this.plugin.getLogger().severe("Could not create broken_mythics.yml: " + e.getMessage());
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.dataFile);
      ConfigurationSection sec = this.config.getConfigurationSection("broken_items");
      if (sec != null) {
         for (String key : sec.getKeys(false)) {
            MythicWorldEventManager.BrokenMythicEntry entry = new MythicWorldEventManager.BrokenMythicEntry();
            entry.id = key;
            entry.itemId = sec.getString(key + ".itemId");
            entry.displayName = sec.getString(key + ".displayName");
            entry.brokenTimestamp = sec.getLong(key + ".brokenTimestamp");
            entry.respawnTimestamp = sec.getLong(key + ".respawnTimestamp");
            entry.status = sec.getString(key + ".status", "PENDING");
            entry.respawnX = sec.getInt(key + ".respawnX", 0);
            entry.respawnY = sec.getInt(key + ".respawnY", 0);
            entry.respawnZ = sec.getInt(key + ".respawnZ", 0);
            this.entries.put(key, entry);
         }
      }
   }

   public void saveData() {
      if (this.config != null) {
         this.config.set("broken_items", null);

         for (MythicWorldEventManager.BrokenMythicEntry entry : this.entries.values()) {
            String path = "broken_items." + entry.id;
            this.config.set(path + ".itemId", entry.itemId);
            this.config.set(path + ".displayName", entry.displayName);
            this.config.set(path + ".brokenTimestamp", entry.brokenTimestamp);
            this.config.set(path + ".respawnTimestamp", entry.respawnTimestamp);
            this.config.set(path + ".status", entry.status);
            this.config.set(path + ".respawnX", entry.respawnX);
            this.config.set(path + ".respawnY", entry.respawnY);
            this.config.set(path + ".respawnZ", entry.respawnZ);
         }

         try {
            this.config.save(this.dataFile);
         } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save broken_mythics.yml: " + e.getMessage());
         }
      }
   }

   public void handleMythicBreak(ItemStack item, Player player, String reason) {
      String itemId = CustomItemManager.getIdByItem(item);
      if (itemId == null) {
         itemId = CustomItemManager.getMythicId(item);
      }

      String name = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
         ? item.getItemMeta().getDisplayName()
         : (itemId != null ? itemId : item.getType().name());
      MythicWorldEventManager.BrokenMythicEntry entry = new MythicWorldEventManager.BrokenMythicEntry();
      entry.id = UUID.randomUUID().toString().substring(0, 8);
      entry.itemId = itemId != null ? itemId : item.getType().name().toLowerCase();
      entry.displayName = name;
      entry.brokenTimestamp = System.currentTimeMillis();
      long timerDays = this.plugin.getConfig().getLong("mythic-respawn.timer-days", 7L);
      entry.respawnTimestamp = entry.brokenTimestamp + timerDays * 24L * 3600L * 1000L;
      entry.status = "PENDING";
      this.entries.put(entry.id, entry);
      this.saveData();
      String alert = ChatColor.translateAlternateColorCodes(
         '&', "&4&l[MYTHIC SHATTERED] &c" + name + " &7has been destroyed! It will reform somewhere in the world border in " + timerDays + " days."
      );

      for (Player p : Bukkit.getOnlinePlayers()) {
         p.sendMessage(alert);
         p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0F, 0.6F);
      }
   }

   private void startTicker() {
      if (this.tickerTask != null) {
         this.tickerTask.cancel();
      }

      this.tickerTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::checkRespawns, 200L, 600L);
   }

   private void checkRespawns() {
      long now = System.currentTimeMillis();
      boolean changed = false;

      for (MythicWorldEventManager.BrokenMythicEntry entry : this.entries.values()) {
         if (entry.isPending() && now >= entry.respawnTimestamp) {
            World world = (World)Bukkit.getWorlds().get(0);
            if (world != null) {
               int half = Math.max(1, this.plugin.getConfig().getInt("mythic-respawn.world-border-size", 10000) / 2);
               int x = ThreadLocalRandom.current().nextInt(-half, half + 1);
               int z = ThreadLocalRandom.current().nextInt(-half, half + 1);
               int y = world.getHighestBlockYAt(x, z);
               Block block = world.getBlockAt(x, y + 1, z);
               block.setType(Material.CHEST);
               if (block.getState() instanceof Chest) {
                  Chest chest = (Chest)block.getState();
                  ItemStack reward = CustomItemManager.getItemById(entry.itemId);
                  if (reward == null) {
                     reward = new ItemStack(Material.NETHERITE_CHESTPLATE);
                  }

                  chest.getBlockInventory().setItem(13, reward);
               }

               entry.status = "RESPAWNED";
               entry.respawnX = x;
               entry.respawnY = y + 1;
               entry.respawnZ = z;
               changed = true;
               String alert = ChatColor.translateAlternateColorCodes(
                  '&',
                  "&6&l[MYTHIC REBORN] &eA shattered mythic (&f"
                     + entry.displayName
                     + "&e) has reformed at &bX: "
                     + x
                     + ", Y: "
                     + (y + 1)
                     + ", Z: "
                     + z
                     + "&e! Coordinates revealed!"
               );

               for (Player p : Bukkit.getOnlinePlayers()) {
                  p.sendMessage(alert);
                  p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
               }
            }
         }
      }

      if (changed) {
         this.saveData();
      }
   }

   public List<MythicWorldEventManager.BrokenMythicEntry> getAllEntries() {
      return new ArrayList<>(this.entries.values());
   }

   public List<MythicWorldEventManager.BrokenMythicEntry> getActiveBrokenMythics() {
      List<MythicWorldEventManager.BrokenMythicEntry> list = new ArrayList<>();

      for (MythicWorldEventManager.BrokenMythicEntry e : this.entries.values()) {
         if (e.isPending()) {
            list.add(e);
         }
      }

      return list;
   }

   public void cleanup() {
      if (this.tickerTask != null) {
         this.tickerTask.cancel();
      }
   }

   public static class BrokenMythicEntry {
      public String id;
      public String itemId;
      public String displayName;
      public long brokenTimestamp;
      public long respawnTimestamp;
      public String status;
      public int respawnX;
      public int respawnY;
      public int respawnZ;

      public boolean isPending() {
         return "PENDING".equalsIgnoreCase(this.status);
      }

      public long getRemainingMillis() {
         return Math.max(0L, this.respawnTimestamp - System.currentTimeMillis());
      }

      public String getRemainingFormatted() {
         long millis = this.getRemainingMillis();
         if (millis <= 0L) {
            return "Ready to respawn";
         } else {
            long seconds = millis / 1000L;
            long days = seconds / 86400L;
            seconds %= 86400L;
            long hours = seconds / 3600L;
            seconds %= 3600L;
            long minutes = seconds / 60L;
            if (days > 0L) {
               return String.format("%dd %dh %dm", days, hours, minutes);
            } else {
               return hours > 0L ? String.format("%dh %dm", hours, minutes) : String.format("%dm %ds", minutes, seconds % 60L);
            }
         }
      }
   }
}
