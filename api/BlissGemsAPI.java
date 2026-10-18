package dev.xoperr.blissgems.api;

import dev.xoperr.blissgems.managers.AbilityManager;
import dev.xoperr.blissgems.managers.EnergyManager;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.managers.TrustedPlayersManager;
import dev.xoperr.blissgems.utils.ConfigManager;
import org.bukkit.entity.Player;

public interface BlissGemsAPI {
   GemRegistry getGemRegistry();

   AbilityManager getAbilityManager();

   EnergyManager getEnergyManager();

   ConfigManager getConfigManager();

   GemManager getGemManager();

   TrustedPlayersManager getTrustedPlayersManager();

   boolean playerHasGem(Player var1, String var2);
}
