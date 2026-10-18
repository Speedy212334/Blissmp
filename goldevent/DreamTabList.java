package dev.xoperr.blissgems.goldevent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class DreamTabList implements Listener {
   private final Plugin plugin;

   public DreamTabList(Plugin plugin) {
      this.plugin = plugin;
   }

   private static boolean dreamSide(Player p) {
      return GoldenDreamWorld.isDreamWorld(p.getWorld());
   }

   public void refresh(Player p, boolean relist) {
      if (p.isOnline()) {
         for (Player other : Bukkit.getOnlinePlayers()) {
            if (other != p) {
               boolean same = dreamSide(other) == dreamSide(p);
               if (relist && other.canSee(p)) {
                  other.listPlayer(p);
               }

               list(other, p, same);
               list(p, other, same);
            }
         }
      }
   }

   public void refreshAll() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         this.refresh(p, false);
      }
   }

   public void listAll() {
      for (Player a : Bukkit.getOnlinePlayers()) {
         for (Player b : Bukkit.getOnlinePlayers()) {
            if (a != b) {
               list(a, b, true);
            }
         }
      }
   }

   private static void list(Player viewer, Player target, boolean show) {
      if (viewer.canSee(target)) {
         if (show) {
            if (!viewer.isListed(target)) {
               viewer.listPlayer(target);
            }
         } else if (viewer.isListed(target)) {
            viewer.unlistPlayer(target);
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onJoin(PlayerJoinEvent event) {
      Bukkit.getScheduler().runTask(this.plugin, () -> this.refresh(event.getPlayer(), true));
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onWorld(PlayerChangedWorldEvent event) {
      if (GoldenDreamWorld.isDreamWorld(event.getFrom()) != dreamSide(event.getPlayer())) {
         this.refresh(event.getPlayer(), false);
      }
   }
}
