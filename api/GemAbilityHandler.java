package dev.xoperr.blissgems.api;

import org.bukkit.entity.Player;

public interface GemAbilityHandler {
   void onPrimary(Player var1, int var2);

   default void onSecondary(Player player, int tier) {
   }

   default void onTertiary(Player player, int tier) {
   }

   default void onQuaternary(Player player, int tier) {
   }

   default void onQuinary(Player player, int tier) {
   }

   default void onSenary(Player player, int tier) {
   }

   default void cleanup(Player player) {
   }
}
