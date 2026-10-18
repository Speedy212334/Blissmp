package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class GemDropListener implements Listener {
   private final BlissGems plugin;

   public GemDropListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onItemDrop(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      ItemStack droppedItem = event.getItemDrop().getItemStack();
      String itemId = CustomItemManager.getIdByItem(droppedItem);
      if (!"flux_gem_t1".equals(itemId) && !"flux_gem_t2".equals(itemId)) {
         if (this.plugin.getConfig().getBoolean("gems.prevent-drop", true)) {
            boolean isLocked = CustomItemManager.isUndroppable(droppedItem);
            if (isLocked) {
               event.setCancelled(true);
               String message = this.plugin.getConfigManager().getFormattedMessage("cannot-drop-gem");
               if (message != null && !message.isEmpty()) {
                  player.sendMessage(message);
               } else {
                  player.sendMessage("§c§lYou cannot drop your gem!");
               }
            }
         }
      } else {
         event.setCancelled(true);
         if (this.plugin.getFluxEnergyManager() != null) {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
               if (player.isOnline() && !player.isDead()) {
                  this.plugin.getFluxEnergyManager().openChargingStation(player);
               }
            });
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onInventoryMoveItem(InventoryMoveItemEvent event) {
      ItemStack item = event.getItem();
      if (CustomItemManager.isUndroppable(item)) {
         event.setCancelled(true);
      }
   }
}
