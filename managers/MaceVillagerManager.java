package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Villager.Type;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class MaceVillagerManager {
   private final BlissGems plugin;
   private final NamespacedKey villagerTypeKey;
   private final NamespacedKey tradeHistoryKey;
   public static final String TYPE_MACE_1 = "mace_1";
   public static final String TYPE_MACE_2 = "mace_2";
   public static final String TYPE_MACE_3 = "mace_3";
   public static final String TYPE_ENERGY = "energy_games";

   public MaceVillagerManager(BlissGems plugin) {
      this.plugin = plugin;
      this.villagerTypeKey = new NamespacedKey(plugin, "custom_villager_type");
      this.tradeHistoryKey = new NamespacedKey(plugin, "custom_trade_history");
   }

   public NamespacedKey getVillagerTypeKey() {
      return this.villagerTypeKey;
   }

   public NamespacedKey getTradeHistoryKey() {
      return this.tradeHistoryKey;
   }

   public boolean isCustomVillager(Villager villager) {
      return villager == null ? false : villager.getPersistentDataContainer().has(this.villagerTypeKey, PersistentDataType.STRING);
   }

   public String getVillagerType(Villager villager) {
      return !this.isCustomVillager(villager) ? null : (String)villager.getPersistentDataContainer().get(this.villagerTypeKey, PersistentDataType.STRING);
   }

   public Villager spawnCustomVillager(Location loc, String type) {
      Villager villager = (Villager)loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
      this.setupVillager(villager, type, null);
      return villager;
   }

   public void setupVillager(Villager villager, String type, String tradeHistory) {
      villager.setAI(true);
      villager.setBreed(false);
      villager.setAdult();
      villager.setRemoveWhenFarAway(false);
      villager.setPersistent(true);
      PersistentDataContainer pdc = villager.getPersistentDataContainer();
      pdc.set(this.villagerTypeKey, PersistentDataType.STRING, type);
      if (tradeHistory != null && !tradeHistory.isEmpty()) {
         pdc.set(this.tradeHistoryKey, PersistentDataType.STRING, tradeHistory);
      } else if (!pdc.has(this.tradeHistoryKey, PersistentDataType.STRING)) {
         pdc.set(this.tradeHistoryKey, PersistentDataType.STRING, "");
      }

      List<MerchantRecipe> recipes = new ArrayList<>();
      switch (type) {
         case "mace_1": {
            villager.setCustomName(ChatColor.translateAlternateColorCodes('&', "&e&lMace Artisan &7(Triad I)"));
            villager.setCustomNameVisible(true);
            villager.setProfession(Profession.WEAPONSMITH);
            villager.setVillagerType(Type.PLAINS);
            ItemStack mace1 = CustomItemManager.getItemById("mace_1");
            MerchantRecipe recipe0 = new MerchantRecipe(mace1, 999999);
            recipe0.addIngredient(new ItemStack(Material.EMERALD, 64));
            recipe0.addIngredient(new ItemStack(Material.DIAMOND, 32));
            recipes.add(recipe0);
            ItemStack helm = CustomItemManager.getItemById("netherite_helmet_mythic");
            MerchantRecipe recipe1 = new MerchantRecipe(helm, 999999);
            recipe1.addIngredient(new ItemStack(Material.EMERALD, 64));
            recipe1.addIngredient(new ItemStack(Material.NETHERITE_SCRAP, 32));
            recipes.add(recipe1);
            break;
         }
         case "mace_2": {
            villager.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lMace Warden &7(Triad II)"));
            villager.setCustomNameVisible(true);
            villager.setProfession(Profession.ARMORER);
            villager.setVillagerType(Type.TAIGA);
            ItemStack mace2 = CustomItemManager.getItemById("mace_limited");
            MerchantRecipe recipe0 = new MerchantRecipe(mace2, 999999);
            recipe0.addIngredient(new ItemStack(Material.EMERALD, 64));
            recipe0.addIngredient(new ItemStack(Material.DIAMOND, 32));
            recipes.add(recipe0);
            ItemStack chest = CustomItemManager.getItemById("netherite_chestplate_mythic");
            MerchantRecipe recipe1 = new MerchantRecipe(chest, 999999);
            recipe1.addIngredient(new ItemStack(Material.EMERALD, 64));
            recipe1.addIngredient(new ItemStack(Material.NETHERITE_SCRAP, 48));
            recipes.add(recipe1);
            ItemStack legs = CustomItemManager.getItemById("netherite_leggings_mythic");
            MerchantRecipe recipe2 = new MerchantRecipe(legs, 999999);
            recipe2.addIngredient(new ItemStack(Material.EMERALD, 64));
            recipe2.addIngredient(new ItemStack(Material.NETHERITE_SCRAP, 40));
            recipes.add(recipe2);
            break;
         }
         case "mace_3": {
            villager.setCustomName(ChatColor.translateAlternateColorCodes('&', "&b&lMace Champion &7(Triad III)"));
            villager.setCustomNameVisible(true);
            villager.setProfession(Profession.TOOLSMITH);
            villager.setVillagerType(Type.DESERT);
            ItemStack mace3 = CustomItemManager.getItemById("mace_3");
            MerchantRecipe recipe0 = new MerchantRecipe(mace3, 999999);
            recipe0.addIngredient(new ItemStack(Material.EMERALD, 64));
            recipe0.addIngredient(new ItemStack(Material.DIAMOND, 32));
            recipes.add(recipe0);
            ItemStack boots = CustomItemManager.getItemById("netherite_boots_mythic");
            MerchantRecipe recipe1 = new MerchantRecipe(boots, 999999);
            recipe1.addIngredient(new ItemStack(Material.EMERALD, 64));
            recipe1.addIngredient(new ItemStack(Material.NETHERITE_SCRAP, 32));
            recipes.add(recipe1);
            break;
         }
         case "energy_games": {
            villager.setCustomName(ChatColor.translateAlternateColorCodes('&', "&a&lEnergy Games Master"));
            villager.setCustomNameVisible(true);
            villager.setProfession(Profession.CLERIC);
            villager.setVillagerType(Type.SAVANNA);
            ItemStack tracker = CustomItemManager.getItemById("player_tracker");
            ItemStack energyBottle = CustomItemManager.getItemById("energy_bottle");
            if (energyBottle == null) {
               energyBottle = new ItemStack(Material.EXPERIENCE_BOTTLE, 10);
            }

            MerchantRecipe recipe0 = new MerchantRecipe(energyBottle, 999999);
            recipe0.addIngredient(tracker);
            recipes.add(recipe0);
         }
      }

      villager.setRecipes(recipes);
   }

   public boolean hasPlayerTraded(Villager villager, int tradeIndex, UUID playerUuid) {
      if (villager != null && playerUuid != null) {
         PersistentDataContainer pdc = villager.getPersistentDataContainer();
         String raw = (String)pdc.get(this.tradeHistoryKey, PersistentDataType.STRING);
         if (raw != null && !raw.isEmpty()) {
            String token = tradeIndex + ":" + playerUuid.toString();
            return raw.contains(token);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void recordPlayerTrade(Villager villager, int tradeIndex, UUID playerUuid) {
      if (villager != null && playerUuid != null) {
         PersistentDataContainer pdc = villager.getPersistentDataContainer();
         String raw = (String)pdc.get(this.tradeHistoryKey, PersistentDataType.STRING);
         if (raw == null) {
            raw = "";
         }

         String token = tradeIndex + ":" + playerUuid.toString();
         if (!raw.contains(token)) {
            raw = raw.isEmpty() ? token : raw + ";" + token;
            pdc.set(this.tradeHistoryKey, PersistentDataType.STRING, raw);
         }
      }
   }

   public ItemStack convertToSoul(Villager villager) {
      String type = this.getVillagerType(villager);
      if (type == null) {
         return null;
      }

      PersistentDataContainer pdc = villager.getPersistentDataContainer();
      String tradeHistory = (String)pdc.get(this.tradeHistoryKey, PersistentDataType.STRING);
      if (tradeHistory == null) {
         tradeHistory = "";
      }

      ItemStack soul = CustomItemManager.getItemById("villager_soul");
      if (soul == null) {
         soul = new ItemStack(Material.NETHER_STAR);
      }

      ItemMeta meta = soul.getItemMeta();
      if (meta != null) {
         String title = villager.getCustomName() != null ? villager.getCustomName() : type;
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lVillager Soul &8(" + title + "&8)"));
         List<String> lore = new ArrayList<>();
         lore.add(ChatColor.GRAY + "Captured soul of a specialized merchant.");
         lore.add(ChatColor.YELLOW + "Shift + Right-Click block to release.");
         lore.add(ChatColor.DARK_GRAY + "Retains all trade history and limits.");
         meta.setLore(lore);
         PersistentDataContainer itemPdc = meta.getPersistentDataContainer();
         itemPdc.set(CustomItemManager.getVillagerSoulKey(), PersistentDataType.STRING, type);
         itemPdc.set(CustomItemManager.getVillagerDataKey(), PersistentDataType.STRING, tradeHistory);
         soul.setItemMeta(meta);
      }

      return soul;
   }

   public Villager restoreFromSoul(Location loc, ItemStack soulItem) {
      if (soulItem != null && soulItem.hasItemMeta()) {
         PersistentDataContainer itemPdc = soulItem.getItemMeta().getPersistentDataContainer();
         if (!itemPdc.has(CustomItemManager.getVillagerSoulKey(), PersistentDataType.STRING)) {
            return null;
         }

         String type = (String)itemPdc.get(CustomItemManager.getVillagerSoulKey(), PersistentDataType.STRING);
         String tradeHistory = (String)itemPdc.get(CustomItemManager.getVillagerDataKey(), PersistentDataType.STRING);
         Villager villager = (Villager)loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
         this.setupVillager(villager, type, tradeHistory);
         loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0.0, 1.0, 0.0), 30, 0.4, 0.6, 0.4, 0.05);
         loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0F, 1.4F);
         return villager;
      } else {
         return null;
      }
   }
}
