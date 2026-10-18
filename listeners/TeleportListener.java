package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class TeleportListener implements Listener {
   private final BlissGems plugin;

   public TeleportListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      Player player = event.getPlayer();
      if (event.getCause() == TeleportCause.SPECTATE && this.plugin.getAstraAbilities().isProjecting(player)) {
         event.setCancelled(true);
         player.sendMessage("§d§oYou cannot teleport to players while projecting!");
      } else {
         if (this.plugin.getFireAbilities().isCharging(player)) {
            this.plugin.getFireAbilities().cancelCharging(player);
            player.sendMessage("§c§oYour fireball charge was cancelled due to teleportation.");
         }

         if (this.plugin.getFluxAbilities().isCharging(player)) {
            this.plugin.getFluxAbilities().cancelCharging(player);
            player.sendMessage("§b⚡ §oYour flux beam charge was cancelled due to teleportation.");
         }
      }
   }
}
