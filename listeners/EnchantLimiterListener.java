package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.EnchantLimiterManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class EnchantLimiterListener implements Listener {
   private final BlissGems plugin;
   private final EnchantLimiterManager limiterManager;

   public EnchantLimiterListener(BlissGems plugin, EnchantLimiterManager limiterManager) {
      this.plugin = plugin;
      this.limiterManager = limiterManager;
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onPrepareAnvil(PrepareAnvilEvent event) {
      ItemStack result = event.getResult();
      if (result != null && result.getType() != Material.AIR) {
         boolean modified = false;
         boolean isLimitedMace = CustomItemManager.isLimitedMace(result);

         for (Entry<Enchantment, Integer> entry : new HashMap(result.getEnchantments()).entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();
            String key = ench.getKey().getKey().toLowerCase();
            if (isLimitedMace) {
               if (key.contains("breach") || key.contains("wind_burst")) {
                  result.removeEnchantment(ench);
                  modified = true;
                  continue;
               }

               if (key.contains("density") && level > 2) {
                  result.addEnchantment(ench, 2);
                  modified = true;
                  continue;
               }
            }

            if (this.limiterManager.isEnabled()) {
               int maxLevel = this.limiterManager.getLimit(ench);
               if (maxLevel == 0) {
                  result.removeEnchantment(ench);
                  modified = true;
               } else if (maxLevel > 0 && level > maxLevel) {
                  result.addEnchantment(ench, maxLevel);
                  modified = true;
               }
            }
         }

         if (modified) {
            event.setResult(result);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onEnchantItem(EnchantItemEvent event) {
      ItemStack item = event.getItem();
      boolean isLimitedMace = CustomItemManager.isLimitedMace(item);
      Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();

      for (Entry<Enchantment, Integer> entry : new HashMap<>(toAdd).entrySet()) {
         Enchantment ench = entry.getKey();
         int level = entry.getValue();
         String key = ench.getKey().getKey().toLowerCase();
         if (isLimitedMace) {
            if (key.contains("breach") || key.contains("wind_burst")) {
               toAdd.remove(ench);
               continue;
            }

            if (key.contains("density") && level > 2) {
               toAdd.put(ench, 2);
               continue;
            }
         }

         if (this.limiterManager.isEnabled()) {
            int maxLevel = this.limiterManager.getLimit(ench);
            if (maxLevel == 0) {
               toAdd.remove(ench);
            } else if (maxLevel > 0 && level > maxLevel) {
               toAdd.put(ench, maxLevel);
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onEnderPearlUse(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         ItemStack item = event.getItem();
         if (item != null && item.getType() == Material.ENDER_PEARL) {
            Player player = event.getPlayer();
            if (player.hasCooldown(Material.ENDER_PEARL)) {
               event.setCancelled(true);
            } else {
               int cdSec = this.limiterManager.getEnderPearlCooldownSeconds();
               if (cdSec > 0) {
                  this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                     if (player.isOnline()) {
                        player.setCooldown(Material.ENDER_PEARL, cdSec * 20);
                     }
                  });
               }
            }
         }
      }
   }
}
