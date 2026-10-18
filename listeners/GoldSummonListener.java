package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class GoldSummonListener implements Listener {
   private final BlissGems plugin;

   public GoldSummonListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   private boolean isBlocked() {
      GoldGemManager gold = this.plugin.getGoldGemManager();
      return gold != null && this.plugin.getConfig().getBoolean("gold.summon.once-per-server", true) && gold.isSummoned();
   }

   private boolean ritualOnly() {
      return this.plugin.getConfig().getBoolean("gold.summon.ritual-only", true);
   }

   private boolean isGoldGem(ItemStack item) {
      return item != null && "gold_gem_t1".equals(CustomItemManager.getIdByItem(item));
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onPrepare(PrepareItemCraftEvent event) {
      if (event.getRecipe() != null && this.isGoldGem(event.getInventory().getResult())) {
         if (this.isBlocked() || this.ritualOnly()) {
            event.getInventory().setResult(null);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onCraft(CraftItemEvent event) {
      if (this.isGoldGem(event.getRecipe().getResult())) {
         if (this.ritualOnly()) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage("§6The Gold Gem answers only to its ritual §7- hold the Fragment Core and right-click.");
         } else if (this.isBlocked()) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
               player.sendMessage(this.plugin.getConfigManager().getMessage("gold-already-summoned"));
            }
         } else {
            if (event.getWhoClicked() instanceof Player player) {
               this.plugin.getGoldGemManager().markSummoned(player);
               String announcement = this.plugin.getConfigManager().getMessage("gold-summoned");
               if (announcement != null && !announcement.isEmpty()) {
                  this.plugin.getServer().broadcastMessage(announcement.replace("{player}", player.getName()));
               }
            }
         }
      }
   }
}
