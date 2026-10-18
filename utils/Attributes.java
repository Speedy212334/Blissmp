package dev.xoperr.blissgems.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

public final class Attributes {
   private Attributes() {
   }

   public static Attribute maxHealth() {
      return byKey("max_health", "generic.max_health");
   }

   public static Attribute attackSpeed() {
      return byKey("attack_speed", "generic.attack_speed");
   }

   private static Attribute byKey(String... keys) {
      for (String key : keys) {
         Attribute attribute = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
         if (attribute != null) {
            return attribute;
         }
      }

      return null;
   }
}
