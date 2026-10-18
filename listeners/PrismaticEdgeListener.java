package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PrismaticEdgeListener implements Listener {
   private static final String ITEM_ID = "prismatic_edge";
   private static final String BEAM_COOLDOWN_ID = "prismatic-beam";
   private static final Color[] PRISM_COLORS = new Color[]{
      Color.fromRGB(106, 11, 184),
      Color.fromRGB(255, 85, 85),
      Color.fromRGB(85, 255, 255),
      Color.fromRGB(85, 255, 85),
      Color.fromRGB(255, 255, 255),
      Color.fromRGB(255, 255, 85),
      Color.fromRGB(170, 0, 0),
      Color.fromRGB(255, 170, 0)
   };
   private final BlissGems plugin;
   private final Map<UUID, Integer> comboHits = new HashMap<>();
   private final Set<UUID> critState = new HashSet<>();

   public PrismaticEdgeListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   private boolean isPrismaticEdge(ItemStack item) {
      return item != null && "prismatic_edge".equals(CustomItemManager.getIdByItem(item));
   }

   @EventHandler
   public void onBeamCast(PlayerInteractEvent event) {
      if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
         if (event.getHand() == EquipmentSlot.HAND) {
            if (event.getPlayer().isSneaking()) {
               if (this.isPrismaticEdge(event.getItem())) {
                  Player player = event.getPlayer();
                  event.setCancelled(true);
                  int cooldownSeconds = this.plugin.getConfig().getInt("prismatic-edge.beam.cooldown-seconds", 120);
                  if (this.plugin.getAbilityManager().isOnCooldown(player, "prismatic-beam")) {
                     int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, "prismatic-beam");
                     player.sendMessage("§c§oPrismatic Beam is recharging - §f" + remaining + "s§c§o left.");
                  } else {
                     this.plugin.getAbilityManager().setCooldown(player, "prismatic-beam", cooldownSeconds);
                     this.fireBeam(player);
                  }
               }
            }
         }
      }
   }

   private void fireBeam(Player player) {
      double range = this.plugin.getConfig().getDouble("prismatic-edge.beam.range", 24.0);
      double damage = this.plugin.getConfig().getDouble("prismatic-edge.beam.damage", 10.0);
      double beamRadius = this.plugin.getConfig().getDouble("prismatic-edge.beam.hit-radius", 1.6);
      int freezeSeconds = this.plugin.getConfig().getInt("prismatic-edge.beam.freeze-seconds", 3);
      int regenSeconds = this.plugin.getConfig().getInt("prismatic-edge.beam.regen-seconds", 8);
      int regenLevel = this.plugin.getConfig().getInt("prismatic-edge.beam.regen-level", 2);
      Location eye = player.getEyeLocation();
      Vector direction = eye.getDirection().normalize();
      HashSet<UUID> alreadyHit = new HashSet<>();
      player.getWorld().playSound(eye, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.2F, 1.4F);
      player.getWorld().playSound(eye, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.8F);

      Location point;
      for (double travelled = 0.0;
         travelled < range && !(point = eye.clone().add(direction.clone().multiply(travelled))).getBlock().getType().isSolid();
         travelled += 0.4
      ) {
         Color color = PRISM_COLORS[(int)(travelled * 2.0) % PRISM_COLORS.length];
         player.getWorld().spawnParticle(Particle.DUST, point, 4, 0.08, 0.08, 0.08, 0.0, new DustOptions(color, 1.4F), true);
         player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.03, 0.03, 0.03, 0.0);

         for (Entity entity : player.getWorld().getNearbyEntities(point, beamRadius, beamRadius, beamRadius)) {
            LivingEntity living;
            if (entity instanceof LivingEntity && !(living = (LivingEntity)entity).equals(player) && alreadyHit.add(living.getUniqueId())) {
               living.damage(damage, player);
               this.applyFreeze(living, freezeSeconds);
               living.getWorld().spawnParticle(Particle.FLASH, living.getLocation().add(0.0, 1.0, 0.0), 2, 0.2, 0.2, 0.2, 0.0);
               living.getWorld().playSound(living.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.8F);
            }
         }
      }

      player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenSeconds * 20, Math.max(0, regenLevel - 1), true, true));
      player.sendMessage("§b§l✦ §fPrismatic Beam released!");
   }

   private void applyFreeze(LivingEntity target, int seconds) {
      target.setFreezeTicks(Math.max(target.getFreezeTicks(), seconds * 20 + 140));
      target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20, 6, true, true));
      target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, seconds * 20, 2, true, true));
      if (target instanceof Player frozen) {
         UUID id = frozen.getUniqueId();
         SpeedAbilities.freezePlayer(id);
         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> SpeedAbilities.unfreezePlayer(id), seconds * 20L);
         frozen.sendMessage("§b§oYou are frozen solid!");
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onComboHit(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player victim) {
         this.resetCombo(victim);
      }

      Entity var9;
      if ((var9 = event.getDamager()) instanceof Player) {
         Player attacker = (Player)var9;
         if (this.isPrismaticEdge(attacker.getInventory().getItemInMainHand())) {
            int required = this.plugin.getConfig().getInt("prismatic-edge.combo.hits-required", 5);
            double critMultiplier = this.plugin.getConfig().getDouble("prismatic-edge.combo.crit-multiplier", 1.5);
            UUID id = attacker.getUniqueId();
            if (this.critState.contains(id)) {
               event.setDamage(event.getDamage() * critMultiplier);
               this.spawnCritEffect(event.getEntity());
            } else {
               int hits = this.comboHits.merge(id, 1, Integer::sum);
               if (hits >= required) {
                  this.critState.add(id);
                  this.comboHits.remove(id);
                  attacker.playSound(attacker.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0F, 1.6F);
                  attacker.sendMessage("§b§l✦ §fCombo complete - §eyour hits now crit§f!");
               } else {
                  attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7F, 1.0F + hits * 0.15F);
               }
            }
         }
      }
   }

   private void spawnCritEffect(Entity target) {
      target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0, 1.0, 0.0), 25, 0.4, 0.4, 0.4, 0.4);
      target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.2F);
   }

   private void resetCombo(Player player) {
      UUID id = player.getUniqueId();
      boolean hadCrit = this.critState.remove(id);
      this.comboHits.remove(id);
      if (hadCrit) {
         player.sendMessage("§7§oYour crit streak was broken.");
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      UUID id = event.getPlayer().getUniqueId();
      this.comboHits.remove(id);
      this.critState.remove(id);
   }
}
