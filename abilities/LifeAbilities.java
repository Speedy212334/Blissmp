package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class LifeAbilities implements GemAbilityHandler {
   private final BlissGems plugin;

   public LifeAbilities(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void onRightClick(Player player, int tier) {
      if (tier == 2 && player.isSneaking()) {
         this.circleOfLife(player);
      } else {
         this.heartDrainer(player);
      }
   }

   @Override
   public void onPrimary(Player player, int tier) {
      this.heartDrainer(player);
   }

   @Override
   public void onSecondary(Player player, int tier) {
      this.circleOfLife(player);
   }

   @Override
   public void onTertiary(Player player, int tier) {
      this.vitalityVortex(player);
   }

   @Override
   public void onQuaternary(Player player, int tier) {
      this.heartLock(player);
   }

   public void heartDrainer(Player player) {
      String abilityKey = "life-drainer";
      if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
         LivingEntity target = this.getTargetEntity(player, 15);
         if (target == null) {
            player.sendMessage("§cNo target found!");
         } else if (target instanceof Player targetPlayer && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
            player.sendMessage("§c§oYou cannot Heart Drain a trusted player!");
         } else {
            int duration = this.plugin.getConfigManager().getAbilityDuration("life-drainer");
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration * 20, 1, false, true));
            double stealHp = this.plugin.getConfig().getDouble("abilities.life-drainer.steal-hp", 4.0);
            if (stealHp > 0.0 && target.getHealth() > 0.0) {
               double drained = Math.min(stealHp, target.getHealth());
               target.damage(stealHp, player);
               double newHealth = Math.min(player.getHealth() + drained, player.getMaxHealth());
               player.setHealth(Math.max(0.0, newHealth));
            }

            DustOptions pinkDust = new DustOptions(ParticleUtils.LIFE_PINK, 1.5F);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, pinkDust, true);
            target.getWorld().spawnParticle(Particle.SCULK_SOUL, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0F, 1.5F);
            this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
            this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Heart Drainer");
         }
      }
   }

   public void circleOfLife(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "life-circle-of-life";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            int duration = this.plugin.getConfigManager().getAbilityDuration("life-circle");
            int durationTicks = duration * 20;
            double radius = this.plugin.getConfig().getDouble("abilities.life-circle.radius", 6.0);
            double healthIncrease = this.plugin.getConfig().getDouble("abilities.life-circle.max-health-increase", 4.0);
            double healthDecrease = this.plugin.getConfig().getDouble("abilities.life-circle.max-health-decrease", 4.0);
            Set<UUID> modifiedInCircle = new HashSet<>();
            NamespacedKey modifierKey = new NamespacedKey(this.plugin, "circle-of-life");
            int[] ticksElapsed = new int[]{0};
            BukkitTask circleTask = this.plugin
               .getServer()
               .getScheduler()
               .runTaskTimer(
                  this.plugin,
                  () -> {
                     if (player.isOnline() && !player.isDead() && ticksElapsed[0] < durationTicks) {
                        Location circleLocation = player.getLocation();
                        this.applyCircleModifier(player, modifierKey, healthIncrease, modifiedInCircle);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, true));

                        for (Object entity : circleLocation.getWorld().getNearbyEntities(circleLocation, radius, radius, radius)) {
                           if (entity != player) {
                              if (!(entity instanceof Player nearby)) {
                                 if (entity instanceof LivingEntity) {
                                    ((LivingEntity)entity).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                                 }
                              } else {
                                 boolean trusted = this.plugin.getTrustedPlayersManager().isTrusted(player, nearby);
                                 if (trusted) {
                                    this.applyCircleModifier(nearby, modifierKey, healthIncrease, modifiedInCircle);
                                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, true));
                                 } else {
                                    this.applyCircleModifier(nearby, modifierKey, -healthDecrease, modifiedInCircle);
                                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                                 }
                              }
                           }
                        }

                        HashSet<UUID> toRemove = new HashSet<>();

                        for (UUID uuid : new HashSet<>(modifiedInCircle)) {
                           Player modified;
                           if (!uuid.equals(player.getUniqueId())
                              && (
                                 (modified = this.plugin.getServer().getPlayer(uuid)) == null
                                    || !modified.isOnline()
                                    || modified.getLocation().distance(circleLocation) > radius
                              )) {
                              if (modified != null && modified.isOnline()) {
                                 this.removeCircleModifierDirect(modified, modifierKey);
                              }

                              toRemove.add(uuid);
                           }
                        }

                        modifiedInCircle.removeAll(toRemove);
                        DustOptions pinkCircleDust = new DustOptions(ParticleUtils.LIFE_PINK_ALT, 1.0F);
                        player.getWorld().spawnParticle(Particle.DUST, circleLocation.clone().add(0.0, 1.0, 0.0), 30, 4.0, 0.5, 4.0, 0.0, pinkCircleDust, true);
                        player.getWorld().spawnParticle(Particle.HEART, circleLocation.clone().add(0.0, 1.0, 0.0), 20, 4.0, 0.5, 4.0, 0.0, null, true);
                        player.getWorld().spawnParticle(Particle.SCULK_SOUL, circleLocation.clone().add(0.0, 0.5, 0.0), 30, 4.0, 0.2, 4.0, 0.0, null, true);
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, circleLocation.clone().add(0.0, 0.2, 0.0), 15, 3.0, 0.1, 3.0, 0.0, null, true);
                        int circlePoints = 32;

                        for (int i = 0; i < circlePoints; i++) {
                           double angle = (double)i / circlePoints * 2.0 * Math.PI;
                           double x = Math.cos(angle) * radius;
                           double z = Math.sin(angle) * radius;
                           player.getWorld().spawnParticle(Particle.DUST, circleLocation.clone().add(x, 0.2, z), 2, 0.1, 0.1, 0.1, 0.0, pinkCircleDust, true);
                           player.getWorld().spawnParticle(Particle.HEART, circleLocation.clone().add(x, 1.0, z), 1, 0.05, 0.05, 0.05, 0.0, null, true);
                           player.getWorld().spawnParticle(Particle.SCULK_SOUL, circleLocation.clone().add(x, 1.8, z), 1, 0.05, 0.05, 0.05, 0.0, null, true);
                        }

                        ticksElapsed[0] += 20;
                     }
                  },
                  0L,
                  20L
               );
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
               circleTask.cancel();

               for (UUID uuid : modifiedInCircle) {
                  Player modified = this.plugin.getServer().getPlayer(uuid);
                  if (modified != null && modified.isOnline()) {
                     this.removeCircleModifierDirect(modified, modifierKey);
                  }
               }

               modifiedInCircle.clear();
               this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
            }, durationTicks);
            this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
            this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Circle of Life");
         }
      }
   }

   private void applyCircleModifier(Player player, NamespacedKey key, double amount, Set<UUID> tracked) {
      AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
      if (attr != null) {
         boolean hasModifier = attr.getModifiers().stream().anyMatch(m -> this.matchesKey(m, key));
         if (!hasModifier) {
            attr.addModifier(new AttributeModifier(key, amount, Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            tracked.add(player.getUniqueId());
         }
      }
   }

   private void removeCircleModifier(Player player, NamespacedKey key, Set<UUID> tracked) {
      this.removeCircleModifierDirect(player, key);
      tracked.remove(player.getUniqueId());
   }

   private void removeCircleModifierDirect(Player player, NamespacedKey key) {
      AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
      if (attr != null) {
         ArrayList<AttributeModifier> toRemove = new ArrayList<>();

         for (AttributeModifier m : attr.getModifiers()) {
            if (this.matchesKey(m, key)) {
               toRemove.add(m);
            }
         }

         for (AttributeModifier m : toRemove) {
            attr.removeModifier(m);
         }
      }
   }

   private boolean matchesKey(AttributeModifier m, NamespacedKey key) {
      try {
         NamespacedKey mk = m.getKey();
         if (mk != null && mk.equals(key)) {
            return true;
         }
      } catch (Throwable var4) {
      }

      String name = m.getName();
      return name != null && (name.equals(key.toString()) || name.equals(key.getKey()));
   }

   public void vitalityVortex(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "life-vitality-vortex";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            int duration = this.plugin.getConfigManager().getAbilityDuration("life-vitality-vortex");
            int durationTicks = duration * 20;
            Biome biome = player.getLocation().getBlock().getBiome();
            String biomeName = biome.name().toUpperCase();

            for (PotionEffect effect : this.getBiomeEffects(biomeName, durationTicks)) {
               player.addPotionEffect(effect);
            }

            DustOptions pinkDust = new DustOptions(ParticleUtils.LIFE_PINK, 1.5F);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 1.0, 1.0, 1.0, 0.0, pinkDust, true);
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0.0, 1.5, 0.0), 15, 0.5, 0.5, 0.5);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.5F);
            this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
            this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Vitality Vortex");
         }
      }
   }

   private PotionEffect[] getBiomeEffects(String biomeName, int durationTicks) {
      if (biomeName.contains("FOREST") || biomeName.contains("PLAINS") || biomeName.contains("MEADOW")) {
         return new PotionEffect[]{
            new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1, true, true),
            new PotionEffect(PotionEffectType.SATURATION, durationTicks, 0, true, true)
         };
      } else if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS") || biomeName.contains("SAVANNA")) {
         return new PotionEffect[]{
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, true, true),
            new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 0, true, true)
         };
      } else if (biomeName.contains("OCEAN") || biomeName.contains("RIVER") || biomeName.contains("BEACH") || biomeName.contains("SWAMP")) {
         return new PotionEffect[]{
            new PotionEffect(PotionEffectType.WATER_BREATHING, durationTicks, 0, true, true),
            new PotionEffect(PotionEffectType.DOLPHINS_GRACE, durationTicks, 0, true, true)
         };
      } else if (biomeName.contains("TAIGA") || biomeName.contains("SNOWY") || biomeName.contains("FROZEN") || biomeName.contains("ICE")) {
         return new PotionEffect[]{
            new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0, true, true), new PotionEffect(PotionEffectType.SPEED, durationTicks, 0, true, true)
         };
      } else if (biomeName.contains("JUNGLE")) {
         return new PotionEffect[]{
            new PotionEffect(PotionEffectType.SPEED, durationTicks, 1, true, true), new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 0, true, true)
         };
      } else if (biomeName.contains("NETHER")
         || biomeName.contains("CRIMSON")
         || biomeName.contains("WARPED")
         || biomeName.contains("BASALT")
         || biomeName.contains("SOUL_SAND")) {
         return new PotionEffect[]{
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, true, true),
            new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 1, true, true)
         };
      } else if (biomeName.contains("END") && !biomeName.contains("ENDER")) {
         return new PotionEffect[]{
            new PotionEffect(PotionEffectType.SLOW_FALLING, durationTicks, 0, true, true),
            new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0, true, true)
         };
      } else {
         return !biomeName.contains("MOUNTAIN")
               && !biomeName.contains("STONY")
               && !biomeName.contains("CAVE")
               && !biomeName.contains("DRIPSTONE")
               && !biomeName.contains("LUSH")
               && !biomeName.contains("DEEP_DARK")
            ? new PotionEffect[]{
               new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 0, true, true),
               new PotionEffect(PotionEffectType.SPEED, durationTicks, 0, true, true)
            }
            : new PotionEffect[]{
               new PotionEffect(PotionEffectType.HASTE, durationTicks, 1, true, true),
               new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0, true, true)
            };
      }
   }

   public void heartLock(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "life-heart-lock";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            int range = this.plugin.getConfig().getInt("abilities.life-heart-lock.range", 15);
            LivingEntity targetEntity = this.getTargetEntity(player, range);
            if (targetEntity == null) {
               player.sendMessage("§cNo target found!");
            } else if (targetEntity instanceof Player targetPlayer && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
               player.sendMessage("§c§oYou cannot Heart Lock a trusted player!");
            } else {
               int duration = this.plugin.getConfigManager().getAbilityDuration("life-heart-lock");
               int durationTicks = duration * 20;
               AttributeInstance targetMaxHealthAttr = targetEntity.getAttribute(Attributes.maxHealth());
               if (targetMaxHealthAttr == null) {
                  player.sendMessage("§cCannot Heart Lock this target!");
               } else {
                  double currentHealth = targetEntity.getHealth();
                  double maxHealth = targetMaxHealthAttr.getValue();
                  double reduction = currentHealth - maxHealth;
                  NamespacedKey heartLockKey = new NamespacedKey(this.plugin, "heart-lock");
                  ArrayList<AttributeModifier> existing = new ArrayList<>();

                  for (AttributeModifier m : targetMaxHealthAttr.getModifiers()) {
                     if (this.matchesKey(m, heartLockKey)) {
                        existing.add(m);
                     }
                  }

                  for (AttributeModifier m : existing) {
                     targetMaxHealthAttr.removeModifier(m);
                  }

                  targetMaxHealthAttr.addModifier(new AttributeModifier(heartLockKey, reduction, Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
                  this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                     AttributeInstance attr;
                     if (targetEntity.isValid() && (attr = targetEntity.getAttribute(Attributes.maxHealth())) != null) {
                        ArrayList<AttributeModifier> toRm = new ArrayList<>();

                        for (AttributeModifier m : attr.getModifiers()) {
                           if (this.matchesKey(m, heartLockKey)) {
                              toRm.add(m);
                           }
                        }

                        for (AttributeModifier m : toRm) {
                           attr.removeModifier(m);
                        }

                        double newMax = attr.getValue();
                        if (targetEntity.getHealth() > newMax) {
                           targetEntity.setHealth(newMax);
                        }
                     }

                     this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
                  }, durationTicks);
                  DustOptions pinkDust = new DustOptions(ParticleUtils.LIFE_PINK, 1.5F);
                  DustOptions darkPink = new DustOptions(Color.fromRGB(150, 0, 100), 1.2F);
                  targetEntity.getWorld().spawnParticle(Particle.DUST, targetEntity.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, pinkDust, true);
                  targetEntity.getWorld().spawnParticle(Particle.DUST, targetEntity.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5, 0.0, darkPink, true);
                  targetEntity.getWorld().playSound(targetEntity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.5F);
                  player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5F, 0.5F);
                  this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
                  this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Heart Lock");
                  if (targetEntity instanceof Player) {
                     ((Player)targetEntity).sendMessage("§d§oYour max health has been locked!");
                  }
               }
            }
         }
      }
   }

   @Override
   public void cleanup(Player player) {
      AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
      if (attr != null) {
         ArrayList<AttributeModifier> toRemove = new ArrayList<>();

         for (AttributeModifier m : attr.getModifiers()) {
            if (this.isLifeModifier(m)) {
               toRemove.add(m);
            }
         }

         for (AttributeModifier m : toRemove) {
            attr.removeModifier(m);
         }

         double max = attr.getValue();
         if (player.getHealth() > max) {
            player.setHealth(max);
         }

         if (!toRemove.isEmpty()) {
            this.plugin.getLogger().info("[LifeAbilities] Removed " + toRemove.size() + " stale life-modifier(s) from " + player.getName());
         }
      }
   }

   private boolean isLifeModifier(AttributeModifier m) {
      if (this.matchesLifeToken(m.getName())) {
         return true;
      }

      try {
         NamespacedKey key = m.getKey();
         if (key != null) {
            if ("blissgems".equals(key.getNamespace())) {
               return true;
            }

            if (this.matchesLifeToken(key.getKey())) {
               return true;
            }
         }
      } catch (Throwable var3) {
      }

      return false;
   }

   private boolean matchesLifeToken(String raw) {
      if (raw == null) {
         return false;
      }

      String norm = raw.toLowerCase().replaceAll("[^a-z0-9]", "");
      return norm.contains("blissgems") || norm.contains("circleoflife") || norm.contains("heartlock");
   }

   private LivingEntity getTargetEntity(Player player, int range) {
      return player.getWorld()
               .rayTraceEntities(
                  player.getEyeLocation(), player.getEyeLocation().getDirection(), range, entity -> entity instanceof LivingEntity && entity != player
               )
            != null
         ? (LivingEntity)player.getWorld()
            .rayTraceEntities(
               player.getEyeLocation(), player.getEyeLocation().getDirection(), range, entity -> entity instanceof LivingEntity && entity != player
            )
            .getHitEntity()
         : null;
   }
}
