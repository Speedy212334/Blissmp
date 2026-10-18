package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class StrengthAbilities implements GemAbilityHandler {
   private final BlissGems plugin;
   private static final Set<PotionEffectType> STRIPPABLE_EFFECTS = Set.of(
      PotionEffectType.SPEED,
      PotionEffectType.STRENGTH,
      PotionEffectType.REGENERATION,
      PotionEffectType.FIRE_RESISTANCE,
      PotionEffectType.WATER_BREATHING,
      PotionEffectType.INVISIBILITY,
      PotionEffectType.NIGHT_VISION,
      PotionEffectType.ABSORPTION,
      PotionEffectType.HEALTH_BOOST,
      PotionEffectType.RESISTANCE,
      PotionEffectType.HASTE,
      PotionEffectType.JUMP_BOOST,
      PotionEffectType.CONDUIT_POWER,
      PotionEffectType.DOLPHINS_GRACE,
      PotionEffectType.LUCK,
      PotionEffectType.HERO_OF_THE_VILLAGE,
      PotionEffectType.SLOW_FALLING,
      PotionEffectType.SATURATION
   );
   private final Map<UUID, UUID> trackingTargets = new HashMap<>();
   private final Map<UUID, BukkitTask> trackingTasks = new HashMap<>();
   private final Map<UUID, Map<UUID, Integer>> stalkCounts = new HashMap<>();
   private final Map<UUID, Integer> chadHitsRemaining = new HashMap<>();
   private final Map<UUID, Long> chadExpiry = new HashMap<>();

   public StrengthAbilities(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void onRightClick(Player player, int tier) {
      if (tier >= 2 && player.isSneaking()) {
         this.frailer(player);
      } else {
         this.chadStrength(player);
      }
   }

   @Override
   public void onPrimary(Player player, int tier) {
      this.chadStrength(player);
   }

   @Override
   public void onSecondary(Player player, int tier) {
      this.frailer(player);
   }

   @Override
   public void onTertiary(Player player, int tier) {
      this.shadowStalker(player);
   }

   @Override
   public void onQuaternary(Player player, int tier) {
      this.nullify(player);
   }

   public void chadStrength(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "strength-chad";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            int empoweredHits = this.plugin.getConfig().getInt("abilities.strength-chad.empowered-hits", 4);
            int durationSeconds = this.plugin.getConfig().getInt("abilities.strength-chad.duration", 10);
            this.chadHitsRemaining.put(player.getUniqueId(), empoweredHits);
            this.chadExpiry.put(player.getUniqueId(), player.getWorld().getFullTime() + durationSeconds * 20L);
            DustOptions redDust = new DustOptions(ParticleUtils.STRENGTH_RED, 1.6F);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 45, 0.5, 0.9, 0.5, 0.0, redDust, true);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0.0, 1.0, 0.0), 25, 0.5, 0.9, 0.5, 0.4);
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.7F, 1.4F);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0F, 0.6F);
            this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
            this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Chad Strength");
            player.sendMessage("§c§l⚔ §cYour next §l" + empoweredHits + "§c hits are empowered for §l" + durationSeconds + "s§c!");
         }
      }
   }

   public int getChadHitsRemaining(Player player) {
      if (player == null) {
         return 0;
      }

      Integer remaining = this.chadHitsRemaining.get(player.getUniqueId());
      return remaining != null ? remaining : 0;
   }

   public double consumeChadBonus(Player player) {
      UUID id = player.getUniqueId();
      Integer remaining = this.chadHitsRemaining.get(id);
      if (remaining != null && remaining > 0) {
         Long expiry = this.chadExpiry.get(id);
         if (expiry != null && player.getWorld().getFullTime() > expiry) {
            this.chadHitsRemaining.remove(id);
            this.chadExpiry.remove(id);
            return 0.0;
         }

         remaining = remaining - 1;
         if (remaining <= 0) {
            this.chadHitsRemaining.remove(id);
            this.chadExpiry.remove(id);
            this.plugin.getAbilityManager().endAbilityDuration(player, "strength-chad");
         } else {
            this.chadHitsRemaining.put(id, remaining);
         }

         return this.plugin.getConfig().getDouble("abilities.strength-chad.bonus-damage", 4.0);
      } else {
         return 0.0;
      }
   }

   public void nullify(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "strength-nullify";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            int range = this.plugin.getConfig().getInt("abilities.strength-nullify.range", 20);
            LivingEntity target = this.getTargetEntity(player, range);
            if (target == null) {
               player.sendMessage("§cNo target found!");
            } else if (target instanceof Player targetPlayer && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
               player.sendMessage("§cYou cannot nullify a trusted player!");
            } else {
               final ArrayList<PotionEffect> stripped = new ArrayList<>();

               for (PotionEffect effect : target.getActivePotionEffects()) {
                  if (!effect.isAmbient() && STRIPPABLE_EFFECTS.contains(effect.getType())) {
                     stripped.add(effect);
                  }
               }

               int strippedCount = stripped.size();

               for (PotionEffect potionEffect : stripped) {
                  target.removePotionEffect(potionEffect.getType());
               }

               int restoreSeconds = this.plugin.getConfig().getInt("abilities.strength-nullify.duration", 8);
               if (restoreSeconds > 0 && !stripped.isEmpty()) {
                  final UUID uUID = target.getUniqueId();
                  final int suppressionTicks = restoreSeconds * 20;
                  (new BukkitRunnable() {
                        public void run() {
                           Entity e = Bukkit.getEntity(uUID);
                           if (e instanceof LivingEntity && !e.isDead()) {
                              LivingEntity le = (LivingEntity)e;

                              for (PotionEffect effect : stripped) {
                                 int remaining = effect.getDuration() - suppressionTicks;
                                 if (remaining > 0) {
                                    le.addPotionEffect(
                                       new PotionEffect(
                                          effect.getType(), remaining, effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()
                                       )
                                    );
                                 }
                              }
                           }
                        }
                     })
                     .runTaskLater(this.plugin, suppressionTicks);
               }

               DustOptions dustOptions = new DustOptions(ParticleUtils.STRENGTH_RED, 1.5F);
               target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.0, dustOptions, true);
               target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.8, 0.5, 0.05);
               target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0, 1.0, 0.0), 10, 0.5, 0.5, 0.5);
               player.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8F, 1.2F);
               target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8F, 1.2F);
               this.plugin.getAbilityManager().useAbility(player, abilityKey);
               String targetName = target instanceof Player ? ((Player)target).getName() : "target";
               String nullifyMsg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Nullify");
               if (nullifyMsg != null) {
                  player.sendMessage(nullifyMsg + " §7(Stripped " + strippedCount + " effects from " + targetName + ")");
               }

               if (target instanceof Player) {
                  ((Player)target).sendMessage("§c§oYour potion effects have been nullified!");
               }
            }
         }
      }
   }

   public void frailer(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "strength-frailer";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            int range = this.plugin.getConfig().getInt("abilities.strength-frailer.range", 15);
            LivingEntity target = this.getTargetEntity(player, range);
            if (target == null) {
               player.sendMessage("§cNo target found!");
            } else if (target instanceof Player targetPlayer && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
               player.sendMessage("§cYou cannot use Frailer on a trusted player!");
            } else {
               int weaknessDuration = this.plugin.getConfig().getInt("abilities.durations.strength-frailer-weakness", 20) * 20;
               int witherDuration = this.plugin.getConfig().getInt("abilities.durations.strength-frailer-wither", 40) * 20;
               int slownessDuration = this.plugin.getConfig().getInt("abilities.durations.strength-frailer-slowness", 20) * 20;
               int slownessLevel = this.plugin.getConfig().getInt("abilities.strength-frailer.slowness-level", 2);
               target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, 0, false, true));
               target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherDuration, 0, false, true));
               target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, Math.max(0, slownessLevel - 1), false, true));
               DustOptions redDust = new DustOptions(ParticleUtils.STRENGTH_RED, 1.5F);
               target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.0, redDust, true);
               target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 1.0, 0.0), 25, 0.5, 0.8, 0.5, 0.05);
               target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
               player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0F, 0.8F);
               target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0F, 0.8F);
               this.plugin.getAbilityManager().useAbility(player, abilityKey);
               this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Frailer Power");
               if (target instanceof Player) {
                  ((Player)target).sendMessage("§c§oYou have been weakened by Frailer Power!");
               }
            }
         }
      }
   }

   public void shadowStalker(final Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "strength-shadow-stalker";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            if (this.trackingTargets.containsKey(player.getUniqueId())) {
               this.endTracking(player);
               player.sendMessage("§c§oStopped tracking.");
            } else {
               ItemStack heldItem = player.getInventory().getItemInMainHand();
               ItemStack offHand = player.getInventory().getItemInOffHand();
               if (offHand != null && offHand.getType() == Material.PLAYER_HEAD) {
                  heldItem = offHand;
               }

               if (heldItem != null && heldItem.getType() != Material.AIR) {
                  UUID targetUUID = null;
                  SkullMeta skullMeta;
                  if (heldItem.getType() == Material.PLAYER_HEAD
                     && heldItem.hasItemMeta()
                     && (skullMeta = (SkullMeta)heldItem.getItemMeta()).getOwningPlayer() != null) {
                     targetUUID = skullMeta.getOwningPlayer().getUniqueId();
                  }

                  if (targetUUID == null) {
                     targetUUID = CustomItemManager.getOwner(heldItem);
                  }

                  if (targetUUID == null) {
                     player.sendMessage("§c§oThis item is not a player head and has no owner!");
                  } else if (targetUUID.equals(player.getUniqueId())) {
                     player.sendMessage("§c§oYou cannot track yourself!");
                  } else {
                     Player targetPlayer = Bukkit.getPlayer(targetUUID);
                     if (targetPlayer != null && targetPlayer.isOnline()) {
                        boolean fromOffHand = heldItem == offHand;
                        if (heldItem.getAmount() > 1) {
                           heldItem.setAmount(heldItem.getAmount() - 1);
                           if (fromOffHand) {
                              player.getInventory().setItemInOffHand(heldItem);
                           } else {
                              player.getInventory().setItemInMainHand(heldItem);
                           }
                        } else if (fromOffHand) {
                           player.getInventory().setItemInOffHand(null);
                        } else {
                           player.getInventory().setItemInMainHand(null);
                        }

                        int durationSeconds = this.plugin.getConfig().getInt("abilities.strength-shadow-stalker.duration", 60);
                        final int duration = durationSeconds * 20;
                        final int invisRange = this.plugin.getConfig().getInt("abilities.strength-shadow-stalker.invisibility-max-range", 2500);
                        final UUID trackerId = player.getUniqueId();
                        final UUID trackedId = targetUUID;
                        this.trackingTargets.put(trackerId, trackedId);
                        if (this.plugin.getAchievementManager() != null) {
                           Map<UUID, Integer> targetCounts = this.stalkCounts.computeIfAbsent(trackerId, k -> new HashMap<>());
                           int count = targetCounts.getOrDefault(trackedId, 0) + 1;
                           targetCounts.put(trackedId, count);
                           if (count >= 17) {
                              this.plugin.getAchievementManager().unlock(player, Achievement.ITS_RABBIT_SEASON);
                           }
                        }

                        player.sendMessage("§5§l☠ §dNow tracking §f" + targetPlayer.getName() + "§d.");
                        targetPlayer.sendMessage("§c§l⚠ §c§oYou are being hunted...");
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 0.7F, 1.5F);
                        DustOptions redDust = new DustOptions(ParticleUtils.STRENGTH_RED, 1.5F);
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5, 0.0, redDust, true);
                        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SNIFF, 1.0F, 1.2F);
                        BukkitTask trackTask = (new BukkitRunnable() {
                              int ticksElapsed = 0;

                              public void run() {
                                 Player tracker = Bukkit.getPlayer(trackerId);
                                 Player tracked = Bukkit.getPlayer(trackedId);
                                 if (tracker != null
                                    && tracker.isOnline()
                                    && tracked != null
                                    && tracked.isOnline()
                                    && this.ticksElapsed < duration
                                    && StrengthAbilities.this.trackingTargets.containsKey(trackerId)) {
                                    this.ticksElapsed++;
                                    if (this.ticksElapsed % 20 == 0) {
                                       boolean sameWorld = tracker.getWorld().equals(tracked.getWorld());
                                       if (sameWorld) {
                                          double distance = tracker.getLocation().distance(tracked.getLocation());
                                          boolean targetInvisible = tracked.hasPotionEffect(PotionEffectType.INVISIBILITY);
                                          if (targetInvisible && distance > invisRange) {
                                             TextComponent msg = new TextComponent("§c§lTRACKING §7… §c" + tracked.getName() + " §7(invisible, too far)");
                                             tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg);
                                          } else {
                                             String direction = StrengthAbilities.this.getDirectionArrow(tracker.getLocation(), tracked.getLocation());
                                             TextComponent msg = new TextComponent(
                                                "§c§lTRACKING §7" + direction + " §c" + tracked.getName() + " §7" + (int)distance + "m"
                                             );
                                             tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg);

                                             for (UUID trustedId : StrengthAbilities.this.plugin.getTrustedPlayersManager().getTrustedPlayers(tracker)) {
                                                Player trustedPlayer = Bukkit.getPlayer(trustedId);
                                                if (trustedPlayer != null
                                                   && trustedPlayer.isOnline()
                                                   && trustedPlayer.getWorld().equals(tracker.getWorld())
                                                   && trustedPlayer.getLocation().distance(tracker.getLocation()) < 50.0) {
                                                   TextComponent trustedMsg = new TextComponent(
                                                      "§8[§cTRACK§8] §7" + direction + " §c" + tracked.getName() + " §7" + (int)distance + "m"
                                                   );
                                                   trustedPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, trustedMsg);
                                                }
                                             }
                                          }

                                          if (this.ticksElapsed % 40 == 0) {
                                             DustOptions trailDust = new DustOptions(ParticleUtils.STRENGTH_RED, 0.6F);
                                             tracker.getWorld()
                                                .spawnParticle(
                                                   Particle.DUST, tracker.getLocation().add(0.0, 2.2, 0.0), 3, 0.15, 0.1, 0.15, 0.0, trailDust, true
                                                );
                                          }
                                       } else {
                                          TextComponent msg = new TextComponent("§c§lTRACKING §7… §c" + tracked.getName() + " §7(different dimension)");
                                          tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg);
                                       }
                                    }

                                    if (this.ticksElapsed % 4 == 0 && tracker.getWorld().equals(tracked.getWorld())) {
                                       StrengthAbilities.this.drawTrackerBeam(tracker, tracked.getLocation().add(0.0, 1.0, 0.0));
                                    }
                                 } else {
                                    StrengthAbilities.this.endTracking(tracker != null ? tracker : player);
                                    this.cancel();
                                 }
                              }
                           })
                           .runTaskTimer(this.plugin, 0L, 1L);
                        this.trackingTasks.put(trackerId, trackTask);
                        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
                        player.sendMessage("§c§l☠ Shadow Stalker §7Tracking §c" + targetPlayer.getName() + " §7for " + durationSeconds + " seconds.");
                     } else {
                        player.sendMessage("§c§oTarget player is not online!");
                     }
                  }
               } else {
                  player.sendMessage("§c§oHold a player head or an item owned by your target!");
               }
            }
         }
      }
   }

   private void drawTrackerBeam(Player tracker, Location target) {
      Location from = tracker.getEyeLocation();
      double distance = from.distance(target);
      if (!(distance < 0.1)) {
         DustOptions beamDust = new DustOptions(ParticleUtils.STRENGTH_RED, 0.9F);
         Vector step = target.toVector().subtract(from.toVector()).multiply(1.0 / distance).multiply(0.5);
         int points = (int)(distance / 0.5);
         Location point = from.clone();

         for (int i = 0; i < points; i++) {
            point.add(step);
            tracker.spawnParticle(Particle.DUST, point, 1, 0.0, 0.0, 0.0, 0.0, beamDust, true);
         }
      }
   }

   private void endTracking(Player player) {
      if (player != null) {
         UUID uuid = player.getUniqueId();
         UUID trackedId = this.trackingTargets.remove(uuid);
         BukkitTask task = this.trackingTasks.remove(uuid);
         if (task != null) {
            task.cancel();
         }

         if (trackedId != null || task != null) {
            this.plugin.getAbilityManager().endAbilityDuration(player, "strength-shadow-stalker");
            if (player.isOnline()) {
               player.sendMessage("§c§oShadow Stalker tracking ended.");
            }

            Player tracked;
            if (trackedId != null && (tracked = Bukkit.getPlayer(trackedId)) != null && tracked.isOnline()) {
               tracked.sendMessage("§a§oYou are no longer being hunted.");
            }
         }
      }
   }

   public boolean isTrackingActive(Player player) {
      return this.trackingTargets.containsKey(player.getUniqueId());
   }

   private LivingEntity getTargetEntity(Player player, int range) {
      RayTraceResult result = player.getWorld()
         .rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range, entity -> entity instanceof LivingEntity && entity != player);
      return result != null ? (LivingEntity)result.getHitEntity() : null;
   }

   private String getDirectionArrow(Location from, Location to) {
      Vector direction = to.toVector().subtract(from.toVector()).normalize();
      double angle = Math.atan2(direction.getZ(), direction.getX());
      double yaw = Math.toRadians(from.getYaw());
      double relative = angle - yaw;

      while (relative > Math.PI) {
         relative -= Math.PI * 2;
      }

      while (relative < -Math.PI) {
         relative += Math.PI * 2;
      }

      if (relative > -Math.PI / 4 && relative <= Math.PI / 4) {
         return "→";
      } else if (relative > Math.PI / 4 && relative <= Math.PI * 3.0 / 4.0) {
         return "↓";
      } else {
         return relative > -Math.PI * 3.0 / 4.0 && relative <= -Math.PI / 4 ? "↑" : "←";
      }
   }

   @Override
   public void cleanup(Player player) {
      this.endTracking(player);
      this.chadHitsRemaining.remove(player.getUniqueId());
      this.chadExpiry.remove(player.getUniqueId());
   }
}
