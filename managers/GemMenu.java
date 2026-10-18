package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.goldevent.RitualItems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GemMenu implements Listener {
   public static final String MYTHICS_URL = "https://modrinth.com/plugin/auratus-hertic-addon";
   public static final String EXPANSION_URL = "https://modrinth.com/plugin/blissgems-expansion";
   private static final Component BANNER = Component.text("\ue201\ue200\ue203\ue202").color(NamedTextColor.WHITE);
   private static final String[] GEMS = new String[]{"astra", "fire", "flux", "life", "puff", "speed", "strength", "wealth"};
   private static final int SHOW_ENERGY = 10;
   private static final int[] LEFT = new int[]{0, 1, 9, 10, 18, 19, 27, 28};
   private static final int[] RIGHT = new int[]{7, 8, 16, 17, 25, 26, 34, 35};
   private static final int[] DIVIDER = new int[]{2, 11, 20, 29, 6, 15, 24, 33};
   private static final int GOLD = 4;
   private static final int AURATUS = 21;
   private static final int HERETIC = 23;
   private static final int MORE_GEMS = 31;
   private static final int BACK = 18;
   private final BlissGems plugin;

   public GemMenu(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void open(Player player) {
      GemMenu.Holder holder = new GemMenu.Holder(GemMenu.Page.GEMS);
      holder.inv = Bukkit.createInventory(holder, 36, BANNER);

      for (int i = 0; i < GEMS.length; i++) {
         if (this.plugin.getConfig().getBoolean("gems.enabled." + GEMS[i], true)) {
            holder.inv.setItem(LEFT[i], showcase(CustomItemManager.getItemById(GEMS[i] + "_gem_t1", 10)));
            holder.inv.setItem(RIGHT[i], showcase(CustomItemManager.getItemById(GEMS[i] + "_gem_t2", 10)));
         }
      }

      ItemStack gold = showcase(CustomItemManager.getItemById("gold_gem_t1"));
      if (gold != null) {
         ItemMeta meta = gold.getItemMeta();
         List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
         lore.add(Component.empty());
         lore.add(mm("<!italic><gradient:#F7971E:#FFD200>Click to see the Gold Gem and its fragments</gradient>"));
         meta.lore(lore);
         gold.setItemMeta(meta);
      }

      holder.inv.setItem(4, gold);

      for (int slot : DIVIDER) {
         holder.inv.setItem(slot, divider());
      }

      boolean mythics = this.mythicsInstalled();
      holder.inv.setItem(21, mythic(5003, "<gradient:#FFD86F:#FC6262><bold>Auratus Gem</bold></gradient>", mythics));
      holder.inv.setItem(23, mythic(5001, "<gradient:#B06AB3:#4568DC><bold>Heretic Gem</bold></gradient>", mythics));
      holder.inv.setItem(31, moreGems());
      player.openInventory(holder.inv);
      player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6F, 1.2F);
   }

   public void openGold(Player player) {
      GemMenu.Holder holder = new GemMenu.Holder(GemMenu.Page.GOLD);
      holder.inv = Bukkit.createInventory(holder, 27, BANNER);
      holder.inv.setItem(4, showcase(CustomItemManager.getItemById("gold_gem_t1")));

      for (int n = 1; n <= 7; n++) {
         holder.inv.setItem(9 + n, showcase(RitualItems.wireFragment(n)));
      }

      holder.inv.setItem(22, showcase(RitualItems.fragmentCore()));
      ItemStack back = new ItemStack(Material.ARROW);
      ItemMeta meta = back.getItemMeta();
      meta.displayName(mm("<!italic><gradient:#C77DFF:#4CC9F0>Back</gradient>"));
      back.setItemMeta(meta);
      holder.inv.setItem(18, back);
      player.openInventory(holder.inv);
      player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6F, 1.4F);
   }

   private boolean mythicsInstalled() {
      return Bukkit.getPluginManager().isPluginEnabled("BlissMythics");
   }

   private static ItemStack showcase(ItemStack item) {
      if (item == null) {
         return null;
      }

      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.getPersistentDataContainer().getKeys().forEach(k -> meta.getPersistentDataContainer().remove(k));
         item.setItemMeta(meta);
      }

      return item;
   }

   private static ItemStack divider() {
      ItemStack pane = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
      ItemMeta meta = pane.getItemMeta();
      meta.setHideTooltip(true);
      pane.setItemMeta(meta);
      return pane;
   }

   private static ItemStack mythic(int model, String name, boolean installed) {
      ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
      ItemMeta meta = item.getItemMeta();
      meta.setCustomModelData(model);
      meta.displayName(mm("<!italic>" + name));
      meta.lore(
         installed
            ? List.of(mm("<!italic><gray>A mythic gem from the BlissMythics addon.</gray>"))
            : List.of(
               mm("<!italic><gray>A mythic gem from the <white>BlissMythics</white> addon.</gray>"),
               mm("<!italic><red>Not installed on this server.</red>"),
               Component.empty(),
               mm("<!italic><gradient:#4CC9F0:#C77DFF>Click for the download link</gradient>")
            )
      );
      item.setItemMeta(meta);
      return item;
   }

   private static ItemStack moreGems() {
      ItemStack item = new ItemStack(Material.DIAMOND_BLOCK);
      ItemMeta meta = item.getItemMeta();
      meta.setEnchantmentGlintOverride(true);
      meta.displayName(mm("<!italic><gradient:#4CC9F0:#C77DFF:#FF85C0><bold>Want more gems?</bold></gradient>"));
      meta.lore(
         List.of(
            mm("<!italic><gray>New gems, powers and items come with</gray>"),
            mm("<!italic><white>BlissGems Expansion</white><gray>.</gray>"),
            Component.empty(),
            mm("<!italic><gradient:#4CC9F0:#C77DFF>Click for the download link</gradient>")
         )
      );
      item.setItemMeta(meta);
      return item;
   }

   private void sendLink(Player player, String intro, String url) {
      player.closeInventory();
      player.sendMessage(mm("<gradient:#C77DFF:#4CC9F0><bold>BlissGems</bold></gradient> <dark_gray>»</dark_gray> " + intro));
      player.sendMessage(
         mm("<gradient:#4CC9F0:#C77DFF><underlined>" + url + "</underlined></gradient>")
            .clickEvent(ClickEvent.openUrl(url))
            .decoration(TextDecoration.ITALIC, false)
      );
      player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.3F);
   }

   private static Component mm(String text) {
      return MiniMessage.miniMessage().deserialize(text);
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof GemMenu.Holder holder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player && event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            int slot = event.getRawSlot();
            if (holder.page == GemMenu.Page.GEMS) {
               if (slot == 4) {
                  this.openGold(player);
               } else if ((slot == 21 || slot == 23) && !this.mythicsInstalled()) {
                  this.sendLink(
                     player,
                     "<gray>Auratus and Heretic come with the </gray><white>BlissMythics</white><gray> addon:</gray>",
                     "https://modrinth.com/plugin/auratus-hertic-addon"
                  );
               } else if (slot == 31) {
                  this.sendLink(
                     player, "<gray>More gems: </gray><white>BlissGems Expansion</white><gray>:</gray>", "https://modrinth.com/plugin/blissgems-expansion"
                  );
               }
            } else if (slot == 18) {
               this.open(player);
            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof GemMenu.Holder) {
         event.setCancelled(true);
      }
   }

   private static final class Holder implements InventoryHolder {
      final GemMenu.Page page;
      Inventory inv;

      Holder(GemMenu.Page page) {
         this.page = page;
      }

      public Inventory getInventory() {
         return this.inv;
      }
   }

   private enum Page {
      GEMS,
      GOLD;
   }
}
