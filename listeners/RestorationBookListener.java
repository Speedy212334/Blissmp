package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class RestorationBookListener implements Listener {
   private static final String ITEM_ID = "restoration_book";
   private final BlissGems plugin;
   private final Set<UUID> activeRituals = new HashSet<>();

   public RestorationBookListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onRestorationBookUse(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         if (event.getHand() == EquipmentSlot.HAND) {
            ItemStack item = event.getItem();
            if (item != null && "restoration_book".equals(CustomItemManager.getIdByItem(item))) {
               event.setCancelled(true);
               Player player = event.getPlayer();
               if (this.activeRituals.contains(player.getUniqueId())) {
                  player.sendMessage("§c§oYour Restoration Ritual is already under way!");
               } else if (this.plugin.getEnergyManager().getEnergyState(player) != EnergyState.BROKEN) {
                  player.sendMessage("§c§oThe Restoration Book only works while your gem is §c§lBROKEN§c§o.");
               } else {
                  List<String> available = this.plugin.getGemManager().getAvailableGemIds();
                  if (available.isEmpty()) {
                     player.sendMessage("§c§oNo gems are available to restore to!");
                  } else {
                     String newGem = available.get((int)(Math.random() * available.size()));
                     int tier = Math.max(1, this.plugin.getGemManager().getGemTier(player));
                     item.setAmount(item.getAmount() - 1);
                     this.activeRituals.add(player.getUniqueId());
                     this.clearExistingGems(player);
                     this.broadcastRitualStart(player);
                     long grantDelay = this.plugin.getGemRitualManager().performRestorationRitual(player, newGem, tier);
                     this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                        this.activeRituals.remove(player.getUniqueId());
                        if (player.isOnline()) {
                           this.plugin.getEnergyManager().setEnergy(player, EnergyState.PRISTINE.getMinEnergy());
                           if (this.plugin.getGemManager().giveGemToOffhand(player, newGem, tier)) {
                              String gemName = this.plugin.getGemManager().getGemDisplayName(newGem);
                              String gemColor = this.plugin.getGemManager().getGemColorCode(newGem);
                              player.sendMessage("§5§l» §fYour gem has been reforged: " + gemColor + "§l" + gemName + " §f(Tier " + tier + ")§5§l «");
                              this.broadcastRitualComplete(player, gemColor + gemName);
                           } else {
                              player.sendMessage("§c§oThe ritual failed to reforge your gem, contact staff.");
                           }
                        }
                     }, grantDelay);
                  }
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onRitualDamage(EntityDamageEvent event) {
      Entity entity = event.getEntity();
      Player player;
      if (entity instanceof Player && this.activeRituals.contains((player = (Player)entity).getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onRitualMove(PlayerMoveEvent event) {
      if (this.activeRituals.contains(event.getPlayer().getUniqueId()) && event.getTo() != null) {
         Location from = event.getFrom();
         Location to = event.getTo();
         if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location held = from.clone();
            held.setYaw(to.getYaw());
            held.setPitch(to.getPitch());
            event.setTo(held);
         }
      }
   }

   private void clearExistingGems(Player player) {
      for (int i = 0; i < player.getInventory().getSize(); i++) {
         ItemStack slot = player.getInventory().getItem(i);
         if (slot != null && this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(slot))) {
            player.getInventory().setItem(i, null);
         }
      }

      ItemStack offhand = player.getInventory().getItemInOffHand();
      if (this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(offhand))) {
         player.getInventory().setItemInOffHand(null);
      }
   }

   private void broadcastRitualStart(Player player) {
      if (this.plugin.getConfig().getBoolean("restoration.broadcast", true)) {
         this.plugin.getServer().broadcastMessage("§5§l✦ §d" + player.getName() + " §7has begun a §5§lRestoration Ritual§7! The sky darkens...");
      }
   }

   private void broadcastRitualComplete(Player player, String gemDisplay) {
      if (this.plugin.getConfig().getBoolean("restoration.broadcast", true)) {
         this.plugin.getServer().broadcastMessage("§5§l✦ §d" + player.getName() + "§7's gem was reforged as " + gemDisplay + " §7at §bPristine§7.");
      }
   }
}
