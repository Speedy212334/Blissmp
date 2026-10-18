package dev.xoperr.blissgems.pedestal;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class PedestalReviveRitual implements Listener {
   private static final Object[][] RING = new Object[][]{
      {GemType.PUFF, 0, 6},
      {GemType.WEALTH, -6, 0},
      {GemType.FLUX, 6, 0},
      {GemType.ASTRA, 0, -6},
      {GemType.STRENGTH, -4, 4},
      {GemType.LIFE, 4, 4},
      {GemType.SPEED, -4, -4},
      {GemType.FIRE, 4, -4}
   };
   private static final GemType[] CIRCLE = new GemType[]{
      GemType.STRENGTH, GemType.ASTRA, GemType.FLUX, GemType.WEALTH, GemType.PUFF, GemType.FIRE, GemType.SPEED, GemType.LIFE
   };
   private static World weatherWorld;
   private static boolean savedStorm;
   private static boolean savedThundering;
   private static int savedWeatherDuration;
   private static int savedThunderDuration;
   private final BlissGems plugin;
   private final PedestalManager manager;
   private final PedestalState state;
   private final Random random = new Random();
   private Map<GemType, ItemDisplay> activeGems;

   public PedestalReviveRitual(BlissGems plugin, PedestalManager manager) {
      this.plugin = plugin;
      this.manager = manager;
      this.state = manager.state();
   }

   @EventHandler(ignoreCancelled = true)
   public void onMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (!this.state.revivalLocked(player.getUniqueId())) {
         this.tryStart(player);
      } else {
         Location from = event.getFrom();
         Location to = event.getTo();
         if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            Location held = from.clone();
            held.setYaw(to.getYaw());
            held.setPitch(to.getPitch());
            event.setTo(held);
         }
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onLockedTeleport(PlayerTeleportEvent event) {
      if (this.state.revivalLocked(event.getPlayer().getUniqueId())) {
         TeleportCause cause = event.getCause();
         if (cause == TeleportCause.ENDER_PEARL || cause == TeleportCause.CHORUS_FRUIT) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player p = event.getPlayer();
      if (!this.state.revivalLocked(p.getUniqueId())) {
         if (p.isInvulnerable() && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
            p.setInvulnerable(false);
            this.plugin.getLogger().info("Pedestal: cleared a stuck invulnerable flag on " + p.getName());
         }

         if (!p.hasGravity()) {
            p.setGravity(true);
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onLockedDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player p && this.state.revivalLocked(p.getUniqueId())) {
         event.setCancelled(true);
      }
   }

   void tryStart(Player player) {
      if (!this.state.pedestalBusy()
         && this.state.ritual() == PedestalState.Ritual.REVIVE
         && this.state.count() >= 5
         && player.getUniqueId().equals(this.state.revivingPlayer())) {
         if (this.onCenter(player)) {
            this.state.setPedestalBusy(true);
            this.awaitStillness(player);
         }
      }
   }

   private boolean onCenter(Player player) {
      Location stand = this.state.standLoc();
      if (stand != null && player.getWorld().equals(stand.getWorld())) {
         Location at = player.getLocation();
         double dx = at.getX() - stand.getX();
         double dz = at.getZ() - stand.getZ();
         return dx * dx + dz * dz <= 0.5625 && Math.abs(at.getY() - stand.getY()) <= 1.0;
      } else {
         return false;
      }
   }

   private void awaitStillness(final Player player) {
      final UUID id = player.getUniqueId();
      (new BukkitRunnable() {
         Location last = player.getLocation().clone();
         int still;
         int waited;

         public void run() {
            PedestalReviveRitual self = PedestalReviveRitual.this;
            if (player.isOnline() && !player.isDead() && self.state.pedestalBusy() && self.onCenter(player) && ++this.waited <= 600) {
               Location at = player.getLocation();
               if (at.distanceSquared(this.last) > 0.0025) {
                  this.still = 0;
                  this.last = at.clone();
               } else if (++this.still >= 60) {
                  this.cancel();
                  self.engageLock(player, id);
               }
            } else {
               this.cancel();
               self.state.setPedestalBusy(false);
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private void engageLock(Player player, UUID id) {
      this.state.setRevivalLocked(id, true);
      this.beginAnimation(player, this.state.mainLoc().add(0.5, -3.0, 0.5));
   }

   private void beginAnimation(Player player, Location focus) {
      if (player.isOnline() && !player.isDead()) {
         player.setGravity(false);
         Map<GemType, ItemDisplay> gems = this.spawnGemRing(focus);
         this.activeGems = gems;
         int levelTicks = this.smoothLevelHead(player);
         long delay = (levelTicks > 0 ? levelTicks + 8L : 1L) + 50L;
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) {
               removeGems(gems);
               this.cleanupAbort(player.getUniqueId(), true);
            } else {
               this.raiseGemsToChest(player, gems, focus);
            }
         }, delay);
      } else {
         this.cleanupAbort(player.getUniqueId(), true);
      }
   }

   private int smoothLevelHead(final Player player) {
      final float yaw = player.getLocation().getYaw();
      final float pitch = player.getLocation().getPitch();
      float target = (float)(Math.ceil(yaw / 90.0 - 0.5) * 90.0);
      final float delta = Location.normalizeYaw(target - yaw);
      if (Math.abs(delta) < 1.0F && Math.abs(pitch) < 1.0F) {
         return 0;
      }

      (new BukkitRunnable() {
         int t;

         public void run() {
            if (!player.isOnline()) {
               this.cancel();
            } else {
               double e = PedestalReviveRitual.ease(Math.min(1.0, ++this.t / 40.0));
               player.setRotation(yaw + (float)(delta * e), (float)(pitch * (1.0 - e)));
               if (this.t >= 40) {
                  this.cancel();
               }
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
      return 40;
   }

   private static double ease(double x) {
      return x < 0.5 ? 2.0 * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 2.0) / 2.0;
   }

   private Map<GemType, ItemDisplay> spawnGemRing(Location focus) {
      Map<GemType, ItemDisplay> gems = new EnumMap<>(GemType.class);

      for (Object[] g : RING) {
         GemType type = (GemType)g[0];
         ItemDisplay display = this.spawnGemDisplay(focus.clone().add(((Integer)g[1]).intValue(), 1.5, ((Integer)g[2]).intValue()), type);
         if (display != null) {
            gems.put(type, display);
         }
      }

      return gems;
   }

   private ItemDisplay spawnGemDisplay(Location at, GemType type) {
      ItemStack item = CustomItemManager.getItemById(GemType.buildOraxenId(type, 1));
      if (item == null) {
         return null;
      }

      Color glow = this.plugin.getGemRitualManager() != null ? this.plugin.getGemRitualManager().getGemColor(type.getId()) : Color.WHITE;
      return (ItemDisplay)at.getWorld()
         .spawn(
            at,
            ItemDisplay.class,
            d -> {
               d.setPersistent(false);
               d.setItemStack(item);
               d.setItemDisplayTransform(ItemDisplayTransform.HEAD);
               d.setTransformation(
                  new Transformation(
                     new Vector3f(), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(0.65F, 0.65F, 0.65F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)
                  )
               );
               d.setGlowing(true);
               d.setGlowColorOverride(glow);
               d.setTeleportDuration(1);
            }
         );
   }

   private void raiseGemsToChest(final Player player, final Map<GemType, ItemDisplay> gems, final Location focus) {
      final double startY = focus.getY() + 1.5;
      final double endY = player.getLocation().getY() + 0.3;
      (new BukkitRunnable() {
         int t;

         public void run() {
            if (!player.isOnline()) {
               this.cancel();
               PedestalReviveRitual.removeGems(gems);
               PedestalReviveRitual.this.cleanupAbort(player.getUniqueId(), true);
            } else {
               double e = PedestalReviveRitual.ease(Math.min(1.0, ++this.t / 60.0));
               PedestalReviveRitual.setGemRingY(gems, startY + (endY - startY) * e);
               if (this.t >= 60) {
                  this.cancel();
                  PedestalReviveRitual.this.new RitualTask(player, focus, gems).runTaskTimer(PedestalReviveRitual.this.plugin, 1L, 1L);
               }
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private static void setGemRingY(Map<GemType, ItemDisplay> gems, double y) {
      for (ItemDisplay d : gems.values()) {
         if (d != null && !d.isDead()) {
            Location l = d.getLocation();
            l.setY(y);
            l.setYaw(0.0F);
            l.setPitch(0.0F);
            d.teleport(l);
         }
      }
   }

   private static void removeGems(Map<GemType, ItemDisplay> gems) {
      if (gems != null) {
         for (ItemDisplay d : gems.values()) {
            if (d != null && !d.isDead()) {
               d.remove();
            }
         }

         gems.clear();
      }
   }

   private void pedestalLightning(Location center) {
      if (center != null) {
         for (int i = 0; i < 3; i++) {
            int a = 7 + this.random.nextInt(9);
            int b = 7 + this.random.nextInt(9);
            int c = 7 + this.random.nextInt(9);
            int d = 7 + this.random.nextInt(9);
            Location[] spots = new Location[]{
               center.clone().add(a, 0.0, -b), center.clone().add(-c, 0.0, d), center.clone().add(c, 0.0, b), center.clone().add(-a, 0.0, -d)
            };
            center.getWorld().strikeLightning(spots[this.random.nextInt(4)]);
         }
      }
   }

   private List<String> rollPool() {
      List<String> pool = new ArrayList<>(this.plugin.getGemManager().getAvailableGemIds());
      if (pool.isEmpty()) {
         for (GemType t : GemType.values()) {
            pool.add(t.getId());
         }
      }

      return pool;
   }

   private void rollRandomGem(Player player) {
      String current = this.plugin.getGemManager().getGemId(player);
      List<String> pool = this.rollPool();
      if (pool.size() > 1 && current != null) {
         pool.remove(current);
      }

      String next = pool.get(this.random.nextInt(pool.size()));
      if (this.plugin.getGemManager().findGemInInventory(player) != null) {
         this.plugin.getGemManager().replaceGem(player, next);
      } else {
         this.plugin.getGemManager().giveGem(player, next, 1);
      }

      player.sendMessage(
         PedestalManager.color(
            "\ud83d\udd2e &bYou have traded your current gem to "
               + this.plugin.getGemManager().getGemColorCode(next)
               + this.plugin.getGemManager().getGemDisplayName(next)
         )
      );
      player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0F, 1.0F);
      GemType type = GemType.fromOraxenId(next + "_gem_t1");
      if (type != null) {
         PedestalManager.applyGemGlow(player, type, 200);
      }
   }

   public static void forceStormAndThunder(World world) {
      if (world != null && weatherWorld == null) {
         weatherWorld = world;
         savedStorm = world.hasStorm();
         savedThundering = world.isThundering();
         savedWeatherDuration = world.getWeatherDuration();
         savedThunderDuration = world.getThunderDuration();
         world.setStorm(true);
         world.setThundering(true);
         world.setWeatherDuration(20000);
         world.setThunderDuration(20000);
      }
   }

   public static void restoreWeather() {
      if (weatherWorld != null) {
         try {
            weatherWorld.setStorm(savedStorm);
            weatherWorld.setThundering(savedThundering);
            weatherWorld.setWeatherDuration(Math.max(savedWeatherDuration, 0));
            weatherWorld.setThunderDuration(Math.max(savedThunderDuration, 0));
         } catch (Throwable var1) {
         }

         weatherWorld = null;
      }
   }

   public void shutdownRestore() {
      UUID id = this.state.revivingPlayer();
      if (id != null && this.state.pedestalBusy()) {
         this.cleanupAbort(id, false);
      }
   }

   private void cleanupAbort(UUID id, boolean resetRitual) {
      removeGems(this.activeGems);
      this.activeGems = null;
      this.state.setRevivalLocked(id, false);
      this.state.setPedestalBusy(false);
      restoreWeather();
      Player player = Bukkit.getPlayer(id);
      if (player != null) {
         player.setGravity(true);
         PedestalManager.clearGemGlow(player);
      }

      this.state.removeBeam();
      if (resetRitual) {
         this.state.resetRitual();
      }
   }

   private final class RitualTask extends BukkitRunnable {
      private final Player player;
      private final Location focus;
      private final Map<GemType, ItemDisplay> gems;
      private int phase;
      private int liftTick;
      private int holdTick;
      private int spinTick;
      private double angle;
      private int nextRollTick = 285;
      private int energy;

      RitualTask(Player player, Location focus, Map<GemType, ItemDisplay> gems) {
         this.player = player;
         this.focus = focus.clone();
         this.gems = gems;
         this.energy = PedestalReviveRitual.this.plugin.getEnergyManager().getEnergy(player);
      }

      public void run() {
         if (this.player.isOnline() && !this.player.isDead() && PedestalReviveRitual.this.state.ritual() == PedestalState.Ritual.REVIVE) {
            switch (this.phase) {
               case 0:
                  this.lift();
                  break;
               case 1:
                  this.hold();
                  break;
               case 2:
                  this.spin();
                  break;
               default:
                  this.finish();
            }
         } else {
            this.abort();
         }
      }

      private void lift() {
         this.player.teleport(this.player.getLocation().add(0.0, 0.02, 0.0));
         PedestalReviveRitual.setGemRingY(this.gems, this.player.getLocation().getY() + 0.3);
         this.liftTick++;
         if (this.liftTick % 40 == 0 && this.liftTick <= 160) {
            PedestalReviveRitual.this.pedestalLightning(PedestalReviveRitual.this.state.mainLoc());
         }

         if (this.liftTick >= 200) {
            this.phase = 1;
         }
      }

      private void hold() {
         PedestalReviveRitual.setGemRingY(this.gems, this.player.getLocation().getY() + 0.3);
         if (++this.holdTick >= 30) {
            this.phase = 2;
         }
      }

      private void spin() {
         double radius = 6.0;
         double speed = 1.0;
         if (this.spinTick >= 229) {
            double k = Math.min((this.spinTick - 229) / 150.0, 1.0);
            radius = 6.0 - 5.5 * k;
            speed = 1.0 + 11.0 * k;
         }

         this.angle += speed;
         Location main = PedestalReviveRitual.this.state.mainLoc();
         Location center = new Location(this.player.getWorld(), main.getX() + 0.5, this.player.getLocation().getY() + 0.3, main.getZ() + 0.5);

         for (int i = 0; i < PedestalReviveRitual.CIRCLE.length; i++) {
            tpGem(this.gems.get(PedestalReviveRitual.CIRCLE[i]), center, this.angle + i * 45.0, radius);
         }

         if (this.spinTick > 0 && this.spinTick % 50 == 0) {
            PedestalReviveRitual.this.pedestalLightning(main);
         }

         int next = this.spinTick + 1;
         if (next == 230 || next == 240 || next == 250 || next == 260 || next == 270) {
            this.energy = Math.min(EnergyState.PRISTINE.getMinEnergy(), this.energy + 1);
            PedestalReviveRitual.this.plugin.getEnergyManager().setEnergy(this.player, this.energy);
         }

         if (this.spinTick == this.nextRollTick && this.spinTick < 379) {
            PedestalReviveRitual.this.rollRandomGem(this.player);
            double left = Math.max(0.0, Math.min(1.0, (379 - this.spinTick) / 94.0));
            this.nextRollTick = this.spinTick + Math.max(2, (int)Math.round(2.0 + 12.0 * left));
         }

         if (++this.spinTick >= 380) {
            this.phase = 3;
         }
      }

      private static void tpGem(ItemDisplay d, Location center, double deg, double radius) {
         if (d != null && !d.isDead()) {
            double r = Math.toRadians(deg);
            Location l = center.clone().add(-radius * Math.sin(r), 0.0, radius * Math.cos(r));
            l.setYaw(0.0F);
            l.setPitch(0.0F);
            d.teleport(l);
         }
      }

      private void finish() {
         this.cancel();
         PedestalReviveRitual self = PedestalReviveRitual.this;
         UUID id = this.player.getUniqueId();
         PedestalReviveRitual.removeGems(this.gems);
         self.activeGems = null;
         String gemId = self.plugin.getGemManager().getGemId(this.player);
         Color color = gemId != null && self.plugin.getGemRitualManager() != null ? self.plugin.getGemRitualManager().getGemColor(gemId) : Color.WHITE;
         Location burst = this.player.getLocation().add(0.0, 1.0, 0.0);
         burst.getWorld().spawnParticle(Particle.DUST, burst, 350, 1.5, 1.5, 1.5, 0.0, new DustOptions(color, 3.0F), true);
         PedestalManager.clearGemGlow(this.player);
         PedestalReviveRitual.restoreWeather();
         self.state.setRevivalLocked(id, false);
         self.state.setPedestalBusy(false);
         self.state.setRevivingPlayer(null);
         self.state.resetCount();
         this.player.setGravity(true);
         Location stand = self.state.standLoc();
         if (stand != null) {
            for (Entity e : stand.getWorld().getNearbyEntities(stand, 10.0, 10.0, 10.0)) {
               if (!e.getUniqueId().equals(id)) {
                  Vector push = e.getLocation().toVector().subtract(stand.toVector());
                  if (push.lengthSquared() < 1.0E-4) {
                     push = new Vector(0, 0, 1);
                  }

                  e.setVelocity(push.normalize().multiply(1.1).setY(0.75));
               }
            }
         }

         String done = PedestalManager.color("&a" + this.player.getName() + " has succesfully completed their Restoration ritual!");

         for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(done);
         }

         self.manager.unlock(this.player, Achievement.REAWAKENING);
         Location at = this.player.getLocation();
         this.player.playSound(at, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0F, 1.0F);
         this.player.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 1.0F);
         this.player.playSound(at, Sound.BLOCK_BELL_RESONATE, 2.0F, 1.0F);
         this.player.playSound(at, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 1.0F);
         this.player.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
         this.player.playSound(at, Sound.ENTITY_WITHER_SPAWN, 0.5F, 0.5F);
         this.player.playSound(at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.5F);
         self.state.setRitual(PedestalState.Ritual.NONE);
         self.state.setActive(false);
         self.state.removeBeam();
         self.state.unbuildStructure();
         self.state.save();
      }

      private void abort() {
         this.cancel();
         PedestalReviveRitual.removeGems(this.gems);
         PedestalReviveRitual.this.cleanupAbort(this.player.getUniqueId(), true);
      }
   }
}
