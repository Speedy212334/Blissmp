package dev.xoperr.blissgems.core.api.protection;

import dev.xoperr.blissgems.core.managers.ProtectionManager;
import org.bukkit.inventory.ItemStack;

public class GemProtectionAPI {
   private static ProtectionManager manager;

   public static void initialize(ProtectionManager protectionManager) {
      manager = protectionManager;
   }

   public static boolean markAsGem(ItemStack item) {
      checkInitialized();
      return manager.markAsGem(item);
   }

   public static boolean unmarkGem(ItemStack item) {
      checkInitialized();
      return manager.unmarkGem(item);
   }

   public static boolean isGem(ItemStack item) {
      checkInitialized();
      return manager.isGem(item);
   }

   public static boolean markAsGem(ItemStack item, String gemId, int tier) {
      checkInitialized();
      return manager.markAsGem(item, gemId, tier);
   }

   public static String getGemId(ItemStack item) {
      checkInitialized();
      return manager.getGemId(item);
   }

   public static int getGemTier(ItemStack item) {
      checkInitialized();
      return manager.getGemTier(item);
   }

   private static void checkInitialized() {
      if (manager == null) {
         throw new IllegalStateException("GemProtectionAPI has not been initialized! Ensure XoperrCore is loaded.");
      }
   }
}
