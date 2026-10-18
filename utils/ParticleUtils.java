package dev.xoperr.blissgems.utils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ParticleUtils {
   public static final Color ASTRA_PURPLE = Color.fromRGB(106, 11, 184);
   public static final Color FIRE_ORANGE = Color.fromRGB(255, 119, 0);
   public static final Color FLUX_CYAN = Color.fromRGB(94, 215, 255);
   public static final Color FLUX_DARK_CYAN = Color.fromRGB(16, 131, 173);
   public static final Color LIFE_PINK = Color.fromRGB(255, 0, 180);
   public static final Color LIFE_PINK_ALT = Color.fromRGB(255, 0, 179);
   public static final Color PUFF_WHITE = Color.fromRGB(255, 255, 255);
   public static final Color SPEED_YELLOW = Color.fromRGB(244, 255, 28);
   public static final Color STRENGTH_RED = Color.fromRGB(199, 0, 10);
   public static final Color WEALTH_GREEN = Color.fromRGB(0, 166, 44);

   public static void drawColoredCircle(Location location, double radius, Color color, float size, int density) {
      World world = location.getWorld();
      if (world != null) {
         DustOptions dustOptions = new DustOptions(color, size);

         for (int i = 0; i < density; i++) {
            double angle = (double)i / density * 2.0 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = location.clone().add(x, 0.0, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dustOptions, true);
         }
      }
   }

   public static void drawExpandingCircles(final Location location, final Color color, final float size, double maxRadius, final Runnable onComplete) {
      final double[] radii = generateRadiiArray(maxRadius);
      (new BukkitRunnable() {
         int index = 0;

         public void run() {
            if (this.index >= radii.length) {
               if (onComplete != null) {
                  onComplete.run();
               }

               this.cancel();
            } else {
               ParticleUtils.drawColoredCircle(location, radii[this.index], color, size, 32);
               if (this.index + 1 < radii.length) {
                  ParticleUtils.drawColoredCircle(location, radii[this.index + 1], color, size, 32);
                  this.index += 2;
               } else {
                  this.index++;
               }
            }
         }
      }).runTaskTimer(Bukkit.getPluginManager().getPlugin("BlissGems"), 0L, 1L);
   }

   private static double[] generateRadiiArray(double maxRadius) {
      int steps = (int)((maxRadius - 0.75) / 0.25) + 1;
      double[] radii = new double[steps];

      for (int i = 0; i < steps; i++) {
         radii[i] = 0.75 + i * 0.25;
      }

      return radii;
   }

   public static void drawColoredLine(Location from, Location to, Color color, float size, int density) {
      World world = from.getWorld();
      if (world != null) {
         DustOptions dustOptions = new DustOptions(color, size);
         double distance = from.distance(to);
         int points = (int)(distance * density);

         for (int i = 0; i <= points; i++) {
            double ratio = (double)i / points;
            double x = from.getX() + (to.getX() - from.getX()) * ratio;
            double y = from.getY() + (to.getY() - from.getY()) * ratio;
            double z = from.getZ() + (to.getZ() - from.getZ()) * ratio;
            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dustOptions, true);
         }
      }
   }

   public static void drawPulsingLine(
      final Location from, final Location to, final Color color, final float size, final int iterations, int ticksBetween, final Runnable onComplete
   ) {
      (new BukkitRunnable() {
         int count = 0;

         public void run() {
            if (this.count >= iterations) {
               if (onComplete != null) {
                  onComplete.run();
               }

               this.cancel();
            } else {
               ParticleUtils.drawColoredLine(from, to, color, size, 10);
               this.count++;
            }
         }
      }).runTaskTimer(Bukkit.getPluginManager().getPlugin("BlissGems"), 0L, ticksBetween);
   }

   public static void drawDome(Location location, Color color, float size, double maxRadius) {
      double[] layerRadii = new double[]{maxRadius, maxRadius - 1.0, maxRadius - 2.0, maxRadius - 3.0, maxRadius - 4.0, maxRadius - 5.0};

      for (int i = 0; i < layerRadii.length; i++) {
         if (!(layerRadii[i] <= 0.0)) {
            Location layerLoc = location.clone().add(0.0, i, 0.0);
            drawColoredCircle(layerLoc, layerRadii[i], color, size, 48);
         }
      }
   }

   public static void drawSpiral(Location location, Color color, float size, double height, double radius, double rotationOffset) {
      World world = location.getWorld();
      if (world != null) {
         DustOptions dustOptions = new DustOptions(color, size);
         int points = (int)(height * 8.0);

         for (int i = 0; i < points; i++) {
            double heightOffset = (double)i / points * height;
            double angle = (double)i / points * 4.0 * Math.PI + rotationOffset;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = location.clone().add(x, heightOffset, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dustOptions, true);
         }
      }
   }

   public static void drawRing(Location location, Color color, float size, double radius, double heightOffset, int density) {
      World world = location.getWorld();
      if (world != null) {
         DustOptions dustOptions = new DustOptions(color, size);

         for (int i = 0; i < density; i++) {
            double angle = (double)i / density * 2.0 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = location.clone().add(x, heightOffset, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dustOptions, true);
         }
      }
   }

   public static void drawBeam(Location from, Vector direction, double length, double startRadius, Particle particleType) {
      World world = from.getWorld();
      if (world != null) {
         direction = direction.normalize();
         int segments = 25;
         int pointsPerCircle = 8;
         double segmentLength = length / segments;

         for (int seg = 0; seg < segments; seg++) {
            Location segmentLoc = from.clone().add(direction.clone().multiply(seg * segmentLength));
            double radius = startRadius * (1.0 - (double)seg / segments);
            if (radius < 0.5) {
               radius = 0.5;
            }

            for (int i = 0; i < pointsPerCircle; i++) {
               double angle = (double)i / pointsPerCircle * 2.0 * Math.PI;
               Vector perpendicular1 = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
               Vector perpendicular2 = direction.clone().crossProduct(perpendicular1).normalize();
               Vector offset = perpendicular1.clone().multiply(Math.cos(angle) * radius).add(perpendicular2.clone().multiply(Math.sin(angle) * radius));
               Location particleLoc = segmentLoc.clone().add(offset);
               world.spawnParticle(particleType, particleLoc, 3, 0.0, 0.0, 0.0, 1.0E-9, null, true);
            }
         }
      }
   }

   public static void drawTrail(Location location, Particle particleType, int count) {
      World world = location.getWorld();
      if (world != null) {
         world.spawnParticle(particleType, location, count, 0.1, 0.1, 0.1, 0.0, null, true);
      }
   }
}
