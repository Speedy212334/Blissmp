package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

public class ItemOwnershipListener implements Listener {
   private static final long SWEEP_INTERVAL_TICKS = 40L;
   private final BlissGems plugin;
   private BukkitTask sweepTask;

   public ItemOwnershipListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void start() {
      if (this.sweepTask == null) {
         this.sweepTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
               this.stampInventory(player);
            }
         }, 40L, 40L);
      }
   }

   public void stop() {
      if (this.sweepTask != null) {
         this.sweepTask.cancel();
         this.sweepTask = null;
      }
   }

   private void stampInventory(Player player) {
      UUID owner = player.getUniqueId();
      PlayerInventory inv = player.getInventory();
      ItemStack[] storage = inv.getStorageContents();
      boolean storageChanged = false;

      for (ItemStack item : storage) {
         if (this.stampIfEligible(item, owner)) {
            storageChanged = true;
         }
      }

      if (storageChanged) {
         inv.setStorageContents(storage);
      }

      ItemStack[] armor = inv.getArmorContents();
      boolean armorChanged = false;

      for (ItemStack item : armor) {
         if (this.stampIfEligible(item, owner)) {
            armorChanged = true;
         }
      }

      if (armorChanged) {
         inv.setArmorContents(armor);
      }

      ItemStack offhand;
      if (this.stampIfEligible(offhand = inv.getItemInOffHand(), owner)) {
         inv.setItemInOffHand(offhand);
      }
   }

   private boolean stampIfEligible(ItemStack item, UUID owner) {
      if (item != null && item.getType() != Material.AIR && item.getMaxStackSize() == 1) {
         boolean stamped = CustomItemManager.setOwner(item, owner);
         UUID currentOwner = CustomItemManager.getOwner(item);
         if (currentOwner != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(currentOwner);
            String name = op.getName() != null ? op.getName() : currentOwner.toString().substring(0, 8);
            setOwnerLore(item, name);
         }

         return stamped;
      } else {
         return false;
      }
   }

   public static void setOwnerLore(ItemStack item, String ownerName) {
      if (item != null && ownerName != null) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            String prefix = "§7Owner: §e";
            boolean found = false;

            for (int i = 0; i < lore.size(); i++) {
               String stripped = ChatColor.stripColor(lore.get(i));
               if (stripped != null && stripped.startsWith("Owner: ")) {
                  lore.set(i, prefix + ownerName);
                  found = true;
                  break;
               }
            }

            if (!found) {
               lore.add(prefix + ownerName);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
         }
      }
   }
}
