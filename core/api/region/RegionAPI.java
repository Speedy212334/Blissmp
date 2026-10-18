package dev.xoperr.blissgems.core.api.region;

import dev.xoperr.blissgems.core.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RegionAPI {
   private static RegionManager manager;

   public static void initialize(RegionManager regionManager) {
      manager = regionManager;
   }

   public static boolean areGemsDisabled(Player player) {
      checkInitialized();
      return manager.areGemsDisabled(player);
   }

   public static boolean areGemsDisabled(Location location) {
      checkInitialized();
      return manager.areGemsDisabled(location);
   }

   public static String getDisabledMessage() {
      checkInitialized();
      return manager.getDisabledMessage();
   }

   public static boolean isWorldGuardEnabled() {
      checkInitialized();
      return manager.isWorldGuardEnabled();
   }

   private static void checkInitialized() {
      if (manager == null) {
         throw new IllegalStateException("RegionAPI has not been initialized!");
      }
   }
}
