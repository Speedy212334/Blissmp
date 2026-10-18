package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.commands.FixedHeartsCommand;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.OraxenGemFixer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerJoinListener implements Listener {
   private final BlissGems plugin;
   private final Random random;

   public PlayerJoinListener(BlissGems plugin) {
      this.plugin = plugin;
      this.random = new Random();
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.getGoldGemManager() != null) {
         this.plugin.getGoldGemManager().load(player.getUniqueId());
         this.plugin.getGoldGemManager().deliverPendingGems(player);
      }

      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         int moved;
         if (player.isOnline() && (moved = CustomItemManager.migrateGemMaterials(player.getInventory())) > 0) {
            this.plugin.getLogger().info("Moved " + moved + " gem(s) of " + player.getName() + " to nautilus shells");
         }
      }, 5L);
      if (OraxenGemFixer.isFixOnJoinEnabled(this.plugin)) {
         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            int fixedx;
            if (player.isOnline() && (fixedx = OraxenGemFixer.fixInventory(this.plugin, player)) > 0) {
               this.plugin.getLogger().info("Replaced " + fixedx + " legacy gem item(s) for " + player.getName());
            }
         }, 20L);
      }

      if (!this.hasReceivedFirstGem(player) && this.plugin.getGemManager().findGemInInventory(player) == null) {
         if (!this.plugin.getConfigManager().isSmpStarted()) {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
               if (player.isOnline()) {
                  this.plugin.getConfigManager().sendFormattedMessage(player, "smp-not-started");
               }
            }, 40L);
         } else {
            String randomGem = this.getRandomEnabledGem();
            if (randomGem != null) {
               String finalGem = randomGem;
               this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                  if (player.isOnline()) {
                     player.sendMessage("");
                     player.sendMessage("§d§l§m                                                  ");
                     player.sendMessage("§d§lWELCOME TO BLISSGEMS!");
                     player.sendMessage("");
                     player.sendMessage("§7§oThe ancient gem ritual begins...");
                     player.sendMessage("§7§oYour destiny is being forged...");
                     player.sendMessage("§d§l§m                                                  ");
                     player.sendMessage("");
                     this.plugin.getGemRitualManager().performGemRitual(player, finalGem, true, 1);
                     this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                        if (this.plugin.getGemManager().giveGem(player, finalGem, 1)) {
                           this.markFirstGemReceived(player);
                           String gemName = this.plugin.getGemManager().getGemDisplayName(finalGem);
                           String gemColor = this.plugin.getGemManager().getGemColorCode(finalGem);
                           String welcomeMsg = this.plugin.getConfigManager().getFormattedMessage("first-gem-received", "gem", gemName);
                           if (welcomeMsg != null && !welcomeMsg.isEmpty()) {
                              player.sendMessage(welcomeMsg);
                           } else {
                              player.sendMessage("");
                              player.sendMessage("§d§l» §fYour gem has been chosen: " + gemColor + "§l" + gemName + "§d§l «");
                              player.sendMessage("");
                           }

                           this.plugin.getLogger().info("Gave " + player.getName() + " their first gem: " + gemName);
                        }
                     }, 20L);
                  }
               }, 40L);
            }
         }
      }

      if (!this.hasBeenGemLockChecked(player)) {
         boolean fixed = false;

         for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            String itemId2;
            if (item != null && (itemId2 = CustomItemManager.getIdByItem(item)) != null && GemType.isGem(itemId2) && CustomItemManager.markAsUndroppable(item)) {
               fixed = true;
            }
         }

         ItemStack offHand = player.getInventory().getItemInOffHand();
         String itemId;
         if (offHand != null
            && (itemId = CustomItemManager.getIdByItem(offHand)) != null
            && GemType.isGem(itemId)
            && CustomItemManager.markAsUndroppable(offHand)) {
            fixed = true;
         }

         this.markGemLockChecked(player);
         if (fixed) {
            this.plugin.getLogger().info("Fixed undroppable tags on gems for " + player.getName());
         }
      }

      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         if (player.isOnline()) {
            if (this.plugin.getConfigManager().isFixedHeartsEnabled()) {
               FixedHeartsCommand.applyTenHearts(this.plugin, player);
            } else {
               if (this.plugin.getLifeAbilities() != null) {
                  this.plugin.getLifeAbilities().cleanup(player);
               }

               if (this.plugin.getSoulManager() != null) {
                  this.plugin.getSoulManager().cleanup(player);
               }
            }
         }
      }, 20L);
      this.plugin.getGemManager().updateActiveGem(player);
      this.plugin.getAbilityManager().loadCooldowns(player.getUniqueId());
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         if (player.isOnline()) {
            this.plugin.getPluginMessagingManager().sendGemData(player);
         }
      }, 20L);
      if (!this.hasSeenBindingTip(player)) {
         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
               player.sendMessage("");
               player.sendMessage("§d§l⚡ Tip: §fYou can customize how your gem abilities are triggered!");
               player.sendMessage("§7Run §f/bliss ability §7to see your current bindings.");
               player.sendMessage("§7Change them with §f/bliss set_ability <slot> <input> §7(e.g. §f/bliss set_ability primary left_click§7).");
               player.sendMessage("");
               this.markBindingTipSeen(player);
            }
         }, 100L);
      }
   }

   private boolean hasSeenBindingTip(Player player) {
      File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
      File file = new File(dataFolder, player.getUniqueId() + ".yml");
      if (!file.exists()) {
         return false;
      }

      YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
      return data.getBoolean("seen-binding-tip", false);
   }

   private void markBindingTipSeen(Player player) {
      File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
      if (!dataFolder.exists()) {
         dataFolder.mkdirs();
      }

      File file;
      YamlConfiguration data = (file = new File(dataFolder, player.getUniqueId() + ".yml")).exists()
         ? YamlConfiguration.loadConfiguration(file)
         : new YamlConfiguration();
      data.set("seen-binding-tip", true);

      try {
         data.save(file);
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to mark binding tip seen for " + player.getName() + ": " + e.getMessage());
      }
   }

   private boolean hasReceivedFirstGem(Player player) {
      File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
      File file = new File(dataFolder, player.getUniqueId() + ".yml");
      if (!file.exists()) {
         return false;
      }

      YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
      return data.getBoolean("received-first-gem", false);
   }

   private void markFirstGemReceived(Player player) {
      File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
      if (!dataFolder.exists()) {
         dataFolder.mkdirs();
      }

      File file;
      YamlConfiguration data = (file = new File(dataFolder, player.getUniqueId() + ".yml")).exists()
         ? YamlConfiguration.loadConfiguration(file)
         : new YamlConfiguration();
      data.set("received-first-gem", true);
      if (!data.contains("energy")) {
         data.set("energy", this.plugin.getConfigManager().getStartingEnergy());
      }

      try {
         data.save(file);
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to save first gem status for " + player.getName() + ": " + e.getMessage());
      }
   }

   private boolean hasBeenGemLockChecked(Player player) {
      File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
      File file = new File(dataFolder, player.getUniqueId() + ".yml");
      if (!file.exists()) {
         return false;
      }

      YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
      return data.getBoolean("gems-lock-checked", false);
   }

   private void markGemLockChecked(Player player) {
      File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
      if (!dataFolder.exists()) {
         dataFolder.mkdirs();
      }

      File file;
      YamlConfiguration data = (file = new File(dataFolder, player.getUniqueId() + ".yml")).exists()
         ? YamlConfiguration.loadConfiguration(file)
         : new YamlConfiguration();
      data.set("gems-lock-checked", true);

      try {
         data.save(file);
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to save gem-lock-checked for " + player.getName() + ": " + e.getMessage());
      }
   }

   private String getRandomEnabledGem() {
      List<String> enabledGems = this.plugin.getGemManager().getAvailableGemIds();
      return enabledGems.isEmpty() ? null : enabledGems.get(this.random.nextInt(enabledGems.size()));
   }
}
