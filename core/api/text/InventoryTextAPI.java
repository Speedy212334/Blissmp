package dev.xoperr.blissgems.core.api.text;

import dev.xoperr.blissgems.core.managers.TextManager;
import org.bukkit.entity.Player;

public class InventoryTextAPI {
   private static TextManager manager;

   public static void initialize(TextManager textManager) {
      manager = textManager;
   }

   public static void sendActionBar(Player player, String message) {
      checkInitialized();
      manager.sendActionBar(player, message);
   }

   public static void clearActionBar(Player player) {
      checkInitialized();
      manager.clearActionBar(player);
   }

   public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
      checkInitialized();
      manager.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
   }

   public static void clearTitle(Player player) {
      checkInitialized();
      manager.clearTitle(player);
   }

   public static void registerTextProvider(InventoryTextProvider provider) {
      checkInitialized();
      manager.registerTextProvider(provider);
   }

   public static void unregisterTextProvider(InventoryTextProvider provider) {
      checkInitialized();
      manager.unregisterTextProvider(provider);
   }

   private static void checkInitialized() {
      if (manager == null) {
         throw new IllegalStateException("InventoryTextAPI has not been initialized! Ensure XoperrCore is loaded.");
      }
   }
}
