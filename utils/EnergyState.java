package dev.xoperr.blissgems.utils;

public enum EnergyState {
   BROKEN(0, 0, "Broken", "§c§lBROKEN"),
   RUINED(1, 1, "Ruined", "§4Ruined"),
   SHATTERED(2, 2, "Shattered", "§6Shattered"),
   CRACKED(3, 3, "Cracked", "§eCracked"),
   SCRATCHED(4, 4, "Scratched", "§aScratched"),
   PRISTINE(5, 5, "Pristine", "§bPristine"),
   PRISTINE_PLUS_1(6, 6, "Pristine +1", "§d§lPristine §5+1"),
   PRISTINE_PLUS_2(7, 7, "Pristine +2", "§d§lPristine §5+2"),
   PRISTINE_PLUS_3(8, 8, "Pristine +3", "§d§lPristine §5+3"),
   PRISTINE_PLUS_4(9, 9, "Pristine +4", "§d§lPristine §5+4"),
   PRISTINE_PLUS_5(10, 10, "Pristine +5", "§d§lPristine §5+5");

   private final int minEnergy;
   private final int maxEnergy;
   private final String name;
   private final String displayName;

   EnergyState(int minEnergy, int maxEnergy, String name, String displayName) {
      this.minEnergy = minEnergy;
      this.maxEnergy = maxEnergy;
      this.name = name;
      this.displayName = displayName;
   }

   public int getMinEnergy() {
      return this.minEnergy;
   }

   public int getMaxEnergy() {
      return this.maxEnergy;
   }

   public String getName() {
      return this.name;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public static EnergyState fromEnergy(int energy) {
      energy = Math.max(0, Math.min(10, energy));

      for (EnergyState state : values()) {
         if (energy >= state.minEnergy && energy <= state.maxEnergy) {
            return state;
         }
      }

      return BROKEN;
   }

   public boolean passivesActive() {
      return this.minEnergy >= 1;
   }

   public boolean abilitiesUsable() {
      return this.minEnergy >= 2;
   }

   public boolean isEnhanced() {
      return this.minEnergy > 5;
   }

   public boolean isMaxEnergy() {
      return this == PRISTINE_PLUS_5;
   }
}
