package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public final class GoldenDreamRitual implements Listener {
   private static final int NEEDED = 7;
   private static final double PILE_R = 2.5;
   private static final double SCAN_R = 6.0;
   private static final long SETTLE_TICKS = 12L;
   private static final int SPIRAL_TICKS = 200;
   private static final int GLITCH_TICKS = 40;
   private static final double BURST_SCALE = 3.0;
   private static final double SPIRAL_SPEED = 0.1;
   private static final double BRAID_R = 4.0;
   private static final double BRAID_AMP = 0.7;
   private static final int BRAID_WAVES = 4;
   private static final double BRAID_STEP = 0.09;
   private static final Particle[] SHOWER_MIX = new Particle[]{Particle.SOUL, Particle.END_ROD, Particle.SCULK_CHARGE};
   private static final String SHOWER_SOUND = "bliss:goldgem_dream_shower";
   private static final double SOUND_RANGE_SQ = 4096.0;
   private static final int[] CLIP_BEATS = new int[]{6, 20, 37, 58, 79, 82, 98, 137, 158, 178};
   private static final boolean[] CLIP_BIG = new boolean[]{true, true, true, false, false, false, true, true, true, false};
   private final BlissGems plugin;
   private final GoldenDream dream;
   private final FragmentCoreRitual fx;
   private final Set<UUID> running = new HashSet<>();
   private final Set<GoldenDreamMass> masses = new HashSet<>();
   private final Map<UUID, Runnable> entryAborts = new HashMap<>();

   GoldenDreamRitual(BlissGems plugin, GoldenDream dream, FragmentCoreRitual fx) {
      this.plugin = plugin;
      this.dream = dream;
      this.fx = fx;
   }

   GoldenDreamWorld world() {
      return this.dream.world();
   }

   @EventHandler(ignoreCancelled = true)
   public void onDrop(PlayerDropItemEvent event) {
      if (RitualItems.isWireFragment(event.getItemDrop().getItemStack())) {
         Player p = event.getPlayer();
         Item dropped = event.getItemDrop();
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.judgePile(p, dropped.getLocation()), 12L);
      }
   }

   private void judgePile(Player p, Location at) {
      if (p != null && at != null && at.getWorld() != null && !this.running.contains(p.getUniqueId())) {
         if (!GoldenDreamWorld.isDreamWorld(at.getWorld())) {
            List<Item> pile = this.collect(p, at);
            if (pile.size() >= 7) {
               if (!this.dream.world().memoryAvailable()) {
                  p.sendMessage(PedestalManager.color("&6The fragments stay silent — the dream has no memory to show yet."));
                  if (p.hasPermission("blissgems.admin")) {
                     p.sendMessage(
                        PedestalManager.color(
                           "&7Put the map's world folder at &f/goldendream_import &7(next to the server jar), or set &fgolden-dream.memory-world: generate&7."
                        )
                     );
                  }
               } else {
                  Location c = centre(pile);

                  for (Item i : pile) {
                     if (i.getLocation().distanceSquared(c) > 6.25) {
                        return;
                     }
                  }

                  this.begin(p, c, pile);
               }
            }
         }
      }
   }

   private List<Item> collect(Player p, Location at) {
      Map<Integer, Item> byNumber = new HashMap<>();

      for (Entity e : at.getWorld().getNearbyEntities(at, 6.0, 6.0, 6.0)) {
         if (e instanceof Item item && !item.isDead() && p.getUniqueId().equals(item.getThrower()) && RitualItems.isWireFragment(item.getItemStack())) {
            int n = RitualItems.wireFragmentNumber(item.getItemStack());
            if (n >= 1 && n <= 7) {
               byNumber.putIfAbsent(n, item);
            }
         }
      }

      return byNumber.size() == 7 ? new ArrayList<>(byNumber.values()) : List.of();
   }

   private static Location centre(List<Item> items) {
      double x = 0.0;
      double y = 0.0;
      double z = 0.0;

      for (Item i : items) {
         Location l = i.getLocation();
         x += l.getX();
         y += l.getY();
         z += l.getZ();
      }

      int n = items.size();
      return new Location(items.get(0).getWorld(), x / n, y / n, z / n);
   }

   private void begin(Player player, final Location centre, List<Item> pile) {
      final UUID id = player.getUniqueId();
      this.running.add(id);
      final List<ItemStack> frags = new ArrayList<>();
      pile.stream().sorted(Comparator.comparingInt(i -> RitualItems.wireFragmentNumber(i.getItemStack()))).forEach(i -> {
         ItemStack one = i.getItemStack().clone();
         one.setAmount(1);
         frags.add(one);
      });

      for (Item i : pile) {
         if (!i.isDead()) {
            i.remove();
         }
      }

      final World w = centre.getWorld();
      w.playSound(centre, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 2.0F, 0.7F);
      this.plugin
         .getLogger()
         .info(
            "Golden Dream: entry ritual triggered by "
               + player.getName()
               + " at "
               + w.getName()
               + " "
               + Math.round(centre.getX())
               + ","
               + Math.round(centre.getY())
               + ","
               + Math.round(centre.getZ())
         );
      final Location low = centre.clone().add(0.0, 1.0, 0.0);
      final Location high = centre.clone().add(0.0, 2.0, 0.0);
      final long prevOffset = player.getPlayerTimeOffset();
      final boolean prevRel = player.isPlayerTimeRelative();
      this.entryAborts.put(id, () -> {
         for (ItemStack f : frags) {
            w.dropItem(centre, f);
         }

         Player p = Bukkit.getPlayer(id);
         if (p != null && p.isOnline()) {
            DreamSky.restore(p, prevOffset, prevRel);
         }
      });
      (new BukkitRunnable() {
         final List<ItemDisplay> shown = new ArrayList<>();
         int t;
         int timer;
         int beat;
         int mix;

         public void run() {
            GoldenDreamRitual self = GoldenDreamRitual.this;
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && !p.isDead()) {
               if (this.t >= 200) {
                  if (this.t == 200) {
                     this.stopShower();
                  }

                  if (this.cover(p, this.t - 200)) {
                     this.release(p, false);
                     this.cancel();
                     new GoldenDreamMass(self.plugin, self, high.clone(), id, prevOffset, prevRel).schedule(200L);
                  }

                  this.t++;
               } else {
                  if (this.t < 40) {
                     if (ThreadLocalRandom.current().nextDouble() < 0.35 + 0.6 * this.t / 40.0) {
                        DreamSky.glitch(p);
                     } else {
                        p.setPlayerTime(prevOffset, prevRel);
                     }
                  } else {
                     DreamSky.domeFade(p, (this.t - 40) / 160.0);
                  }

                  if (this.t == 0) {
                     GoldenDreamRitual.explode(w, low);
                  }

                  if (this.t % 2 == 0) {
                     GoldenDreamRitual.drawBraid(w, high);
                  }

                  if (self.fx != null && --this.timer <= 0) {
                     this.timer = 4;
                     self.fx.flash(w, low, 1, 0.0, 0.0, 0.0);
                     this.beat++;
                     int rings = this.beat % 2 == 0 ? 1 : 2;

                     for (int i = 0; i < rings; i++) {
                        Particle part = GoldenDreamRitual.SHOWER_MIX[++this.mix % GoldenDreamRitual.SHOWER_MIX.length];
                        double f = 1.0 + Math.pow(ThreadLocalRandom.current().nextDouble(), 2.0) * 1.2;
                        int base = part == Particle.SCULK_CHARGE ? 45 : 30;
                        self.fx.angledBurst(w, low, 45.0, (int)(base * (0.6 + 0.4 * f)), 0.55 * f, part);
                     }
                  }

                  if (this.t == 0) {
                     for (Player near : w.getPlayers()) {
                        if (near.getLocation().distanceSquared(low) < 4096.0) {
                           near.playSound(low, "bliss:goldgem_dream_shower", SoundCategory.MASTER, 1.4F, 1.0F);
                        }
                     }

                     if (self.fx != null) {
                        for (ItemStack f : frags) {
                           this.shown.add(self.fx.spawnDisplay(high, f, 0.7F));
                        }
                     }
                  }

                  for (int b = 0; b < GoldenDreamRitual.CLIP_BEATS.length; b++) {
                     if (GoldenDreamRitual.CLIP_BEATS[b] == this.t) {
                        boolean big = GoldenDreamRitual.CLIP_BIG[b];
                        double speed = (big ? 2.4 : 1.44) * 3.0;
                        int points = (int)((big ? 40 : 28) * 1.5);
                        w.spawnParticle(Particle.END_ROD, low, (int)((big ? 26 : 14) * 3.0), 1.2000000000000002, 1.2000000000000002, 1.2000000000000002, 0.18);

                        for (int i = 0; i < points; i++) {
                           double a = (Math.PI * 2) * i / points;
                           w.spawnParticle(Particle.POOF, low.getX(), low.getY(), low.getZ(), 0, Math.cos(a), 0.0, Math.sin(a), speed, null, true);
                        }
                        break;
                     }
                  }

                  double r = 4.0 * (1.0 - this.t / 200.0);

                  for (int i = 0; i < this.shown.size(); i++) {
                     ItemDisplay d = this.shown.get(i);
                     if (d.isValid()) {
                        double a = i * ((Math.PI * 2) / this.shown.size()) + this.t * 0.1;
                        d.teleport(new Location(w, high.getX() + Math.cos(a) * r, high.getY(), high.getZ() + Math.sin(a) * r));
                     }
                  }

                  this.t++;
               }
            } else {
               this.stopShower();

               for (ItemStack f : frags) {
                  w.dropItem(centre, f);
               }

               this.release(p, true);
               this.cancel();
            }
         }

         private void stopShower() {
            for (Player near : w.getPlayers()) {
               if (near.getLocation().distanceSquared(low) < 4096.0) {
                  near.stopSound("bliss:goldgem_dream_shower", SoundCategory.MASTER);
               }
            }
         }

         private boolean cover(Player p, int k) {
            if (k == 0) {
               for (ItemDisplay d : this.shown) {
                  if (d.isValid()) {
                     d.remove();
                  }
               }

               this.shown.clear();
               DreamSky.closeEyes(p, 32);
               DreamSky.domeFade(p, 1.0);
            }

            return k >= 32;
         }

         private void release(Player p, boolean aborted) {
            if (aborted) {
               GoldenDreamRitual.this.entryAborts.remove(id);
            }

            for (ItemDisplay d : this.shown) {
               if (d.isValid()) {
                  d.remove();
               }
            }

            this.shown.clear();
            if (aborted && p != null && p.isOnline()) {
               DreamSky.restore(p, prevOffset, prevRel);
            }

            if (aborted) {
               GoldenDreamRitual.this.finish(id);
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private static void explode(World w, Location at) {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      w.spawnParticle(Particle.EXPLOSION_EMITTER, at, 3, 1.5, 1.0, 1.5, 0.0, null, true);

      for (int i = 0; i < 8; i++) {
         w.spawnParticle(
            Particle.EXPLOSION_EMITTER,
            at.clone().add(r.nextDouble(-4.5, 4.5), r.nextDouble(-0.5, 3.0), r.nextDouble(-4.5, 4.5)),
            1,
            0.0,
            0.0,
            0.0,
            0.0,
            null,
            true
         );
      }

      if (Particle.FLASH.getDataType() == Color.class) {
         w.spawnParticle(Particle.FLASH, at, 6, 1.0, 1.0, 1.0, 0.0, Color.WHITE, true);
      } else {
         w.spawnParticle(Particle.FLASH, at, 6, 1.0, 1.0, 1.0, 0.0, null, true);
      }

      DustOptions gold = new DustOptions(GoldenDreamMass.HOLDER_GOLD, 2.0F);

      for (int i = 0; i < 120; i++) {
         double a = (Math.PI * 2) * i / 120.0;
         w.spawnParticle(Particle.POOF, at.getX(), at.getY(), at.getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 7.199999999999999, null, true);
         w.spawnParticle(Particle.DUST, at.getX() + Math.cos(a) * 9.0, at.getY(), at.getZ() + Math.sin(a) * 9.0, 2, 0.1, 0.1, 0.1, 0.0, gold, true);
      }

      w.spawnParticle(Particle.END_ROD, at, 160, 1.2, 1.2, 1.2, 0.6, null, true);
      w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 4.0F, 0.55F);
      w.playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 3.0F, 0.7F);
   }

   private static void drawBraid(World w, Location c) {
      int steps = (int)Math.ceil((Math.PI * 2) * Math.sqrt(23.84) / 0.09);
      DustOptions dust = new DustOptions(GoldenDreamMass.HOLDER_GOLD, 0.5F);

      for (int strand = 0; strand < 2; strand++) {
         for (int i = 0; i < steps; i++) {
            double a = i * ((Math.PI * 2) / steps);
            w.spawnParticle(
               Particle.DUST,
               c.getX() + Math.cos(a) * 4.0,
               c.getY() + 0.7 * Math.sin(4.0 * a + strand * Math.PI),
               c.getZ() + Math.sin(a) * 4.0,
               1,
               0.0,
               0.0,
               0.0,
               0.0,
               dust,
               true
            );
         }
      }
   }

   void finish(UUID id) {
      if (id != null) {
         this.running.remove(id);
      }
   }

   void register(GoldenDreamMass m) {
      this.masses.add(m);
   }

   void unregister(GoldenDreamMass m) {
      this.masses.remove(m);
   }

   void entryStarted(UUID id) {
      this.entryAborts.remove(id);
   }

   public boolean isRunning(Player p) {
      return p != null && this.running.contains(p.getUniqueId());
   }

   public void abortAll() {
      for (Runnable r : new ArrayList<>(this.entryAborts.values())) {
         try {
            r.run();
         } catch (Throwable var5) {
         }
      }

      this.entryAborts.clear();

      for (GoldenDreamMass m : new ArrayList<>(this.masses)) {
         try {
            m.abortNow();
         } catch (Throwable var4) {
         }
      }

      this.masses.clear();
      this.running.clear();
   }
}
