package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World.Environment;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class EndSkyVisualManager {
   private final BlissGems plugin;
   private final BossBar endSkyBossBar;
   private final Set<UUID> playersInEnd = new HashSet<>();
   private BukkitTask task;
   private boolean enabled;

   public EndSkyVisualManager(BlissGems plugin) {
      this.plugin = plugin;
      this.enabled = plugin.getConfig().getBoolean("end-sky.enabled", true);
      this.endSkyBossBar = Bukkit.createBossBar(
         ChatColor.translateAlternateColorCodes('&', "&2✦ &a&lTHE END &2✦"), BarColor.GREEN, BarStyle.SOLID, new BarFlag[0]
      );
      this.endSkyBossBar.setVisible(true);
      if (this.enabled) {
         this.start();
      }
   }

   public void start() {
      if (this.task != null) {
         this.task.cancel();
      }

      this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
   }

   private void tick() {
      Set<UUID> currentInIsland = new HashSet<>();
      DustOptions darkGreenDust = new DustOptions(Color.fromRGB(15, 60, 20), 1.8F);
      DustOptions limeDust = new DustOptions(Color.fromRGB(50, 160, 60), 1.2F);

      for (Player player : Bukkit.getOnlinePlayers()) {
         World world = player.getWorld();
         if (world.getEnvironment() == Environment.THE_END || world.getName().toLowerCase().endsWith("_the_end")) {
            Location loc = player.getLocation();
            double distSq = loc.getX() * loc.getX() + loc.getZ() * loc.getZ();
            if (distSq <= 360000.0) {
               currentInIsland.add(player.getUniqueId());
               if (!this.endSkyBossBar.getPlayers().contains(player)) {
                  this.endSkyBossBar.addPlayer(player);
               }

               Location eyeLoc = player.getEyeLocation();

               for (int i = 0; i < (this.plugin.getConfig().getBoolean("end-sky.green-black-gradient", true) ? 15 : 0); i++) {
                  double ox = ThreadLocalRandom.current().nextDouble(-15.0, 15.0);
                  double oy = ThreadLocalRandom.current().nextDouble(5.0, 20.0);
                  double oz = ThreadLocalRandom.current().nextDouble(-15.0, 15.0);
                  Location pLoc = eyeLoc.clone().add(ox, oy, oz);
                  player.spawnParticle(Particle.DUST, pLoc, 1, 0.0, 0.0, 0.0, darkGreenDust);
                  if (i % 3 == 0) {
                     player.spawnParticle(Particle.DUST, pLoc, 1, 0.0, 0.0, 0.0, limeDust);
                  }
               }

               if (ThreadLocalRandom.current().nextInt(3) == 0) {
                  double ox = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                  double oy = ThreadLocalRandom.current().nextDouble(8.0, 16.0);
                  double oz = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                  player.spawnParticle(Particle.DRAGON_BREATH, eyeLoc.clone().add(ox, oy, oz), 4, 0.2, 0.2, 0.2, 0.01);
               }
               continue;
            }
         }

         if (this.endSkyBossBar.getPlayers().contains(player)) {
            this.endSkyBossBar.removePlayer(player);
         }
      }

      this.playersInEnd.clear();
      this.playersInEnd.addAll(currentInIsland);
   }

   public void cleanup() {
      if (this.task != null) {
         this.task.cancel();
      }

      if (this.endSkyBossBar != null) {
         this.endSkyBossBar.removeAll();
      }
   }
}
