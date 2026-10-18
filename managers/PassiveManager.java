package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.utils.GemType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PassiveManager {
   private final BlissGems plugin;
   private final Map<UUID, Long> lastLifeHeal = new HashMap<>();
   private final Map<UUID, Long> lastLifeAbsorption = new HashMap<>();

   public PassiveManager(BlissGems plugin) {
      this.plugin = plugin;
      this.startPassiveEffectTask();
   }

   private void startPassiveEffectTask() {
      int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
      (new BukkitRunnable() {
            public void run() {
               for (Player player : PassiveManager.this.plugin.getServer().getOnlinePlayers()) {
                  if (!player.isDead()
                     && PassiveManager.this.plugin.getGemManager().hasGemForPassives(player)
                     && PassiveManager.this.plugin.getEnergyManager().arePassivesActive(player)) {
                     PassiveManager.this.applyPassiveEffects(player);
                  }
               }
            }
         })
         .runTaskTimer(this.plugin, interval, interval);
   }

   private void applyPassiveEffects(Player player) {
      if (this.plugin.getGemLockManager() == null || !this.plugin.getGemLockManager().isLocked(player)) {
         if (this.plugin.getRegionManager() == null || !this.plugin.getRegionManager().areGemsDisabled(player)) {
            int tier = this.plugin.getGemManager().getTierForPassives(player);
            String gemId = this.plugin.getGemManager().getGemIdForPassives(player);
            GemType gemType = this.plugin.getGemManager().getGemTypeForPassives(player);
            if (gemType != null) {
               switch (gemType) {
                  case ASTRA:
                     this.applyAstraPassives(player);
                     break;
                  case FIRE:
                     this.applyFirePassives(player);
                     break;
                  case FLUX:
                     this.applyFluxPassives(player);
                     break;
                  case LIFE:
                     this.applyLifePassives(player);
                     break;
                  case PUFF:
                     this.applyPuffPassives(player);
                     break;
                  case SPEED:
                     this.applySpeedPassives(player);
                     break;
                  case STRENGTH:
                     this.applyStrengthPassives(player);
                     break;
                  case WEALTH:
                     this.applyWealthPassives(player);
               }

               this.applyConfiguredExtraEffects(player, gemType, tier);
            } else {
               GemRegistryImpl registry;
               if (gemId != null && (registry = this.plugin.getGemRegistry()) != null) {
                  GemPassiveHandler handler = registry.getPassiveHandler(gemId);
                  if (handler != null) {
                     handler.applyPassives(player, tier);
                  }

                  this.applyConfiguredExtraEffectsById(player, gemId, tier);
               }
            }
         }
      }
   }

   private void applyConfiguredExtraEffectsById(Player player, String gemId, int tier) {
      String path = "passives.extra-effects." + gemId + ".t" + tier;
      List<Map<?, ?>> entries = this.plugin.getConfig().getMapList(path);
      if (entries != null && !entries.isEmpty()) {
         int duration = this.plugin.getConfigManager().getPassiveUpdateInterval() + 10;

         for (Map<?, ?> entry : entries) {
            Object typeObj = entry.get("type");
            if (typeObj != null) {
               PotionEffectType type = PotionEffectType.getByName(typeObj.toString().toUpperCase());
               if (type == null) {
                  this.plugin.getLogger().warning("Unknown potion effect '" + typeObj + "' in " + path);
               } else {
                  Object levelObj = entry.get("level");
                  int level = levelObj instanceof Number ? ((Number)levelObj).intValue() : 1;
                  int amplifier = Math.max(0, level - 1);
                  player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false), true);
               }
            }
         }
      }
   }

   private void applyConfiguredExtraEffects(Player player, GemType gemType, int tier) {
      String path = "passives.extra-effects." + gemType.name().toLowerCase() + ".t" + tier;
      List<Map<?, ?>> entries = this.plugin.getConfig().getMapList(path);
      if (entries != null && !entries.isEmpty()) {
         int duration = this.plugin.getConfigManager().getPassiveUpdateInterval() + 10;

         for (Map<?, ?> entry : entries) {
            Object typeObj = entry.get("type");
            if (typeObj != null) {
               PotionEffectType type = PotionEffectType.getByName(typeObj.toString().toUpperCase());
               if (type == null) {
                  this.plugin.getLogger().warning("Unknown potion effect '" + typeObj + "' in " + path);
               } else {
                  Object levelObj = entry.get("level");
                  int level = levelObj instanceof Number ? ((Number)levelObj).intValue() : 1;
                  int amplifier = Math.max(0, level - 1);
                  player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false), true);
               }
            }
         }
      }
   }

   private void applyAstraPassives(Player player) {
   }

   private void applyFirePassives(Player player) {
      int tier = this.plugin.getGemManager().getTierForPassives(player);
      if (this.plugin.getConfigManager().isFireResistanceEnabled(tier)) {
         int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
         player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, interval + 10, 0, true, false), true);
      }
   }

   private void applyFluxPassives(Player player) {
      player.removePotionEffect(PotionEffectType.WEAKNESS);
      player.removePotionEffect(PotionEffectType.SLOWNESS);
      player.removePotionEffect(PotionEffectType.HUNGER);
      if (player.isSprinting()) {
         this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.SPRINT);
      }
   }

   private void applyLifePassives(Player player) {
      if (!player.isDead()) {
         player.removePotionEffect(PotionEffectType.WITHER);
         int tier = this.plugin.getGemManager().getTierForPassives(player);
         long now = System.currentTimeMillis();
         UUID id = player.getUniqueId();
         long healIntervalMs = this.plugin.getConfigManager().getLifeHealInterval(tier) * 50L;
         if (now - this.lastLifeHeal.getOrDefault(id, 0L) >= healIntervalMs) {
            this.lastLifeHeal.put(id, now);
            double healAmount = this.plugin.getConfigManager().getLifeHealAmount(tier) * 2.0;
            double currentHealth = player.getHealth();
            double maxHealth = player.getMaxHealth();
            if (currentHealth < maxHealth) {
               player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
            }
         }

         int absorptionBase = this.plugin.getConfig().getInt("passives.life.tier" + tier + ".absorption-per-block", 0);
         long absorptionCooldownMs = this.plugin.getConfig().getLong("passives.life.absorption-regen-seconds", 30L) * 1000L;
         if (player.getAbsorptionAmount() > 0.0) {
            this.lastLifeAbsorption.put(id, now);
         }

         boolean shieldReady = player.getAbsorptionAmount() <= 0.0 && now - this.lastLifeAbsorption.getOrDefault(id, 0L) >= absorptionCooldownMs;
         if (absorptionBase > 0 && shieldReady) {
            int radius = this.plugin.getConfig().getInt("passives.life.absorption-scan-radius", 5);
            Map<Material, Integer> blockCounts = new HashMap<>();
            Location loc = player.getLocation();

            for (int x = -radius; x <= radius; x++) {
               for (int y = -radius; y <= radius; y++) {
                  for (int z = -radius; z <= radius; z++) {
                     Block b = loc.getBlock().getRelative(x, y, z);
                     Material mat = b.getType();
                     if (mat != Material.AIR && (mat.isSolid() || Tag.LEAVES.isTagged(mat) || mat.name().contains("MOSS") || mat.name().contains("GRASS"))) {
                        blockCounts.put(mat, blockCounts.getOrDefault(mat, 0) + 1);
                     }
                  }
               }
            }

            int totalAbsorption = absorptionBase * blockCounts.size();
            int maxAbsorption = this.plugin.getConfig().getInt("passives.life.max-absorption-hearts", 20);
            totalAbsorption = Math.min(totalAbsorption, maxAbsorption);
            if (totalAbsorption > 0) {
               int durationTicks = 1200;
               player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, durationTicks, Math.max(0, totalAbsorption / 2 - 1), true, false));
               this.lastLifeAbsorption.put(id, now);
            }
         }
      }
   }

   private void applyPuffPassives(Player player) {
   }

   private void applySpeedPassives(Player player) {
      int tier = this.plugin.getGemManager().getTierForPassives(player);
      int speedLevel = this.plugin.getConfigManager().getSpeedLevel(tier);
      int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
      int duration = interval + 10;
      player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, speedLevel, true, false), true);
      player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, duration, 0, true, false), true);
   }

   private void applyStrengthPassives(Player player) {
      int tier = this.plugin.getGemManager().getTierForPassives(player);
      int strengthLevel = this.plugin.getConfigManager().getStrengthLevel(tier);
      int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
      int duration = interval + 10;
      PotionEffect existing = player.getPotionEffect(PotionEffectType.STRENGTH);
      if (existing == null || existing.getAmplifier() <= strengthLevel && (existing.getAmplifier() != strengthLevel || existing.getDuration() <= duration + 5)) {
         player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, strengthLevel, true, false), true);
      }
   }

   private void applyWealthPassives(Player player) {
      int tier = this.plugin.getGemManager().getTierForPassives(player);
      int luckLevel = this.plugin.getConfigManager().getLuckLevel(tier);
      int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
      int duration = interval + 10;
      player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration, luckLevel, true, false), true);
      player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, 0, true, false), true);
   }

   public void registerBuiltInHandlers(GemRegistry registry) {
      registry.registerPassives("astra", (player, tier) -> this.applyAstraPassives(player));
      registry.registerPassives("fire", (player, tier) -> this.applyFirePassives(player));
      registry.registerPassives("flux", (player, tier) -> this.applyFluxPassives(player));
      registry.registerPassives("life", (player, tier) -> this.applyLifePassives(player));
      registry.registerPassives("puff", (player, tier) -> this.applyPuffPassives(player));
      registry.registerPassives("speed", (player, tier) -> this.applySpeedPassives(player));
      registry.registerPassives("strength", (player, tier) -> this.applyStrengthPassives(player));
      registry.registerPassives("wealth", (player, tier) -> this.applyWealthPassives(player));
   }
}
