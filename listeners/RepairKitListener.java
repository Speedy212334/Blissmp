package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class RepairKitListener implements Listener {
   private final BlissGems plugin;

   public RepairKitListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         Player player = event.getPlayer();
         ItemStack item = event.getItem();
         if (item != null) {
            String itemId = CustomItemManager.getIdByItem(item);
            if (itemId != null && itemId.equals("repair_kit")) {
               event.setCancelled(true);
               player.sendMessage("§d§oTo use the Repair Kit, drop it on top of a Beacon to create a Pedestal!");
               player.sendMessage("§d§oThe Pedestal will restore energy to all nearby players.");
            }
         }
      }
   }

   @EventHandler
   public void onPlayerDropItem(PlayerDropItemEvent event) {
      final Item droppedItem = event.getItemDrop();
      final ItemStack item = droppedItem.getItemStack();
      final Player player = event.getPlayer();
      String itemId = CustomItemManager.getIdByItem(item);
      if (itemId != null && itemId.equals("repair_kit")) {
         if (this.plugin.getPedestalManager() == null || !this.plugin.getPedestalManager().isNearPedestal(player.getLocation(), 8.0)) {
            (new BukkitRunnable() {
                  public void run() {
                     if (droppedItem.isValid() && !droppedItem.isDead()) {
                        Location itemLoc = droppedItem.getLocation();
                        Block blockBelow = itemLoc.subtract(0.0, 1.0, 0.0).getBlock();
                        if (blockBelow.getType() == Material.BEACON) {
                           if (RepairKitListener.this.plugin.getSpawnBeaconManager() != null
                              && !RepairKitListener.this.plugin.getSpawnBeaconManager().canRepairGems(player)) {
                              return;
                           }

                           droppedItem.remove();
                           boolean success = RepairKitListener.this.plugin.getRepairKitManager().createPedestal(blockBelow.getLocation());
                           if (success) {
                              player.sendMessage("§d§l✦ §d§oPedestal created! It will restore energy to nearby players.");

                              for (Entity entity : itemLoc.getWorld().getNearbyEntities(itemLoc, 15.0, 15.0, 15.0)) {
                                 if (entity instanceof Player && entity != player) {
                                    Player nearbyPlayer = (Player)entity;
                                    nearbyPlayer.sendMessage("§d§o" + player.getName() + " created a Repair Kit Pedestal nearby!");
                                 }
                              }

                              RepairKitListener.this.broadcastRepairRitual(player);
                           } else {
                              player.getInventory().addItem(new ItemStack[]{item});
                              player.sendMessage("§c§oCouldn't create pedestal at this location!");
                           }
                        }
                     }
                  }
               })
               .runTaskLater(this.plugin, 20L);
         }
      }
   }

   private void broadcastRepairRitual(Player player) {
      if (this.plugin.getConfig().getBoolean("repair-kit.broadcast", true)) {
         this.plugin.getServer().broadcastMessage("§d§l✦ §d" + player.getName() + " §7has begun a §d§lRepair Ritual§7!");
      }
   }

   @EventHandler
   public void onItemSpawn(ItemSpawnEvent event) {
      final Item item = event.getEntity();
      ItemStack itemStack = item.getItemStack();
      String itemId = CustomItemManager.getIdByItem(itemStack);
      if (itemId != null && itemId.equals("repair_kit")) {
         (new BukkitRunnable() {
               public void run() {
                  if (item.isValid() && !item.isDead()) {
                     Location itemLoc = item.getLocation();
                     if (RepairKitListener.this.plugin.getPedestalManager() == null
                        || !RepairKitListener.this.plugin.getPedestalManager().isNearPedestal(itemLoc, 8.0)) {
                        Block blockBelow = itemLoc.clone().subtract(0.0, 1.0, 0.0).getBlock();
                        if (blockBelow.getType() == Material.BEACON) {
                           item.remove();
                           boolean success = RepairKitListener.this.plugin.getRepairKitManager().createPedestal(blockBelow.getLocation());
                           if (success) {
                              for (Entity entity : itemLoc.getWorld().getNearbyEntities(itemLoc, 15.0, 15.0, 15.0)) {
                                 if (entity instanceof Player player) {
                                    player.sendMessage("§d§l✦ §d§oRepair Kit Pedestal activated!");
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            })
            .runTaskLater(this.plugin, 40L);
      }
   }
}
