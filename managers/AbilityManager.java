package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class AbilityManager {
   private final BlissGems plugin;
   private final Map<UUID, Map<String, Long>> cooldowns;
   private final Map<UUID, Map<String, Long>> activeAbilities = new HashMap<>();
   private final Map<UUID, Map<String, BukkitTask>> durationTasks = new HashMap<>();
   private final File cooldownDataFolder;
   private final Set<UUID> noCooldown = new HashSet<>();

   public AbilityManager(BlissGems plugin) {
      this.plugin = plugin;
      this.cooldowns = new HashMap<>();
      this.cooldownDataFolder = new File(plugin.getDataFolder(), "cooldowns");
      if (!this.cooldownDataFolder.exists()) {
         this.cooldownDataFolder.mkdirs();
      }
   }

   public boolean toggleNoCooldown(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.noCooldown.remove(uuid)) {
         return false;
      }

      this.noCooldown.add(uuid);
      this.clearCooldowns(player);
      return true;
   }

   public boolean hasNoCooldown(Player player) {
      return this.noCooldown.contains(player.getUniqueId());
   }

   public boolean isOnCooldown(Player player, String abilityKey) {
      if (this.noCooldown.contains(player.getUniqueId())) {
         return false;
      }

      if (this.isAbilityActive(player, abilityKey)) {
         return true;
      }

      Map<String, Long> playerCooldowns = this.cooldowns.get(player.getUniqueId());
      if (playerCooldowns == null) {
         return false;
      }

      Long endTime = playerCooldowns.get(abilityKey);
      return endTime == null ? false : System.currentTimeMillis() < endTime;
   }

   public int getRemainingCooldown(Player player, String abilityKey) {
      if (this.noCooldown.contains(player.getUniqueId())) {
         return 0;
      }

      Map<String, Long> playerCooldowns = this.cooldowns.get(player.getUniqueId());
      if (playerCooldowns == null) {
         return 0;
      }

      Long endTime = playerCooldowns.get(abilityKey);
      if (endTime == null) {
         return 0;
      }

      long remaining = endTime - System.currentTimeMillis();
      return remaining > 0L ? (int)Math.ceil(remaining / 1000.0) : 0;
   }

   public void setCooldown(Player player, String abilityKey, int seconds) {
      if (!this.noCooldown.contains(player.getUniqueId())) {
         Map playerCooldowns = this.cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
         playerCooldowns.put(abilityKey, System.currentTimeMillis() + seconds * 1000L);
         this.saveCooldowns(player.getUniqueId());
      }
   }

   public boolean isAbilityActive(Player player, String abilityKey) {
      if (player != null && !this.noCooldown.contains(player.getUniqueId())) {
         Map<String, Long> active = this.activeAbilities.get(player.getUniqueId());
         if (active == null) {
            return false;
         } else {
            Long endTime = active.get(abilityKey);
            if (endTime == null) {
               return false;
            } else if (System.currentTimeMillis() >= endTime) {
               this.endAbilityDuration(player, abilityKey);
               return false;
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public int getRemainingActiveDuration(Player player, String abilityKey) {
      if (player != null && !this.noCooldown.contains(player.getUniqueId())) {
         Map<String, Long> active = this.activeAbilities.get(player.getUniqueId());
         if (active == null) {
            return 0;
         }

         Long endTime = active.get(abilityKey);
         if (endTime == null) {
            return 0;
         }

         long remaining = endTime - System.currentTimeMillis();
         return remaining > 0L ? (int)Math.ceil(remaining / 1000.0) : 0;
      } else {
         return 0;
      }
   }

   public boolean canUseAbility(Player player, String abilityKey) {
      if (this.plugin.getRegionManager() != null && this.plugin.getRegionManager().areGemsDisabled(player)) {
         String message = this.plugin.getRegionManager().getDisabledMessage();
         if (message != null && !message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
         }

         return false;
      } else if (!this.plugin.getEnergyManager().canUseAbilities(player)) {
         this.plugin.getConfigManager().sendFormattedMessage(player, "ability-no-energy");
         return false;
      } else if (!abilityKey.startsWith("astra-") && this.plugin.getAstraAbilities().isAbilitySuppressed(player)) {
         player.sendMessage("§4§l§oYour gem abilities are nullified by a Dimensional Void!");
         return false;
      } else if (this.isAbilityActive(player, abilityKey)) {
         player.sendMessage("§c§oThis ability is currently active!");
         return false;
      } else {
         return !this.isOnCooldown(player, abilityKey);
      }
   }

   public void useAbility(Player player, String abilityKey) {
      int cooldown = this.plugin.getConfigManager().getAbilityCooldown(abilityKey);
      this.setCooldown(player, abilityKey, cooldown);
   }

   public void useAbilityWithDuration(final Player player, final String abilityKey, int durationSeconds) {
      if (player != null) {
         if (!this.noCooldown.contains(player.getUniqueId())) {
            UUID uuid = player.getUniqueId();
            Map playerTasks = this.durationTasks.computeIfAbsent(uuid, k -> new HashMap<>());
            BukkitTask oldTask = (BukkitTask)playerTasks.remove(abilityKey);
            if (oldTask != null) {
               oldTask.cancel();
            }

            if (durationSeconds <= 0) {
               this.useAbility(player, abilityKey);
            } else {
               Map playerActive = this.activeAbilities.computeIfAbsent(uuid, k -> new HashMap<>());
               playerActive.put(abilityKey, System.currentTimeMillis() + durationSeconds * 1000L);
               BukkitTask task = (new BukkitRunnable() {
                  public void run() {
                     AbilityManager.this.endAbilityDuration(player, abilityKey);
                  }
               }).runTaskLater(this.plugin, durationSeconds * 20L);
               playerTasks.put(abilityKey, task);
            }
         }
      }
   }

   public void endAbilityDuration(Player player, String abilityKey) {
      if (player != null) {
         UUID uuid = player.getUniqueId();
         Map<String, BukkitTask> playerTasks = this.durationTasks.get(uuid);
         BukkitTask task;
         if (playerTasks != null && (task = playerTasks.remove(abilityKey)) != null) {
            task.cancel();
         }

         Map<String, Long> playerActive;
         boolean wasActive = (playerActive = this.activeAbilities.get(uuid)) != null && playerActive.remove(abilityKey) != null;
         if (wasActive && !this.noCooldown.contains(uuid)) {
            this.useAbility(player, abilityKey);
         }
      }
   }

   public void clearCooldowns(Player player) {
      UUID uuid = player.getUniqueId();
      Map<String, BukkitTask> tasks = this.durationTasks.remove(uuid);
      if (tasks != null) {
         for (BukkitTask task : tasks.values()) {
            if (task != null) {
               task.cancel();
            }
         }
      }

      this.activeAbilities.remove(uuid);
      this.cooldowns.remove(uuid);
      File cooldownFile = new File(this.cooldownDataFolder, player.getUniqueId() + ".yml");
      if (cooldownFile.exists()) {
         cooldownFile.delete();
      }
   }

   public void clearCache(UUID uuid) {
      Map<String, BukkitTask> tasks = this.durationTasks.remove(uuid);
      if (tasks != null) {
         for (BukkitTask task : tasks.values()) {
            if (task != null) {
               task.cancel();
            }
         }
      }

      this.activeAbilities.remove(uuid);
      this.cooldowns.remove(uuid);
   }

   public void loadCooldowns(UUID uuid) {
      File cooldownFile = new File(this.cooldownDataFolder, uuid + ".yml");
      if (cooldownFile.exists()) {
         YamlConfiguration config = YamlConfiguration.loadConfiguration(cooldownFile);
         HashMap<String, Long> playerCooldowns = new HashMap<>();
         Set<String> keys = config.getKeys(false);
         if (keys != null) {
            for (String key : keys) {
               long endTime = config.getLong(key, 0L);
               if (endTime > System.currentTimeMillis()) {
                  playerCooldowns.put(key, endTime);
               }
            }
         }

         if (!playerCooldowns.isEmpty()) {
            this.cooldowns.put(uuid, playerCooldowns);
         }
      }
   }

   private void saveCooldowns(UUID uuid) {
      Map<String, Long> playerCooldowns = this.cooldowns.get(uuid);
      if (playerCooldowns != null && !playerCooldowns.isEmpty()) {
         File cooldownFile = new File(this.cooldownDataFolder, uuid + ".yml");
         YamlConfiguration config = new YamlConfiguration();

         for (Entry<String, Long> entry : playerCooldowns.entrySet()) {
            if (entry.getValue() > System.currentTimeMillis()) {
               config.set(entry.getKey(), entry.getValue());
            }
         }

         try {
            config.save(cooldownFile);
         } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save cooldowns for " + uuid + ": " + e.getMessage());
         }
      }
   }

   public Map<String, Long> getCooldownsFor(UUID uuid) {
      Map<String, Long> cds = this.cooldowns.get(uuid);
      return cds == null ? Collections.emptyMap() : new HashMap<>(cds);
   }

   public Map<String, Long> getActiveAbilitiesFor(UUID uuid) {
      Map<String, Long> active = this.activeAbilities.get(uuid);
      return active == null ? Collections.emptyMap() : new HashMap<>(active);
   }

   public void saveAllCooldowns() {
      for (UUID uuid : this.cooldowns.keySet()) {
         this.saveCooldowns(uuid);
      }
   }
}
