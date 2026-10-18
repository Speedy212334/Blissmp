package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class ReviveBeaconManager {
   private final BlissGems plugin;
   private final Map<UUID, ReviveBeaconManager.ReviveBeacon> activeBeacons = new HashMap<>();

   public ReviveBeaconManager(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void activateBeacon(Player player, Location location, int duration, double range) {
      UUID playerId = player.getUniqueId();
      ReviveBeaconManager.ReviveBeacon previous = this.activeBeacons.get(playerId);
      if (previous != null) {
         previous.cancel();
      }

      ReviveBeaconManager.ReviveBeacon beacon = new ReviveBeaconManager.ReviveBeacon(this.plugin, player, location, duration, range);
      this.activeBeacons.put(playerId, beacon);
      beacon.start();
   }

   public boolean canRevive(Player player) {
      ReviveBeaconManager.ReviveBeacon beacon = this.activeBeacons.get(player.getUniqueId());
      return beacon != null && beacon.isActive() && beacon.isInRange(player.getLocation());
   }

   public Location getReviveLocation(Player player) {
      ReviveBeaconManager.ReviveBeacon beacon = this.activeBeacons.get(player.getUniqueId());
      return beacon != null ? beacon.getLocation() : null;
   }

   public void removeBeacon(Player player) {
      ReviveBeaconManager.ReviveBeacon beacon = this.activeBeacons.remove(player.getUniqueId());
      if (beacon != null) {
         beacon.cancel();
      }
   }

   public void cleanup() {
      for (ReviveBeaconManager.ReviveBeacon beacon : this.activeBeacons.values()) {
         beacon.cancel();
      }

      this.activeBeacons.clear();
   }

   private static class ReviveBeacon {
      private final BlissGems plugin;
      private final Player player;
      private final Location location;
      private final double range;
      private final long expiryTime;
      private int taskId = -1;
      private boolean active = true;

      public ReviveBeacon(BlissGems plugin, Player player, Location location, int duration, double range) {
         this.plugin = plugin;
         this.player = player;
         this.location = location.clone();
         this.range = range;
         this.expiryTime = System.currentTimeMillis() + duration * 1000L;
      }

      public void start() {
         int durationTicks = (int)((this.expiryTime - System.currentTimeMillis()) / 50L);
         this.taskId = this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            this.active = false;
            this.player.sendMessage("§c§lYour Revive Beacon has expired!");
         }, durationTicks);
         this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, task -> {
            if (this.active && System.currentTimeMillis() < this.expiryTime) {
               if (this.location.getWorld() != null) {
                  this.location.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, this.location.clone().add(0.0, 1.0, 0.0), 10, 0.5, 0.5, 0.5, 0.05);
               }
            } else {
               task.cancel();
            }
         }, 0L, 20L);
      }

      public boolean isActive() {
         return this.active && System.currentTimeMillis() < this.expiryTime;
      }

      public boolean isInRange(Location loc) {
         return this.location.getWorld() != null && this.location.getWorld().equals(loc.getWorld()) ? this.location.distance(loc) <= this.range : false;
      }

      public Location getLocation() {
         return this.location.clone();
      }

      public void cancel() {
         this.active = false;
         if (this.taskId != -1) {
            this.plugin.getServer().getScheduler().cancelTask(this.taskId);
         }
      }
   }
}
