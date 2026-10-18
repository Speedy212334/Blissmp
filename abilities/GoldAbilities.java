package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class GoldAbilities implements GemAbilityHandler, Listener {
   public static final String BEAM_COOLDOWN_ID = "gold-beam";
   private static final String SOUL_MENU_TITLE = "§6§lHarvested Souls";
   private static final Set<String> PRIMARY_REQUIRES_TIER_2 = Set.of("wealth", "strength");
   private final BlissGems plugin;
   private final Map<UUID, BukkitRunnable> charging = new HashMap<>();

   public GoldAbilities(BlissGems plugin) {
      this.plugin = plugin;
   }

   @Override
   public void onPrimary(Player player, int tier) {
      this.channel(player, AbilitySlot.PRIMARY);
   }

   @Override
   public void onSecondary(Player player, int tier) {
      this.channel(player, AbilitySlot.SECONDARY);
   }

   @Override
   public void onQuinary(Player player, int tier) {
      this.channel(player, AbilitySlot.TERTIARY);
   }

   @Override
   public void onSenary(Player player, int tier) {
      this.channel(player, AbilitySlot.QUATERNARY);
   }

   private void channel(Player player, AbilitySlot slot) {
      GoldGemManager gold = this.plugin.getGoldGemManager();
      String soul = gold.getActive(player.getUniqueId());
      if (soul == null) {
         player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
      } else {
         GemRegistryImpl registry = this.plugin.getGemRegistry();
         GemAbilityHandler handler = registry != null ? registry.getAbilityHandler(soul) : null;
         if (handler == null) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
         } else {
            int soulTier = gold.getActiveTier(player.getUniqueId());
            boolean primaryNeedsTier2 = slot == AbilitySlot.PRIMARY && PRIMARY_REQUIRES_TIER_2.contains(soul);
            if ((slot != AbilitySlot.PRIMARY || primaryNeedsTier2) && soulTier < 2) {
               String message = this.plugin.getConfigManager().getMessage("gold-soul-too-weak");
               if (message.isEmpty()) {
                  message = "§c§oThe {gem} soul was taken at Tier 1, it has no second power to give.";
               }

               player.sendMessage(message.replace("{gem}", this.plugin.getGemManager().getGemDisplayName(soul)));
            } else {
               this.plugin.getGemManager().setChannelTierOverride(player.getUniqueId(), soulTier);

               try {
                  switch (slot) {
                     case PRIMARY:
                        handler.onPrimary(player, soulTier);
                        return;
                     case SECONDARY:
                        handler.onSecondary(player, soulTier);
                        return;
                     case TERTIARY:
                        handler.onTertiary(player, soulTier);
                        return;
                     default:
                        handler.onQuaternary(player, soulTier);
                  }
               } finally {
                  this.plugin.getGemManager().clearChannelTierOverride(player.getUniqueId());
               }
            }
         }
      }
   }

   @Override
   public void onTertiary(final Player player, int tier) {
      final UUID playerId = player.getUniqueId();
      if (this.charging.containsKey(playerId)) {
         this.breakCharge(player, "gold-beam-cancelled");
      } else if (this.plugin.getGoldGemManager().isGoldGemInMainHand(player)) {
         if (this.plugin.getAbilityManager().isOnCooldown(player, "gold-beam")) {
            int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, "gold-beam");
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-cooldown").replace("{seconds}", String.valueOf(remaining)));
         } else {
            final int chargeTicks = Math.max(1, this.plugin.getConfig().getInt("gold.beam.charge-ticks", 60));
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-charging"));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.4F, 0.5F);
            BukkitRunnable task = new BukkitRunnable() {
               int ticks = 0;

               public void run() {
                  if (player.isOnline() && !player.isDead()) {
                     Location eye = player.getEyeLocation();
                     Vector direction = eye.getDirection().normalize();

                     for (double distance = 1.0; distance <= 4.0; distance += 0.5) {
                        player.getWorld()
                           .spawnParticle(
                              Particle.DUST,
                              eye.clone().add(direction.clone().multiply(distance)),
                              2,
                              0.1,
                              0.1,
                              0.1,
                              0.0,
                              new DustOptions(Color.fromRGB(255, 200, 40), 1.4F)
                           );
                     }

                     if (this.ticks % 10 == 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.2F, 0.6F + this.ticks / 120.0F);
                     }

                     if (++this.ticks >= chargeTicks) {
                        GoldAbilities.this.charging.remove(playerId);
                        this.cancel();
                        GoldAbilities.this.fireBeam(player);
                     }
                  } else {
                     GoldAbilities.this.charging.remove(playerId);
                     this.cancel();
                  }
               }
            };
            this.charging.put(playerId, task);
            task.runTaskTimer(this.plugin, 1L, 1L);
         }
      }
   }

   private void fireBeam(Player player) {
      double range = this.plugin.getConfig().getDouble("gold.beam.range", 60.0);
      double damage = this.plugin.getConfig().getDouble("gold.beam.damage", 24.0);
      int cooldown = this.plugin.getConfig().getInt("gold.beam.cooldown", 300);
      Location eye = player.getEyeLocation();
      Vector direction = eye.getDirection().normalize();
      RayTraceResult blockHit = player.getWorld().rayTraceBlocks(eye, direction, range);
      double reach = blockHit != null && blockHit.getHitPosition() != null ? blockHit.getHitPosition().distance(eye.toVector()) : range;
      RayTraceResult entityHit = player.getWorld()
         .rayTraceEntities(eye, direction, reach, 0.6, entity -> entity instanceof LivingEntity && !entity.equals(player));
      Vector sideways = direction.clone().crossProduct(new Vector(0.0, 1.0, 0.0));
      if (sideways.lengthSquared() < 1.0E-4) {
         sideways = new Vector(1.0, 0.0, 0.0);
      }

      sideways.normalize();
      Vector upwards = direction.clone().crossProduct(sideways).normalize();
      DustOptions core = new DustOptions(Color.fromRGB(255, 235, 130), 3.0F);
      DustOptions corona = new DustOptions(Color.fromRGB(255, 170, 0), 1.6F);

      for (double distance = 0.0; distance <= reach; distance += 0.2) {
         Location at = eye.clone().add(direction.clone().multiply(distance));
         player.getWorld().spawnParticle(Particle.DUST, at, 6, 0.12, 0.12, 0.12, 0.0, core);
         player.getWorld().spawnParticle(Particle.END_ROD, at, 1, 0.05, 0.05, 0.05, 0.0);
         double spin = distance * 1.2;

         for (int step = 0; step < 4; step++) {
            double angle = spin + step * Math.PI / 2.0;
            Location ring = at.clone().add(sideways.clone().multiply(Math.cos(angle) * 0.55)).add(upwards.clone().multiply(Math.sin(angle) * 0.55));
            player.getWorld().spawnParticle(Particle.DUST, ring, 2, 0.05, 0.05, 0.05, 0.0, corona);
         }
      }

      player.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.6F, 1.2F);
      this.plugin.getAbilityManager().setCooldown(player, "gold-beam", cooldown);
      Entity entity2;
      if (entityHit != null && (entity2 = entityHit.getHitEntity()) instanceof LivingEntity) {
         LivingEntity target = (LivingEntity)entity2;
         target.damage(damage, player);
         target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 6, 0.6, 0.6, 0.6, 0.0);
         target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 60, 0.5, 0.7, 0.5, 0.0, core);
      } else {
         player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-missed"));
      }
   }

   private void breakCharge(Player player, String messageKey) {
      BukkitRunnable task = this.charging.remove(player.getUniqueId());
      if (task != null) {
         task.cancel();
         player.sendMessage(this.plugin.getConfigManager().getMessage(messageKey));
         player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2F, 0.8F);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onChargerDamaged(EntityDamageEvent event) {
      Entity entity = event.getEntity();
      Player player;
      if (entity instanceof Player && this.charging.containsKey((player = (Player)entity).getUniqueId())) {
         this.breakCharge(player, "gold-beam-broken");
      }
   }

   @Override
   public void onQuaternary(Player player, int tier) {
      this.openSoulMenu(player);
   }

   public void openSoulMenu(Player player) {
      Map<String, GoldGemManager.Harvest> souls = this.plugin.getGoldGemManager().getHarvested(player.getUniqueId());
      if (souls.isEmpty()) {
         player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
      } else {
         Inventory menu = Bukkit.createInventory(null, 27, "§6§lHarvested Souls");
         String selected = this.plugin.getGoldGemManager().getActive(player.getUniqueId());
         int slot = 0;

         for (Entry<String, GoldGemManager.Harvest> soul : souls.entrySet()) {
            ItemStack icon = CustomItemManager.getItemById(soul.getKey() + "_gem_t" + soul.getValue().tier());
            if (icon != null) {
               ItemMeta meta = icon.getItemMeta();
               if (meta != null && soul.getKey().equals(selected)) {
                  meta.setDisplayName(meta.getDisplayName() + " §a§l< CHANNELLED >");
                  icon.setItemMeta(meta);
               }

               menu.setItem(slot++, icon);
            }
         }

         player.openInventory(menu);
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onSoulMenuClick(InventoryClickEvent event) {
      if ("§6§lHarvested Souls".equals(event.getView().getTitle())) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player && event.getCurrentItem() != null) {
            String itemId = CustomItemManager.getIdByItem(event.getCurrentItem());
            String gemId = itemId != null ? this.plugin.getGemRegistry().gemIdFromItemId(itemId) : null;
            if (gemId != null) {
               if (this.plugin.getGoldGemManager().setActive(player, gemId)) {
                  player.sendMessage(
                     this.plugin.getConfigManager().getMessage("gold-soul-selected").replace("{gem}", this.plugin.getGemManager().getGemDisplayName(gemId))
                  );
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.4F);
               }

               player.closeInventory();
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onHolderAttack(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player) {
         if (this.plugin.getGoldGemManager().holdsGoldGem(player)) {
            double chance = this.plugin.getConfig().getDouble("gold.damage-boost.chance", 0.15);
            if (!(Math.random() >= chance)) {
               double multiplier = this.plugin.getConfig().getDouble("gold.damage-boost.multiplier", 1.5);
               event.setDamage(event.getDamage() * multiplier);
               player.getWorld()
                  .spawnParticle(
                     Particle.DUST,
                     event.getEntity().getLocation().add(0.0, 1.0, 0.0),
                     10,
                     0.3,
                     0.3,
                     0.3,
                     0.0,
                     new DustOptions(Color.fromRGB(255, 215, 0), 1.2F)
                  );
               player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
            }
         }
      }
   }

   @Override
   public void cleanup(Player player) {
      BukkitRunnable task = this.charging.remove(player.getUniqueId());
      if (task != null) {
         task.cancel();
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.cleanup(event.getPlayer());
      this.plugin.getGoldGemManager().unload(event.getPlayer().getUniqueId());
   }
}
