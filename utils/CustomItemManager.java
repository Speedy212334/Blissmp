package dev.xoperr.blissgems.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomItemManager {
   public static final Material GEM_MATERIAL = Material.NAUTILUS_SHELL;
   private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
   private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
   private static final Map<String, CustomItemManager.CustomItemData> ITEM_REGISTRY = new HashMap<>();
   private static final Set<Material> GEM_MATERIALS = EnumSet.noneOf(Material.class);
   private static NamespacedKey ITEM_ID_KEY;
   private static NamespacedKey UNDROPPABLE_KEY;
   private static NamespacedKey OWNER_KEY;
   private static NamespacedKey GOLD_INSTANCE_KEY;
   private static NamespacedKey DOUBLE_DURABILITY_KEY;
   private static NamespacedKey MYTHIC_KEY;
   private static NamespacedKey MACE_LIMITED_KEY;
   private static NamespacedKey VILLAGER_SOUL_KEY;
   private static NamespacedKey VILLAGER_DATA_KEY;

   public static void initialize(JavaPlugin plugin) {
      ITEM_ID_KEY = new NamespacedKey(plugin, "item_id");
      UNDROPPABLE_KEY = new NamespacedKey(plugin, "locked_item");
      OWNER_KEY = new NamespacedKey(plugin, "item_owner");
      GOLD_INSTANCE_KEY = new NamespacedKey(plugin, "gold_instance_id");
      DOUBLE_DURABILITY_KEY = new NamespacedKey(plugin, "double_durability");
      MYTHIC_KEY = new NamespacedKey(plugin, "special_mythic");
      MACE_LIMITED_KEY = new NamespacedKey(plugin, "mace_limited");
      VILLAGER_SOUL_KEY = new NamespacedKey(plugin, "villager_soul");
      VILLAGER_DATA_KEY = new NamespacedKey(plugin, "villager_data");
      GemCosmetics.initialize(plugin);
   }

   public static boolean isGoldSkinned(ItemStack item) {
      String id = getIdByItem(item);
      return id != null && !"gold_gem_t1".equals(id) && getGoldInstanceId(item) != null;
   }

   public static void setGoldInstanceId(ItemStack item, UUID id) {
      if (GOLD_INSTANCE_KEY != null && item != null && id != null) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.getPersistentDataContainer().set(GOLD_INSTANCE_KEY, PersistentDataType.STRING, id.toString());
            item.setItemMeta(meta);
         }
      }
   }

   public static UUID getGoldInstanceId(ItemStack item) {
      if (GOLD_INSTANCE_KEY != null && item != null) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return null;
         }

         String raw = (String)meta.getPersistentDataContainer().get(GOLD_INSTANCE_KEY, PersistentDataType.STRING);
         if (raw == null) {
            return null;
         }

         try {
            return UUID.fromString(raw);
         } catch (IllegalArgumentException e) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static UUID ensureGoldInstanceId(ItemStack item) {
      UUID existing = getGoldInstanceId(item);
      if (existing != null) {
         return existing;
      }

      if (GOLD_INSTANCE_KEY != null && item != null) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return null;
         }

         UUID id = UUID.randomUUID();
         meta.getPersistentDataContainer().set(GOLD_INSTANCE_KEY, PersistentDataType.STRING, id.toString());
         item.setItemMeta(meta);
         return id;
      } else {
         return null;
      }
   }

   public static boolean setOwner(ItemStack item, UUID owner) {
      if (OWNER_KEY != null && owner != null && item != null && item.getType() != Material.AIR) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         }

         if (meta.getPersistentDataContainer().has(OWNER_KEY, PersistentDataType.STRING)) {
            return false;
         }

         meta.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.STRING, owner.toString());
         item.setItemMeta(meta);
         return true;
      } else {
         return false;
      }
   }

   public static UUID getOwner(ItemStack item) {
      if (OWNER_KEY != null && item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return null;
         }

         String raw = (String)meta.getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
         if (raw == null) {
            return null;
         }

         try {
            return UUID.fromString(raw);
         } catch (IllegalArgumentException ex) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean hasOwner(ItemStack item) {
      return getOwner(item) != null;
   }

   private static void registerItem(String id, Material material, int customModelData, String displayName) {
      ITEM_REGISTRY.put(id, new CustomItemManager.CustomItemData(material, customModelData, displayName, null));
   }

   private static void registerItem(String id, Material material, int customModelData, String displayName, List<String> lore) {
      ITEM_REGISTRY.put(id, new CustomItemManager.CustomItemData(material, customModelData, displayName, lore));
      if (id.contains("_gem_t")) {
         GEM_MATERIALS.add(material);
      }
   }

   public static void registerAddonItem(String id, Material material, int customModelData, String displayName, List<String> lore) {
      if (ITEM_REGISTRY.containsKey(id)) {
         Bukkit.getLogger().warning("[BlissGems] Custom item id '" + id + "' already registered — skipping duplicate.");
      } else {
         ITEM_REGISTRY.put(id, new CustomItemManager.CustomItemData(material, customModelData, displayName, lore));
         if (id.contains("_gem_t")) {
            GEM_MATERIALS.add(material);
         }
      }
   }

   public static boolean mayBeGem(ItemStack item) {
      return item != null && GEM_MATERIALS.contains(item.getType());
   }

   public static String getIdByItem(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return null;
         } else {
            return meta.getPersistentDataContainer().has(ITEM_ID_KEY, PersistentDataType.STRING)
               ? (String)meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING)
               : null;
         }
      } else {
         return null;
      }
   }

   public static ItemStack getItemById(String id) {
      return getItemById(id, -1);
   }

   public static ItemStack getItemById(String id, int energy) {
      CustomItemManager.CustomItemData data = ITEM_REGISTRY.get(id);
      if (data == null) {
         return null;
      }

      ItemStack item = new ItemStack(data.material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      }

      int customModelData = data.customModelData;
      if (GemType.isGem(id) && energy >= 0) {
         customModelData = getPristineModelData(data.customModelData, energy, id);
      }

      meta.setCustomModelData(customModelData);
      meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, id);
      if (GemType.isGem(id)) {
         meta.getPersistentDataContainer().set(UNDROPPABLE_KEY, PersistentDataType.BYTE, (byte)1);
      }

      if (GemType.isGem(id) && !id.startsWith("gold_gem")) {
         renderGem(meta, id, data, energy);
      } else if (!GemCosmetics.apply(meta, id)) {
         meta.setDisplayName(data.displayName);
         if (GemType.isGem(id)) {
            applyPristinePlusVisuals(meta, data.lore, energy);
         } else if (data.lore != null && !data.lore.isEmpty()) {
            meta.setLore(data.lore);
         }
      } else if (GemType.isGem(id)) {
         applyEnhancedGlint(meta, energy);
      }

      applySignatureEnchants(id, meta);
      item.setItemMeta(meta);
      if ("gold_gem_t1".equals(id)) {
         ensureGoldInstanceId(item);
      }

      return item;
   }

   private static void applySignatureEnchants(String id, ItemMeta meta) {
      if ("prismatic_edge".equals(id)) {
         meta.addEnchant(Enchantment.SHARPNESS, 7, true);
         meta.addEnchant(Enchantment.MENDING, 1, true);
         meta.addEnchant(Enchantment.UNBREAKING, 3, true);
         if (MYTHIC_KEY != null) {
            meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "prismatic_edge");
         }
      } else if ("netherite_helmet_mythic".equals(id)) {
         meta.addEnchant(Enchantment.PROTECTION, 3, true);
         meta.addEnchant(Enchantment.UNBREAKING, 3, true);
         meta.addEnchant(Enchantment.MENDING, 1, true);
         meta.addEnchant(Enchantment.RESPIRATION, 3, true);
         meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
         if (DOUBLE_DURABILITY_KEY != null) {
            meta.getPersistentDataContainer().set(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE, (byte)1);
         }

         if (MYTHIC_KEY != null) {
            meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "netherite_helmet");
         }
      } else if ("netherite_chestplate_mythic".equals(id)) {
         meta.addEnchant(Enchantment.PROTECTION, 3, true);
         meta.addEnchant(Enchantment.UNBREAKING, 3, true);
         meta.addEnchant(Enchantment.MENDING, 1, true);
         if (DOUBLE_DURABILITY_KEY != null) {
            meta.getPersistentDataContainer().set(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE, (byte)1);
         }

         if (MYTHIC_KEY != null) {
            meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "netherite_chestplate");
         }
      } else if ("netherite_leggings_mythic".equals(id)) {
         meta.addEnchant(Enchantment.PROTECTION, 3, true);
         meta.addEnchant(Enchantment.UNBREAKING, 3, true);
         meta.addEnchant(Enchantment.MENDING, 1, true);
         if (Enchantment.SWIFT_SNEAK != null) {
            meta.addEnchant(Enchantment.SWIFT_SNEAK, 3, true);
         }

         if (DOUBLE_DURABILITY_KEY != null) {
            meta.getPersistentDataContainer().set(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE, (byte)1);
         }

         if (MYTHIC_KEY != null) {
            meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "netherite_leggings");
         }
      } else if ("netherite_boots_mythic".equals(id)) {
         meta.addEnchant(Enchantment.PROTECTION, 3, true);
         meta.addEnchant(Enchantment.UNBREAKING, 3, true);
         meta.addEnchant(Enchantment.MENDING, 1, true);
         meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
         meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
         if (Enchantment.SOUL_SPEED != null) {
            meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
         }

         if (DOUBLE_DURABILITY_KEY != null) {
            meta.getPersistentDataContainer().set(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE, (byte)1);
         }

         if (MYTHIC_KEY != null) {
            meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "netherite_boots");
         }
      } else if ("mace_1".equals(id)) {
         if (MYTHIC_KEY != null) {
            meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "mace_1");
         }
      } else if ("mace_limited".equals(id)) {
         if (MACE_LIMITED_KEY != null) {
            meta.getPersistentDataContainer().set(MACE_LIMITED_KEY, PersistentDataType.BYTE, (byte)1);
         }

         if (MYTHIC_KEY != null) {
            meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "mace_limited");
         }
      } else if ("mace_3".equals(id) && MYTHIC_KEY != null) {
         meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "mace_3");
      }
   }

   private static int getPristineModelData(int baseModelData, int energy, String itemId) {
      if (itemId != null && itemId.startsWith("gold_gem")) {
         return baseModelData;
      }

      int pristineOffset = energy <= 2 ? 0 : (energy <= 5 ? 20 : (energy <= 8 ? 30 : 40));
      return baseModelData + pristineOffset;
   }

   private static void applyEnhancedGlint(ItemMeta meta, int energy) {
      if (EnergyState.fromEnergy(energy).isEnhanced()) {
         meta.addEnchant(Enchantment.UNBREAKING, 1, true);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
      } else {
         meta.removeEnchant(Enchantment.UNBREAKING);
      }
   }

   public static int migrateGemMaterials(Inventory inv) {
      int changed = 0;

      for (int i = 0; i < inv.getSize(); i++) {
         ItemStack item = inv.getItem(i);
         if (item != null && item.getType() != GEM_MATERIAL) {
            String id = getIdByItem(item);
            if (id != null && (GemType.isGem(id) || id.startsWith("gold_gem") || isGoldSkinned(item))) {
               inv.setItem(i, item.withType(GEM_MATERIAL));
               changed++;
            }
         }
      }

      return changed;
   }

   public static void updateGemTexture(ItemStack item, int energy) {
      if (mayBeGem(item) && item.hasItemMeta()) {
         String id = getIdByItem(item);
         if (id != null && GemType.isGem(id)) {
            if (!id.startsWith("gold_gem") && !isGoldSkinned(item)) {
               CustomItemManager.CustomItemData data = ITEM_REGISTRY.get(id);
               if (data != null) {
                  ItemMeta before = item.getItemMeta();
                  ItemMeta meta = item.getItemMeta();
                  if (meta != null) {
                     int pristineModelData = getPristineModelData(data.customModelData, energy, id);
                     meta.setCustomModelData(pristineModelData);
                     renderGem(meta, id, data, energy);
                     if (!meta.equals(before)) {
                        item.setItemMeta(meta);
                     }
                  }
               }
            }
         }
      }
   }

   private static void renderGem(ItemMeta meta, String id, CustomItemManager.CustomItemData data, int energy) {
      List<Component> kept = new ArrayList<>();
      List<Component> old = meta.lore();
      if (old != null) {
         for (Component c : old) {
            String plain = PLAIN.serialize(c);
            if (plain.startsWith("Owner: ") || plain.startsWith("Charges: ")) {
               kept.add(c);
            }
         }
      }

      Component name = GemCosmetics.name(id);
      meta.displayName(name != null ? name : legacy(data.displayName));
      List<Component> lore = new ArrayList<>();
      List<Component> base = GemCosmetics.lore(id);
      if (base != null) {
         lore.addAll(base);
      } else if (data.lore != null) {
         for (String line : data.lore) {
            lore.add(legacy(line));
         }
      }

      if (energy >= 0) {
         lore.add(0, energyLine(energy));
      }

      if (!kept.isEmpty()) {
         lore.add(Component.empty());
         lore.addAll(kept);
      }

      meta.lore(lore.isEmpty() ? null : lore);
      applyEnhancedGlint(meta, energy);
   }

   public static Component energyLine(int energy) {
      EnergyState state = EnergyState.fromEnergy(energy);
      String color = state == EnergyState.PRISTINE ? "§b" : state.getDisplayName().substring(0, 2);
      return legacy(" §f(" + color + state.getName() + "§f)");
   }

   private static Component legacy(String text) {
      return LEGACY.deserialize(text).decorationIfAbsent(TextDecoration.ITALIC, State.FALSE);
   }

   private static void applyPristinePlusVisuals(ItemMeta meta, List<String> baseLore, int energy) {
      EnergyState state = EnergyState.fromEnergy(energy);
      boolean enhanced = state.isEnhanced();
      ArrayList<String> lore = new ArrayList<>();
      if (baseLore != null) {
         lore.addAll(baseLore);
      }

      if (enhanced) {
         if (!lore.isEmpty()) {
            lore.add("");
         }

         lore.add("§5✮ " + state.getDisplayName());
      }

      meta.setLore(lore.isEmpty() ? null : lore);
      if (enhanced) {
         meta.addEnchant(Enchantment.UNBREAKING, 1, true);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
      } else {
         meta.removeEnchant(Enchantment.UNBREAKING);
      }
   }

   public static boolean isCustomItem(ItemStack item) {
      return getIdByItem(item) != null;
   }

   public static boolean markAsUndroppable(ItemStack item) {
      if (item != null && item.getType() != Material.AIR && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         }

         PersistentDataContainer container = meta.getPersistentDataContainer();
         if (container.has(UNDROPPABLE_KEY, PersistentDataType.BYTE) && (Byte)container.get(UNDROPPABLE_KEY, PersistentDataType.BYTE) == 1) {
            return false;
         }

         container.set(UNDROPPABLE_KEY, PersistentDataType.BYTE, (byte)1);
         item.setItemMeta(meta);
         return true;
      } else {
         return false;
      }
   }

   public static boolean isUndroppable(ItemStack item) {
      if (item == null || item.getType() == Material.AIR) {
         return false;
      }

      if (!item.hasItemMeta()) {
         return false;
      }

      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return false;
      }

      PersistentDataContainer container = meta.getPersistentDataContainer();
      return container.has(UNDROPPABLE_KEY, PersistentDataType.BYTE) && (Byte)container.get(UNDROPPABLE_KEY, PersistentDataType.BYTE) == 1;
   }

   public static boolean isDoubleDurability(ItemStack item) {
      if (item != null && item.hasItemMeta() && DOUBLE_DURABILITY_KEY != null) {
         Byte b = (Byte)item.getItemMeta().getPersistentDataContainer().get(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE);
         return b != null && b == 1;
      } else {
         return false;
      }
   }

   public static boolean isLimitedMace(ItemStack item) {
      if (item != null && item.hasItemMeta() && MACE_LIMITED_KEY != null) {
         Byte b = (Byte)item.getItemMeta().getPersistentDataContainer().get(MACE_LIMITED_KEY, PersistentDataType.BYTE);
         return b != null && b == 1;
      } else {
         return false;
      }
   }

   public static boolean isMythic(ItemStack item) {
      return item != null && item.hasItemMeta() && MYTHIC_KEY != null
         ? item.getItemMeta().getPersistentDataContainer().has(MYTHIC_KEY, PersistentDataType.STRING)
         : false;
   }

   public static String getMythicId(ItemStack item) {
      return item != null && item.hasItemMeta() && MYTHIC_KEY != null
         ? (String)item.getItemMeta().getPersistentDataContainer().get(MYTHIC_KEY, PersistentDataType.STRING)
         : null;
   }

   public static NamespacedKey getVillagerSoulKey() {
      return VILLAGER_SOUL_KEY;
   }

   public static NamespacedKey getVillagerDataKey() {
      return VILLAGER_DATA_KEY;
   }

   public static NamespacedKey getDoubleDurabilityKey() {
      return DOUBLE_DURABILITY_KEY;
   }

   public static NamespacedKey getMythicKey() {
      return MYTHIC_KEY;
   }

   public static NamespacedKey getMaceLimitedKey() {
      return MACE_LIMITED_KEY;
   }

   static {
      registerItem(
         "mace_1",
         Material.MACE,
         6001,
         "§e§lHeavy Mace",
         List.of("§7A perfectly balanced battle mace.", "", "§a✔ Fully Enchantable", "§8Part of the original triad.")
      );
      registerItem(
         "mace_limited",
         Material.MACE,
         6002,
         "§c§lTempered Mace",
         List.of("§7A heavy, restricted battle mace.", "", "§c✖ Density max level II", "§c✖ Wind Burst & Breach Restricted", "§6⚡ 60s Combat Cooldown")
      );
      registerItem("mace_3", Material.MACE, 6003, "§b§lOvercharged Mace", List.of("§7Surging with raw kinetic force.", "", "§b✦ High Impact Triad Weapon"));
      registerItem(
         "netherite_helmet_mythic",
         Material.NETHERITE_HELMET,
         7001,
         "§6§lMythic Netherite Helmet",
         List.of("§7Forged with ancient durability.", "", "§a✦ Protection III", "§a✦ Maxed Secondary Enchants", "§d✦ Double Durability (814 Max)")
      );
      registerItem(
         "netherite_chestplate_mythic",
         Material.NETHERITE_CHESTPLATE,
         7002,
         "§6§lMythic Netherite Chestplate",
         List.of("§7Forged with ancient durability.", "", "§a✦ Protection III", "§a✦ Maxed Secondary Enchants", "§d✦ Double Durability (1184 Max)")
      );
      registerItem(
         "netherite_leggings_mythic",
         Material.NETHERITE_LEGGINGS,
         7003,
         "§6§lMythic Netherite Leggings",
         List.of("§7Forged with ancient durability.", "", "§a✦ Protection III", "§a✦ Maxed Secondary Enchants", "§d✦ Double Durability (1110 Max)")
      );
      registerItem(
         "netherite_boots_mythic",
         Material.NETHERITE_BOOTS,
         7004,
         "§6§lMythic Netherite Boots",
         List.of("§7Forged with ancient durability.", "", "§a✦ Protection III", "§a✦ Maxed Secondary Enchants", "§d✦ Double Durability (962 Max)")
      );
      registerItem(
         "player_tracker",
         Material.COMPASS,
         8001,
         "§a§lPlayer Tracker",
         List.of("§7Points towards the nearest player.", "", "§eTrade with the Energy Games Villager", "§efor raw Energy.")
      );
      registerItem(
         "villager_soul",
         Material.NETHER_STAR,
         8002,
         "§d§lVillager Soul",
         List.of("§7An essence encapsulating a merchant.", "", "§eShift + Right-Click block to release.", "§8Preserves all trade history and limits.")
      );
      registerItem(
         "astra_gem_t1",
         GEM_MATERIAL,
         1001,
         "§d§lASTRA GEM",
         List.of(
            "§f§lMANAGE THE TIDES OF THE COSMOS",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Soul Capture & Soul Healing",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l\ud83d\udd2a ASTRAL DAGGERS",
            "§7Conjure 3 phantom daggers, launch each one by one",
            "",
            "§8Upgrade to Tier 2 for Astral Projection,",
            "§8Dimensional Drift & Void!"
         )
      );
      registerItem(
         "fire_gem_t1",
         GEM_MATERIAL,
         1002,
         "§d§lFIRE GEM",
         List.of(
            "§f§lMANIPULATE FIRE",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Fire Resistance",
            "§7- Flame & Fire Aspect on weapons",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l\ud83d\udd25 CHARGED FIREBALL",
            "§7Hold to charge, release to fire",
            "§7Stand on obsidian to prevent charge decay",
            "",
            "§8Upgrade to Tier 2 for Campfire,",
            "§8Crisp & Meteor Shower!"
         )
      );
      registerItem(
         "flux_gem_t1",
         GEM_MATERIAL,
         1003,
         "§d§lFLUX GEM",
         List.of(
            "§f§lWITH GREAT POWER COMES GREAT RESPONSIBILITY",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Immune to Weakness, Slowness & Hunger",
            "§7- Shocking Chance (stun on arrow hits)",
            "",
            "§b\ud83c\udf1f §b§lABILITY",
            "§7- Conduction (/bliss conduction → nearest copper block)",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l✠ FLUX BEAM",
            "§7Charge and fire a powerful beam",
            "§7Charged beam deals massive armor damage",
            "",
            "§8Upgrade to Tier 2 for Ground & more!"
         )
      );
      registerItem(
         "life_gem_t1",
         GEM_MATERIAL,
         1004,
         "§d§lLIFE GEM",
         List.of(
            "§f§lCONTROL THE BALANCE OF LIFE",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Wither immunity",
            "§7- Continuous healing",
            "§7- Unbreaking on tools",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l\ud83d\udc98 HEART DRAINER",
            "§7Siphon health from your enemies",
            "",
            "§8Upgrade to Tier 2 for Life Circle,",
            "§8Vitality Vortex & Heart Lock!"
         )
      );
      registerItem(
         "puff_gem_t1",
         GEM_MATERIAL,
         1005,
         "§d§lPUFF GEM",
         List.of(
            "§f§lBE THE BIGGEST BIRD",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- No fall damage",
            "§7- Power & Punch on bows",
            "",
            "§b\ud83c\udf1f §b§lABILITY",
            "§7- Double Jump",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l☁ DASH",
            "§7Dashes in the direction you're looking",
            "§7Deals damage if passing through enemies",
            "",
            "§8Upgrade to Tier 2 for Breezy Bash!"
         )
      );
      registerItem(
         "speed_gem_t1",
         GEM_MATERIAL,
         1006,
         "§d§lSPEED GEM",
         List.of(
            "§f§lWATCH THE WORLD TURN INTO A BLUR",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Speed I & Dolphin's Grace",
            "§7- Efficiency on tools",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l⚡ BLUR",
            "§7Summons successive lightning strikes",
            "§7dealing damage and knockback",
            "",
            "§8Upgrade to Tier 2 for Speed Storm",
            "§8& Terminal Velocity!"
         )
      );
      registerItem(
         "strength_gem_t1",
         GEM_MATERIAL,
         1007,
         "§d§lSTRENGTH GEM",
         List.of(
            "§f§lHAVE THE STRENGTH OF AN ARMY",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Strength I",
            "§7- Sharpness II on weapons",
            "§7- Bloodthorns (more damage at low HP)",
            "",
            "§8No active abilities at Tier 1",
            "§8Upgrade to Tier 2 for Nullify, Frailer",
            "§8& Shadow Stalker!"
         )
      );
      registerItem(
         "wealth_gem_t1",
         GEM_MATERIAL,
         1008,
         "§d§lWEALTH GEM",
         List.of(
            "§f§lFUEL AN EMPIRE",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Luck & Hero of the Village",
            "§7- Mending, Fortune & Looting on tools",
            "§7- Durability Chip (extra armor damage)",
            "",
            "§8No active abilities at Tier 1",
            "§8Upgrade to Tier 2 for Pockets,",
            "§8Unfortunate & more!"
         )
      );
      registerItem(
         "gold_gem_t1",
         GEM_MATERIAL,
         1009,
         "§6§lGOLD GEM",
         List.of(
            "§f§lWATCH THE LINES OF REALITY FRAY AS EIGHT SOULS BECOME ONE",
            "§6(Dormant)",
            "",
            "§6\ud83c\udf1f §6§lPASSIVES",
            "§7- §kunstable power",
            "§7- §kharvested souls",
            "§7- §ksoulbound vessel",
            "§7- §kfraying lines",
            "",
            "§6\ud83c\udf1f §6§lABILITY",
            "§7- §ksundering beam",
            "",
            "§6\ud83c\udf1f §6§lPOWERS",
            "§7- §kchannelled soul",
            "§7- §krepurposing",
            "",
            "§7- §kawakening",
            "§7- §keight as one"
         )
      );
      registerItem(
         "astra_gem_t2",
         GEM_MATERIAL,
         2001,
         "§d§lASTRA GEM",
         List.of(
            "§f§lMANAGE THE TIDES OF THE COSMOS",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Soul Capture & Soul Healing",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l\ud83d\udd2a ASTRAL DAGGERS",
            "§7Conjure 3 phantom daggers, launch each one by one",
            "",
            "§b§l\ud83d\udc7b ASTRAL PROJECTION",
            "§7Scout in spectator mode",
            "§7Sub-abilities: §dSpook §7& §dTag",
            "",
            "§b§l\ud83c\udf00 DIMENSIONAL DRIFT",
            "§7Dash forward through the rift, briefly invisible",
            "",
            "§b§l\ud83d\udd73 DIMENSIONAL VOID",
            "§7Nullify enemy gem abilities in radius"
         )
      );
      registerItem(
         "fire_gem_t2",
         GEM_MATERIAL,
         2002,
         "§d§lFIRE GEM",
         List.of(
            "§f§lMANIPULATE FIRE",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Fire Resistance",
            "§7- Flame & Fire Aspect on weapons",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l\ud83d\udd25 CHARGED FIREBALL",
            "§7Hold to charge, release to fire",
            "§7Stand on obsidian to prevent charge decay",
            "",
            "§b§l\ud83e\udd7e COZY CAMPFIRE",
            "§7Spawns a campfire granting allies Regen IV",
            "",
            "§b§l\ud83e\uddca CRISP",
            "§7Evaporate water, replace blocks with nether",
            "",
            "§b§l\ud83e\udde8 METEOR SHOWER",
            "§7Rain fire on a target area"
         )
      );
      registerItem(
         "flux_gem_t2",
         GEM_MATERIAL,
         2003,
         "§d§lFLUX GEM",
         List.of(
            "§f§lWITH GREAT POWER COMES GREAT RESPONSIBILITY",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Immune to Weakness, Slowness & Hunger",
            "§7- Shocking Chance (stun on arrow hits)",
            "",
            "§b\ud83c\udf1f §b§lABILITY",
            "§7- Conduction (/bliss conduction → nearest copper block)",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l✠ FLUX BEAM",
            "§7Chargeable beam dealing massive armor damage",
            "",
            "§b§l\ud83c\udf00 GROUND",
            "§7Freeze enemies in place",
            "",
            "§b§l\ud83d\udca5 FLASHBANG",
            "§7Blindness and Nausea to enemies in radius",
            "",
            "§b§l⚡ KINETIC BURST",
            "§7Radial knockback with sonic boom"
         )
      );
      registerItem(
         "life_gem_t2",
         GEM_MATERIAL,
         2004,
         "§d§lLIFE GEM",
         List.of(
            "§f§lCONTROL THE BALANCE OF LIFE",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Wither immunity",
            "§7- Continuous healing",
            "§7- Unbreaking on tools",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l\ud83d\udc98 HEART DRAINER",
            "§7Siphon health from your enemies",
            "",
            "§b§l✨ CIRCLE OF LIFE",
            "§7Zone that decreases enemy max HP",
            "§7and increases yours and allies' HP",
            "",
            "§b§l\ud83d\udcab VITALITY VORTEX",
            "§7Grants effects based on your surroundings",
            "",
            "§b§l\ud83d\udd12 HEART LOCK",
            "§7Cap enemy max HP at their current HP"
         )
      );
      registerItem(
         "puff_gem_t2",
         GEM_MATERIAL,
         2005,
         "§d§lPUFF GEM",
         List.of(
            "§f§lBE THE BIGGEST BIRD",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- No fall damage",
            "§7- Power & Punch on bows",
            "",
            "§b\ud83c\udf1f §b§lABILITY",
            "§7- Double Jump",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l☁ DASH",
            "§7Dashes in the direction you're looking",
            "§7Deals damage if passing through enemies",
            "",
            "§b§l⏫ BREEZY BASH",
            "§7Launch an enemy skyward then slam them",
            "",
            "§b§l\ud83c\udf2a GROUP BREEZY BASH",
            "§7Send all nearby enemies flying away"
         )
      );
      registerItem(
         "speed_gem_t2",
         GEM_MATERIAL,
         2006,
         "§d§lSPEED GEM",
         List.of(
            "§f§lWATCH THE WORLD TURN INTO A BLUR",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Speed I & Dolphin's Grace",
            "§7- Efficiency on tools",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l⚡ BLUR",
            "§7Summons successive lightning strikes",
            "§7dealing damage and knockback",
            "",
            "§b§l\ud83c\udf29 SPEED STORM",
            "§7Freezes enemies while granting allies",
            "§7Speed and Haste",
            "",
            "§b§l\ud83d\udca8 TERMINAL VELOCITY",
            "§7Speed III + Haste II for a short duration"
         )
      );
      registerItem(
         "strength_gem_t2",
         GEM_MATERIAL,
         2007,
         "§d§lSTRENGTH GEM",
         List.of(
            "§f§lHAVE THE STRENGTH OF AN ARMY",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Strength I",
            "§7- Sharpness V on weapons",
            "§7- Bloodthorns (more damage at low HP)",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l⚔ CHAD STRENGTH",
            "§7Empower your next few hits with bonus damage",
            "",
            "§b§l\ud83d\udc94 FRAILER",
            "§7Apply Weakness I (20s), Slowness & Wither I (40s)",
            "",
            "§b§l\ud83d\udd0d SHADOW STALKER",
            "§7Consume a player head or an owned item",
            "§7to track a player's location",
            "",
            "§b§l\ud83d\udeab NULLIFY",
            "§7Temporarily strip a target's potion effects"
         )
      );
      registerItem(
         "wealth_gem_t2",
         GEM_MATERIAL,
         2008,
         "§d§lWEALTH GEM",
         List.of(
            "§f§lFUEL AN EMPIRE",
            "",
            "§a\ud83c\udf1f §a§lPASSIVES",
            "§7- Luck & Hero of the Village",
            "§7- Mending, Fortune & Looting on tools",
            "§7- Durability Chip & Armor Mend",
            "",
            "§d\ud83c\udf1f §d§lPOWERS",
            "§b§l\ud83d\udcb8 UNFORTUNATE",
            "§7Chance to disable enemy actions",
            "",
            "§b§l\ud83c\udf92 POCKETS",
            "§79 extra inventory slots (/bliss pockets)",
            "",
            "§b§l\ud83d\udd12 ITEM LOCK",
            "§7Lock an enemy's held item temporarily",
            "",
            "§b§l✨ AMPLIFICATION",
            "§7Boost all enchantments for 45s",
            "",
            "§b§l\ud83c\udf40 RICH RUSH",
            "§7Increased drop rates for ~3 min"
         )
      );
      registerItem(
         "gem_upgrader",
         Material.ENCHANTED_BOOK,
         3001,
         "§6§l§nGem Upgrader",
         List.of(
            "§7Right Click to upgrade any Tier 1 gem to Tier 2",
            "",
            "§8Works for ALL gem types:",
            "§5Astra §8• §cFire §8• §bFlux §8• §dLife",
            "§fPuff §8• §aSpeed §8• §6Strength §8• §eWealth"
         )
      );
      registerItem(
         "prismatic_edge",
         Material.NETHERITE_SWORD,
         5001,
         "§b§l§nPrismatic Edge",
         List.of(
            "§7A blade holding the light of every gem.",
            "",
            "§bSneak + Left Click §7to fire a §dprismatic beam",
            "§7that freezes whoever it strikes.",
            "",
            "§7Land §e5 hits in a row §7and every hit",
            "§7after that crits - until you are struck."
         )
      );
      registerItem(
         "restoration_book",
         Material.ENCHANTED_BOOK,
         3002,
         "§5§l§nRestoration Book",
         List.of(
            "§7Right Click while your gem is §c§lBROKEN",
            "§7to begin a §5Restoration Ritual§7.",
            "",
            "§7Your gem is reforged at random and",
            "§7returns at §bPristine§7.",
            "",
            "§8The whole server will know."
         )
      );
      registerItem("energy_bottle", Material.GHAST_TEAR, 4001, "§b§lEnergy Bottle");
      registerItem("gem_trader", Material.EMERALD, 4002, "§2§lGem Trader");
      registerItem("repair_kit", Material.BEACON, 4003, "§d§lRepair Kit");
      registerItem("gem_fragment", Material.PRISMARINE_SHARD, 4004, "§3§lGem Fragment");
      registerItem(
         "wire_fragment",
         Material.LIGHTNING_ROD,
         4006,
         "§6§lWire Fragment",
         List.of("§7A strand torn from the lines of reality.", "", "§8Seven of these and a Fragment Core", "§8summon the Gold Gem.")
      );
      registerItem(
         "fragment_core",
         Material.NETHER_STAR,
         4007,
         "§6§lFragment Core",
         List.of("§7What was left behind after the Golden Dream.", "", "§8Binds seven Wire Fragments into one.")
      );
      registerItem(
         "revive_beacon",
         Material.BEACON,
         4005,
         "§e§lRevive Beacon",
         Arrays.asList("§7A powerful beacon that can revive players", "§7from the brink of death.", "", "§6Right-click to activate")
      );
   }

   private static class CustomItemData {
      final Material material;
      final int customModelData;
      final String displayName;
      final List<String> lore;

      CustomItemData(Material material, int customModelData, String displayName, List<String> lore) {
         this.material = material;
         this.customModelData = customModelData;
         this.displayName = displayName;
         this.lore = lore;
      }
   }
}
