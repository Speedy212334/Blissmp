package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class GemLockManager {
   public static final String LOCK_GLYPH = "ꐬ";
   private final BlissGems plugin;
   private final Map<UUID, Long> lockedUntil = new ConcurrentHashMap<>();

   public GemLockManager(BlissGems plugin) {
      this.plugin = plugin;
   }

   public boolean isLocked(Player player) {
      return player != null && this.isLocked(player.getUniqueId());
   }

   public boolean isLocked(UUID id) {
      Long until = this.lockedUntil.get(id);
      if (until == null) {
         return false;
      } else if (System.currentTimeMillis() >= until) {
         this.lockedUntil.remove(id);
         return false;
      } else {
         return true;
      }
   }

   public int getRemainingSeconds(UUID id) {
      Long until = this.lockedUntil.get(id);
      if (until == null) {
         return 0;
      }

      long ms = until - System.currentTimeMillis();
      return ms <= 0L ? 0 : (int)Math.ceil(ms / 1000.0);
   }

   public void lock(Player target, int seconds) {
      this.lockedUntil.put(target.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
   }

   public void clear(UUID id) {
      this.lockedUntil.remove(id);
   }

   public void castGemLock(Player caster) {
      String key = "auratus-gemlock";
      if (this.plugin.getAbilityManager().isOnCooldown(caster, key)) {
         caster.sendMessage("§6ꐬ §7Gem Lock on cooldown: §c" + this.plugin.getAbilityManager().getRemainingCooldown(caster, key) + "s");
      } else if (this.plugin.getAbilityManager().canUseAbility(caster, key)) {
         int range = this.plugin.getConfig().getInt("abilities.auratus-gemlock.range", 20);
         int duration = this.plugin.getConfig().getInt("abilities.auratus-gemlock.duration", 10);
         Player target = this.raycastPlayer(caster, range);
         if (target == null) {
            caster.sendMessage("§c§oNo player in your sights to lock!");
         } else if (target.getUniqueId().equals(caster.getUniqueId())) {
            caster.sendMessage("§c§oYou can't lock your own gem!");
         } else if (this.plugin.getTrustedPlayersManager() != null && this.plugin.getTrustedPlayersManager().isTrusted(caster, target)) {
            caster.sendMessage("§c§oYou can't lock a trusted player!");
         } else if (!this.plugin.getGemManager().hasActiveGem(target)) {
            caster.sendMessage("§c§o" + target.getName() + " has no gem to lock!");
         } else {
            this.lock(target, duration);
            this.plugin.getAbilityManager().useAbility(caster, key);
            caster.sendMessage("§6§oYou locked " + target.getName() + "'s gem for " + duration + "s!");
            target.sendMessage("§6§lꐬ §c§lYour gem has been LOCKED! §7(" + duration + "s)");
            DustOptions gold = new DustOptions(Color.fromRGB(255, 200, 60), 1.6F);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.0, gold, true);
            target.getWorld().spawnParticle(Particle.WAX_ON, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.8, 0.5, 0.05);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.2F, 0.7F);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0F, 0.6F);
            caster.playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
         }
      }
   }

   private Player raycastPlayer(Player caster, int range) {
      RayTraceResult result = caster.getWorld()
         .rayTraceEntities(
            caster.getEyeLocation(), caster.getEyeLocation().getDirection(), range, 0.5, entity -> entity instanceof Player && !entity.equals(caster)
         );
      return result != null && result.getHitEntity() instanceof Player ? (Player)result.getHitEntity() : null;
   }
}
