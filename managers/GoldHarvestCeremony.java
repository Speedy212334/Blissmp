package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class GoldHarvestCeremony implements Listener {
   private static final String DISPLAY_TAG = "blissgems_ritual_display";
   private final BlissGems plugin;
   private final Map<UUID, Location> pinned = new HashMap<>();

   public GoldHarvestCeremony(BlissGems plugin) {
      this.plugin = plugin;
   }

   public boolean isInCeremony(Player player) {
      return this.pinned.containsKey(player.getUniqueId());
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onFatalBlow(EntityDamageByEntityEvent event) {
      if (this.plugin.getConfig().getBoolean("gold.harvest-ceremony.enabled", true)) {
         Entity entity = event.getEntity();
         Player victim;
         if (entity instanceof Player && !this.isInCeremony(victim = (Player)entity)) {
            Player killer = this.resolveKiller(event.getDamager());
            if (killer != null && !this.isInCeremony(killer)) {
               if (killer.getWorld().equals(victim.getWorld())) {
                  if (!(victim.getHealth() - event.getFinalDamage() > 0.0)) {
                     ItemStack mainHand = victim.getInventory().getItemInMainHand();
                     ItemStack offHand = victim.getInventory().getItemInOffHand();
                     boolean hasTotem = mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING
                        || offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING;
                     if (!hasTotem) {
                        GoldGemManager gold = this.plugin.getGoldGemManager();
                        if (gold != null && gold.canHarvest(killer, victim)) {
                           GoldGemManager.Harvest taken = gold.peekGem(victim);
                           if (taken != null) {
                              event.setCancelled(true);
                              this.begin(killer, victim, taken);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Player resolveKiller(Entity damager) {
      if (damager instanceof Player player) {
         return player;
      } else {
         Projectile projectile;
         ProjectileSource projectileSource;
         if (damager instanceof Projectile && (projectileSource = (projectile = (Projectile)damager).getShooter()) instanceof Player) {
            Player shooter = (Player)projectileSource;
            return shooter;
         } else {
            return null;
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onCeremonyDamage(EntityDamageEvent event) {
      Entity entity = event.getEntity();
      Player player;
      if (entity instanceof Player && this.isInCeremony(player = (Player)entity)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.pinned.remove(event.getPlayer().getUniqueId());
   }

   private void begin(final Player killer, final Player victim, GoldGemManager.Harvest taken) {
      final int duration = Math.max(20, this.plugin.getConfig().getInt("gold.harvest-ceremony.duration-ticks", 80));
      final double riseHeight = this.plugin.getConfig().getDouble("gold.harvest-ceremony.rise-height", 3.0);
      final boolean tint = this.plugin.getConfig().getBoolean("gold.harvest-ceremony.screen-tint", true);
      final Color colour = this.plugin.getGoldGemManager().soulColour(taken.gemId());
      Location a = victim.getLocation();
      Location b = killer.getLocation();
      final Location stage = new Location(victim.getWorld(), (a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0 + 1.2, (a.getZ() + b.getZ()) / 2.0);
      final ItemDisplay display = this.spawnGem(stage, taken);
      this.pin(killer);
      this.pin(victim);
      victim.getWorld().playSound(stage, Sound.BLOCK_BEACON_ACTIVATE, 1.6F, 0.5F);
      (new BukkitRunnable() {
            int ticks = 0;

            public void run() {
               boolean bothHere = killer.isOnline()
                  && victim.isOnline()
                  && GoldHarvestCeremony.this.isInCeremony(killer)
                  && GoldHarvestCeremony.this.isInCeremony(victim);
               if (bothHere && this.ticks < duration) {
                  double progress = (double)this.ticks / duration;
                  Location gemAt = stage.clone().add(0.0, riseHeight * progress, 0.0);
                  display.teleport(gemAt);
                  display.setTransformation(
                     new Transformation(
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        new AxisAngle4f((float)Math.toRadians(this.ticks * 8), 0.0F, 1.0F, 0.0F),
                        new Vector3f(1.0F, 1.0F, 1.0F),
                        new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
                     )
                  );
                  victim.getWorld().spawnParticle(Particle.DUST, gemAt, 12, 0.25, 0.25, 0.25, 0.0, new DustOptions(colour, 1.4F));
                  GoldHarvestCeremony.this.stare(killer, gemAt, this.ticks);
                  GoldHarvestCeremony.this.stare(victim, gemAt, this.ticks);
                  if (tint) {
                     GoldHarvestCeremony.this.tint(killer, colour, progress);
                     GoldHarvestCeremony.this.tint(victim, colour, progress);
                  }

                  if (this.ticks % 8 == 0) {
                     float pitch = 0.6F + (float)progress;
                     victim.getWorld().playSound(gemAt, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2F, pitch);
                  }

                  this.ticks++;
               } else {
                  this.cancel();
                  if (display.isValid()) {
                     display.remove();
                  }

                  GoldHarvestCeremony.this.release(killer);
                  GoldHarvestCeremony.this.release(victim);
                  if (bothHere) {
                     GoldHarvestCeremony.this.finish(killer, victim, colour);
                  }
               }
            }
         })
         .runTaskTimer(this.plugin, 1L, 1L);
   }

   private ItemDisplay spawnGem(Location at, GoldGemManager.Harvest taken) {
      ItemDisplay display = (ItemDisplay)at.getWorld().spawn(at, ItemDisplay.class);
      ItemStack icon = CustomItemManager.getItemById(taken.gemId() + "_gem_t" + taken.tier(), 10);
      if (icon != null) {
         display.setItemStack(icon);
      }

      display.setBillboard(Billboard.FIXED);
      display.setViewRange(100.0F);
      display.setGlowing(true);
      display.addScoreboardTag("blissgems_ritual_display");
      return display;
   }

   private void finish(Player killer, Player victim, Color colour) {
      GoldGemManager gold = this.plugin.getGoldGemManager();
      GoldGemManager.Harvest taken = gold.takeGem(victim, null);
      if (taken != null) {
         gold.absorb(killer, victim, taken);
      }

      victim.getWorld().spawnParticle(Particle.DUST, victim.getEyeLocation(), 60, 0.6, 0.8, 0.6, 0.0, new DustOptions(colour, 2.0F));
      victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2F, 1.4F);
      victim.setHealth(Math.max(1.0, Math.min(victim.getHealth(), 2.0)));
      victim.sendMessage(ChatColor.GOLD + "§l[GOLD GEM] " + ChatColor.YELLOW + "Your gem has been severed and absorbed into the Gold Gem!");
   }

   private void pin(Player player) {
      this.pinned.put(player.getUniqueId(), player.getLocation().clone());
      int padding = 200;
      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, padding, 255, false, false));
      player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, padding, 250, false, false));
   }

   private void release(Player player) {
      this.pinned.remove(player.getUniqueId());
      player.removePotionEffect(PotionEffectType.SLOWNESS);
      player.removePotionEffect(PotionEffectType.JUMP_BOOST);
   }

   private void stare(Player player, Location target, int ticks) {
      Location anchor = this.pinned.get(player.getUniqueId());
      if (anchor != null) {
         Vector toGem = target.toVector().subtract(player.getEyeLocation().toVector());
         if (!(toGem.lengthSquared() < 1.0E-4)) {
            Location look = anchor.clone().setDirection(toGem);
            look.setYaw(look.getYaw() + (float)(Math.sin(ticks / 7.0) * 7.0));
            look.setPitch(look.getPitch() + (float)(Math.sin(ticks / 11.0) * 4.0));
            player.teleport(look);
         }
      }
   }

   private void tint(Player player, Color colour, double progress) {
      int density = (int)(10.0 + 30.0 * progress);
      Location eye = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.4));
      player.spawnParticle(Particle.DUST, eye, density, 0.35, 0.35, 0.35, 0.0, new DustOptions(colour, 2.4F));
   }

   public void cleanup() {
      for (UUID id : new ArrayList<>(this.pinned.keySet())) {
         Player player = this.plugin.getServer().getPlayer(id);
         if (player != null) {
            this.release(player);
         } else {
            this.pinned.remove(id);
         }
      }
   }
}
