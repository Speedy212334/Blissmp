package dev.xoperr.blissgems.core.api.text;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface InventoryTextProvider {
   String getActionBarText(Player var1, ItemStack var2, ItemStack var3);

   default boolean shouldUpdate(Player player, ItemStack mainHand, ItemStack offHand) {
      return true;
   }

   default int getPriority() {
      return 0;
   }
}
