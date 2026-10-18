package dev.xoperr.blissgems.utils;

public enum GemType {
   ASTRA("astra", "Astra", "Wield phantom daggers and explore astral dimensions"),
   FIRE("fire", "Fire", "Burn your enemies with charged fireballs and cozy campfires"),
   FLUX("flux", "Flux", "Stun enemies and unleash electric power"),
   LIFE("life", "Life", "Heal yourself and drain the life from enemies"),
   PUFF("puff", "Puff", "Defy gravity with double jumps and immunity to fall damage"),
   SPEED("speed", "Speed", "Move faster and sedate your foes"),
   STRENGTH("strength", "Strength", "Empower your strikes and hunt your enemies"),
   WEALTH("wealth", "Wealth", "Find fortune and fuel an empire");

   private final String id;
   private final String displayName;
   private final String description;

   GemType(String id, String displayName, String description) {
      this.id = id;
      this.displayName = displayName;
      this.description = description;
   }

   public String getId() {
      return this.id;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getDescription() {
      return this.description;
   }

   public String getColor() {
      return switch (this) {
         case ASTRA -> "§d";
         case FIRE -> "§c";
         case FLUX -> "§b";
         case LIFE -> "§a";
         case PUFF -> "§f";
         case SPEED -> "§e";
         case STRENGTH -> "§4";
         case WEALTH -> "§6";
      };
   }

   public static GemType fromOraxenId(String oraxenId) {
      if (oraxenId == null) {
         return null;
      }

      for (GemType type : values()) {
         if (oraxenId.toLowerCase().startsWith(type.id + "_gem")) {
            return type;
         }
      }

      return null;
   }

   public static int getTierFromOraxenId(String oraxenId) {
      if (oraxenId == null) {
         return 1;
      } else {
         return oraxenId.endsWith("_gem_t2") ? 2 : 1;
      }
   }

   public static String buildOraxenId(GemType type, int tier) {
      return type.getId() + "_gem_t" + tier;
   }

   public static boolean isGem(String oraxenId) {
      return oraxenId != null && oraxenId.contains("_gem_t");
   }
}
