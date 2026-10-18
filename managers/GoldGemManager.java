package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class GoldGemManager {
   public static final String GOLD_ITEM_ID = "gold_gem_t1";
   public static final int SOULS_TO_AWAKEN = 8;
   public static final int BASE_MODEL_DATA = 1009;
   private final BlissGems plugin;
   private final Map<UUID, Map<String, GoldGemManager.Harvest>> harvested = new HashMap<>();
   private final Map<UUID, String> active = new HashMap<>();
   private final Map<UUID, UUID> trackedInstance = new HashMap<>();
   private final Set<UUID> trimsDisabled = new HashSet<>();

   public GoldGemManager(BlissGems plugin) {
      this.plugin = plugin;
      this.startPassiveTask();
   }

   public boolean holdsGoldGem(Player player) {
      ItemStack gem = this.findGoldGem(player);
      if (gem == null) {
         return false;
      }

      this.syncInstance(player, gem);
      return true;
   }

   private ItemStack findGoldGem(Player player) {
      ItemStack offhand = player.getInventory().getItemInOffHand();
      if (this.isGoldGem(offhand)) {
         return offhand;
      }

      ItemStack mainhand = player.getInventory().getItemInMainHand();
      if (this.isGoldGem(mainhand)) {
         return mainhand;
      }

      for (ItemStack item : player.getInventory().getContents()) {
         if (this.isGoldGem(item)) {
            return item;
         }
      }

      return null;
   }

   private void syncInstance(Player player, ItemStack gem) {
      UUID onItem = CustomItemManager.ensureGoldInstanceId(gem);
      if (onItem != null) {
         UUID playerId = player.getUniqueId();
         UUID tracked = this.trackedInstance.get(playerId);
         if (tracked == null) {
            this.trackedInstance.put(playerId, onItem);
            this.save(playerId);
         } else {
            if (!tracked.equals(onItem)) {
               this.harvested.remove(playerId);
               this.active.remove(playerId);
               this.trackedInstance.put(playerId, onItem);
               this.save(playerId);
            }
         }
      }
   }

   public boolean isGoldGemInMainHand(Player player) {
      return this.isGoldGem(player.getInventory().getItemInMainHand());
   }

   public boolean isGoldGem(ItemStack item) {
      return item != null && ("gold_gem_t1".equals(CustomItemManager.getIdByItem(item)) || CustomItemManager.isGoldSkinned(item));
   }

   public Map<String, GoldGemManager.Harvest> getHarvested(UUID playerId) {
      this.syncInstanceIfOnline(playerId);
      return this.harvested.getOrDefault(playerId, Map.of());
   }

   public String getActive(UUID playerId) {
      this.syncInstanceIfOnline(playerId);
      return this.active.get(playerId);
   }

   public int getActiveTier(UUID playerId) {
      String gemId = this.getActive(playerId);
      GoldGemManager.Harvest soul = gemId != null ? this.getHarvested(playerId).get(gemId) : null;
      return soul != null ? soul.tier() : 1;
   }

   private void syncInstanceIfOnline(UUID playerId) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
         ItemStack gem = this.findGoldGem(player);
         if (gem != null) {
            this.syncInstance(player, gem);
         }
      }
   }

   public boolean setActive(Player player, String gemId) {
      if (!this.getHarvested(player.getUniqueId()).containsKey(gemId)) {
         return false;
      }

      this.active.put(player.getUniqueId(), gemId);
      this.save(player.getUniqueId());
      this.refreshGoldItem(player);
      return true;
   }

   public String cycleActive(Player player) {
      UUID playerId = player.getUniqueId();
      Map<String, GoldGemManager.Harvest> souls = this.getHarvested(playerId);
      if (souls.isEmpty()) {
         return null;
      }

      ArrayList<String> order = new ArrayList<>(souls.keySet());
      String current = this.active.get(playerId);
      int index = current != null ? order.indexOf(current) : -1;
      String next = order.get((index + 1) % order.size());
      this.active.put(playerId, next);
      this.save(playerId);
      this.refreshGoldItem(player);
      return next;
   }

   public boolean isTrimEnabled(UUID playerId) {
      return !this.trimsDisabled.contains(playerId);
   }

   public boolean toggleTrims(Player player) {
      UUID playerId = player.getUniqueId();
      if (this.trimsDisabled.remove(playerId)) {
         this.applyTrims(player);
         this.save(playerId);
         return true;
      } else {
         this.trimsDisabled.add(playerId);
         this.clearTrims(player);
         this.save(playerId);
         return false;
      }
   }

   public void fillSoul(Player player, String gemId, int tier) {
      UUID playerId = player.getUniqueId();
      this.harvested.computeIfAbsent(playerId, id -> new LinkedHashMap<>()).put(gemId, new GoldGemManager.Harvest(gemId, tier, null));
      this.active.putIfAbsent(playerId, gemId);
      this.save(playerId);
      this.refreshGoldItem(player);
   }

   public boolean removeSoul(Player player, String gemId) {
      UUID playerId = player.getUniqueId();
      this.getHarvested(playerId);
      Map<String, GoldGemManager.Harvest> souls = this.harvested.get(playerId);
      if (souls != null && souls.remove(gemId) != null) {
         if (gemId.equals(this.active.get(playerId))) {
            this.active.remove(playerId);
            if (!souls.isEmpty()) {
               this.active.put(playerId, souls.keySet().iterator().next());
            }
         }

         if (souls.isEmpty()) {
            this.harvested.remove(playerId);
         }

         this.save(playerId);
         this.refreshGoldItem(player);
         return true;
      } else {
         return false;
      }
   }

   public int clearSouls(Player player) {
      UUID playerId = player.getUniqueId();
      int removed = this.getHarvested(playerId).size();
      this.harvested.remove(playerId);
      this.active.remove(playerId);
      this.save(playerId);
      this.refreshGoldItem(player);
      return removed;
   }

   public void harvestOnDeath(PlayerDeathEvent event) {
      Player victim = event.getEntity();
      Player killer = victim.getKiller();
      if (this.canHarvest(killer, victim)) {
         GoldGemManager.Harvest taken = this.takeGem(victim, event.getDrops());
         if (taken != null) {
            this.absorb(killer, victim, taken);
         }
      }
   }

   public boolean canHarvest(Player killer, Player victim) {
      return killer != null && victim != null && !killer.equals(victim) && this.holdsGoldGem(killer)
         ? this.plugin.getEnergyManager().getEnergyState(victim) != EnergyState.BROKEN
         : false;
   }

   public GoldGemManager.Harvest peekGem(Player victim) {
      UUID owner = victim.getUniqueId();

      for (int slot = 0; slot < victim.getInventory().getSize(); slot++) {
         GoldGemManager.Harvest found = this.describe(victim.getInventory().getItem(slot), owner);
         if (found != null) {
            return found;
         }
      }

      return this.describe(victim.getInventory().getItemInOffHand(), owner);
   }

   public GoldGemManager.Harvest takeGem(Player victim, List<ItemStack> drops) {
      GoldGemManager.Harvest taken = null;
      UUID owner = victim.getUniqueId();
      if (drops != null) {
         for (ItemStack item : new ArrayList<>(drops)) {
            GoldGemManager.Harvest found = this.describe(item, owner);
            if (found != null) {
               taken = found;
               drops.remove(item);
               break;
            }
         }
      }

      for (int slot = 0; slot < victim.getInventory().getSize(); slot++) {
         GoldGemManager.Harvest found = this.describe(victim.getInventory().getItem(slot), owner);
         if (found != null) {
            if (taken == null) {
               taken = found;
            }

            victim.getInventory().setItem(slot, null);
         }
      }

      GoldGemManager.Harvest offhand = this.describe(victim.getInventory().getItemInOffHand(), owner);
      if (offhand != null) {
         if (taken == null) {
            taken = offhand;
         }

         victim.getInventory().setItemInOffHand(null);
      }

      if (taken != null) {
         this.plugin.getGemManager().updateActiveGem(victim);
      }

      return taken;
   }

   public void absorb(Player killer, Player victim, GoldGemManager.Harvest taken) {
      this.harvested.computeIfAbsent(killer.getUniqueId(), id -> new LinkedHashMap<>()).put(taken.gemId(), taken);
      this.active.putIfAbsent(killer.getUniqueId(), taken.gemId());
      this.save(killer.getUniqueId());
      this.shatter(victim.getLocation(), taken.gemId());
      this.refreshGoldItem(killer);
      String message = this.plugin.getConfigManager().getMessage("gold-soul-repurposed");
      if (message != null && !message.isEmpty()) {
         Bukkit.broadcastMessage(message.replace("{player}", victim.getName()));
      }

      killer.playSound(killer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 0.7F);
      int soulCount = this.getHarvested(killer.getUniqueId()).size();
      killer.sendMessage(
         this.plugin
            .getConfigManager()
            .getMessage("gold-soul-harvested")
            .replace("{gem}", this.plugin.getGemManager().getGemDisplayName(taken.gemId()))
            .replace("{count}", String.valueOf(soulCount))
      );
      String awakened;
      if (soulCount >= 8 && (awakened = this.plugin.getConfigManager().getMessage("gold-awakened")) != null && !awakened.isEmpty()) {
         Bukkit.broadcastMessage(awakened.replace("{player}", killer.getName()));
      }
   }

   private GoldGemManager.Harvest describe(ItemStack item, UUID owner) {
      String gemId = this.harvestableGemId(item);
      if (gemId == null) {
         return null;
      }

      int tier = this.plugin.getGemRegistry().tierFromItemId(CustomItemManager.getIdByItem(item));
      return new GoldGemManager.Harvest(gemId, tier, owner);
   }

   public void refreshGoldItem(Player player) {
      PlayerInventory inv = player.getInventory();

      for (int slot = 0; slot < inv.getSize(); slot++) {
         ItemStack item = inv.getItem(slot);
         if (this.isGoldGem(item)) {
            ItemStack fresh = this.buildGoldItem(player, item);
            if (fresh != null) {
               inv.setItem(slot, fresh);
            }
         }
      }

      ItemStack off = inv.getItemInOffHand();
      if (this.isGoldGem(off)) {
         ItemStack fresh = this.buildGoldItem(player, off);
         if (fresh != null) {
            inv.setItemInOffHand(fresh);
         }
      }

      this.plugin.getGemManager().updateActiveGem(player);
   }

   private ItemStack buildGoldItem(Player player, ItemStack current) {
      UUID instance = CustomItemManager.getGoldInstanceId(current);
      Map<String, GoldGemManager.Harvest> souls = this.harvested.getOrDefault(player.getUniqueId(), Map.of());
      String active = this.active.get(player.getUniqueId());
      GoldGemManager.Harvest soul = active != null ? souls.get(active) : null;
      ItemStack base = soul != null
         ? CustomItemManager.getItemById(soul.gemId() + "_gem_t" + soul.tier(), this.plugin.getEnergyManager().getEnergy(player))
         : CustomItemManager.getItemById("gold_gem_t1");
      if (base == null) {
         return null;
      }

      ItemStack item = new ItemStack(CustomItemManager.GEM_MATERIAL);
      ItemMeta meta = base.getItemMeta();
      if (meta == null) {
         return null;
      }

      ArrayList<String> lore = new ArrayList<>();
      if (soul != null) {
         meta.setDisplayName(
            "§6§lGOLD GEM §8» "
               + this.plugin.getGemManager().getGemColorCode(soul.gemId())
               + "§l"
               + this.plugin.getGemManager().getGemDisplayName(soul.gemId())
               + " §8(T"
               + soul.tier()
               + ")"
         );
         if (meta.getLore() != null) {
            lore.addAll(meta.getLore());
         }

         lore.add("");
      } else {
         lore.add("§f§lWATCH THE LINES OF REALITY FRAY AS EIGHT SOULS BECOME ONE");
      }

      lore.add(this.stateLine(souls.size()));
      lore.add("§6\ud83c\udf1f §6§lHARVESTED SOULS");
      if (souls.isEmpty()) {
         lore.add("§8- the gem is silent -");
      } else {
         lore.add(this.colourBar(souls.keySet()));

         for (Entry<String, GoldGemManager.Harvest> e : souls.entrySet()) {
            lore.add(
               this.plugin.getGemManager().getGemColorCode(e.getKey())
                  + "- "
                  + this.plugin.getGemManager().getGemDisplayName(e.getKey())
                  + " §8(T"
                  + e.getValue().tier()
                  + ")"
                  + (e.getKey().equals(active) ? " §a◀" : "")
            );
         }
      }

      lore.add("§7/bliss gold §8- switch soul, Sundering Beam");
      meta.setLore(lore);
      meta.setCustomModelData(1009 + Math.min(souls.size(), 8));
      item.setItemMeta(meta);
      item.setAmount(1);
      if (instance != null) {
         CustomItemManager.setGoldInstanceId(item, instance);
      } else {
         CustomItemManager.ensureGoldInstanceId(item);
      }

      return item;
   }

   private String colourBar(Iterable<String> gemIds) {
      StringBuilder bar = new StringBuilder();

      for (String gemId : gemIds) {
         bar.append(this.plugin.getGemManager().getGemColorCode(gemId)).append("❖");
      }

      return bar.toString();
   }

   private String stateLine(int soulCount) {
      if (soulCount <= 0) {
         return "§6(Dormant)";
      } else {
         return soulCount >= 8 ? "§6§l(Awakened)" : "§6(Awakening — " + soulCount + "/8)";
      }
   }

   private String harvestableGemId(ItemStack item) {
      if (item == null) {
         return null;
      } else {
         String itemId = CustomItemManager.getIdByItem(item);
         if (itemId != null && !"gold_gem_t1".equals(itemId)) {
            String gemId = this.plugin.getGemRegistry().gemIdFromItemId(itemId);
            return gemId != null && GemManager.builtInType(gemId) != null ? gemId : null;
         } else {
            return null;
         }
      }
   }

   private void shatter(Location location, String harvestedId) {
      if (location.getWorld() != null) {
         location.getWorld()
            .spawnParticle(Particle.DUST, location.clone().add(0.0, 1.0, 0.0), 12, 0.4, 0.6, 0.4, 0.0, new DustOptions(this.soulColour(harvestedId), 1.8F));
         location.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.2F, 0.6F);
         int amount = this.plugin.getConfig().getInt("gold.shards-per-harvest", 3);
         if (amount > 0) {
            ItemStack fragment = CustomItemManager.getItemById("gem_fragment");
            if (fragment != null) {
               fragment.setAmount(amount);
               location.getWorld().dropItemNaturally(location, fragment);
            }
         }
      }
   }

   public Color soulColour(String gemId) {
      return this.plugin.getGemRitualManager() == null ? Color.fromRGB(255, 215, 0) : this.plugin.getGemRitualManager().getGemColor(gemId);
   }

   public void resetProgress(Player player) {
      UUID playerId = player.getUniqueId();
      Map<String, GoldGemManager.Harvest> souls = this.getHarvested(playerId);
      boolean hadSouls = !souls.isEmpty();
      if (hadSouls && this.plugin.getConfig().getBoolean("gold.death.return-souls", true)) {
         for (GoldGemManager.Harvest soul : new ArrayList<>(souls.values())) {
            this.returnSoul(player, soul);
         }
      }

      this.harvested.remove(playerId);
      this.active.remove(playerId);
      this.save(playerId);
      this.refreshGoldItem(player);
      this.clearTrims(player);
      if (hadSouls) {
         player.sendMessage(this.plugin.getConfigManager().getMessage("gold-progress-reset"));
      }

      if (this.plugin.getConfig().getBoolean("gold.death.break-gem", true) && this.holdsGoldGem(player)) {
         this.plugin.getEnergyManager().setEnergy(player, 0);
         player.sendMessage(this.plugin.getConfigManager().getMessage("gold-gem-broken"));
      }
   }

   private void returnSoul(Player holder, GoldGemManager.Harvest soul) {
      if (soul.owner() != null && !soul.owner().equals(holder.getUniqueId())) {
         String itemId = soul.gemId() + "_gem_t" + soul.tier();
         Player owner = this.plugin.getServer().getPlayer(soul.owner());
         if (owner != null && owner.isOnline()) {
            this.plugin.getGemManager().giveGemToOffhand(owner, soul.gemId(), soul.tier());
            owner.sendMessage(
               this.plugin
                  .getConfigManager()
                  .getMessage("gold-soul-returned")
                  .replace("{player}", holder.getName())
                  .replace("{gem}", this.plugin.getGemManager().getGemDisplayName(soul.gemId()))
            );
         } else {
            this.queuePendingGem(soul.owner(), itemId);
         }
      }
   }

   private void queuePendingGem(UUID ownerId, String itemId) {
      File file = this.getPlayerFile(ownerId);
      YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
      List pending = data.getStringList("gold.pending-return");
      pending.add(itemId);
      data.set("gold.pending-return", pending);

      try {
         data.save(file);
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to queue a returned gem for " + ownerId + ": " + e.getMessage());
      }
   }

   public void deliverPendingGems(Player player) {
      File file = this.getPlayerFile(player.getUniqueId());
      if (file.exists()) {
         YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
         List<String> pending = data.getStringList("gold.pending-return");
         if (!pending.isEmpty()) {
            for (String itemId : pending) {
               int marker = itemId.indexOf("_gem_t");
               if (marker > 0) {
                  String gemId = itemId.substring(0, marker);
                  int tier = "2".equals(itemId.substring(marker + 6)) ? 2 : 1;
                  this.plugin.getGemManager().giveGemToOffhand(player, gemId, tier);
                  player.sendMessage(
                     this.plugin
                        .getConfigManager()
                        .getMessage("gold-soul-returned")
                        .replace("{player}", "a fallen Gold Gem")
                        .replace("{gem}", this.plugin.getGemManager().getGemDisplayName(gemId))
                  );
               }
            }

            data.set("gold.pending-return", null);

            try {
               data.save(file);
            } catch (IOException e) {
               this.plugin.getLogger().warning("Failed to clear returned gems for " + player.getName() + ": " + e.getMessage());
            }
         }
      }
   }

   private void startPassiveTask() {
      int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
      (new BukkitRunnable() {
         public void run() {
            for (Player player : GoldGemManager.this.plugin.getServer().getOnlinePlayers()) {
               if (!player.isDead() && GoldGemManager.this.holdsGoldGem(player)) {
                  GoldGemManager.this.applyTrims(player);
                  if (!GoldGemManager.this.getHarvested(player.getUniqueId()).isEmpty()) {
                     GoldGemManager.this.applyHarvestedPassives(player);
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, interval, interval);
   }

   private void applyHarvestedPassives(Player player) {
      if (this.plugin.getGemLockManager() == null || !this.plugin.getGemLockManager().isLocked(player)) {
         if (this.plugin.getRegionManager() == null || !this.plugin.getRegionManager().areGemsDisabled(player)) {
            GemRegistryImpl registry = this.plugin.getGemRegistry();
            if (registry != null) {
               for (Entry<String, GoldGemManager.Harvest> soul : this.getHarvested(player.getUniqueId()).entrySet()) {
                  GemPassiveHandler handler = registry.getPassiveHandler(soul.getKey());
                  if (handler != null) {
                     handler.applyPassives(player, soul.getValue().tier());
                  }
               }

               this.drawSoulAura(player);
            }
         }
      }
   }

   private void drawSoulAura(Player player) {
      ArrayList<String> souls = new ArrayList<>(this.getHarvested(player.getUniqueId()).keySet());
      if (!souls.isEmpty() && player.getWorld() != null) {
         Location centre = player.getLocation().add(0.0, 1.0, 0.0);
         int points = souls.size() * 3;

         for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2) * i / points;
            Location at = centre.clone().add(Math.cos(angle) * 0.8, 0.0, Math.sin(angle) * 0.8);
            player.getWorld().spawnParticle(Particle.DUST, at, 1, 0.0, 0.15, 0.0, 0.0, new DustOptions(this.soulColour(souls.get(i % souls.size())), 0.9F));
         }
      }
   }

   private void applyTrims(Player player) {
      if (this.plugin.getConfig().getBoolean("gold.armour-trim.enabled", true)) {
         if (!this.trimsDisabled.contains(player.getUniqueId())) {
            TrimPattern pattern = this.trimPattern();
            ItemStack[] armour = player.getInventory().getArmorContents();
            boolean changed = false;

            for (ItemStack piece : armour) {
               ArmorMeta meta;
               ItemMeta itemMeta;
               if (piece != null && (itemMeta = piece.getItemMeta()) instanceof ArmorMeta && !(meta = (ArmorMeta)itemMeta).hasTrim()) {
                  meta.setTrim(new ArmorTrim(TrimMaterial.GOLD, pattern));
                  meta.getPersistentDataContainer().set(this.trimKey(), PersistentDataType.BYTE, (byte)1);
                  piece.setItemMeta(meta);
                  changed = true;
               }
            }

            if (changed) {
               player.getInventory().setArmorContents(armour);
            }
         }
      }
   }

   private void clearTrims(Player player) {
      ItemStack[] armour = player.getInventory().getArmorContents();
      boolean changed = false;

      for (ItemStack piece : armour) {
         ArmorMeta meta;
         ItemMeta itemMeta;
         if (piece != null
            && (itemMeta = piece.getItemMeta()) instanceof ArmorMeta
            && (meta = (ArmorMeta)itemMeta).getPersistentDataContainer().has(this.trimKey(), PersistentDataType.BYTE)) {
            meta.setTrim(null);
            meta.getPersistentDataContainer().remove(this.trimKey());
            piece.setItemMeta(meta);
            changed = true;
         }
      }

      if (changed) {
         player.getInventory().setArmorContents(armour);
      }
   }

   private NamespacedKey trimKey() {
      return new NamespacedKey(this.plugin, "gold_trim");
   }

   private TrimPattern trimPattern() {
      String name = this.plugin.getConfig().getString("gold.armour-trim.pattern", "FLOW");
      TrimPattern pattern = (TrimPattern)Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(name.toLowerCase()));
      return pattern != null ? pattern : TrimPattern.FLOW;
   }

   private File getSummonFile() {
      return new File(this.plugin.getDataFolder(), "gold.yml");
   }

   public boolean isSummoned() {
      return YamlConfiguration.loadConfiguration(this.getSummonFile()).getBoolean("summoned", false);
   }

   public void markSummoned(Player summoner) {
      File file = this.getSummonFile();
      YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
      data.set("summoned", true);
      data.set("summoned-by", summoner.getUniqueId().toString());
      data.set("summoned-by-name", summoner.getName());
      data.set("summoned-at", System.currentTimeMillis());

      try {
         data.save(file);
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to record the Gold Gem summon: " + e.getMessage());
      }
   }

   private GoldGemManager.Harvest deserialise(String stored) {
      String[] parts = stored.split(":");
      if (parts.length != 0 && !parts[0].isEmpty()) {
         int tier = 1;
         if (parts.length > 1) {
            try {
               tier = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
               tier = 1;
            }
         }

         UUID owner = null;
         if (parts.length > 2) {
            try {
               owner = UUID.fromString(parts[2]);
            } catch (IllegalArgumentException ignored) {
               owner = null;
            }
         }

         return new GoldGemManager.Harvest(parts[0], tier, owner);
      } else {
         return null;
      }
   }

   private File getPlayerFile(UUID playerId) {
      File folder = new File(this.plugin.getDataFolder(), "playerdata");
      folder.mkdirs();
      return new File(folder, playerId + ".yml");
   }

   public void load(UUID playerId) {
      File file = this.getPlayerFile(playerId);
      if (file.exists()) {
         YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
         List<String> souls = data.getStringList("gold.harvested");
         if (!souls.isEmpty()) {
            LinkedHashMap<String, GoldGemManager.Harvest> parsed = new LinkedHashMap<>();

            for (String soul : souls) {
               GoldGemManager.Harvest entry = this.deserialise(soul);
               if (entry != null) {
                  parsed.put(entry.gemId(), entry);
               }
            }

            this.harvested.put(playerId, parsed);
         }

         String selected;
         if ((selected = data.getString("gold.active")) != null) {
            this.active.put(playerId, selected);
         }

         String instance;
         if ((instance = data.getString("gold.instance")) != null) {
            try {
               this.trackedInstance.put(playerId, UUID.fromString(instance));
            } catch (IllegalArgumentException var11) {
            }
         }

         if (data.getBoolean("gold.trims-disabled", false)) {
            this.trimsDisabled.add(playerId);
         }
      }
   }

   public void save(UUID playerId) {
      File file = this.getPlayerFile(playerId);
      YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
      Map<String, GoldGemManager.Harvest> souls = this.getHarvested(playerId);
      if (souls.isEmpty()) {
         data.set("gold.harvested", null);
         data.set("gold.active", null);
      } else {
         List<String> serialised = new ArrayList<>();

         for (GoldGemManager.Harvest soul : souls.values()) {
            serialised.add(soul.gemId() + ":" + soul.tier() + (soul.owner() != null ? ":" + soul.owner().toString() : ""));
         }

         data.set("gold.harvested", serialised);
         data.set("gold.active", this.active.get(playerId));
      }

      UUID instance = this.trackedInstance.get(playerId);
      data.set("gold.instance", instance != null ? instance.toString() : null);
      data.set("gold.trims-disabled", this.trimsDisabled.contains(playerId) ? true : null);

      try {
         data.save(file);
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to save Gold Gem data for " + playerId + ": " + e.getMessage());
      }
   }

   public void unload(UUID playerId) {
      this.harvested.remove(playerId);
      this.active.remove(playerId);
      this.trackedInstance.remove(playerId);
      this.trimsDisabled.remove(playerId);
   }

   public record Harvest(String gemId, int tier, UUID owner) {
   }
}
