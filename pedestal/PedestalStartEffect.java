package dev.xoperr.blissgems.pedestal;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class PedestalStartEffect {
   private PedestalStartEffect() {
   }

   static void play(BlissGems plugin, Item item, Location main, Color color, final boolean restoration) {
      final World w = main.getWorld();
      if (w != null) {
         final Location centre = main.clone().add(0.5, 1.2, 0.5);
         ItemStack shown = item.getItemStack().clone();
         shown.setAmount(1);
         final ItemDisplay display = (ItemDisplay)w.spawn(item.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(shown);
            d.setPersistent(false);
            d.setBillboard(Billboard.FIXED);
            d.setGlowing(true);
            d.setGlowColorOverride(color);
            d.setBrightness(new Brightness(15, 15));
            d.setTeleportDuration(2);
         });
         final DustOptions dust = new DustOptions(color, 1.3F);
         w.playSound(centre, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 3.0F, restoration ? 0.7F : 1.2F);
         w.playSound(centre, restoration ? Sound.ENTITY_EVOKER_PREPARE_SUMMON : Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.MASTER, 2.0F, 1.0F);
         final Location from = item.getLocation().clone();
         (new BukkitRunnable() {
               int t;

               public void run() {
                  if (!display.isValid()) {
                     this.cancel();
                  } else {
                     double f = Math.min(1.0, this.t / 36.0);
                     double ease = f * f * (3.0 - 2.0 * f);
                     Location at = from.clone().add(centre.clone().add(0.0, 1.6 * ease, 0.0).toVector().subtract(from.toVector()).multiply(ease));
                     display.teleport(at);
                     float spin = (float)(this.t * 0.35);
                     float scale = (float)(0.6 + 0.6 * ease);
                     display.setTransformation(
                        new Transformation(new Vector3f(), new Quaternionf().rotateY(spin), new Vector3f(scale, scale, scale), new Quaternionf())
                     );

                     for (int strand = 0; strand < 2; strand++) {
                        double a = this.t * 0.45 + strand * Math.PI;
                        double r = 1.6 * (1.0 - 0.6 * ease);
                        w.spawnParticle(
                           Particle.DUST,
                           centre.getX() + Math.cos(a) * r,
                           centre.getY() - 0.8 + 2.6 * ease,
                           centre.getZ() + Math.sin(a) * r,
                           2,
                           0.02,
                           0.02,
                           0.02,
                           0.0,
                           dust,
                           true
                        );
                     }

                     w.spawnParticle(Particle.ENCHANT, at, 6, 0.3, 0.3, 0.3, 0.6, null, true);
                     if (this.t % 6 == 0) {
                        w.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.MASTER, 1.2F, 0.6F + (float)ease);
                     }

                     if (this.t >= 40) {
                        this.cancel();
                        display.remove();
                        PedestalStartEffect.burst(w, at, centre, dust, restoration);
                     } else {
                        this.t++;
                     }
                  }
               }
            })
            .runTaskTimer(plugin, 0L, 1L);
      }
   }

   private static void burst(World w, Location at, Location centre, DustOptions dust, boolean restoration) {
      w.strikeLightningEffect(centre.clone().add(0.0, -1.2, 0.0));
      if (Particle.FLASH.getDataType() == Color.class) {
         w.spawnParticle(Particle.FLASH, at, 2, 0.0, 0.0, 0.0, 0.0, Color.WHITE, true);
      } else {
         w.spawnParticle(Particle.FLASH, at, 2, 0.0, 0.0, 0.0, 0.0, null, true);
      }

      w.spawnParticle(Particle.END_ROD, at, 60, 0.2, 0.2, 0.2, 0.25, null, true);
      w.spawnParticle(restoration ? Particle.REVERSE_PORTAL : Particle.HAPPY_VILLAGER, at, 80, 0.6, 0.6, 0.6, 0.2, null, true);

      for (double y = 0.0; y < 12.0; y += 0.25) {
         w.spawnParticle(Particle.DUST, centre.getX(), centre.getY() - 1.0 + y, centre.getZ(), 2, 0.12, 0.05, 0.12, 0.0, dust, true);
      }

      for (int i = 0; i < 48; i++) {
         double a = (Math.PI * 2) * i / 48.0;
         w.spawnParticle(
            Particle.DUST, centre.getX() + Math.cos(a) * 2.5, centre.getY() - 1.0, centre.getZ() + Math.sin(a) * 2.5, 1, 0.0, 0.0, 0.0, 0.0, dust, true
         );
      }

      w.playSound(centre, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 2.0F, restoration ? 0.6F : 1.0F);
      w.playSound(centre, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.MASTER, 2.0F, 0.8F);
   }
}
