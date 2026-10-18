package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.GemType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class SoulManager {
   private final BlissGems plugin;
   private final NamespacedKey capturedMobKey;
   private final Map<UUID, List<SoulManager.CapturedMob>> playerSouls = new HashMap<>();
   private static final int MAX_CAPTURED_SOULS = 2;
   private static final String SOUL_MODIFIER_PREFIX = "soul_absorb_";
   private int soulModifierCounter = 0;
   private final Map<UUID, List<UUID>> activeSoulModifiers = new HashMap<>();
   private final Map<UUID, Long> lastCaptureTimes = new ConcurrentHashMap<>();
   private final Map<UUID, Double> soulBonus = new HashMap<>();
   private final Map<UUID, AttributeModifier> soulModifier = new HashMap<>();
   private final Map<UUID, BukkitTask> soulExpiry = new HashMap<>();

   public long getLastCaptureTime(UUID uuid) {
      return this.lastCaptureTimes.getOrDefault(uuid, 0L);
   }

   public SoulManager(BlissGems plugin) {
      this.plugin = plugin;
      this.capturedMobKey = new NamespacedKey(plugin, "captured_mob");
   }

   private int astraTier(Player player) {
      return Math.max(1, this.plugin.getGemManager().getTierFor(player, GemType.ASTRA));
   }

   public void absorbSoul(Player player, Entity killedEntity) {
      boolean isPlayer = killedEntity instanceof Player;
      double gain = isPlayer
         ? this.plugin.getConfig().getDouble("abilities.soul-absorption.player-kill-hearts", 6.0)
         : this.plugin.getConfig().getDouble("abilities.soul-absorption.mob-kill-hearts", 2.0);
      double cap = this.plugin.getConfig().getDouble("passives.astra.tier" + this.astraTier(player) + ".soul-max-bonus", 8.0);
      int durationSeconds = this.plugin.getConfig().getInt("abilities.soul-absorption.duration", 60);
      AttributeInstance maxHealthAttr = player.getAttribute(Attributes.maxHealth());
      if (maxHealthAttr != null && !(gain <= 0.0) && !(cap <= 0.0)) {
         UUID id = player.getUniqueId();
         double before = this.soulBonus.getOrDefault(id, 0.0);
         double total = Math.min(cap, before + gain);
         if (total > before) {
            AttributeModifier old = this.soulModifier.remove(id);
            if (old != null) {
               maxHealthAttr.removeModifier(old);
               this.unpersistModifierUuid(id, old.getUniqueId());
            }

            UUID modifierUuid = UUID.randomUUID();
            AttributeModifier modifier = new AttributeModifier(
               modifierUuid, "soul-absorb-" + modifierUuid.toString().replace("-", ""), total, Operation.ADD_NUMBER
            );
            maxHealthAttr.addModifier(modifier);
            this.soulModifier.put(id, modifier);
            this.soulBonus.put(id, total);
            this.persistModifierUuid(id, modifierUuid);
            player.setHealth(Math.min(maxHealthAttr.getValue(), player.getHealth() + (total - before)));
         }

         BukkitTask prev = this.soulExpiry.remove(id);
         if (prev != null) {
            prev.cancel();
         }

         this.soulExpiry.put(id, this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.fadeSoulBonus(player), durationSeconds * 20L));
         player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0.0, 1.0, 0.0), 6, 0.5, 0.5, 0.5, 0.05);
         player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0.0, 1.0, 0.0), 4, 0.5, 0.5, 0.5, 0.0);
         player.playSound(player.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, 1.0F, 1.5F);
         player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, false, true, true));
         player.sendActionBar(PedestalManager.color("&5✦ &dSoul absorbed &7(" + fmtHearts(total) + "/" + fmtHearts(cap) + "❤ for " + durationSeconds + "s)"));
      }
   }

   private static String fmtHearts(double hp) {
      double h = hp / 2.0;
      return h == Math.floor(h) ? String.valueOf((int)h) : String.valueOf(h);
   }

   private void fadeSoulBonus(Player player) {
      UUID id = player.getUniqueId();
      this.soulExpiry.remove(id);
      this.soulBonus.remove(id);
      AttributeModifier modifier = this.soulModifier.remove(id);
      if (modifier != null) {
         this.unpersistModifierUuid(id, modifier.getUniqueId());
         AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
         if (player.isOnline() && attr != null) {
            attr.removeModifier(modifier);
            if (player.getHealth() > attr.getValue()) {
               player.setHealth(attr.getValue());
            }
         }
      }
   }

   public boolean captureMob(Player player, LivingEntity mob) {
      if (mob instanceof Player) {
         player.sendMessage("§c§oCannot capture players!");
         return false;
      } else {
         List<SoulManager.CapturedMob> souls = this.playerSouls.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
         if (souls.size() >= 2) {
            player.sendMessage("§c§oYour gem is full! (Max 2 souls)");
            return false;
         } else if (!this.isCapturable(mob)) {
            player.sendMessage("§c§oThis entity cannot be captured!");
            return false;
         } else {
            AttributeInstance mobMax = mob.getAttribute(Attributes.maxHealth());
            double max = mobMax != null ? mobMax.getValue() : mob.getHealth();
            double limit = this.plugin.getConfig().getDouble("passives.astra.tier" + this.astraTier(player) + ".capture-max-health", 40.0);
            if (max > limit) {
               player.sendMessage("§5\ud83d\udd2e §cThe " + new SoulManager.CapturedMob(mob.getType(), null).getDisplayName() + " is too great to bind.");
               return false;
            } else {
               double fraction = this.plugin.getConfig().getDouble("passives.astra.capture-health-fraction", 0.5);
               if (mob.getHealth() > max * fraction) {
                  player.sendMessage("§5\ud83d\udd2e §7Weaken it first §8(below " + (int)Math.round(fraction * 100.0) + "% health)");
                  return false;
               } else {
                  SoulManager.CapturedMob capturedMob = new SoulManager.CapturedMob(mob.getType(), mob.getCustomName());
                  souls.add(capturedMob);
                  mob.remove();
                  Location mobLoc = mob.getLocation();
                  mobLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, mobLoc.add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.1);
                  mobLoc.getWorld().spawnParticle(Particle.WITCH, mobLoc, 30, 0.5, 0.5, 0.5, 0.0);
                  player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0F, 2.0F);
                  String mobName = capturedMob.getDisplayName();
                  player.sendMessage("§5\ud83d\udd2e §dCaptured " + mobName + " soul. §7(" + souls.size() + "/2)");
                  this.lastCaptureTimes.put(player.getUniqueId(), System.currentTimeMillis());
                  return true;
               }
            }
         }
      }
   }

   public SoulManager.CapturedMob consumeSoul(Player player) {
      List<SoulManager.CapturedMob> souls = this.playerSouls.get(player.getUniqueId());
      return souls != null && !souls.isEmpty() ? souls.remove(souls.size() - 1) : null;
   }

   public void releaseAllSouls(Player player) {
      List<SoulManager.CapturedMob> souls = this.playerSouls.get(player.getUniqueId());
      if (souls != null && !souls.isEmpty()) {
         Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
         spawnLoc.setY(player.getLocation().getY());
         Collections.reverse(souls);
         int released = 0;

         for (SoulManager.CapturedMob capturedMob : souls) {
            Entity spawned = player.getWorld().spawnEntity(spawnLoc, capturedMob.getType());
            if (capturedMob.getCustomName() != null && spawned instanceof LivingEntity) {
               spawned.setCustomName(capturedMob.getCustomName());
               spawned.setCustomNameVisible(true);
            }

            spawnLoc.getWorld().spawnParticle(Particle.PORTAL, spawnLoc.add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.1);
            released++;
         }

         souls.clear();
         player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0F, 0.8F);
         player.sendMessage("§d§lReleased " + released + " captured soul(s)!");
      } else {
         player.sendMessage("§c§oNo souls captured!");
      }
   }

   public List<SoulManager.CapturedMob> getCapturedSouls(Player player) {
      return this.playerSouls.getOrDefault(player.getUniqueId(), new ArrayList<>());
   }

   public int getCapturedCount(Player player) {
      return this.getCapturedSouls(player).size();
   }

   private boolean isCapturable(LivingEntity mob) {
      EntityType type = mob.getType();
      return type != EntityType.ENDER_DRAGON
         && type != EntityType.WITHER
         && type != EntityType.WARDEN
         && type != EntityType.PLAYER
         && type != EntityType.ARMOR_STAND
         && !(mob instanceof NPC);
   }

   public void clearSouls(UUID playerId) {
      this.playerSouls.remove(playerId);
   }

   public void cleanup(Player player) {
      AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
      if (attr != null) {
         Set<UUID> persistedUuids = this.loadPersistedModifierUuids(player.getUniqueId());
         ArrayList<AttributeModifier> toRemove = new ArrayList<>();

         for (AttributeModifier m : attr.getModifiers()) {
            if (m.getUniqueId() != null && persistedUuids.contains(m.getUniqueId())) {
               toRemove.add(m);
            } else if (this.isSoulModifier(m)) {
               toRemove.add(m);
            }
         }

         for (AttributeModifier m : toRemove) {
            attr.removeModifier(m);
         }

         double max = attr.getValue();
         if (player.getHealth() > max) {
            player.setHealth(max);
         }

         this.activeSoulModifiers.remove(player.getUniqueId());
         this.soulBonus.remove(player.getUniqueId());
         this.soulModifier.remove(player.getUniqueId());
         BukkitTask expiry = this.soulExpiry.remove(player.getUniqueId());
         if (expiry != null) {
            expiry.cancel();
         }

         this.clearPersistedModifierUuids(player.getUniqueId());
         if (!toRemove.isEmpty()) {
            this.plugin.getLogger().info("[SoulManager] Removed " + toRemove.size() + " stale soul-absorb modifier(s) from " + player.getName());
         }
      }
   }

   private boolean isSoulModifier(AttributeModifier m) {
      String name = m.getName();
      if (name != null) {
         String lower = name.toLowerCase();
         if (lower.contains("soul_absorb") || lower.contains("soul-absorb")) {
            return true;
         }

         if (lower.startsWith("blissgems:")) {
            return true;
         }
      }

      try {
         NamespacedKey key = m.getKey();
         if (key != null) {
            if ("blissgems".equals(key.getNamespace())) {
               return true;
            }

            String value = key.getKey();
            if (value != null && (value.contains("soul_absorb") || value.contains("soul-absorb"))) {
               return true;
            }
         }
      } catch (Throwable var5) {
      }

      return false;
   }

   private File getPlayerFile(UUID playerId) {
      File folder = new File(this.plugin.getDataFolder(), "playerdata");
      if (!folder.exists()) {
         folder.mkdirs();
      }

      return new File(folder, playerId + ".yml");
   }

   private void persistModifierUuid(UUID playerId, UUID modifierUuid) {
      File f = this.getPlayerFile(playerId);
      YamlConfiguration data = YamlConfiguration.loadConfiguration(f);
      List list = data.getStringList("soul-modifier-uuids");
      list.add(modifierUuid.toString());
      data.set("soul-modifier-uuids", list);

      try {
         data.save(f);
      } catch (IOException var7) {
      }
   }

   private void unpersistModifierUuid(UUID playerId, UUID modifierUuid) {
      File f = this.getPlayerFile(playerId);
      if (f.exists()) {
         YamlConfiguration data = YamlConfiguration.loadConfiguration(f);
         List list = data.getStringList("soul-modifier-uuids");
         list.remove(modifierUuid.toString());
         data.set("soul-modifier-uuids", list.isEmpty() ? null : list);

         try {
            data.save(f);
         } catch (IOException var7) {
         }
      }
   }

   private Set<UUID> loadPersistedModifierUuids(UUID playerId) {
      File f = this.getPlayerFile(playerId);
      if (!f.exists()) {
         return Collections.emptySet();
      }

      YamlConfiguration data = YamlConfiguration.loadConfiguration(f);
      List<String> list = data.getStringList("soul-modifier-uuids");
      HashSet<UUID> out = new HashSet<>();

      for (String s : list) {
         try {
            out.add(UUID.fromString(s));
         } catch (IllegalArgumentException var9) {
         }
      }

      return out;
   }

   private void clearPersistedModifierUuids(UUID playerId) {
      File f = this.getPlayerFile(playerId);
      if (f.exists()) {
         YamlConfiguration data = YamlConfiguration.loadConfiguration(f);
         if (data.contains("soul-modifier-uuids")) {
            data.set("soul-modifier-uuids", null);

            try {
               data.save(f);
            } catch (IOException var5) {
            }
         }
      }
   }

   public static class CapturedMob {
      private final EntityType type;
      private final String customName;

      public CapturedMob(EntityType type, String customName) {
         this.type = type;
         this.customName = customName;
      }

      public EntityType getType() {
         return this.type;
      }

      public String getCustomName() {
         return this.customName;
      }

      public String getDisplayName() {
         return this.customName != null && !this.customName.isEmpty() ? this.customName : this.type.toString().toLowerCase().replace("_", " ");
      }
   }
}
