package dev.xoperr.blissgems.utils;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class OraxenGemFixer {
   public static final String FIX_ON_JOIN_PATH = "oraxen-items.fix-on-join";

   private OraxenGemFixer() {
   }

   public static boolean isFixOnJoinEnabled(BlissGems plugin) {
      return plugin.getConfig().getBoolean("oraxen-items.fix-on-join", true);
   }

   public static void setFixOnJoinEnabled(BlissGems plugin, boolean enabled) {
      plugin.getConfig().set("oraxen-items.fix-on-join", enabled);
      plugin.saveConfig();
   }

   public static int fixInventory(BlissGems plugin, Player player) {
      PlayerInventory inv = player.getInventory();
      int fixed = 0;

      for (int slot = 0; slot < inv.getSize(); slot++) {
         ItemStack replacement = rebuildIfLegacy(plugin, player, inv.getItem(slot));
         if (replacement != null) {
            inv.setItem(slot, replacement);
            fixed++;
         }
      }

      return fixed;
   }

   private static ItemStack rebuildIfLegacy(BlissGems plugin, Player player, ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         String id = CustomItemManager.getIdByItem(item);
         if (id == null) {
            return null;
         }

         if (!GemCosmetics.has(id) || CustomItemManager.isGoldSkinned(item)) {
            return null;
         }

         if (GemType.isGem(id)) {
            CustomItemManager.updateGemTexture(item, plugin.getEnergyManager().getEnergy(player));
            return null;
         }

         int energy = -1;
         ItemStack fresh = CustomItemManager.getItemById(id, energy);
         if (fresh == null) {
            return null;
         }

         fresh.setAmount(item.getAmount());
         return fresh;
      } else {
         return null;
      }
   }
}
