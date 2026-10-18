package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class BrokenGemDamageListener implements Listener {
   private final BlissGems plugin;

   public BrokenGemDamageListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onGemHolderDamagePlayer(EntityDamageByEntityEvent event) {
      if (this.plugin.getConfig().getBoolean("combat.broken-gem-bonus.enabled", true)) {
         if (event.getEntity() instanceof Player victim) {
            Player attacker = this.resolveAttacker(event.getDamager());
            if (attacker != null && !attacker.equals(victim)) {
               if (this.plugin.getEnergyManager().getEnergyState(victim) == EnergyState.BROKEN) {
                  if (this.plugin.getGemManager().hasGemInOffhand(attacker)) {
                     if (this.plugin.getRegionManager() == null || !this.plugin.getRegionManager().areGemsDisabled(attacker)) {
                        double multiplier = this.plugin.getConfig().getDouble("combat.broken-gem-bonus.damage-multiplier", 1.5);
                        if (!(multiplier <= 1.0)) {
                           event.setDamage(event.getDamage() * multiplier);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Player resolveAttacker(Entity damager) {
      if (damager instanceof Player player) {
         return player;
      } else {
         Projectile projectile;
         ProjectileSource shooter;
         if (damager instanceof Projectile && (shooter = (projectile = (Projectile)damager).getShooter()) instanceof Player) {
            Player player = (Player)shooter;
            return player;
         } else {
            return null;
         }
      }
   }
}
