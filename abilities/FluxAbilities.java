package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.managers.FluxEnergyManager;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.ParticleUtils;
import dev.xoperr.blissgems.utils.TrueDamage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FluxAbilities implements GemAbilityHandler {
   private final BlissGems plugin;
   private static final Set<UUID> stunnedPlayers = new HashSet<>();

   public FluxAbilities(BlissGems plugin) {
      this.plugin = plugin;
   }

   public static boolean isPlayerStunned(UUID playerId) {
      return stunnedPlayers.contains(playerId);
   }

   public boolean isCharging(Player player) {
      return this.plugin.getFluxEnergyManager() != null && this.plugin.getFluxEnergyManager().isCharging(player);
   }

   public int getCharge(Player player) {
      return this.plugin.getFluxEnergyManager() != null ? (int)this.plugin.getFluxEnergyManager().getBeamCharge(player.getUniqueId()) : 0;
   }

   public void onRightClick(Player player, int tier) {
      if (tier == 2 && player.isSneaking()) {
         this.ground(player);
      } else {
         this.fluxBeam(player);
      }
   }

   @Override
   public void onPrimary(Player player, int tier) {
      this.fluxBeam(player);
   }

   @Override
   public void onSecondary(Player player, int tier) {
      this.ground(player);
   }

   @Override
   public void onTertiary(Player player, int tier) {
      this.flashbang(player);
   }

   @Override
   public void onQuaternary(Player player, int tier) {
      this.kineticBurst(player);
   }

   public void fluxBeam(Player player) {
      String abilityKey = "flux-beam";
      if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
         FluxEnergyManager manager = this.plugin.getFluxEnergyManager();
         if (manager != null) {
            UUID uuid = player.getUniqueId();
            double charge = manager.getBeamCharge(uuid);
            boolean charging = manager.isCharging(player);
            if (charge >= 10.0) {
               this.fireChargedBeam(player);
            } else if (charging) {
               player.sendMessage("§b⚡ §oCharging Flux Beam in progress... (" + String.format("%.1f%%", charge) + " / 10% min to fire)");
            } else {
               double watts = manager.getWatts(uuid);
               if (watts <= 0.0) {
                  player.sendMessage("§c§lYour Flux Gem has 0 Watts! §7Use §f/bliss charge §7to open the charging station and insert conductive fuels.");
               } else {
                  manager.setCharging(player, true);
                  player.sendMessage("§b⚡ §oCharging Flux Beam from battery... Right-click again once charged (min 10%) to fire!");
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.7F, 1.5F);
               }
            }
         }
      }
   }

   private void fireChargedBeam(Player player) {
      FluxEnergyManager manager = this.plugin.getFluxEnergyManager();
      if (manager != null) {
         UUID uuid = player.getUniqueId();
         double charge = manager.getBeamCharge(uuid);
         if (charge < 10.0) {
            player.sendMessage("§c§oNot enough charge! (Minimum 10% needed to fire)");
         } else {
            LivingEntity target = this.getTargetEntity(player, 30);
            if (target == null) {
               player.sendMessage("§cNo target found!");
            } else {
               manager.setBeamCharge(uuid, 0.0);
               manager.setCharging(player, false);
               if (charge >= 100.0 && this.plugin.getAchievementManager() != null) {
                  this.plugin.getAchievementManager().unlock(player, Achievement.OVERCHARGED);
               }

               if (charge >= 200.0) {
                  player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5F, 1.0F);
                  player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5F, 0.8F);
               }

               if (target instanceof Player targetPlayer2 && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer2)) {
                  int maxArmorDamage = this.plugin.getConfig().getInt("abilities.damage.flux-beam-max-armor-damage", 250);
                  int armorRestore = (int)(charge / 100.0 * maxArmorDamage);
                  this.restoreArmorDurability(targetPlayer2, armorRestore);
                  player.sendMessage("§a§lRestored " + targetPlayer2.getName() + "'s armor! (+" + armorRestore + " durability)");
                  this.drawBeamParticles(player.getEyeLocation(), targetPlayer2.getEyeLocation().add(0.0, 1.0, 0.0));
               } else {
                  double baseDamage = this.plugin.getConfig().getDouble("abilities.damage.flux-beam-base", 14.0);
                  double damageMultiplier = 1.0 + charge / 50.0;
                  double finalDamage = baseDamage * damageMultiplier;
                  int maxArmorDamage = this.plugin.getConfig().getInt("abilities.damage.flux-beam-max-armor-damage", 250);
                  int armorDamage = (int)(charge / 100.0 * maxArmorDamage);
                  Player tp;
                  if (!(target instanceof Player) || (tp = (Player)target).getGameMode() != GameMode.CREATIVE && tp.getGameMode() != GameMode.SPECTATOR) {
                     TrueDamage.apply(target, finalDamage, player);
                     if (target instanceof Player targetPlayer) {
                        this.damageArmorPieces(targetPlayer, armorDamage);
                        targetPlayer.sendTitle("", "§c§l-" + String.format("%.1f", finalDamage) + " HP", 5, 15, 10);
                        targetPlayer.spigot()
                           .sendMessage(
                              ChatMessageType.ACTION_BAR,
                              new TextComponent("§c§l⚡ FLUX BEAM HIT! -" + String.format("%.1f", finalDamage) + " HP §7(Armor: -" + armorDamage + ")")
                           );
                     }

                     this.drawBeamParticles(player.getEyeLocation(), target.getEyeLocation().add(0.0, 1.0, 0.0));
                     Location hitLoc = target.getLocation().add(0.0, 1.0, 0.0);
                     DustOptions redDamage = new DustOptions(Color.fromRGB(255, 50, 50), 1.5F);
                     DustOptions cyanHit = new DustOptions(ParticleUtils.FLUX_CYAN, 1.3F);
                     target.getWorld().spawnParticle(Particle.DUST, hitLoc, 150, 1.2, 1.5, 1.2, 0.0, cyanHit, true);
                     target.getWorld().spawnParticle(Particle.DUST, hitLoc, 100, 0.9, 1.2, 0.9, 0.0, redDamage, true);
                     target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 200, 1.0, 1.3, 1.0, 0.15);
                     target.getWorld().spawnParticle(Particle.CRIT, hitLoc, 80, 0.8, 1.0, 0.8, 0.3);
                     target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, hitLoc, 100, 0.9, 1.2, 0.9, 0.2);
                     target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, hitLoc, 60, 0.7, 1.0, 0.7, 0.08);
                     target.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 5, 0.5, 0.5, 0.5, 0.0);

                     for (int i = 0; i < 32; i++) {
                        double angle = i / 32.0 * 2.0 * Math.PI;
                        double radius = 1.5;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(x, 0.5, z), 5, 0.1, 0.3, 0.1, 0.0, redDamage, true);
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(x, 0.5, z), 3, 0.1, 0.2, 0.1, 0.05);
                     }

                     player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 1.8F);
                     target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5F, 1.5F);
                     target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.5F);
                     target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 0.8F);
                     this.plugin.getAbilityManager().useAbility(player, "flux-beam");
                     String targetName = target instanceof Player ? ((Player)target).getName() : target.getType().name();
                     player.sendMessage("§b⚡§l FLUX BEAM HIT! §r§o" + targetName + " §7(" + charge + "% charge)");
                     player.sendMessage("§c  ❤ Damage: " + String.format("%.1f", finalDamage) + " HP §8| §7Armor: -" + armorDamage);
                  } else {
                     player.sendMessage("§cCannot hit players in creative/spectator mode!");
                  }
               }
            }
         }
      }
   }

   private void drawBeamParticles(Location from, Location to) {
      double distance = from.distance(to);
      int points = (int)(distance * 8.0);
      DustOptions cyanCore = new DustOptions(ParticleUtils.FLUX_CYAN, 1.2F);
      DustOptions cyanOuter = new DustOptions(ParticleUtils.FLUX_CYAN, 0.8F);

      for (int i = 0; i <= points; i++) {
         double ratio = (double)i / points;
         double x = from.getX() + (to.getX() - from.getX()) * ratio;
         double y = from.getY() + (to.getY() - from.getY()) * ratio;
         double z = from.getZ() + (to.getZ() - from.getZ()) * ratio;
         new Location(from.getWorld(), x, y, z);
         from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, x, y, z, 8, 0.08, 0.08, 0.08, 0.03);
         from.getWorld().spawnParticle(Particle.DUST, x, y, z, 6, 0.1, 0.1, 0.1, 0.0, cyanCore, true);
         from.getWorld().spawnParticle(Particle.DUST, x, y, z, 4, 0.15, 0.15, 0.15, 0.0, cyanOuter, true);
         from.getWorld().spawnParticle(Particle.END_ROD, x, y, z, 3, 0.05, 0.05, 0.05, 0.01);
         from.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, x, y, z, 2, 0.08, 0.08, 0.08, 0.01);
         if (i % 3 == 0) {
            double angle = i * 0.5;
            double arcRadius = 0.25;

            for (int arc = 0; arc < 3; arc++) {
               double arcAngle = angle + arc * Math.PI * 2.0 / 3.0;
               double offsetX = Math.cos(arcAngle) * arcRadius;
               double offsetZ = Math.sin(arcAngle) * arcRadius;
               from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, x + offsetX, y, z + offsetZ, 1, 0.02, 0.02, 0.02, 0.0);
            }
         }
      }
   }

   private void damageArmorPieces(Player player, int durabilityDamage) {
      if (durabilityDamage > 0) {
         for (ItemStack armor : player.getInventory().getArmorContents()) {
            ItemMeta meta;
            if (armor != null && armor.getType() != Material.AIR && (meta = armor.getItemMeta()) instanceof Damageable) {
               Damageable damageable = (Damageable)meta;
               int newDamage = damageable.getDamage() + durabilityDamage;
               if (newDamage >= armor.getType().getMaxDurability()) {
                  armor.setAmount(0);
               } else {
                  damageable.setDamage(newDamage);
                  armor.setItemMeta(meta);
               }
            }
         }
      }
   }

   private void restoreArmorDurability(Player player, int durabilityRestore) {
      if (durabilityRestore > 0) {
         for (ItemStack armor : player.getInventory().getArmorContents()) {
            ItemMeta meta;
            if (armor != null && armor.getType() != Material.AIR && (meta = armor.getItemMeta()) instanceof Damageable) {
               Damageable damageable = (Damageable)meta;
               int newDamage = Math.max(0, damageable.getDamage() - durabilityRestore);
               damageable.setDamage(newDamage);
               armor.setItemMeta(meta);
            }
         }

         player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5);
         player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5F, 2.0F);
      }
   }

   public void cancelCharging(Player player) {
      if (this.plugin.getFluxEnergyManager() != null) {
         this.plugin.getFluxEnergyManager().setCharging(player, false);
      }
   }

   @Override
   public void cleanup(Player player) {
      this.cancelCharging(player);
   }

   public void ground(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "flux-ground";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            LivingEntity target = this.getTargetEntity(player, 20);
            if (target == null) {
               player.sendMessage("§cNo target found!");
            } else if (target instanceof Player targetPlayer && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
               player.sendMessage("§c§lYou cannot stun " + targetPlayer.getName() + " because they are trusted!");
            } else {
               final int stunDurationSeconds = this.plugin.getConfig().getInt("abilities.durations.flux-ground-freeze", 4);
               int stunDuration = stunDurationSeconds * 20;
               target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunDuration, 3, false, true));
               target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, stunDuration, 1, false, true));
               target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, stunDuration, 0, false, true));
               target.setNoDamageTicks(0);
               if (target instanceof Player targetPlayer) {
                  stunnedPlayers.add(targetPlayer.getUniqueId());
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> stunnedPlayers.remove(targetPlayer.getUniqueId()), stunDuration);
                  (new BukkitRunnable() {
                     int count = 0;

                     public void run() {
                        if (targetPlayer.isOnline() && !targetPlayer.isDead() && ++this.count <= stunDurationSeconds * 2) {
                           int slot1 = (int)(Math.random() * 9.0);
                           int slot2 = (int)(Math.random() * 9.0);
                           if (slot1 != slot2) {
                              ItemStack i1 = targetPlayer.getInventory().getItem(slot1);
                              ItemStack i2 = targetPlayer.getInventory().getItem(slot2);
                              targetPlayer.getInventory().setItem(slot1, i2);
                              targetPlayer.getInventory().setItem(slot2, i1);
                           }
                        } else {
                           this.cancel();
                        }
                     }
                  }).runTaskTimer(this.plugin, 10L, 10L);
               }

               DustOptions darkCyanDust = new DustOptions(ParticleUtils.FLUX_DARK_CYAN, 1.5F);
               target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 200, 1.0, 1.8, 1.0, 0.0, darkCyanDust, true);
               target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0.0, 1.0, 0.0), 100, 0.7, 1.2, 0.7);
               target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation(), 60, 0.7, 0.2, 0.7);
               target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 0.5, 0.0), 40, 0.5, 0.5, 0.5);

               for (int i = 0; i < 24; i++) {
                  double angle = i / 24.0 * 2.0 * Math.PI;
                  double x = Math.cos(angle) * 1.8;
                  double z = Math.sin(angle) * 1.8;
                  target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(x, 0.1, z), 8, 0.15, 0.15, 0.15, 0.0, darkCyanDust, true);
                  target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(x, 0.5, z), 3, 0.1, 0.1, 0.1, 0.0);
               }

               target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.5F);
               target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8F, 1.5F);
               player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0F, 1.2F);
               player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 50, 1.2, 1.2, 1.2);
               this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, stunDurationSeconds);
               String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Ground");
               if (msg != null && !msg.isEmpty()) {
                  player.sendMessage(msg);
               }

               if (target instanceof Player targetPlayer) {
                  targetPlayer.sendMessage("§c§lYou have been grounded for " + stunDurationSeconds + " seconds! You cannot move or jump!");
               }

               String targetName = target instanceof Player ? ((Player)target).getName() : target.getType().name();
               player.sendMessage("§b⚡ §oGrounded " + targetName + " for " + stunDurationSeconds + " seconds!");
            }
         }
      }
   }

   public void flashbang(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "flux-flashbang";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            double radius = this.plugin.getConfig().getDouble("abilities.flux-flashbang.radius", 8.0);
            int duration = this.plugin.getConfigManager().getAbilityDuration("flux-flashbang") * 20;
            Location center = player.getLocation();
            int affectedCount = 0;

            for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
               if (entity instanceof LivingEntity && !entity.equals(player)) {
                  LivingEntity target = (LivingEntity)entity;
                  if (!(target instanceof Player targetPlayer && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer))) {
                     target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, true));
                     target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 1, false, true));
                     DustOptions white = new DustOptions(Color.WHITE, 1.5F);
                     target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 80, 0.5, 0.5, 0.5, 0.0, white, true);
                     affectedCount++;
                     if (target instanceof Player) {
                        ((Player)target).sendMessage("§f§lFLASHBANGED! §7You are blinded!");
                     }
                  }
               }
            }

            DustOptions brightWhite = new DustOptions(Color.WHITE, 2.0F);
            center.getWorld()
               .spawnParticle(Particle.DUST, center.clone().add(0.0, 1.0, 0.0), 200, radius / 2.0, radius / 2.0, radius / 2.0, 0.0, brightWhite, true);
            center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(0.0, 1.0, 0.0), 50, radius / 3.0, radius / 3.0, radius / 3.0, 0.2);
            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0.0, 1.0, 0.0), 80, radius / 2.0, radius / 2.0, radius / 2.0, 0.1);
            player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 1.8F);
            player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 2.0F);
            int flashDurationSeconds = this.plugin.getConfigManager().getAbilityDuration("flux-flashbang");
            this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, flashDurationSeconds);
            String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Flashbang");
            if (msg != null && !msg.isEmpty()) {
               player.sendMessage(msg);
            }

            player.sendMessage("§b⚡ §oFlashbang! Blinded " + affectedCount + " enemies!");
         }
      }
   }

   public void kineticBurst(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "flux-kinetic-burst";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            double radius = this.plugin.getConfig().getDouble("abilities.flux-kinetic-burst.radius", 6.0);
            double knockbackPower = this.plugin.getConfig().getDouble("abilities.flux-kinetic-burst.knockback", 2.6);
            Location center = player.getLocation();
            int affectedCount = 0;

            for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
               if (!entity.equals(player) && !(entity instanceof Player targetPlayer && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer))
                  )
                {
                  Vector direction;
                  if ((direction = entity.getLocation().toVector().subtract(center.toVector())).lengthSquared() > 0.0) {
                     direction.normalize();
                  }

                  direction.setY(0.5);
                  direction.multiply(knockbackPower);
                  entity.setVelocity(direction);
                  affectedCount++;
               }
            }

            player.getWorld().spawnParticle(Particle.SONIC_BOOM, center.clone().add(0.0, 1.0, 0.0), 10, 0.5, 0.5, 0.5, 0.0);
            DustOptions cyan = new DustOptions(ParticleUtils.FLUX_CYAN, 1.5F);
            player.getWorld().spawnParticle(Particle.DUST, center.clone().add(0.0, 1.0, 0.0), 150, radius / 2.0, radius / 2.0, radius / 2.0, 0.0, cyan, true);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0.0, 1.0, 0.0), 100, radius / 2.0, radius / 2.0, radius / 2.0, 0.2);
            ParticleUtils.drawRing(center, ParticleUtils.FLUX_CYAN, 1.2F, radius * 0.33, 0.1, 32);
            ParticleUtils.drawRing(center, ParticleUtils.FLUX_CYAN, 1.2F, radius * 0.66, 0.1, 32);
            ParticleUtils.drawRing(center, ParticleUtils.FLUX_CYAN, 1.2F, radius, 0.1, 32);
            player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5F, 0.8F);
            player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.5F);
            int kineticDurationSeconds = this.plugin.getConfigManager().getAbilityDuration("flux-kinetic-burst");
            this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, kineticDurationSeconds);
            String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Kinetic Burst");
            if (msg != null && !msg.isEmpty()) {
               player.sendMessage(msg);
            }

            player.sendMessage("§b⚡ §oKinetic Burst! Knocked back " + affectedCount + " entities!");
         }
      }
   }

   public void conduction(Player player) {
      String abilityKey = "flux-conduction";
      if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
         int range = this.plugin.getConfig().getInt("abilities.flux-conduction.range", 10);
         Location playerLoc = player.getLocation();
         Block closest = null;
         double closestDist = Double.MAX_VALUE;

         for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
               for (int z = -range; z <= range; z++) {
                  Block block = playerLoc.getBlock().getRelative(x, y, z);
                  double dist;
                  if (isCopperBlock(block.getType()) && (dist = block.getLocation().distanceSquared(playerLoc)) < closestDist) {
                     closestDist = dist;
                     closest = block;
                  }
               }
            }
         }

         if (closest == null) {
            player.sendMessage("§c⚡ §oNo copper blocks within " + range + " blocks!");
         } else {
            Block feet = closest.getRelative(0, 1, 0);
            Block head = closest.getRelative(0, 2, 0);
            if (feet.isPassable() && head.isPassable()) {
               Location departure = player.getLocation().clone();
               Location tpLoc = closest.getLocation().add(0.5, 1.0, 0.5);
               tpLoc.setYaw(departure.getYaw());
               tpLoc.setPitch(departure.getPitch());
               if (this.plugin.getAchievementManager() != null) {
                  int distance = (int)departure.distance(tpLoc);
                  this.plugin.getAchievementManager().addProgress(player, Achievement.ZIP_AWAY, distance);
               }

               player.teleport(tpLoc);
               DustOptions cyan = new DustOptions(ParticleUtils.FLUX_CYAN, 1.2F);
               departure.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, departure.clone().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.1);
               departure.getWorld().spawnParticle(Particle.DUST, departure.clone().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5, 0.0, cyan, true);
               tpLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, tpLoc.clone().add(0.0, 0.5, 0.0), 50, 0.5, 0.5, 0.5, 0.1);
               tpLoc.getWorld().spawnParticle(Particle.DUST, tpLoc.clone().add(0.0, 0.5, 0.0), 40, 0.5, 0.5, 0.5, 0.0, cyan, true);
               player.playSound(departure, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.5F);
               player.playSound(tpLoc, Sound.BLOCK_COPPER_BULB_TURN_ON, 1.0F, 1.2F);
               player.sendMessage("§b⚡ §oConducted to copper!");
               this.plugin.getAbilityManager().useAbility(player, abilityKey);
            } else {
               player.sendMessage("§c⚡ §oThere is no safe space above that copper block!");
            }
         }
      }
   }

   private static boolean isCopperBlock(Material material) {
      String name = material.name();
      return name.contains("COPPER") && !name.endsWith("_ORE") && !name.startsWith("RAW_");
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
