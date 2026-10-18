package dev.xoperr.blissgems.goldevent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class WakeFloat {
   private static final double RISE = 0.07;
   private static final double HEIGHT = 20.0;
   private static final Map<UUID, Location> FROM = new HashMap<>();
   private static BukkitTask task;

   private WakeFloat() {
   }

   public static int start(Plugin plugin, GoldenDream dream) {
      int added = 0;

      for (Player p : Bukkit.getOnlinePlayers()) {
         if (!FROM.containsKey(p.getUniqueId())
            && GoldenDreamWorld.isDreamWorld(p.getWorld())
            && !p.hasMetadata("NPC")
            && !dream.roster().isMemory(p.getUniqueId())
            && !p.isDead()) {
            FROM.put(p.getUniqueId(), p.getLocation().clone());
            p.setGravity(false);
            p.setAllowFlight(true);
            added++;
         }
      }

      if (!FROM.isEmpty() && task == null) {
         task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Entry<UUID, Location>> it = FROM.entrySet().iterator();

            while (it.hasNext()) {
               Entry<UUID, Location> e = it.next();
               Player px = Bukkit.getPlayer(e.getKey());
               if (px == null || !px.isOnline()) {
                  it.remove();
               } else if (!px.isDead() && GoldenDreamWorld.isDreamWorld(px.getWorld())) {
                  if (px.getLocation().getY() < e.getValue().getY() + 20.0) {
                     px.setVelocity(new Vector(0.0, 0.07, 0.0));
                  } else {
                     ground(px);
                     if (dream.world().inDream(px.getUniqueId())) {
                        dream.world().wake(px);
                     } else if (e.getValue().isWorldLoaded()) {
                        px.setVelocity(new Vector());
                        px.teleport(e.getValue());
                        px.setFallDistance(0.0F);
                     }

                     px.kick(Component.text("Wake Up."));
                     it.remove();
                  }
               } else {
                  ground(px);
                  it.remove();
               }
            }

            if (FROM.isEmpty() && task != null) {
               task.cancel();
               task = null;
            }
         }, 0L, 1L);
         return added;
      } else {
         return added;
      }
   }

   private static void ground(Player p) {
      p.setGravity(true);
      if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
         p.setAllowFlight(false);
      }
   }

   public static void shutdown() {
      for (UUID id : FROM.keySet()) {
         Player p = Bukkit.getPlayer(id);
         if (p != null) {
            ground(p);
         }
      }

      FROM.clear();
      if (task != null) {
         task.cancel();
      }

      task = null;
   }
}
