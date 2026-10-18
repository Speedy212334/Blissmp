package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlissGuiManager implements Listener {
   private final BlissGems plugin;
   private static final String GUI_TITLE = "§5§lBlissGems Menu";

   public BlissGuiManager(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void openMainMenu(Player player) {
      Inventory gui = Bukkit.createInventory(null, 27, "§5§lBlissGems Menu");
      gui.setItem(11, this.createGemInfoItem(player));
      gui.setItem(13, this.createEnergyInfoItem(player));
      gui.setItem(15, this.createSettingsItem(player));
      ItemStack border = this.createBorderItem();

      for (int i = 0; i < 27; i++) {
         if (gui.getItem(i) == null) {
            gui.setItem(i, border);
         }
      }

      player.openInventory(gui);
   }

   private ItemStack createGemInfoItem(Player player) {
      GemType gemType = this.plugin.getGemManager().getGemType(player);
      int tier = this.plugin.getGemManager().getGemTier(player);
      ArrayList<String> lore = new ArrayList<>();
      ItemStack item;
      if (gemType != null) {
         item = new ItemStack(Material.ECHO_SHARD);
         ItemMeta meta = item.getItemMeta();
         meta.setDisplayName("§d§lYour Gem");
         lore.add("§7Type: §f" + gemType.getDisplayName());
         lore.add("§7Tier: §f" + tier);
         lore.add("");
         lore.add("§7Description:");

         for (String line : gemType.getDescription().split("\n")) {
            lore.add("§8" + line);
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      } else {
         item = new ItemStack(Material.BARRIER);
         ItemMeta meta = item.getItemMeta();
         meta.setDisplayName("§c§lNo Gem");
         lore.add("§7You don't have a gem equipped!");
         lore.add("§7Get one from an admin or find one.");
         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createEnergyInfoItem(Player player) {
      int energy = this.plugin.getEnergyManager().getEnergy(player);
      EnergyState state = this.plugin.getEnergyManager().getEnergyState(player);
      ItemStack item = new ItemStack(Material.NETHER_STAR);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName("§b§lEnergy Status");
      ArrayList<String> lore = new ArrayList<>();
      lore.add("§7Energy: §f" + energy + "§8/§f10");
      lore.add("§7State: §f" + state.getDisplayName());
      lore.add("");
      StringBuilder energyBar = new StringBuilder("§8[");

      for (int i = 0; i < 10; i++) {
         if (i < energy) {
            energyBar.append("§a■");
         } else {
            energyBar.append("§7■");
         }
      }

      energyBar.append("§8]");
      lore.add(energyBar.toString());
      lore.add("");
      if (energy == 0) {
         lore.add("§c✘ Abilities disabled");
         lore.add("§c✘ Passives disabled");
      } else if (energy == 1) {
         lore.add("§a✔ Abilities enabled");
         lore.add("§c✘ Passives disabled");
      } else {
         lore.add("§a✔ Abilities enabled");
         lore.add("§a✔ Passives enabled");
      }

      meta.setLore(lore);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createSettingsItem(Player player) {
      ItemStack item = new ItemStack(Material.COMPARATOR);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName("§e§lSettings");
      ArrayList<String> lore = new ArrayList<>();
      lore.add("§7Current Settings:");
      lore.add("");
      boolean clickEnabled = this.plugin.getClickActivationManager().isClickActivationEnabled(player);
      lore.add("§7Click Activation: " + (clickEnabled ? "§aEnabled" : "§cDisabled"));
      lore.add("§8Use /bliss toggle_click to change");
      lore.add("");
      int trustedCount = this.plugin.getTrustedPlayersManager().getTrustedPlayers(player).size();
      lore.add("§7Trusted Players: §f" + trustedCount);
      lore.add("§8Use /bliss trust <player>");
      lore.add("");
      if (player.hasPermission("blissgems.admin")) {
         boolean banEnabled = this.plugin.getConfigManager().isBanOnZeroEnergyEnabled();
         lore.add("§c§lAdmin Setting:");
         lore.add("§7Ban on 0 Energy: " + (banEnabled ? "§aEnabled" : "§cDisabled"));
         lore.add("§8Use /bliss bannable <true/false>");
      }

      meta.setLore(lore);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createBorderItem() {
      ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(" ");
      item.setItemMeta(meta);
      return item;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getView().getTitle().equals("§5§lBlissGems Menu")) {
         event.setCancelled(true);
      }
   }
}
