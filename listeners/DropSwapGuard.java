package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class DropSwapGuard implements Listener {
   private final BlissGems plugin;
   private volatile long tick = 0L;
   private final Map<UUID, Long> lastDropTick = new HashMap<>();
   private final Map<UUID, Long> lastHeldTick = new HashMap<>();

   public DropSwapGuard(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void start() {
      (new BukkitRunnable() {
         public void run() {
            DropSwapGuard.this.tick++;
            if (DropSwapGuard.this.tick % 200L == 0L) {
               DropSwapGuard.this.lastDropTick.entrySet().removeIf(e -> DropSwapGuard.this.tick - e.getValue() > 5L);
               DropSwapGuard.this.lastHeldTick.entrySet().removeIf(e -> DropSwapGuard.this.tick - e.getValue() > 5L);
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
   public void onHeld(PlayerItemHeldEvent event) {
      UUID id = event.getPlayer().getUniqueId();
      Long dropped = this.lastDropTick.get(id);
      if (dropped != null && dropped == this.tick) {
         event.setCancelled(true);
         this.resync(event.getPlayer());
      } else {
         this.lastHeldTick.put(id, this.tick);
      }
   }

   @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
   public void onDrop(PlayerDropItemEvent event) {
      UUID id = event.getPlayer().getUniqueId();
      Long held = this.lastHeldTick.get(id);
      if (held != null && held == this.tick) {
         event.setCancelled(true);
         this.resync(event.getPlayer());
      } else {
         this.lastDropTick.put(id, this.tick);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      UUID id = event.getPlayer().getUniqueId();
      this.lastDropTick.remove(id);
      this.lastHeldTick.remove(id);
   }

   private void resync(Player player) {
      this.plugin.getServer().getScheduler().runTask(this.plugin, () -> player.updateInventory());
   }
}
