package dev.xoperr.blissgems.core.api.enchant;

import dev.xoperr.blissgems.core.managers.AutoEnchantManager;
import org.bukkit.entity.Player;

public class AutoEnchantAPI {
   private static AutoEnchantManager manager;

   public static void initialize(AutoEnchantManager autoEnchantManager) {
      manager = autoEnchantManager;
   }

   public static void registerRule(EnchantmentRule rule) {
      checkInitialized();
      manager.registerRule(rule);
   }

   public static void unregisterRule(String gemId) {
      checkInitialized();
      manager.unregisterRule(gemId);
   }

   public static EnchantmentRule getRule(String gemId) {
      checkInitialized();
      return manager.getRule(gemId);
   }

   public static boolean hasRule(String gemId) {
      checkInitialized();
      return manager.hasRule(gemId);
   }

   public static void updatePlayer(Player player) {
      checkInitialized();
      manager.updatePlayer(player);
   }

   public static void clearPlayerEnchantments(Player player) {
      checkInitialized();
      manager.clearPlayerEnchantments(player);
   }

   public static void setEnabled(boolean enabled) {
      checkInitialized();
      manager.setEnabled(enabled);
   }

   public static boolean isEnabled() {
      checkInitialized();
      return manager.isEnabled();
   }

   private static void checkInitialized() {
      if (manager == null) {
         throw new IllegalStateException("AutoEnchantAPI has not been initialized! Ensure XoperrCore is loaded.");
      }
   }
}
