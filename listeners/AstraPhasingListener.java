package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.SoulManager;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.GemType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class AstraPhasingListener implements Listener {
   private static final Set<DamageCause> PHASEABLE = EnumSet.of(
      DamageCause.ENTITY_ATTACK,
      DamageCause.ENTITY_SWEEP_ATTACK,
      DamageCause.PROJECTILE,
      DamageCause.ENTITY_EXPLOSION,
      DamageCause.BLOCK_EXPLOSION,
      DamageCause.MAGIC,
      DamageCause.THORNS,
      DamageCause.LIGHTNING,
      DamageCause.SONIC_BOOM,
      DamageCause.DRAGON_BREATH,
      DamageCause.FALLING_BLOCK
   );
   private static final Color PHASE_COLOR = Color.fromRGB(190, 120, 255);
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND
   };
   private final BlissGems plugin;
   private final Map<UUID, Integer> phasedUntil = new HashMap<>();
   private final Map<UUID, Integer> phaseReady = new HashMap<>();
   private final Map<UUID, Integer> guardReady = new HashMap<>();

   public AstraPhasingListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   private boolean active(Player p) {
      if (!this.plugin.getGemManager().hasGemTypeInOffhand(p, GemType.ASTRA)) {
         return false;
      } else if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(p)) {
         return false;
      } else {
         return !this.plugin.getEnergyManager().arePassivesActive(p)
            ? false
            : this.plugin.getRegionManager() == null || !this.plugin.getRegionManager().areGemsDisabled(p);
      }
   }

   private String tierPath(Player p, String key) {
      int tier = Math.max(1, this.plugin.getGemManager().getTierFor(p, GemType.ASTRA));
      return "passives.astra.tier" + tier + "." + key;
   }

   public boolean isPhased(Player p) {
      Integer until = this.phasedUntil.get(p.getUniqueId());
      return until != null && Bukkit.getCurrentTick() < until;
   }

   @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
   public void onDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player p) {
         if (this.isPhased(p)) {
            if (event.getCause() != DamageCause.KILL && event.getCause() != DamageCause.VOID) {
               event.setCancelled(true);
            }
         } else if (this.active(p)) {
            int now = Bukkit.getCurrentTick();
            UUID id = p.getUniqueId();
            double after = p.getHealth() + p.getAbsorptionAmount() - event.getFinalDamage();
            double trigger = this.plugin.getConfig().getDouble("passives.astra.soul-guard.trigger-health", 4.0);
            if (after <= trigger && now >= this.guardReady.getOrDefault(id, 0) && this.plugin.getConfig().getBoolean("passives.astra.soul-guard.enabled", true)
               )
             {
               SoulManager.CapturedMob soul = this.plugin.getSoulManager().consumeSoul(p);
               if (soul != null) {
                  event.setCancelled(true);
                  double heal = this.plugin.getConfig().getDouble(this.tierPath(p, "soul-guard-heal"), 6.0);
                  double max = p.getAttribute(Attributes.maxHealth()).getValue();
                  p.setHealth(Math.min(max, p.getHealth() + heal));
                  this.guardReady.put(id, now + 20 * this.plugin.getConfig().getInt("passives.astra.soul-guard.cooldown-seconds", 60));
                  this.phase(p, true);
                  p.sendActionBar(PedestalManager.color("&5\ud83d\udd2e &dThe " + soul.getDisplayName() + " soul took the blow &7(+" + heal / 2.0 + "❤)"));
                  p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0.0, 1.0, 0.0), 30, 0.4, 0.6, 0.4, 0.05);
                  p.playSound(p.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1.0F, 0.8F);
                  if (this.plugin.getAchievementManager() != null && after <= 0.0) {
                     this.plugin.getAchievementManager().unlock(p, Achievement.SAVED_BY_THE_DICE);
                  }

                  return;
               }
            }

            if (PHASEABLE.contains(event.getCause()) && now >= this.phaseReady.getOrDefault(id, 0)) {
               double chance = this.plugin.getConfig().getDouble(this.tierPath(p, "phase-chance"), 0.05);
               if (!(ThreadLocalRandom.current().nextDouble() >= chance)) {
                  boolean fatal = event.getFinalDamage() >= p.getHealth() + p.getAbsorptionAmount();
                  event.setCancelled(true);
                  this.phase(p, false);
                  if (fatal && this.plugin.getAchievementManager() != null) {
                     this.plugin.getAchievementManager().unlock(p, Achievement.SAVED_BY_THE_DICE);
                  }
               }
            }
         }
      }
   }

   private void phase(Player p, boolean fromSoul) {
      int now = Bukkit.getCurrentTick();
      int ticks = this.plugin.getConfig().getInt(this.tierPath(p, "phase-ticks"), 20);
      int cooldown = this.plugin.getConfig().getInt("passives.astra.phase-cooldown-seconds", 8);
      UUID id = p.getUniqueId();
      this.phasedUntil.put(id, now + ticks);
      if (!fromSoul) {
         this.phaseReady.put(id, now + ticks + cooldown * 20);
      }

      boolean ownInvis = !p.hasPotionEffect(PotionEffectType.INVISIBILITY);
      if (ownInvis) {
         p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks, 0, true, false, false));
      }

      boolean hideGear = this.plugin.getConfig().getBoolean("passives.astra.phase-hides-gear", true);
      if (hideGear) {
         this.sendGear(p, true);
      }

      Location at = p.getLocation().add(0.0, 1.25, 0.0);
      p.getWorld().spawnParticle(Particle.DUST, at, 24, 0.3, 0.5, 0.3, 0.0, new DustOptions(PHASE_COLOR, 1.0F));
      p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, at, 16, 0.3, 0.5, 0.3, 0.05);
      p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5F, 1.6F);
      if (!fromSoul) {
         p.sendActionBar(PedestalManager.color("&5✦ &dPhased"));
      }

      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         Integer until = this.phasedUntil.get(id);
         if (until != null && Bukkit.getCurrentTick() >= until) {
            this.phasedUntil.remove(id);
            if (p.isOnline()) {
               if (ownInvis) {
                  p.removePotionEffect(PotionEffectType.INVISIBILITY);
               }

               if (hideGear) {
                  this.sendGear(p, false);
               }
            }
         }
      }, ticks);
   }

   private void sendGear(Player p, boolean hidden) {
      ItemStack air = new ItemStack(Material.AIR);

      for (Player viewer : p.getWorld().getPlayers()) {
         if (viewer != p) {
            for (EquipmentSlot slot : SLOTS) {
               viewer.sendEquipmentChange(p, slot, hidden ? air : p.getInventory().getItem(slot));
            }
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      UUID id = event.getPlayer().getUniqueId();
      this.phasedUntil.remove(id);
      this.phaseReady.remove(id);
      this.guardReady.remove(id);
   }
}
