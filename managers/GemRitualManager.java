package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class GemRitualManager {
   private final BlissGems plugin;
   public static final String RITUAL_TAG = "blissgems_ritual_display";

   public GemRitualManager(BlissGems plugin) {
      this.plugin = plugin;
   }

   public static int sweepWorld(World world) {
      int removed = 0;

      for (Entity e : world.getEntitiesByClasses(new Class[]{ItemDisplay.class})) {
         if (e.getScoreboardTags().contains("blissgems_ritual_display")) {
            e.remove();
            removed++;
         }
      }

      return removed;
   }

   public static int sweepAll(Server server) {
      int removed = 0;

      for (World world : server.getWorlds()) {
         removed += sweepWorld(world);
      }

      return removed;
   }

   public void performGemRitual(Player player, GemType gemType, boolean isFirstGem, int tier) {
      this.performGemRitual(player, gemType != null ? gemType.getId() : null, isFirstGem, tier);
   }

   public void performGemRitual(final Player player, String gemId, boolean isFirstGem, int tier) {
      if (!isFirstGem && this.plugin.getAchievementManager() != null) {
         this.plugin.getAchievementManager().unlock(player, Achievement.REAWAKENING);
      }

      Location loc = player.getLocation().clone();
      Color gemColor = this.getGemColor(gemId);
      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 300, 0, false, false));
      player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1, false, false));
      final ArrayList<ItemDisplay> orbitingGems = new ArrayList<>();
      List<String> allGems = this.getRitualGemIds(tier);
      Location playerLoc = player.getLocation();

      for (int i = 0; i < allGems.size(); i++) {
         String gemItemId = this.getGemItemId(allGems.get(i), tier);
         ItemStack gemItem = CustomItemManager.getItemById(gemItemId, 10);
         if (gemItem != null) {
            double angleOffset = (double)i / allGems.size() * 2.0 * Math.PI;
            double radius = 2.5;
            double height = 1.5;
            double x = Math.cos(angleOffset) * radius;
            double z = Math.sin(angleOffset) * radius;
            Location gemLoc = playerLoc.clone().add(x, height, z);
            ItemDisplay itemDisplay = (ItemDisplay)player.getWorld().spawn(gemLoc, ItemDisplay.class);
            itemDisplay.setItemStack(gemItem);
            itemDisplay.setBillboard(Billboard.FIXED);
            itemDisplay.setViewRange(100.0F);
            itemDisplay.setGlowing(true);
            itemDisplay.addScoreboardTag("blissgems_ritual_display");
            orbitingGems.add(itemDisplay);
         }
      }

      (new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 80;

            public void run() {
               if (player.isOnline() && this.ticks < 80) {
                  for (int i = 0; i < orbitingGems.size(); i++) {
                     ItemDisplay gem = orbitingGems.get(i);
                     if (gem != null && gem.isValid()) {
                        double angleOffset = (double)i / orbitingGems.size() * 2.0 * Math.PI;
                        double angle = this.ticks / 20.0 * Math.PI + angleOffset;
                        double radius = 2.5;
                        double height = 1.5 + Math.sin(this.ticks / 10.0) * 0.3;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location newLoc = player.getLocation().clone().add(x, height, z);
                        gem.teleport(newLoc);
                        float rotation = this.ticks * 5 % 360;
                        Transformation transform = new Transformation(
                           new Vector3f(0.0F, 0.0F, 0.0F),
                           new AxisAngle4f((float)Math.toRadians(rotation), 0.0F, 1.0F, 0.0F),
                           new Vector3f(0.7F, 0.7F, 0.7F),
                           new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
                        );
                        gem.setTransformation(transform);
                     }
                  }

                  if (this.ticks % 10 == 0) {
                     player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6F, 1.0F + this.ticks / 80.0F);
                  }

                  this.ticks++;
               } else {
                  for (ItemDisplay gem : orbitingGems) {
                     if (gem != null && gem.isValid()) {
                        gem.remove();
                     }
                  }

                  orbitingGems.clear();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
      this.plugin
         .getServer()
         .getScheduler()
         .runTaskLater(
            this.plugin,
            () -> {
               if (player.isOnline()) {
                  final Location playerCenter = player.getLocation().add(0.0, 1.5, 0.0);
                  final ArrayList<ItemDisplay> selectionGems = new ArrayList<>();
                  final ArrayList<String> selectionIds = new ArrayList<>();

                  for (int ix = 0; ix < allGems.size(); ix++) {
                     String gemItemIdx = this.getGemItemId(allGems.get(ix), tier);
                     ItemStack gemItemx = CustomItemManager.getItemById(gemItemIdx, 10);
                     if (gemItemx != null) {
                        double angleOffsetx = (double)ix / allGems.size() * 2.0 * Math.PI;
                        double startAngle = (Math.PI * 4) + angleOffsetx;
                        double radiusx = 2.5;
                        double heightx = 1.5;
                        double xx = Math.cos(startAngle) * radiusx;
                        double zx = Math.sin(startAngle) * radiusx;
                        Location startLoc = player.getLocation().clone().add(xx, heightx, zx);
                        ItemDisplay itemDisplayx = (ItemDisplay)player.getWorld().spawn(startLoc, ItemDisplay.class);
                        itemDisplayx.setItemStack(gemItemx);
                        itemDisplayx.setBillboard(Billboard.FIXED);
                        itemDisplayx.setViewRange(100.0F);
                        itemDisplayx.setGlowing(true);
                        itemDisplayx.addScoreboardTag("blissgems_ritual_display");
                        selectionGems.add(itemDisplayx);
                        selectionIds.add(allGems.get(ix));
                     }
                  }

                  (new BukkitRunnable() {
                        int ticks = 0;
                        final int maxTicks = 40;

                        public void run() {
                           if (player.isOnline() && this.ticks < 40) {
                              double progress = this.ticks / 40.0;

                              for (int i = 0; i < selectionGems.size(); i++) {
                                 ItemDisplay gem = selectionGems.get(i);
                                 if (gem != null && gem.isValid()) {
                                    String currentGem = selectionIds.get(i);
                                    double angleOffset = (double)i / selectionGems.size() * 2.0 * Math.PI;
                                    double startAngle = (Math.PI * 4) + angleOffset;
                                    double startRadius = 2.5;
                                    double startHeight = 1.5;
                                    Location startLoc = player.getLocation()
                                       .add(Math.cos(startAngle) * startRadius, startHeight, Math.sin(startAngle) * startRadius);
                                    boolean isWinner = currentGem.equals(gemId);
                                    Location targetLoc = isWinner ? playerCenter.clone() : startLoc.clone().add(0.0, -3.0, 0.0);
                                    Location currentLoc = startLoc.clone()
                                       .add(
                                          (targetLoc.getX() - startLoc.getX()) * progress,
                                          (targetLoc.getY() - startLoc.getY()) * progress,
                                          (targetLoc.getZ() - startLoc.getZ()) * progress
                                       );
                                    gem.teleport(currentLoc);
                                    float rotation = isWinner ? this.ticks * 15 % 360 : this.ticks * 3 % 360;
                                    Transformation transform = new Transformation(
                                       new Vector3f(0.0F, 0.0F, 0.0F),
                                       new AxisAngle4f((float)Math.toRadians(rotation), 0.0F, 1.0F, 0.0F),
                                       isWinner ? new Vector3f(1.0F, 1.0F, 1.0F) : new Vector3f(0.7F, 0.7F, 0.7F),
                                       new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
                                    );
                                    gem.setTransformation(transform);
                                    if (isWinner) {
                                       Color color = GemRitualManager.this.getGemColor(currentGem);
                                       player.getWorld().spawnParticle(Particle.FIREWORK, currentLoc, 3, 0.1, 0.1, 0.1, 0.05);
                                       player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, currentLoc, 2, 0.1, 0.1, 0.1, 0.01);
                                    }
                                 }
                              }

                              if (this.ticks % 5 == 0) {
                                 player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7F, 1.0F + (float)progress);
                              }

                              this.ticks++;
                           } else {
                              for (ItemDisplay gem : selectionGems) {
                                 if (gem != null && gem.isValid()) {
                                    gem.remove();
                                 }
                              }

                              selectionGems.clear();
                              this.cancel();
                           }
                        }
                     })
                     .runTaskTimer(this.plugin, 0L, 1L);
                  this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                     player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                     player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8F, 1.8F);
                  }, 40L);
               }
            },
            80L
         );
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> (new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            if (player.isOnline() && this.ticks < 20) {
               double radius = this.ticks / 20.0 * 5.0;

               for (int i = 0; i < 32; i++) {
                  double angle = i / 32.0 * 2.0 * Math.PI;
                  double x = Math.cos(angle) * radius;
                  double z = Math.sin(angle) * radius;
                  Location particleLoc = loc.clone().add(x, 0.1, z);
                  DustOptions dust = new DustOptions(gemColor, 1.5F);
                  player.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, 0.0, dust, true);
                  player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.0, 0.0, 0.0, 0.01);
               }

               if (this.ticks % 5 == 0) {
                  player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5F, 1.5F);
               }

               this.ticks++;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L), 120L);
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> (new BukkitRunnable() {
         int ticks = 0;
         final int maxTicks = 40;

         public void run() {
            if (player.isOnline() && this.ticks < 40) {
               double progress = this.ticks / 40.0;
               double height = progress * 5.0;

               for (int spiral = 0; spiral < 3; spiral++) {
                  double spiralOffset = spiral / 3.0 * 2.0 * Math.PI;
                  double angle = progress * 6.0 * Math.PI + spiralOffset;
                  double radius = 1.5 * (1.0 - progress * 0.5);
                  double x = Math.cos(angle) * radius;
                  double z = Math.sin(angle) * radius;
                  Location particleLoc = loc.clone().add(x, height, z);
                  DustOptions dust = new DustOptions(gemColor, 1.8F);
                  player.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, 0.0, dust, true);
                  player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 8, 0.2, 0.2, 0.2, 0.5);
                  player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
               }

               for (double y = 0.0; y <= height; y += 0.3) {
                  DustOptions pillarDust = new DustOptions(gemColor, 0.8F);
                  player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.0, y, 0.0), 2, 0.15, 0.1, 0.15, 0.0, pillarDust, true);
               }

               if (this.ticks % 10 == 0) {
                  player.playSound(loc, Sound.BLOCK_BELL_USE, 0.7F, 1.0F + (float)progress);
                  player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5F, 1.5F);
               }

               this.ticks++;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L), 140L);
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         Location center = loc.clone().add(0.0, 5.0, 0.0);
         DustOptions explosionDust = new DustOptions(gemColor, 2.5F);
         player.getWorld().spawnParticle(Particle.DUST, center, 200, 1.5, 1.5, 1.5, 0.0, explosionDust, true);
         player.getWorld().spawnParticle(Particle.FIREWORK, center, 100, 1.0, 1.0, 1.0, 0.2);
         player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 80, 1.2, 1.2, 1.2, 0.1);
         player.getWorld().spawnParticle(Particle.END_ROD, center, 60, 1.5, 1.5, 1.5, 0.15);
         player.getWorld().spawnParticle(Particle.ENCHANT, center, 150, 2.0, 2.0, 2.0, 1.0);

         for (double y = 0.0; y <= 10.0; y += 0.2) {
            DustOptions beamDust = new DustOptions(gemColor, 1.5F);
            player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.0, y, 0.0), 8, 0.2, 0.1, 0.2, 0.0, beamDust, true);
            player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0.0, y, 0.0), 3, 0.15, 0.1, 0.15, 0.02);
         }

         player.playSound(loc, Sound.ITEM_TOTEM_USE, 1.0F, 1.0F);
         player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6F, 2.0F);
         player.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.5F);
         player.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5F, 1.5F);
         (new BukkitRunnable() {
            int ticks = 0;

            public void run() {
               if (player.isOnline() && this.ticks < 20) {
                  for (int i = 0; i < 10; i++) {
                     double randomX = (Math.random() - 0.5) * 3.0;
                     double randomZ = (Math.random() - 0.5) * 3.0;
                     double heightOffset = 5.0 - this.ticks / 20.0 * 4.5;
                     Location convergeStart = loc.clone().add(randomX, heightOffset, randomZ);
                     Location playerLoc = player.getLocation().add(0.0, 1.0, 0.0);
                     DustOptions convergeDust = new DustOptions(gemColor, 1.2F);
                     player.getWorld().spawnParticle(Particle.DUST, convergeStart, 1, 0.0, 0.0, 0.0, 0.0, convergeDust, true);
                     player.getWorld().spawnParticle(Particle.END_ROD, convergeStart, 1, 0.0, 0.0, 0.0, 0.01);
                  }

                  this.ticks++;
               } else {
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }, 180L);
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         Location playerCenter = player.getLocation().add(0.0, 1.0, 0.0);
         DustOptions finalDust = new DustOptions(gemColor, 2.0F);
         player.getWorld().spawnParticle(Particle.DUST, playerCenter, 150, 0.8, 1.0, 0.8, 0.0, finalDust, true);
         player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, playerCenter, 50, 0.5, 0.8, 0.5, 0.1);
         player.getWorld().spawnParticle(Particle.ENCHANT, playerCenter, 100, 0.6, 1.0, 0.6, 0.8);
         player.getWorld().spawnParticle(Particle.FIREWORK, playerCenter, 30, 0.5, 0.5, 0.5, 0.1);
         player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, playerCenter, 40, 0.6, 0.8, 0.6, 0.0);

         for (int ix = 0; ix < 360; ix += 10) {
            double angle = Math.toRadians(ix);
            double xx = Math.cos(angle) * 2.0;
            double zx = Math.sin(angle) * 2.0;
            DustOptions burstDust = new DustOptions(gemColor, 1.5F);
            player.getWorld().spawnParticle(Particle.DUST, playerCenter.clone().add(xx, 0.0, zx), 5, 0.1, 0.1, 0.1, 0.0, burstDust, true);
            player.getWorld().spawnParticle(Particle.END_ROD, playerCenter.clone().add(xx, 0.0, zx), 2, 0.0, 0.0, 0.0, 0.05);
         }

         player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
         player.playSound(loc, Sound.BLOCK_BELL_USE, 1.0F, 2.0F);
         player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 2.0F);
         player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
      }, 200L);
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> (new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            if (player.isOnline() && this.ticks < 40) {
               double angle = this.ticks / 40.0 * 4.0 * Math.PI;
               double radius = 1.5;
               double x = Math.cos(angle) * radius;
               double z = Math.sin(angle) * radius;
               Location orbitLoc = player.getLocation().add(x, 1.5, z);
               DustOptions orbitDust = new DustOptions(gemColor, 1.0F);
               player.getWorld().spawnParticle(Particle.DUST, orbitLoc, 3, 0.1, 0.1, 0.1, 0.0, orbitDust, true);
               player.getWorld().spawnParticle(Particle.END_ROD, orbitLoc, 1, 0.0, 0.0, 0.0, 0.01);
               if (this.ticks % 5 == 0) {
                  Location playerLoc = player.getLocation().add(0.0, 1.0, 0.0);
                  player.getWorld().spawnParticle(Particle.END_ROD, playerLoc, 3, 0.5, 0.5, 0.5, 0.02);
               }

               this.ticks++;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L), 200L);
   }

   public long performRestorationRitual(final Player player, String gemId, int tier) {
      final Location loc = player.getLocation().clone();
      final World world = player.getWorld();
      final int buildUpTicks = this.plugin.getConfig().getInt("restoration.build-up-ticks", 120);
      double witnessRadius = this.plugin.getConfig().getDouble("restoration.witness-radius", 12.0);
      double witnessLaunch = this.plugin.getConfig().getDouble("restoration.witness-launch-power", 1.4);
      boolean controlWeather = this.plugin.getConfig().getBoolean("restoration.control-weather", true);
      boolean hadStorm = world.isThundering();
      boolean hadRain = world.hasStorm();
      if (controlWeather) {
         world.setStorm(true);
         world.setWeatherDuration(buildUpTicks + 400);
         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            world.setThundering(true);
            world.setThunderDuration(buildUpTicks + 300);
         }, buildUpTicks / 3L);
      }

      (new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            if (player.isOnline() && this.ticks < buildUpTicks) {
               double progress = (double)this.ticks / buildUpTicks;
               int strikeGap = Math.max(10, (int)(160.0 * (1.0 - progress)));
               if (this.ticks % strikeGap == 0) {
                  double spread = 14.0 * (1.0 - progress) + 2.0;
                  double angle = Math.random() * 2.0 * Math.PI;
                  Location strike = loc.clone().add(Math.cos(angle) * spread, 0.0, Math.sin(angle) * spread);
                  world.strikeLightningEffect(strike);
                  world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 0.8F + (float)progress * 0.6F);
               }

               if (this.ticks % 2 == 0) {
                  double angle = this.ticks / 6.0;
                  double radius = 3.0 * (1.0 - progress) + 0.5;
                  Location swirl = loc.clone().add(Math.cos(angle) * radius, this.ticks % 40 / 10.0, Math.sin(angle) * radius);
                  world.spawnParticle(Particle.LARGE_SMOKE, swirl, 3, 0.2, 0.2, 0.2, 0.01);
                  world.spawnParticle(Particle.ELECTRIC_SPARK, swirl, 2, 0.2, 0.2, 0.2, 0.05);
               }

               this.ticks++;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         if (player.isOnline()) {
            world.strikeLightningEffect(loc);
            this.performGemRitual(player, gemId, false, tier);
         }
      }, buildUpTicks);
      long finaleTick = buildUpTicks + 210L;
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         Location center = player.isOnline() ? player.getLocation() : loc;
         world.strikeLightningEffect(center);
         world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 0.6F);
         world.playSound(center, Sound.ITEM_TOTEM_USE, 1.0F, 0.8F);
         world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 1.0, 0.5, 1.0, 0.0);
         world.spawnParticle(Particle.FLASH, center, 5, 0.5, 0.5, 0.5, 0.0);

         for (Entity entity : world.getNearbyEntities(center, witnessRadius, witnessRadius, witnessRadius)) {
            Player witness;
            if (entity instanceof Player && !(witness = (Player)entity).equals(player)) {
               Vector away = witness.getLocation().toVector().subtract(center.toVector());
               if (away.lengthSquared() < 0.01) {
                  away = witness.getLocation().getDirection().multiply(-1);
               }

               away = away.normalize().multiply(witnessLaunch).setY(0.6);
               witness.setVelocity(away);
               witness.playSound(witness.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);
            }
         }

         if (controlWeather) {
            world.setThundering(hadStorm);
            world.setStorm(hadRain);
            if (!hadRain) {
               world.setWeatherDuration(6000);
            }
         }
      }, finaleTick);
      return buildUpTicks + 200L;
   }

   public void performReviveBeaconRitual(final Player player, final Location location) {
      final Color ritualColor = Color.fromRGB(255, 215, 0);
      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false));
      (new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            if (player.isOnline() && this.ticks < 20) {
               double radius = this.ticks / 20.0 * 6.0;

               for (int i = 0; i < 32; i++) {
                  double angle = i / 32.0 * 2.0 * Math.PI;
                  double x = Math.cos(angle) * radius;
                  double z = Math.sin(angle) * radius;
                  Location particleLoc = location.clone().add(x, 0.1, z);
                  DustOptions dust = new DustOptions(ritualColor, 2.0F);
                  player.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, 0.0, dust, true);
                  player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, particleLoc, 2, 0.0, 0.0, 0.0, 0.02);
               }

               if (this.ticks % 5 == 0) {
                  player.playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.7F, 1.5F);
               }

               this.ticks++;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> (new BukkitRunnable() {
         int ticks = 0;
         final int maxTicks = 30;

         public void run() {
            if (player.isOnline() && this.ticks < 30) {
               double progress = this.ticks / 30.0;
               double height = progress * 8.0;

               for (double y = 0.0; y <= height; y += 0.3) {
                  DustOptions pillarDust = new DustOptions(ritualColor, 1.5F);
                  player.getWorld().spawnParticle(Particle.DUST, location.clone().add(0.0, y, 0.0), 3, 0.2, 0.1, 0.2, 0.0, pillarDust, true);
                  player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0.0, y, 0.0), 1, 0.1, 0.1, 0.1, 0.01);
               }

               if (this.ticks % 10 == 0) {
                  player.playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.0F + (float)progress);
               }

               this.ticks++;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L), 20L);
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         Location center = location.clone().add(0.0, 8.0, 0.0);
         DustOptions explosionDust = new DustOptions(ritualColor, 3.0F);
         player.getWorld().spawnParticle(Particle.DUST, center, 300, 2.0, 2.0, 2.0, 0.0, explosionDust, true);
         player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 150, 1.5, 1.5, 1.5, 0.15);
         player.getWorld().spawnParticle(Particle.FIREWORK, center, 80, 1.0, 1.0, 1.0, 0.2);
         player.getWorld().spawnParticle(Particle.END_ROD, center, 100, 2.0, 2.0, 2.0, 0.2);

         for (double y = 0.0; y <= 20.0; y += 0.2) {
            DustOptions beamDust = new DustOptions(ritualColor, 2.0F);
            player.getWorld().spawnParticle(Particle.DUST, location.clone().add(0.0, y, 0.0), 12, 0.3, 0.1, 0.3, 0.0, beamDust, true);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0.0, y, 0.0), 3, 0.2, 0.1, 0.2, 0.03);
         }

         player.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.0F);
         player.playSound(location, Sound.ITEM_TOTEM_USE, 1.0F, 1.2F);
         player.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5F, 2.0F);
      }, 50L);
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> (new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            if (player.isOnline() && this.ticks < 40) {
               double angle = this.ticks / 40.0 * 4.0 * Math.PI;
               double radius = 2.0;

               for (int ring = 0; ring < 3; ring++) {
                  double ringHeight = ring * 2.0 + 1.0;
                  double x = Math.cos(angle + ring * Math.PI / 1.5) * radius;
                  double z = Math.sin(angle + ring * Math.PI / 1.5) * radius;
                  Location orbitLoc = location.clone().add(x, ringHeight, z);
                  DustOptions orbitDust = new DustOptions(ritualColor, 1.2F);
                  player.getWorld().spawnParticle(Particle.DUST, orbitLoc, 5, 0.1, 0.1, 0.1, 0.0, orbitDust, true);
                  player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, orbitLoc, 1, 0.0, 0.0, 0.0, 0.01);
               }

               if (this.ticks % 5 == 0) {
                  player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0.0, 2.0, 0.0), 5, 0.5, 0.5, 0.5, 0.02);
               }

               this.ticks++;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L), 60L);
   }

   private List<String> getRitualGemIds(int tier) {
      ArrayList<String> ids = new ArrayList<>(this.plugin.getGemManager().getAvailableGemIds());
      ids.removeIf(id -> CustomItemManager.getItemById(this.getGemItemId(id, tier), 10) == null);
      return ids;
   }

   private String getGemItemId(String gemId, int tier) {
      return gemId + "_gem_t" + tier;
   }

   public Color getGemColor(String gemId) {
      GemType gemType = GemManager.builtInType(gemId);
      if (gemType != null) {
         return switch (gemType) {
            case ASTRA -> Color.fromRGB(106, 11, 184);
            case FIRE -> Color.fromRGB(255, 85, 85);
            case FLUX -> Color.fromRGB(85, 255, 255);
            case LIFE -> Color.fromRGB(85, 255, 85);
            case PUFF -> Color.fromRGB(255, 255, 255);
            case SPEED -> Color.fromRGB(255, 255, 85);
            case STRENGTH -> Color.fromRGB(170, 0, 0);
            case WEALTH -> Color.fromRGB(255, 170, 0);
            default -> Color.WHITE;
         };
      } else {
         return this.colorFromChatCode(this.plugin.getGemManager().getGemColorCode(gemId));
      }
   }

   private Color colorFromChatCode(String code) {
      if (code != null && code.length() >= 2) {
         return switch (Character.toLowerCase(code.charAt(1))) {
            case '0' -> Color.fromRGB(0, 0, 0);
            case '1' -> Color.fromRGB(0, 0, 170);
            case '2' -> Color.fromRGB(0, 170, 0);
            case '3' -> Color.fromRGB(0, 170, 170);
            case '4' -> Color.fromRGB(170, 0, 0);
            case '5' -> Color.fromRGB(170, 0, 170);
            case '6' -> Color.fromRGB(255, 170, 0);
            case '7' -> Color.fromRGB(170, 170, 170);
            case '8' -> Color.fromRGB(85, 85, 85);
            case '9' -> Color.fromRGB(85, 85, 255);
            default -> Color.fromRGB(255, 255, 255);
            case 'a' -> Color.fromRGB(85, 255, 85);
            case 'b' -> Color.fromRGB(85, 255, 255);
            case 'c' -> Color.fromRGB(255, 85, 85);
            case 'd' -> Color.fromRGB(255, 85, 255);
            case 'e' -> Color.fromRGB(255, 255, 85);
         };
      } else {
         return Color.fromRGB(255, 255, 255);
      }
   }
}
