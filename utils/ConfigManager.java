package dev.xoperr.blissgems.utils;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class ConfigManager {
   private final BlissGems plugin;
   private FileConfiguration config;
   private static final String CONFIG_VERSION = "1.0.0";
   private static final MiniMessage MINI = MiniMessage.miniMessage();
   private static final LegacyComponentSerializer SECTION_HEX = LegacyComponentSerializer.builder()
      .character('§')
      .hexColors()
      .useUnusualXRepeatedCharacterHexFormat()
      .build();
   private static final String LEGACY_TAGS = "0black 1dark_blue 2dark_green 3dark_aqua 4dark_red 5dark_purple 6gold 7gray 8dark_gray 9blue agreen baqua cred dlight_purple eyellow fwhite kobfuscated lbold mstrikethrough nunderlined oitalic rreset";

   public ConfigManager(BlissGems plugin) {
      this.plugin = plugin;
      this.reload();
      this.autoRepairConfig();
      this.runMigrations();
   }

   private void runMigrations() {
      File configFile = new File(this.plugin.getDataFolder(), "config.yml");
      if (configFile.exists()) {
         YamlConfiguration user = YamlConfiguration.loadConfiguration(configFile);
         List<String> before = new ArrayList<>(user.getStringList("config-migrations"));
         List<String> changed = ConfigMigrations.apply(user);
         if (!before.equals(user.getStringList("config-migrations"))) {
            try {
               user.save(configFile);
            } catch (IOException e) {
               this.plugin.getLogger().warning("Could not save config.yml after migrations: " + e.getMessage());
               return;
            }

            if (!changed.isEmpty()) {
               this.plugin.getLogger().info("Updated " + changed.size() + " default value(s) in config.yml (new balance): " + String.join(", ", changed));
            }

            this.reload();
         }
      }
   }

   public void reload() {
      this.plugin.reloadConfig();
      this.config = this.plugin.getConfig();
   }

   private void autoRepairConfig() {
      File configFile = new File(this.plugin.getDataFolder(), "config.yml");
      if (configFile.exists()) {
         YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
            new InputStreamReader(this.plugin.getResource("config.yml"), StandardCharsets.UTF_8)
         );
         YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
         String currentVersion = userConfig.getString("config-version", "unknown");
         String expectedVersion;
         boolean isOutdated = !currentVersion.equals(expectedVersion = defaultConfig.getString("config-version", "1.0.0"));
         boolean needsRepair = false;
         int missingKeys = 0;
         Set<String> defaultKeys = defaultConfig.getKeys(true);

         for (String key : defaultKeys) {
            if (!defaultConfig.isConfigurationSection(key) && !userConfig.contains(key)) {
               needsRepair = true;
               missingKeys++;
            }
         }

         if (!needsRepair) {
            if (isOutdated) {
               this.config.set("config-version", expectedVersion);

               try {
                  this.config.save(configFile);
                  this.plugin.getLogger().info("Updated config version from " + currentVersion + " to " + expectedVersion);
               } catch (IOException e) {
                  this.plugin.getLogger().warning("Failed to update config version: " + e.getMessage());
               }
            }
         } else {
            try {
               File backupFile = new File(this.plugin.getDataFolder(), "config.yml.backup");
               Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
               this.plugin.getLogger().info("Created config backup: config.yml.backup");
            } catch (IOException e) {
               this.plugin.getLogger().warning("Failed to backup config: " + e.getMessage());
            }

            if (isOutdated) {
               this.plugin.getLogger().warning("Detected outdated config! (v" + currentVersion + " -> v" + expectedVersion + ")");
            }

            this.plugin.getLogger().warning("Auto-repairing config: " + missingKeys + " missing entries detected.");

            for (String key : defaultKeys) {
               if (!defaultConfig.isConfigurationSection(key) && !userConfig.contains(key)) {
                  this.config.set(key, defaultConfig.get(key));
                  this.plugin.getLogger().info("  + Added: " + key);
               }
            }

            this.config.set("config-version", expectedVersion);

            try {
               this.config.save(configFile);
               this.plugin.getLogger().info("Config auto-repair complete! " + missingKeys + " keys added.");
               this.plugin.getLogger().info("Your old config was backed up to config.yml.backup");
               this.reload();
            } catch (IOException e) {
               this.plugin.getLogger().log(Level.SEVERE, "Failed to save repaired config!", e);
            }
         }
      }
   }

   public FileConfiguration getConfig() {
      return this.config;
   }

   public int getEnergyGainOnKill() {
      return this.config.getInt("energy.gain-on-kill", 1);
   }

   public int getEnergyLossOnDeath() {
      return this.config.getInt("energy.loss-on-death", 1);
   }

   public int getMaxEnergy() {
      return this.config.getInt("energy.max-energy", 10);
   }

   public int getStartingEnergy() {
      return this.config.getInt("energy.starting-energy", 10);
   }

   public int getRuinedThreshold() {
      return this.config.getInt("energy.ruined-threshold", 1);
   }

   public int getBrokenThreshold() {
      return this.config.getInt("energy.broken-threshold", 0);
   }

   public boolean isBanOnZeroEnergyEnabled() {
      return this.config.getBoolean("energy.ban-on-zero-energy", false);
   }

   public void setBanOnZeroEnergy(boolean enabled) {
      this.config.set("energy.ban-on-zero-energy", enabled);

      try {
         this.config.save(new File(this.plugin.getDataFolder(), "config.yml"));
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to save config: " + e.getMessage());
      }
   }

   public boolean isSmpStarted() {
      return this.config.getBoolean("smp.started", false);
   }

   public void setSmpStarted(boolean started) {
      this.config.set("smp.started", started);

      try {
         this.config.save(new File(this.plugin.getDataFolder(), "config.yml"));
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to save config: " + e.getMessage());
      }
   }

   public int getSmpAutoStartThreshold() {
      return this.config.getInt("smp.auto-start-threshold", 10);
   }

   public boolean isFixedHeartsEnabled() {
      return this.config.getBoolean("fixed-hearts.enabled", false);
   }

   public void setFixedHeartsEnabled(boolean enabled) {
      this.config.set("fixed-hearts.enabled", enabled);

      try {
         this.config.save(new File(this.plugin.getDataFolder(), "config.yml"));
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to save config: " + e.getMessage());
      }
   }

   public boolean isGemEnabled(GemType type) {
      return this.config.getBoolean("gems.enabled." + type.getId(), true);
   }

   public boolean isSingleGemOnly() {
      return this.config.getBoolean("gems.single-gem-only", true);
   }

   public boolean isUpgraderDropOnTier2DeathEnabled() {
      return this.config.getBoolean("gems.drop-upgrader-on-tier2-death", false);
   }

   public boolean isTier1AutoEnchantEnabled() {
      return this.config.getBoolean("auto-enchant.tier1-enabled", true);
   }

   public int getPassiveUpdateInterval() {
      return this.config.getInt("passives.update-interval", 20);
   }

   public double getPhaseChance(int tier) {
      String path = "passives.astra.tier" + tier + ".phase-chance";
      return this.config.getDouble(path, tier == 1 ? 0.1 : 0.15);
   }

   public double getLifeHealAmount(int tier) {
      String path = "passives.life.tier" + tier + ".heal-amount";
      return this.config.getDouble(path, tier == 1 ? 0.3 : 0.5);
   }

   public int getLifeHealInterval(int tier) {
      String path = "passives.life.tier" + tier + ".heal-interval";
      return this.config.getInt(path, 100);
   }

   public double getUndeadDamageMultiplier(int tier) {
      String path = "passives.life.tier" + tier + ".undead-damage-multiplier";
      return this.config.getDouble(path, tier == 1 ? 2.0 : 3.0);
   }

   public double getSaturationMultiplier(int tier) {
      String path = "passives.life.tier" + tier + ".saturation-multiplier";
      return this.config.getDouble(path, tier == 1 ? 1.5 : 2.0);
   }

   public int getGoldenAppleAbsorptionLevel(int tier) {
      String path = "passives.life.tier" + tier + ".golden-apple-absorption-level";
      return this.config.getInt(path, tier == 1 ? 0 : 1);
   }

   public double getShockingArrowDamage(int tier) {
      String path = "passives.flux.tier" + tier + ".shocking-arrow-damage";
      return this.config.getDouble(path, tier == 1 ? 2.0 : 4.0);
   }

   public double getFlowStateSpeedBoost(int tier) {
      String path = "passives.flux.tier" + tier + ".flow-state-speed-boost";
      return this.config.getDouble(path, tier == 1 ? 0.15 : 0.2);
   }

   public boolean isDoubleJumpEnabled(int tier) {
      String path = "passives.puff.tier" + tier + ".double-jump-enabled";
      return this.config.getBoolean(path, true);
   }

   public double getDoubleJumpVelocity(int tier) {
      String path = "passives.puff.tier" + tier + ".double-jump-velocity";
      return this.config.getDouble(path, tier == 1 ? 0.6 : 0.8);
   }

   public double getLaunchVelocity(int tier) {
      String path = "passives.puff.tier" + tier + ".launch-velocity";
      return this.config.getDouble(path, tier == 1 ? 3.5 : 4.5);
   }

   public boolean isFallDamageImmunity(int tier) {
      String path = "passives.puff.tier" + tier + ".fall-damage-immunity";
      return this.config.getBoolean(path, true);
   }

   public boolean isSculkImmunity(int tier) {
      String path = "passives.puff.tier" + tier + ".sculk-immunity";
      return this.config.getBoolean(path, tier == 2);
   }

   public boolean isAutoSmeltEnabled(int tier) {
      String path = "passives.fire.tier" + tier + ".auto-smelt";
      return this.config.getBoolean(path, true);
   }

   public boolean isFireResistanceEnabled(int tier) {
      String path = "passives.fire.tier" + tier + ".fire-resistance";
      return this.config.getBoolean(path, true);
   }

   public boolean isSoulSandImmunity(int tier) {
      String path = "passives.speed.tier" + tier + ".soul-sand-immunity";
      return this.config.getBoolean(path, true);
   }

   public int getSpeedLevel(int tier) {
      String path = "passives.speed.tier" + tier + ".speed-level";
      return this.config.getInt(path, tier == 1 ? 0 : 1);
   }

   public int getStrengthLevel(int tier) {
      String path = "passives.strength.tier" + tier + ".strength-level";
      return this.config.getInt(path, tier == 1 ? 0 : 1);
   }

   public double getBloodthornsMaxBonusDamage(int tier) {
      String path = "passives.strength.tier" + tier + ".bloodthorns-max-bonus-damage";
      return this.config.getDouble(path, tier == 1 ? 7.0 : 10.0);
   }

   public int getLuckLevel(int tier) {
      String path = "passives.wealth.tier" + tier + ".luck-level";
      return this.config.getInt(path, tier == 1 ? 0 : 1);
   }

   public double getVillagerDiscount(int tier) {
      String path = "passives.wealth.tier" + tier + ".villager-discount";
      return this.config.getDouble(path, tier == 1 ? 0.1 : 0.2);
   }

   @Deprecated
   public double getPhaseChance() {
      return this.getPhaseChance(2);
   }

   @Deprecated
   public double getLifeHealAmount() {
      return this.getLifeHealAmount(2);
   }

   @Deprecated
   public int getLifeHealInterval() {
      return this.getLifeHealInterval(2);
   }

   @Deprecated
   public double getUndeadDamageMultiplier() {
      return this.getUndeadDamageMultiplier(2);
   }

   @Deprecated
   public double getSaturationMultiplier() {
      return this.getSaturationMultiplier(2);
   }

   public int getGlobalAbilityCooldown() {
      return this.config.getInt("abilities.global-cooldown", 1);
   }

   public int getAbilityCooldown(String abilityKey) {
      return this.config.getInt("abilities.cooldowns." + abilityKey, 10);
   }

   public double getAbilityDamage(String abilityKey) {
      return this.config.getDouble("abilities.damage." + abilityKey, 4.0);
   }

   public int getAbilityDuration(String abilityKey) {
      return this.config.getInt("abilities.durations." + abilityKey, 10);
   }

   public boolean isAutoEnchantEnabled() {
      return this.config.getBoolean("auto-enchant.enabled", true);
   }

   public boolean shouldPlayUpgradeEffects() {
      return this.config.getBoolean("upgrader.play-effects", true);
   }

   public String getUpgradeParticle() {
      return this.config.getString("upgrader.particle", "ENCHANT");
   }

   public int getUpgradeParticleCount() {
      return this.config.getInt("upgrader.particle-count", 50);
   }

   public String getUpgradeSound() {
      return this.config.getString("upgrader.sound", "ENTITY_PLAYER_LEVELUP");
   }

   public void playConfiguredEffect(Player player, String section, Particle defaultParticle, int defaultCount, Sound defaultSound) {
      Particle particle = defaultParticle;
      String particleName = this.config.getString(section + ".particle");
      if (particleName != null) {
         try {
            particle = Particle.valueOf(particleName.trim().toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning(section + ".particle: unknown particle '" + particleName + "'");
         }
      }

      Sound sound = defaultSound;
      String soundName = this.config.getString(section + ".sound");
      if (soundName != null) {
         try {
            sound = Sound.valueOf(soundName.trim().toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning(section + ".sound: unknown sound '" + soundName + "'");
         }
      }

      int count = this.config.getInt(section + ".particle-count", defaultCount);
      player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
      if (count > 0 && particle.getDataType() == Void.class) {
         player.spawnParticle(particle, player.getLocation().add(0.0, 1.0, 0.0), count, 0.5, 0.5, 0.5);
      }
   }

   public int getTraderCooldown() {
      return this.config.getInt("trader.cooldown", 5);
   }

   public boolean shouldPlayTradeEffects() {
      return this.config.getBoolean("trader.play-effects", true);
   }

   public boolean isEnergyBottleDropEnabled() {
      return this.config.getBoolean("energy-bottle.drop-enabled", true);
   }

   public int getRepairKitEnergyPerSecond() {
      return this.config.getInt("repair-kit.energy-per-second", 1);
   }

   public int getRepairKitMaxEnergy() {
      return this.config.getInt("repair-kit.max-total-energy", 10);
   }

   public int getRepairKitRadius() {
      return this.config.getInt("repair-kit.heal-radius", 10);
   }

   public int getRepairKitUpdateInterval() {
      return this.config.getInt("repair-kit.update-interval", 20);
   }

   public boolean shouldPrioritizeLowestEnergy() {
      return this.config.getBoolean("repair-kit.prioritize-lowest", true);
   }

   public String getMessage(String key) {
      return renderMessage(this.config.getString("messages." + key, ""));
   }

   static String renderMessage(String raw) {
      if (raw != null && !raw.isEmpty()) {
         if (raw.indexOf(60) < 0) {
            return raw.replace("&", "§");
         }

         StringBuilder mm = new StringBuilder();

         for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '&' && i + 1 < raw.length()) {
               char code = Character.toLowerCase(raw.charAt(i + 1));
               int at = "0black 1dark_blue 2dark_green 3dark_aqua 4dark_red 5dark_purple 6gold 7gray 8dark_gray 9blue agreen baqua cred dlight_purple eyellow fwhite kobfuscated lbold mstrikethrough nunderlined oitalic rreset"
                     .indexOf(" " + code)
                  + 1;
               if ("0black 1dark_blue 2dark_green 3dark_aqua 4dark_red 5dark_purple 6gold 7gray 8dark_gray 9blue agreen baqua cred dlight_purple eyellow fwhite kobfuscated lbold mstrikethrough nunderlined oitalic rreset"
                     .charAt(0)
                  == code) {
                  at = 0;
               }

               if (at >= 0
                  && (
                     at == 0
                        || "0black 1dark_blue 2dark_green 3dark_aqua 4dark_red 5dark_purple 6gold 7gray 8dark_gray 9blue agreen baqua cred dlight_purple eyellow fwhite kobfuscated lbold mstrikethrough nunderlined oitalic rreset"
                              .charAt(at - 1)
                           == ' '
                  )) {
                  int end = "0black 1dark_blue 2dark_green 3dark_aqua 4dark_red 5dark_purple 6gold 7gray 8dark_gray 9blue agreen baqua cred dlight_purple eyellow fwhite kobfuscated lbold mstrikethrough nunderlined oitalic rreset"
                     .indexOf(32, at);
                  mm.append('<')
                     .append(
                        "0black 1dark_blue 2dark_green 3dark_aqua 4dark_red 5dark_purple 6gold 7gray 8dark_gray 9blue agreen baqua cred dlight_purple eyellow fwhite kobfuscated lbold mstrikethrough nunderlined oitalic rreset",
                        at + 1,
                        end < 0
                           ? "0black 1dark_blue 2dark_green 3dark_aqua 4dark_red 5dark_purple 6gold 7gray 8dark_gray 9blue agreen baqua cred dlight_purple eyellow fwhite kobfuscated lbold mstrikethrough nunderlined oitalic rreset"
                              .length()
                           : end
                     )
                     .append('>');
                  i++;
                  continue;
               }
            }

            mm.append(c);
         }

         return SECTION_HEX.serialize(MINI.deserialize(mm.toString()));
      } else {
         return "";
      }
   }

   public String getPrefix() {
      return this.getMessage("prefix");
   }

   public String getFormattedMessage(String key, Object... replacements) {
      if (this.areOptionalMessagesDisabled() && key.equals("ability-activated")) {
         return null;
      }

      String message = this.getMessage(key);
      if (message != null && !message.trim().isEmpty()) {
         for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
               message = message.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
            }
         }

         String result = this.getPrefix() + " §f" + message;
         String cleanResult = result.replaceAll("§[0-9a-fk-or]", "").trim();
         return !cleanResult.equals("BlissGems »") && !cleanResult.isEmpty() ? result : null;
      } else {
         return null;
      }
   }

   public void sendFormattedMessage(CommandSender sender, String key, Object... replacements) {
      String message = this.getFormattedMessage(key, replacements);
      if (message != null && !message.isEmpty()) {
         sender.sendMessage(message);
      }
   }

   public boolean areOptionalMessagesDisabled() {
      return this.config.getBoolean("disable-optional-messages", true);
   }
}
