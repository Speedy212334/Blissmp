package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class UpgraderListener implements Listener {
   private final BlissGems plugin;
   private final Map<UUID, Long> lastUpgrade = new HashMap<>();
   private static final long UPGRADE_DEBOUNCE_MS = 400L;

   public UpgraderListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         if (event.getHand() == EquipmentSlot.HAND || event.getHand() == EquipmentSlot.OFF_HAND) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null) {
               String oraxenId = CustomItemManager.getIdByItem(item);
               if (oraxenId != null && oraxenId.equals("gem_upgrader")) {
                  event.setCancelled(true);
                  long now = System.currentTimeMillis();
                  Long last = this.lastUpgrade.get(player.getUniqueId());
                  if (last == null || now - last >= 400L) {
                     this.lastUpgrade.put(player.getUniqueId(), now);
                     if (!this.plugin.getGemManager().hasActiveGem(player)) {
                        this.plugin.getConfigManager().sendFormattedMessage(player, "no-gem");
                     } else {
                        String currentGemId = this.plugin.getGemManager().getGemId(player);
                        int currentTier = this.plugin.getGemManager().getGemTier(player);
                        if (currentTier != 1) {
                           this.plugin.getConfigManager().sendFormattedMessage(player, "upgrade-already-tier2");
                        } else {
                           if (this.plugin.getGemManager().upgradeGem(player, currentGemId)) {
                              item.setAmount(item.getAmount() - 1);
                              if (this.plugin.getConfigManager().shouldPlayUpgradeEffects()) {
                                 try {
                                    String soundName = this.plugin.getConfigManager().getUpgradeSound();
                                    Sound sound = Sound.valueOf(soundName);
                                    player.playSound(player.getLocation(), sound, 1.0F, 1.5F);
                                    String particleName = this.plugin.getConfigManager().getUpgradeParticle();
                                    Particle particle = Particle.valueOf(particleName);
                                    int count = this.plugin.getConfigManager().getUpgradeParticleCount();
                                    player.spawnParticle(particle, player.getLocation().add(0.0, 1.0, 0.0), count, 0.5, 0.5, 0.5);
                                 } catch (IllegalArgumentException e) {
                                    this.plugin.getLogger().warning("Invalid particle or sound in config: " + e.getMessage());
                                 }
                              }

                              this.plugin.getConfigManager().sendFormattedMessage(player, "upgrade-success");
                              if (this.plugin.getAchievementManager() != null) {
                                 this.plugin.getAchievementManager().unlock(player, Achievement.THE_NEXT_LEVEL);
                              }
                           } else {
                              player.sendMessage("§cFailed to upgrade gem!");
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
