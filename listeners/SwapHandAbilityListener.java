package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class SwapHandAbilityListener implements Listener {
   private final BlissGems plugin;

   public SwapHandAbilityListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onSwap(PlayerSwapHandItemsEvent event) {
      Player player = event.getPlayer();
      ItemStack mainHand = event.getMainHandItem();
      ItemStack offHand = event.getOffHandItem();
      boolean mainIsGem = this.isGem(mainHand);
      boolean offIsGem = this.isGem(offHand);
      if (mainIsGem || offIsGem) {
         if (this.plugin.getBlissCommand() != null && this.plugin.getClickActivationManager().isClickActivationEnabled(player)) {
            AbilityBinding input = AbilityBinding.swapHand(player.isSneaking());
            AbilitySlot slot = this.plugin.getAbilityBindingManager() != null ? this.plugin.getAbilityBindingManager().getSlot(player, input) : null;
            if (slot != null) {
               event.setCancelled(true);
               this.plugin.getBlissCommand().triggerSlot(player, slot);
            }
         }
      }
   }

   private boolean isGem(ItemStack item) {
      if (item == null) {
         return false;
      }

      String id = CustomItemManager.getIdByItem(item);
      if (id == null) {
         return false;
      }

      if (GemType.isGem(id)) {
         return true;
      }

      GemRegistryImpl registry = this.plugin.getGemRegistry();
      return registry != null && registry.isRegisteredGem(id);
   }
}
