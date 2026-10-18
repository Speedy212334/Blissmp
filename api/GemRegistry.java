package dev.xoperr.blissgems.api;

import java.util.Collection;
import java.util.List;

public interface GemRegistry {
   void registerGem(GemDefinition var1);

   void registerAbilities(String var1, GemAbilityHandler var2);

   void registerPassives(String var1, GemPassiveHandler var2);

   void registerCooldowns(String var1, List<CooldownEntry> var2);

   GemDefinition getGem(String var1);

   GemAbilityHandler getAbilityHandler(String var1);

   GemPassiveHandler getPassiveHandler(String var1);

   List<CooldownEntry> getCooldownEntries(String var1);

   Collection<GemDefinition> getAllGems();

   boolean isRegisteredGem(String var1);

   String gemIdFromItemId(String var1);

   int tierFromItemId(String var1);
}
