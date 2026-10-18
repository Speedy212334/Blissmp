package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

public class WealthAbilities implements GemAbilityHandler {
   private final BlissGems plugin;
   private final Map<UUID, Inventory> pocketsInventories;
   private final Map<UUID, Boolean> autoSmeltEnabled;
   private final File pocketsDataFolder;
   private static final Set<UUID> unfortunatePlayers = new HashSet<>();
   private static final Map<UUID, ItemStack> itemLockedPlayers = new HashMap<>();
   private final Set<UUID> amplifiedPlayers = new HashSet<>();
   private static final String AMP_PDC_PREFIX = "amp_orig_";
   private static final String AE_PDC_PREFIX = "ae_orig_";
   private static final Set<UUID> richRushPlayers = new HashSet<>();

   public WealthAbilities(BlissGems plugin) {
      this.plugin = plugin;
      this.pocketsInventories = new HashMap<>();
      this.autoSmeltEnabled = new HashMap<>();
      this.pocketsDataFolder = new File(plugin.getDataFolder(), "pockets");
      if (!this.pocketsDataFolder.exists()) {
         this.pocketsDataFolder.mkdirs();
      }
   }

   public void onRightClick(Player player, int tier) {
      if (tier == 2 && player.isSneaking()) {
         this.richRush(player);
      } else {
         this.unfortunate(player);
      }
   }

   @Override
   public void onPrimary(Player player, int tier) {
      this.unfortunate(player);
   }

   @Override
   public void onSecondary(Player player, int tier) {
      this.richRush(player);
   }

   @Override
   public void onTertiary(Player player, int tier) {
      this.itemLock(player);
   }

   @Override
   public void onQuaternary(Player player, int tier) {
      this.amplification(player);
   }

