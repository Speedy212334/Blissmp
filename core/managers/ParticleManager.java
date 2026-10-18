package dev.xoperr.blissgems.core.managers;

import dev.xoperr.blissgems.core.api.particle.CustomParticle;
import dev.xoperr.blissgems.core.api.particle.ParticleBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class ParticleManager {
   private final Plugin plugin;
   private final List<CustomParticle> activeParticles;

   public ParticleManager(Plugin plugin) {
      this.plugin = plugin;
      this.activeParticles = new ArrayList<>();
      this.startCleanupTask();
   }

   public CustomParticle spawnParticle(ParticleBuilder builder, Location location) {
      if (location != null && location.getWorld() != null) {
         ItemDisplay display = (ItemDisplay)location.getWorld()
            .spawn(
               location,
               ItemDisplay.class,
               entity -> {
                  entity.setItemStack(builder.buildItemStack());
                  entity.setBillboard(builder.getBillboard());
                  entity.setBrightness(new Brightness(builder.getBlockLight(), builder.getSkyLight()));
                  entity.setViewRange(builder.getViewRange());
                  entity.setInterpolationDuration(builder.getInterpolationDuration());
                  entity.setInterpolationDelay(builder.getInterpolationDelay());
                  float scale = builder.getScale();
                  entity.setTransformation(
                     new Transformation(
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
                     )
                  );
               }
            );
         CustomParticle particle = new CustomParticle(display, builder.getParticleId());
         this.activeParticles.add(particle);
         return particle;
      } else {
         throw new IllegalArgumentException("Location and world cannot be null");
      }
   }

   public void removeAllParticles() {
      for (CustomParticle particle : new ArrayList<>(this.activeParticles)) {
         particle.remove();
      }

      this.activeParticles.clear();
   }

   public int getActiveParticleCount() {
      this.cleanupInvalidParticles();
      return this.activeParticles.size();
   }

   public void cleanup() {
      this.removeAllParticles();
   }

   private void startCleanupTask() {
      this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, this::cleanupInvalidParticles, 100L, 100L);
   }

   private void cleanupInvalidParticles() {
      Iterator<CustomParticle> iterator = this.activeParticles.iterator();

      while (iterator.hasNext()) {
         CustomParticle particle = iterator.next();
         if (!particle.isActive()) {
            iterator.remove();
         }
      }
   }
}
