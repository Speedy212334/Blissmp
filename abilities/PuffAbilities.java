package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PuffAbilities implements GemAbilityHandler {
   private final BlissGems plugin;
   private final Set<UUID> fallDamageImmune = new HashSet<>();

   public PuffAbilities(BlissGems plugin) {
      this.plugin = plugin;
   }

   public boolean hasFallDamageImmunity(Player player) {
      return this.fallDamageImmune.contains(player.getUniqueId());
   }

   public void removeFallDamageImmunity(Player player) {
      this.fallDamageImmune.remove(player.getUniqueId());
   }

   public void spawnGroundImpactRing(final Location center) {
      final World world = center.getWorld();
      if (world != null) {
         world.spawnParticle(Particle.EXPLOSION, center.clone().add(0.0, 0.2, 0.0), 1);
         world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 1.4F);
         world.playSound(center, Sound.ENTITY_BREEZE_LAND, 1.0F, 0.6F);
         (new BukkitRunnable() {
               int tick = 0;
               final int maxTicks = 12;

               public void run() {
                  if (this.tick < 12 && world.isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4)) {
                     double progress = this.tick / 12.0;
                     double radius = 0.6 + progress * 4.2;
                     int g = Math.max(70, (int)(225.0 - progress * 150.0));
                     int b = Math.max(0, (int)(80.0 - progress * 80.0));
                     DustOptions dust = new DustOptions(Color.fromRGB(255, g, b), 1.7F);
                     Location c = center.clone().add(0.0, 0.15, 0.0);
                     int points = Math.max(12, (int)(radius * 10.0));

                     for (int i = 0; i < points; i++) {
                        double a = (Math.PI * 2) * i / points;
                        world.spawnParticle(
                           Particle.DUST, c.getX() + Math.cos(a) * radius, c.getY(), c.getZ() + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0, dust
                        );
                     }

                     if (this.tick % 2 == 0) {
                        world.spawnParticle(Particle.LAVA, center.clone().add(0.0, 0.3, 0.0), 3, 0.5, 0.1, 0.5, 0.0);
                        world.spawnParticle(Particle.FLAME, center.clone().add(0.0, 0.2, 0.0), 4, radius * 0.4, 0.05, radius * 0.4, 0.01);
                     }

                     this.tick++;
                  } else {
                     this.cancel();
                  }
               }
            })
            .runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   public void impactRingOnLand(final Player target) {
      (new BukkitRunnable() {
         int waited = 0;

         public void run() {
            if (target == null || !target.isOnline() || this.waited > 100) {
               this.cancel();
            } else if (target.isOnGround()) {
               PuffAbilities.this.spawnGroundImpactRing(target.getLocation());
               this.cancel();
            } else {
               this.waited++;
            }
         }
      }).runTaskTimer(this.plugin, 2L, 1L);
   }

   public void onRightClick(Player player, int tier) {
      if (tier == 2 && player.isSneaking()) {
         this.breezyBash(player);
      } else {
         this.dash(player);
      }
   }

   @Override
   public void onPrimary(Player player, int tier) {
      this.dash(player);
   }

   @Override
   public void onSecondary(Player player, int tier) {
      this.breezyBash(player);
   }

   @Override
   public void onTertiary(Player player, int tier) {
      this.groupBreezyBash(player);
   }

   @Override
   public void onQuaternary(Player player, int tier) {
      this.updraft(player);
   }

   public void dash(Player player) {
      String abilityKey = "puff-dash";
      if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
         double power = this.plugin.getConfig().getDouble("abilities.puff-dash.power", 3.2);
         double vertical = this.plugin.getConfig().getDouble("abilities.puff-dash.vertical", 0.18);
         Vector direction = player.getLocation().getDirection();
         direction.setY(vertical);
         direction.multiply(power);
         player.setVelocity(direction);
         this.plugin
            .getServer()
            .getScheduler()
            .runTaskLater(
               this.plugin,
               () -> {
                  double damage = this.plugin.getConfigManager().getAbilityDamage("puff-dash");

                  for (Entity entity : player.getNearbyEntities(2.5, 2.0, 2.5)) {
                     if (entity instanceof LivingEntity
                        && entity != player
                        && (!(entity instanceof Player) || !this.plugin.getTrustedPlayersManager().isTrusted(player, (Player)entity))) {
                        ((LivingEntity)entity).damage(damage, player);
                     }
                  }
               },
               5L
            );
         player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0F, 1.5F);
         DustOptions whiteDust = new DustOptions(ParticleUtils.PUFF_WHITE, 1.5F);
         player.spawnParticle(Particle.DUST, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.0, whiteDust, true);
         player.spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
         this.plugin.getAbilityManager().useAbility(player, abilityKey);
         String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dash");
         if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
         }
      }
   }

   public void breezyBash(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "puff-breezy-bash";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            Vector velocity = player.getVelocity();
            velocity.setY(2.0);
            player.setVelocity(velocity);
            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0F, 1.0F);
            DustOptions whiteDust = new DustOptions(ParticleUtils.PUFF_WHITE, 1.5F);
            player.spawnParticle(Particle.DUST, player.getLocation(), 50, 1.0, 1.0, 1.0, 0.0, whiteDust, true);
            player.spawnParticle(Particle.CLOUD, player.getLocation(), 40, 1.0, 1.0, 1.0, 0.2);
            UUID uuid = player.getUniqueId();
            this.fallDamageImmune.add(uuid);
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.fallDamageImmune.remove(uuid), 200L);
            this.plugin.getAbilityManager().useAbility(player, abilityKey);
            String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Breezy Bash");
            if (msg != null && !msg.isEmpty()) {
               player.sendMessage(msg);
            }
         }
      }
   }

   public void updraft(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "puff-updraft";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            double radius = this.plugin.getConfig().getDouble("abilities.puff-updraft.radius", 6.0);
            double launchVelocity = this.plugin.getConfig().getDouble("abilities.puff-updraft.launch-velocity", 3.2);
            int hitCount = 0;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
               if (entity instanceof Player target && !this.plugin.getTrustedPlayersManager().isTrusted(player, target)) {
                  target.setVelocity(new Vector(0.0, launchVelocity, 0.0));
                  target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0F, 0.8F);
                  target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                  target.sendMessage("§b§oAn updraft flings you into the sky!");
                  this.impactRingOnLand(target);
                  hitCount++;
               }
            }

            DustOptions whiteDust = new DustOptions(ParticleUtils.PUFF_WHITE, 1.5F);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 60, 2.0, 1.0, 2.0, 0.0, whiteDust, true);
            player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0.0, 0.5, 0.0), 20, 2.0, 0.5, 2.0);
            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0F, 0.7F);
            this.plugin.getAbilityManager().useAbility(player, abilityKey);
            String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Updraft");
            if (msg != null && !msg.isEmpty()) {
               player.sendMessage(msg);
            }

            if (hitCount > 0) {
               player.sendMessage("§f§o" + hitCount + " player(s) sent flying!");
            }
         }
      }
   }

   public void groupBreezyBash(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "puff-group-bash";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            double radius = this.plugin.getConfig().getDouble("abilities.puff-group-bash.radius", 6.0);
            double knockback = this.plugin.getConfig().getDouble("abilities.puff-group-bash.knockback", 2.5);
            int hitCount = 0;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
               if (entity instanceof Player target && !this.plugin.getTrustedPlayersManager().isTrusted(player, target)) {
                  Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector());
                  if (direction.lengthSquared() < 0.01) {
                     direction = new Vector(1, 0, 0);
                  }

                  direction.normalize().multiply(knockback);
                  direction.setY(1.5);
                  target.setVelocity(direction);
                  target.sendMessage("§f§oA gust of wind blows you away!");
                  hitCount++;
               }
            }

            DustOptions whiteDust = new DustOptions(ParticleUtils.PUFF_WHITE, 1.5F);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 60, 2.0, 1.0, 2.0, 0.0, whiteDust, true);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 0.5, 0.0), 50, 2.0, 0.5, 2.0, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0F, 0.8F);
            this.plugin.getAbilityManager().useAbility(player, abilityKey);
            String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Group Breezy Bash");
            if (msg != null && !msg.isEmpty()) {
               player.sendMessage(msg);
            }

            if (hitCount > 0) {
               player.sendMessage("§f§o" + hitCount + " player(s) knocked away!");
            }
         }
      }
   }
}
