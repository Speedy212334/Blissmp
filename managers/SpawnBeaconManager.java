package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.BanList.Type;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpawnBeaconManager {
   private final BlissGems plugin;
   private Location beaconLocation;
   private double radius;
   private List<Integer> openHoursUtc;
   private int durationMinutes;
   private boolean requireBeaconForRepairs;
   private long lastReviveBeaconTossTime = 0L;
   private UUID lastReviveBeaconTosser = null;

   public SpawnBeaconManager(BlissGems plugin) {
      this.plugin = plugin;
      this.reload();
   }

   public void reload() {
      String worldName = this.plugin.getConfig().getString("spawn-beacon.world", "world");
      World world = Bukkit.getWorld(worldName);
      if (world == null && !Bukkit.getWorlds().isEmpty()) {
         world = (World)Bukkit.getWorlds().get(0);
      }

      double x = this.plugin.getConfig().getDouble("spawn-beacon.x", 0.5);
      double y = this.plugin.getConfig().getDouble("spawn-beacon.y", 64.0);
      double z = this.plugin.getConfig().getDouble("spawn-beacon.z", 0.5);
      this.beaconLocation = world != null ? new Location(world, x, y, z) : null;
      this.radius = this.plugin.getConfig().getDouble("spawn-beacon.radius", 15.0);
      this.openHoursUtc = this.plugin.getConfig().getIntegerList("spawn-beacon.repair-schedule.open-hours-utc");
      if (this.openHoursUtc.isEmpty()) {
         this.openHoursUtc = Arrays.asList(0, 6, 12, 18);
      }

      this.durationMinutes = this.plugin.getConfig().getInt("spawn-beacon.repair-schedule.duration-minutes", 60);
      this.requireBeaconForRepairs = this.plugin.getConfig().getBoolean("spawn-beacon.require-beacon-for-repairs", true);
   }

   public Location getBeaconLocation() {
      return this.beaconLocation;
   }

   public double getRadius() {
      return this.radius;
   }

   public boolean isRequireBeaconForRepairs() {
      return this.requireBeaconForRepairs;
   }

   public boolean isWithinBeacon(Location loc) {
      if (this.beaconLocation != null && loc != null) {
         return loc.getWorld() != this.beaconLocation.getWorld() ? false : loc.distanceSquared(this.beaconLocation) <= this.radius * this.radius;
      } else {
         return false;
      }
   }

   public boolean isRepairWindowOpen() {
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      int currentHour = now.getHour();
      int currentMinute = now.getMinute();

      for (int openHour : this.openHoursUtc) {
         int startMinute = openHour * 60;
         int endMinute = startMinute + this.durationMinutes;
         int nowMinute = currentHour * 60 + currentMinute;
         if (nowMinute >= startMinute && nowMinute < endMinute) {
            return true;
         }
      }

      return false;
   }

   public String getRepairScheduleStatusString() {
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      int currentHour = now.getHour();
      int currentMinute = now.getMinute();
      int nowMinute = currentHour * 60 + currentMinute;

      for (int openHour : this.openHoursUtc) {
         int startMinute = openHour * 60;
         int endMinute = startMinute + this.durationMinutes;
         if (nowMinute >= startMinute && nowMinute < endMinute) {
            int remainingMinutes = endMinute - nowMinute;
            return ChatColor.GREEN + "OPEN " + ChatColor.GRAY + "(Closes in " + remainingMinutes + "m)";
         }
      }

      int minDiff = Integer.MAX_VALUE;

      for (int openHour : this.openHoursUtc) {
         int startMinute = openHour * 60;
         int diff = startMinute - nowMinute;
         if (diff < 0) {
            diff += 1440;
         }

         if (diff < minDiff) {
            minDiff = diff;
         }
      }

      int h = minDiff / 60;
      int m = minDiff % 60;
      return ChatColor.RED + "CLOSED " + ChatColor.GRAY + "(Opens in " + (h > 0 ? h + "h " : "") + m + "m)";
   }

   public boolean canRepairGems(Player player) {
      if (!this.requireBeaconForRepairs) {
         return true;
      } else if (!this.isWithinBeacon(player.getLocation())) {
         player.sendMessage(
            ChatColor.translateAlternateColorCodes(
               '&',
               "&c&l[SPAWN BEACON] &cGem repairs can only be conducted at the Spawn Beacon! &7(Location: &fX: "
                  + (int)this.beaconLocation.getX()
                  + ", Y: "
                  + (int)this.beaconLocation.getY()
                  + ", Z: "
                  + (int)this.beaconLocation.getZ()
                  + "&7)"
            )
         );
         return false;
      } else if (!this.isRepairWindowOpen()) {
         player.sendMessage(
            ChatColor.translateAlternateColorCodes(
               '&',
               "&c&l[SPAWN BEACON] &cThe Spawn Beacon repair matrix is currently &lCLOSED&c! &7Status: "
                  + this.getRepairScheduleStatusString()
                  + ". &eUse /news for schedule."
            )
         );
         return false;
      } else {
         return true;
      }
   }

   public void onReviveBeaconDropped(Player tosser, Item itemEntity) {
      if (this.isWithinBeacon(itemEntity.getLocation())) {
         this.lastReviveBeaconTossTime = System.currentTimeMillis();
         this.lastReviveBeaconTosser = tosser.getUniqueId();
         itemEntity.remove();
         Location loc = this.beaconLocation != null ? this.beaconLocation : tosser.getLocation();
         loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0.0, 1.0, 0.0), 50, 1.0, 2.0, 1.0, 0.1);
         loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.0, 1.0, 0.0), 60, 0.5, 1.5, 0.5, 0.2);
         loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.5F, 1.2F);
         loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.8F);
         tosser.sendMessage(
            ChatColor.translateAlternateColorCodes(
               '&', "&e&l✦ [SPAWN BEACON] &6Revive Beacon absorbed! Toss the victim's &fPlayer Head &6at the beacon within 30 seconds to resurrect them!"
            )
         );
      }
   }

   public boolean onPlayerHeadDropped(Player tosser, Item headEntity) {
      if (!this.isWithinBeacon(headEntity.getLocation())) {
         return false;
      } else if (System.currentTimeMillis() - this.lastReviveBeaconTossTime > 30000L) {
         return false;
      } else {
         ItemStack item = headEntity.getItemStack();
         if (item.getType() != Material.PLAYER_HEAD) {
            return false;
         } else {
            SkullMeta meta = (SkullMeta)item.getItemMeta();
            if (meta == null) {
               return false;
            } else {
               OfflinePlayer target = meta.getOwningPlayer();
               if (target != null && target.getName() != null) {
                  headEntity.remove();
                  this.lastReviveBeaconTossTime = 0L;
                  this.resurrectPlayer(tosser, target);
                  return true;
               } else {
                  tosser.sendMessage(ChatColor.RED + "This player head is unidentifiable.");
                  return false;
               }
            }
         }
      }
   }

   public void resurrectPlayer(Player summoner, OfflinePlayer victim) {
      Location targetLoc = this.beaconLocation != null ? this.beaconLocation.clone().add(0.0, 1.0, 0.0) : summoner.getLocation();

      try {
         BanList banList = Bukkit.getBanList(Type.PROFILE);
         if (banList.isBanned(victim.getName())) {
            banList.pardon(victim.getName());
         }

         if (victim.getUniqueId() != null && banList.isBanned(victim.getUniqueId().toString())) {
            banList.pardon(victim.getUniqueId().toString());
         }
      } catch (Exception var8) {
      }

      int restoreAmount = this.plugin.getConfig().getInt("revive-beacon.restore-energy", 3);
      if (victim.isOnline()) {
         Player onlineVictim = victim.getPlayer();
         this.plugin.getEnergyManager().setEnergy(onlineVictim, restoreAmount);
         onlineVictim.teleport(targetLoc);
         double maxHealth = onlineVictim.getAttribute(Attributes.maxHealth()).getValue();
         onlineVictim.setHealth(maxHealth);
         onlineVictim.setFoodLevel(20);
         onlineVictim.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
         onlineVictim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
         onlineVictim.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
         onlineVictim.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, targetLoc, 150, 1.0, 2.0, 1.0, 0.3);
         onlineVictim.getWorld().playSound(targetLoc, Sound.ITEM_TOTEM_USE, 1.0F, 1.0F);
         onlineVictim.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.0F);
         onlineVictim.sendMessage(
            ChatColor.translateAlternateColorCodes(
               '&', "&a&l✦ [RESURRECTION] &fYou have been brought back from death at the Spawn Beacon by &e" + summoner.getName() + "&f!"
            )
         );
      } else {
         this.plugin.getEnergyManager().setEnergy(victim.getUniqueId(), restoreAmount);
      }

      String broadcast = ChatColor.translateAlternateColorCodes(
         '&', "&d&l✦ [RESURRECTION] &e" + victim.getName() + " &7has been resurrected at the &bSpawn Beacon &7by &e" + summoner.getName() + "&7!"
      );

      for (Player p : Bukkit.getOnlinePlayers()) {
         p.sendMessage(broadcast);
         p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.2F);
      }
   }
}
