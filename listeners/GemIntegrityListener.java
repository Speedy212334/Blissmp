package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class GemIntegrityListener implements Listener {
   private final BlissGems plugin;
   private final NamespacedKey uidKey;
   private final NamespacedKey ownerKey;
   private final File file;
   private final Map<UUID, String> registered = new HashMap<>();
   private boolean dirty;

   public GemIntegrityListener(BlissGems plugin) {
      this.plugin = plugin;
      this.uidKey = new NamespacedKey(plugin, "gem_uid");
      this.ownerKey = new NamespacedKey(plugin, "gem_uid_owner");
      this.file = new File(plugin.getDataFolder(), "gem_ids.yml");
      YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);

      for (String key : data.getKeys(false)) {
         try {
            this.registered.put(UUID.fromString(key), data.getString(key));
         } catch (IllegalArgumentException var6) {
         }
      }
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("gems.anti-dupe.enabled", true);
   }

   public void start() {
      long every = Math.max(20L, this.plugin.getConfig().getLong("gems.anti-dupe.check-interval-ticks", 40L));
      this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
         if (this.enabled()) {
            for (Player p : this.plugin.getServer().getOnlinePlayers()) {
               this.check(p);
            }

            this.save();
         }
      }, every, every);
   }

   public void save() {
      if (this.dirty) {
         YamlConfiguration data = new YamlConfiguration();
         this.registered.forEach((k, v) -> data.set(k.toString(), v));

         try {
            data.save(this.file);
            this.dirty = false;
         } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save gem_ids.yml: " + e.getMessage());
         }
      }
   }

   private void later(Player p) {
      if (this.enabled()) {
         this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            if (p.isOnline()) {
               this.check(p);
            }
         });
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onJoin(PlayerJoinEvent e) {
      this.later(e.getPlayer());
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onRespawn(PlayerRespawnEvent e) {
      this.later(e.getPlayer());
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onWorld(PlayerChangedWorldEvent e) {
      this.later(e.getPlayer());
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPickup(EntityPickupItemEvent e) {
      if (e.getEntity() instanceof Player p && this.isSoulGem(e.getItem().getItemStack())) {
         this.later(p);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onClose(InventoryCloseEvent e) {
      if (e.getPlayer() instanceof Player p) {
         this.later(p);
      }
   }

   private boolean isSoulGem(ItemStack item) {
      if (CustomItemManager.mayBeGem(item) && item.hasItemMeta()) {
         String id = CustomItemManager.getIdByItem(item);
         return id != null && !id.startsWith("gold_gem") && this.plugin.getGemManager().isAnyGem(id);
      } else {
         return false;
      }
   }

   private String uid(ItemStack item) {
      return (String)item.getItemMeta().getPersistentDataContainer().get(this.uidKey, PersistentDataType.STRING);
   }

   private String owner(ItemStack item) {
      return (String)item.getItemMeta().getPersistentDataContainer().get(this.ownerKey, PersistentDataType.STRING);
   }

   private boolean heldElsewhere(String uid, Player self) {
      for (Player other : this.plugin.getServer().getOnlinePlayers()) {
         if (other != self) {
            for (ItemStack it : other.getInventory().getContents()) {
               if (this.isSoulGem(it) && uid.equals(this.uid(it))) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public void check(Player player) {
      PlayerInventory inv = player.getInventory();
      String me = player.getUniqueId().toString();
      List<Integer> slots = new ArrayList<>();

      for (int i = 0; i < inv.getSize(); i++) {
         if (this.isSoulGem(inv.getItem(i))) {
            slots.add(i);
         }
      }

      if (!slots.isEmpty()) {
         Set<Integer> remove = new HashSet<>();

         for (int slot : slots) {
            ItemStack it = inv.getItem(slot);
            String uid = this.uid(it);
            String owner = this.owner(it);
            if (uid != null && owner != null && !owner.equals(me) && this.heldElsewhere(uid, player)) {
               remove.add(slot);
            }
         }

         Set<String> seen = new HashSet<>();

         for (int slot : slots) {
            if (!remove.contains(slot)) {
               String uid = this.uid(inv.getItem(slot));
               if (uid != null && !seen.add(uid)) {
                  remove.add(slot);
               }
            }
         }

         List<Integer> left = new ArrayList<>(slots);
         left.removeAll(remove);
         Integer keeper = null;
         String reg = this.registered.get(player.getUniqueId());

         for (int pass = 0; pass < 4 && keeper == null; pass++) {
            for (int slot : left) {
               ItemStack it = inv.getItem(slot);
               String uid = this.uid(it);

               boolean match = switch (pass) {
                  case 0 -> {
                     if (uid != null && uid.equals(reg)) {
                        yield true;
                     }

                     yield false;
                  }
                  case 1 -> {
                     if (uid != null && me.equals(this.owner(it))) {
                        yield true;
                     }

                     yield false;
                  }
                  case 2 -> uid == null;
                  default -> true;
               };
               if (match) {
                  keeper = slot;
                  break;
               }
            }
         }

         if (this.plugin.getConfigManager().isSingleGemOnly()) {
            for (int slot : left) {
               if (keeper == null || slot != keeper) {
                  remove.add(slot);
               }
            }
         }

         for (int slot : remove) {
            inv.setItem(slot, null);
         }

         if (!remove.isEmpty()) {
            this.plugin.getLogger().warning("[Anti-dupe] Removed " + remove.size() + " duplicated gem(s) from " + player.getName() + ".");
            player.sendMessage("§c§lA duplicated gem was removed from your inventory.");
         }

         for (int slot : slots) {
            if (!remove.contains(slot)) {
               ItemStack it = inv.getItem(slot);
               if (this.uid(it) == null || this.owner(it) == null) {
                  ItemMeta meta = it.getItemMeta();
                  PersistentDataContainer pdc = meta.getPersistentDataContainer();
                  if (this.uid(it) == null) {
                     pdc.set(this.uidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
                  }

                  if (this.owner(it) == null) {
                     pdc.set(this.ownerKey, PersistentDataType.STRING, me);
                  }

                  it.setItemMeta(meta);
               }
            }
         }

         if (keeper != null) {
            String uid = this.uid(inv.getItem(keeper));
            if (!uid.equals(reg)) {
               this.registered.put(player.getUniqueId(), uid);
               this.dirty = true;
            }
         }
      }
   }
}