   private boolean requireTier2(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) >= 2) {
         return true;
      }

      player.sendMessage("§c§oThis ability requires Tier 2!");
      return false;
   }

   public void pockets(Player player) {
      if (this.requireTier2(player)) {
         Inventory pockets = this.pocketsInventories.computeIfAbsent(player.getUniqueId(), uuid -> {
            Inventory inv = Bukkit.createInventory(null, 9, "§6§lPockets");
            this.loadPocketsInventory(player.getUniqueId(), inv);
            return inv;
         });
         player.openInventory(pockets);
         player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
      }
   }

   public void unfortunate(Player player) {
      if (this.requireTier2(player)) {
         String abilityKey = "wealth-unfortunate";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            RayTraceResult target = player.getWorld()
               .rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0, entity -> entity instanceof Player && entity != player);
            Entity entity2;
            if (target != null && (entity2 = target.getHitEntity()) instanceof Player) {
               Player targetPlayer = (Player)entity2;
               if (this.plugin.getTrustedPlayersManager() != null && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                  player.sendMessage("§c§lYou cannot use Unfortunate on a trusted player!");
               } else {
                  int duration = this.plugin.getConfigManager().getAbilityDuration("wealth-unfortunate");
                  UUID targetUUID = targetPlayer.getUniqueId();
                  unfortunatePlayers.add(targetUUID);
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                     unfortunatePlayers.remove(targetUUID);
                     if (targetPlayer.isOnline()) {
                        targetPlayer.sendMessage("§a§oUnfortunate has worn off.");
                     }

                     this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
                  }, duration * 20L);
                  DustOptions greenDust = new DustOptions(ParticleUtils.WEALTH_GREEN, 1.5F);
                  targetPlayer.getWorld().spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, greenDust, true);
                  targetPlayer.getWorld().spawnParticle(Particle.SMOKE, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
                  player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.0F, 0.8F);
                  targetPlayer.sendMessage("§c§oYou've been afflicted with Unfortunate! Actions disabled for " + duration + "s!");
                  this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
                  this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Unfortunate");
               }
            } else {
               player.sendMessage("§cNo player target found!");
            }
         }
      }
   }

   public void itemLock(Player player) {
      if (this.requireTier2(player)) {
         String abilityKey = "wealth-item-lock";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            RayTraceResult target = player.getWorld()
               .rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0, entity -> entity instanceof Player && entity != player);
            Entity entity2;
            if (target != null && (entity2 = target.getHitEntity()) instanceof Player) {
               Player targetPlayer = (Player)entity2;
               if (this.plugin.getTrustedPlayersManager() != null && this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                  player.sendMessage("§c§lYou cannot use Item Lock on a trusted player!");
               } else {
                  ItemStack targetItem = targetPlayer.getInventory().getItemInMainHand();
                  if (targetItem != null && !targetItem.getType().isAir()) {
                     UUID targetUUID = targetPlayer.getUniqueId();
                     itemLockedPlayers.put(targetUUID, targetItem.clone());
                     int duration = this.plugin.getConfig().getInt("abilities.durations.wealth-item-lock", 10);
                     Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                        itemLockedPlayers.remove(targetUUID);
                        if (targetPlayer.isOnline()) {
                           targetPlayer.sendMessage("§a§oItem Lock has worn off.");
                        }

                        this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
                     }, duration * 20L);
                     DustOptions greenDust = new DustOptions(ParticleUtils.WEALTH_GREEN, 1.5F);
                     targetPlayer.getWorld()
                        .spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, greenDust, true);
                     targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0F, 0.5F);
                     String itemName = targetItem.getType().name().toLowerCase().replace('_', ' ');
                     if (targetItem.hasItemMeta() && targetItem.getItemMeta().hasDisplayName()) {
                        itemName = targetItem.getItemMeta().getDisplayName();
                     }

                     targetPlayer.sendMessage("§c§oYour " + itemName + " has been locked for " + duration + "s!");
                     this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
                     this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Item Lock");
                  } else {
                     player.sendMessage("§cTarget isn't holding an item!");
                  }
               }
            } else {
               player.sendMessage("§cNo player target found!");
            }
         }
      }
   }

   public void richRush(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "wealth-rich-rush";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            int duration = this.plugin.getConfigManager().getAbilityDuration("wealth-rich-rush");
            UUID uuid = player.getUniqueId();
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration * 20, 2, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration * 20, 3, false, true));
            richRushPlayers.add(uuid);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               richRushPlayers.remove(uuid);
               this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
               Player p = Bukkit.getPlayer(uuid);
               if (p != null && p.isOnline()) {
                  p.sendMessage("§e§oRich Rush has worn off.");
               }
            }, duration * 20L);
            DustOptions greenDust = new DustOptions(ParticleUtils.WEALTH_GREEN, 1.5F);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
            player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.0, greenDust, true);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5);
            this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
            this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Rich Rush");
            player.sendMessage("§6§lRich Rush! §eMob and ore drops doubled for " + duration + "s!");
         }
      }
   }

   public static boolean hasRichRush(UUID uuid) {
      return richRushPlayers.contains(uuid);
   }

   public void amplification(Player player) {
      if (this.plugin.getGemManager().getGemTier(player) < 2) {
         player.sendMessage("§c§oThis ability requires Tier 2!");
      } else {
         String abilityKey = "wealth-amplification";
         if (this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            UUID uuid = player.getUniqueId();
            if (this.amplifiedPlayers.contains(uuid)) {
               player.sendMessage("§c§oAmplification is already active!");
            } else {
               int duration = this.plugin.getConfigManager().getAbilityDuration("wealth-amplification");
               boolean anyAmplified = false;
               ItemStack[] armor = player.getInventory().getArmorContents();

               for (int i = 0; i < armor.length; i++) {
                  if (this.amplifyItem(armor[i])) {
                     anyAmplified = true;
                  }
               }

               player.getInventory().setArmorContents(armor);
               ItemStack mainHand = player.getInventory().getItemInMainHand();
               if (this.amplifyItem(mainHand)) {
                  anyAmplified = true;
               }

               if (this.amplifyItem(player.getInventory().getItemInOffHand())) {
                  anyAmplified = true;
               }

               this.amplifiedPlayers.add(uuid);
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                  this.amplifiedPlayers.remove(uuid);
                  this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null && p.isOnline()) {
                     this.revertAllAmplifiedItems(p);
                     p.sendMessage("§e§oAmplification has worn off. Enchantments restored.");
                  }
               }, duration * 20L);
               DustOptions greenDust = new DustOptions(ParticleUtils.WEALTH_GREEN, 1.5F);
               player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.5F);
               player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 0.5, 1.0, 0.5, 0.0, greenDust, true);
               player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 1.0, 0.0), 80, 0.5, 1.0, 0.5);
               if (this.plugin.getAchievementManager() != null && anyAmplified) {
                  this.plugin.getAchievementManager().unlock(player, Achievement.BOUNDARY_BREAK);
               }

               this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
               this.plugin.getConfigManager().sendFormattedMessage(player, "ability-activated", "ability", "Amplification");
            }
         }
      }
   }

   private boolean amplifyItem(ItemStack item) {
      if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
         Map<Enchantment, Integer> enchants = item.getEnchantments();
         if (enchants.isEmpty()) {
            return false;
         }

         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         }

         PersistentDataContainer pdc = meta.getPersistentDataContainer();
         boolean modified = false;

         for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int currentLevel = entry.getValue();
            NamespacedKey aeKey = new NamespacedKey(this.plugin, "ae_orig_" + enchant.getKey().getKey());
            NamespacedKey ampKey;
            if (!pdc.has(aeKey, PersistentDataType.INTEGER)
               && !pdc.has(ampKey = new NamespacedKey(this.plugin, "amp_orig_" + enchant.getKey().getKey()), PersistentDataType.INTEGER)) {
               pdc.set(ampKey, PersistentDataType.INTEGER, currentLevel);
               meta.addEnchant(enchant, currentLevel + 1, true);
               modified = true;
            }
         }

         if (modified) {
            item.setItemMeta(meta);
         }

         return modified;
      } else {
         return false;
      }
   }

   private void revertAllAmplifiedItems(Player p) {
      for (int i = 0; i < p.getInventory().getSize(); i++) {
         ItemStack item = p.getInventory().getItem(i);
         if (item != null && !item.getType().isAir() && stripAmplifyEnchants(item)) {
            p.getInventory().setItem(i, item);
         }
      }

      ItemStack[] armor = p.getInventory().getArmorContents();
      boolean armorModified = false;

      for (int i = 0; i < armor.length; i++) {
         if (armor[i] != null && !armor[i].getType().isAir() && stripAmplifyEnchants(armor[i])) {
            armorModified = true;
         }
      }

      if (armorModified) {
         p.getInventory().setArmorContents(armor);
      }

      ItemStack offHand = p.getInventory().getItemInOffHand();
      if (offHand != null && !offHand.getType().isAir() && stripAmplifyEnchants(offHand)) {
         p.getInventory().setItemInOffHand(offHand);
      }

      ItemStack mainHand = p.getInventory().getItemInMainHand();
      if (mainHand != null && !mainHand.getType().isAir() && stripAmplifyEnchants(mainHand)) {
         p.getInventory().setItemInMainHand(mainHand);
      }

      p.updateInventory();
   }

   public static boolean stripAmplifyEnchants(ItemStack item) {
      if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         }

         PersistentDataContainer pdc = meta.getPersistentDataContainer();
         boolean modified = false;

         for (NamespacedKey key : new HashSet(pdc.getKeys())) {
            if (key.getKey().startsWith("amp_orig_")) {
               String enchName = key.getKey().substring("amp_orig_".length());
               Enchantment enchant = (Enchantment)Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchName));
               if (enchant == null) {
                  enchant = (Enchantment)Registry.ENCHANTMENT.get(new NamespacedKey("blissgems", enchName));
               }

               if (enchant != null) {
                  Integer orig = (Integer)pdc.get(key, PersistentDataType.INTEGER);
                  if (orig != null) {
                     if (orig <= 0) {
                        meta.removeEnchant(enchant);
                     } else {
                        meta.addEnchant(enchant, orig, true);
                     }

                     modified = true;
                  }
               } else {
                  for (Enchantment e : meta.getEnchants().keySet()) {
                     if (e.getKey().getKey().equalsIgnoreCase(enchName)) {
                        Integer orig = (Integer)pdc.get(key, PersistentDataType.INTEGER);
                        if (orig != null) {
                           if (orig <= 0) {
                              meta.removeEnchant(e);
                           } else {
                              meta.addEnchant(e, orig, true);
                           }

                           modified = true;
                        }
                        break;
                     }
                  }
               }

               pdc.remove(key);
               modified = true;
            }
         }

         if (modified) {
            item.setItemMeta(meta);
         }

         return modified;
      } else {
         return false;
      }
   }

   public boolean isAmplified(UUID uuid) {
      return this.amplifiedPlayers.contains(uuid);
   }

   public static boolean hasAmplifyEnchants(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

         for (NamespacedKey key : pdc.getKeys()) {
            if (key.getKey().startsWith("amp_orig_")) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static NamespacedKey getAmpKey(ItemStack item, Enchantment enchant) {
      return item.hasItemMeta() && enchant != null ? new NamespacedKey("blissgems", "amp_orig_" + enchant.getKey().getKey()) : null;
   }

   public static boolean isUnfortunate(UUID uuid) {
      return unfortunatePlayers.contains(uuid);
   }

   public static boolean shouldUnfortunateFail(UUID uuid, double failChance) {
      return unfortunatePlayers.contains(uuid) && Math.random() < failChance;
   }

   public static boolean isItemLocked(UUID uuid) {
      return itemLockedPlayers.containsKey(uuid);
   }

   public static ItemStack getLockedItem(UUID uuid) {
      return itemLockedPlayers.get(uuid);
   }

   @Override
   public void cleanup(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.amplifiedPlayers.remove(uuid)) {
         this.revertAllAmplifiedItems(player);
         this.plugin.getAbilityManager().endAbilityDuration(player, "wealth-amplification");
      }

      unfortunatePlayers.remove(uuid);
      itemLockedPlayers.remove(uuid);
      if (richRushPlayers.remove(uuid)) {
         this.plugin.getAbilityManager().endAbilityDuration(player, "wealth-rich-rush");
      }
   }

   public Inventory getPocketsInventory(UUID uuid) {
      return this.pocketsInventories.get(uuid);
   }

   public boolean isAutoSmeltEnabled(Player player) {
      return this.autoSmeltEnabled.getOrDefault(player.getUniqueId(), true);
   }

   public void setAutoSmelt(Player player, boolean enabled) {
      this.autoSmeltEnabled.put(player.getUniqueId(), enabled);
   }

   public void savePocketsInventory(UUID uuid) {
      Inventory inv = this.pocketsInventories.get(uuid);
      if (inv != null) {
         File file = new File(this.pocketsDataFolder, uuid.toString() + ".yml");
         YamlConfiguration data = new YamlConfiguration();

         for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
               data.set("items." + i, item);
            }
         }

         try {
            data.save(file);
         } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save pockets inventory for " + uuid + ": " + e.getMessage());
         }
      }
   }

   private void loadPocketsInventory(UUID uuid, Inventory inv) {
      File file = new File(this.pocketsDataFolder, uuid.toString() + ".yml");
      if (file.exists()) {
         YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
         if (data.contains("items")) {
            for (String key : data.getConfigurationSection("items").getKeys(false)) {
               try {
                  int slot = Integer.parseInt(key);
                  ItemStack item = data.getItemStack("items." + key);
                  if (item != null && slot >= 0 && slot < inv.getSize()) {
                     inv.setItem(slot, item);
                  }
               } catch (NumberFormatException e) {
                  this.plugin.getLogger().warning("Invalid slot key in pockets data: " + key);
               }
            }
         }
      }
   }

   public void saveAllPockets() {
      for (UUID uuid : this.pocketsInventories.keySet()) {
         this.savePocketsInventory(uuid);
      }
   }
}
