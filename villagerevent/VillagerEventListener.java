package dev.xoperr.blissgems.villagerevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.scheduler.BukkitTask;

public final class VillagerEventListener implements Listener {
   private final BlissGems plugin;
   private final VillagerEventManager manager;
   private final VillagerEventState state;
   private final VillagerEventItems items;
   private final Map<Location, BukkitTask> discLoops = new HashMap<>();
   private final Map<Location, Integer> lastDiscTick = new HashMap<>();
   private boolean finalTriggerPending;

   public VillagerEventListener(BlissGems plugin, VillagerEventManager manager) {
      this.plugin = plugin;
      this.manager = manager;
      this.state = manager.state();
      this.items = manager.items();
      Bukkit.getScheduler().runTaskTimer(plugin, this::backgroundScan, 80L, 80L);
   }

   @EventHandler
   public void onVillagerDeath(EntityDeathEvent event) {
      if (this.state.isEventRunning()) {
         if (event.getEntity() instanceof ZombieVillager zombie) {
            this.onZombieVillagerDeath(zombie);
         } else if (event.getEntity() instanceof Villager villager && !this.state.isSoulDropped(villager.getUniqueId())) {
            for (int id = 1; id <= 3; id++) {
               if (this.state.isVillagerAlive(id) && villager.getUniqueId().equals(this.state.villagerUuid(id))) {
                  this.dropSoul(id, villager);
                  return;
               }
            }

            if (villager.customName() != null && villager.getScoreboardTags().contains("BlissEventVillager")) {
               String name = PlainTextComponentSerializer.plainText().serialize(villager.customName());

               for (int id = 1; id <= 3; id++) {
                  if (this.state.isVillagerAlive(id)) {
                     UUID uuid = this.state.villagerUuid(id);
                     if ((uuid == null || Bukkit.getEntity(uuid) == null) && name.equals(VillagerEventItems.ownerName(id))) {
                        this.plugin
                           .getLogger()
                           .info("Villager event: soul " + id + " dropped via name fallback (registered entity " + uuid + " was replaced or missing).");
                        this.dropSoul(id, villager);
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private void dropSoul(int id, Entity at) {
      this.state.lockSoulDrop(at.getUniqueId());
      this.state.setVillagerAlive(id, false);
      this.state.markVillagerKilled(id);
      this.state.setVillagerLoc(id, at.getLocation());
      this.state.clearVillager(id);
      at.getWorld().dropItemNaturally(at.getLocation(), this.items.soul(id));
      at.getWorld().playSound(at.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 2.0F);
      this.state.setSoulLastPos(id, at.getLocation());
      this.state.save();
      this.checkRaidLost();
   }

   private void onZombieVillagerDeath(ZombieVillager zombie) {
      for (int id = 1; id <= 3; id++) {
         Location home = this.state.villagerLoc(id);
         if (this.state.isVillagerAlive(id)
            && this.state.villagerUuid(id) != null
            && home != null
            && home.getWorld() != null
            && home.getWorld().equals(zombie.getWorld())
            && home.distance(zombie.getLocation()) <= 5.0) {
            this.dropSoul(id, zombie);
            return;
         }
      }
   }

   private void checkRaidLost() {
      if (this.state.isFinalActive()) {
         for (int id = 1; id <= 3; id++) {
            if (this.state.isVillagerAlive(id)) {
               return;
            }
         }

         this.manager.failRaid();
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onVillagerTransform(EntityTransformEvent event) {
      if (this.state.isEventRunning() && event.getEntity() instanceof Villager v && this.isEventVillager(v)) {
         event.setCancelled(true);
      }
   }

   private boolean isEventVillager(Villager v) {
      if (v.getScoreboardTags().contains("BlissEventVillager")) {
         return true;
      }

      for (int id = 1; id <= 3; id++) {
         if (v.getUniqueId().equals(this.state.villagerUuid(id))) {
            return true;
         }
      }

      return false;
   }

   @EventHandler
   public void onVillagerDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Villager v) {
         for (int var4 = 1; var4 <= 3; var4++) {
            if (v.getUniqueId().equals(this.state.villagerUuid(var4))) {
               if (!this.state.isEventRunning()) {
                  event.setCancelled(true);
               }

               return;
            }
         }
      }
   }

   private boolean isRegisteredVillager(Villager v) {
      for (int id = 1; id <= 3; id++) {
         if (v.getUniqueId().equals(this.state.villagerUuid(id))) {
            return true;
         }
      }

      return false;
   }

   @EventHandler(priority = EventPriority.LOW)
   public void onEventVillagerInteract(PlayerInteractEntityEvent event) {
      if (event.getRightClicked() instanceof Villager v) {
         boolean registered = this.isRegisteredVillager(v);
         if (registered || v.getScoreboardTags().contains("BlissEventVillager")) {
            if (!this.state.isFinalActive() || !registered) {
               event.setCancelled(true);
               if (registered) {
                  event.getPlayer().sendMessage(PedestalManager.color("&7This villager won't trade until &6the Last Raid &7(phase 3)."));
               } else if (!this.state.isEventRunning()) {
                  v.remove();
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.LOW)
   public void onEarlyMerchantOpen(InventoryOpenEvent event) {
      if (!this.state.isFinalActive() && event.getInventory() instanceof MerchantInventory inv && inv.getMerchant() instanceof Villager v) {
         if (this.isRegisteredVillager(v) || v.getScoreboardTags().contains("BlissEventVillager")) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onPlayerDeath(PlayerDeathEvent event) {
      if (this.state.isEventRunning()) {
         Player p = event.getEntity();

         for (int id = 1; id <= 3; id++) {
            if (p.getName().equals(this.state.soulHolder(id))) {
               this.state.clearSoulHolder(id);
               this.state.setSoulLastPos(id, p.getLocation());
            }
         }
      }
   }

   @EventHandler
   public void onSoulDrop(PlayerDropItemEvent event) {
      if (this.state.isEventRunning()) {
         int id = this.items.soulIdOf(event.getItemDrop().getItemStack());
         if (id != -1 && event.getPlayer().getName().equals(this.state.soulHolder(id))) {
            this.state.clearSoulHolder(id);
            this.state.setSoulLastPos(id, event.getItemDrop().getLocation());
            this.state.save();
         }
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPickup(EntityPickupItemEvent event) {
      if (event.getEntity() instanceof Player p && this.state.isEventRunning()) {
         int id = this.items.soulIdOf(event.getItem().getItemStack());
         if (id != -1) {
            this.state.setSoulHolder(id, p.getName());
            this.state.clearSoulLastPos(id);
            this.state.save();
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player p = event.getPlayer();
      boolean changed = false;

      for (int id = 1; id <= 3; id++) {
         if (p.getName().equals(this.state.soulHolder(id))) {
            this.state.setSoulLastPos(id, p.getLocation());
            this.state.clearSoulHolder(id);
            changed = true;
         }
      }

      if (changed) {
         this.state.save();
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (this.state.isEventRunning() && event.getPlayer().isOnline()) {
            this.scanInventoryForSouls(event.getPlayer());
         }
      }, 20L);
   }

   private void backgroundScan() {
      if (this.state.isEventRunning()) {
         if (!this.finalTriggerPending && this.state.isActive() && !this.state.isFinalActive() && this.state.villagersReturned() >= 3) {
            this.manager.beginRaid();
         } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
               this.scanInventoryForSouls(p);
            }
         }
      }
   }

   private void scanInventoryForSouls(Player p) {
      for (ItemStack item : p.getInventory().getContents()) {
         int id = this.items.soulIdOf(item);
         if (id != -1 && !p.getName().equals(this.state.soulHolder(id))) {
            this.state.setSoulHolder(id, p.getName());
            this.state.clearSoulLastPos(id);
         }
      }
   }

   @EventHandler
   public void onSoulItemSpawn(ItemSpawnEvent event) {
      if (this.items.soulIdOf(event.getEntity().getItemStack()) > 0) {
         protect(event.getEntity());
         Location compass = this.state.compassLoc();
         Location at = event.getEntity().getLocation();
         if (this.state.isEventRunning()
            && compass != null
            && compass.getWorld() != null
            && compass.getWorld().equals(at.getWorld())
            && compass.distanceSquared(at) <= 1600.0) {
            event.getEntity().setGlowing(true);
         }
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onSoulItemDespawn(ItemDespawnEvent event) {
      if (this.items.soulIdOf(event.getEntity().getItemStack()) > 0) {
         event.setCancelled(true);
         protect(event.getEntity());
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onSoulItemDamaged(EntityDamageEvent event) {
      if (event.getEntity() instanceof Item item && event.getCause() != DamageCause.VOID && this.items.soulIdOf(item.getItemStack()) > 0) {
         event.setCancelled(true);
         protect(item);
         item.setFireTicks(0);
      }
   }

   private static void protect(Item item) {
      item.setWillAge(false);
      item.setInvulnerable(true);
   }

   @EventHandler
   public void onSoulBundleClick(InventoryClickEvent event) {
      ItemStack cursor = event.getCursor();
      ItemStack current = event.getCurrentItem();
      if (isBundle(cursor) && this.items.soulIdOf(current) > 0 || isBundle(current) && this.items.soulIdOf(cursor) > 0) {
         event.setCancelled(true);
      }
   }

   private static boolean isBundle(ItemStack item) {
      return item != null && item.getType().name().endsWith("BUNDLE");
   }

   @EventHandler(ignoreCancelled = true)
   public void onSoulPlace(BlockPlaceEvent event) {
      if (this.items.soulIdOf(event.getItemInHand()) != -1) {
         event.setCancelled(true);
         event.getPlayer().sendMessage(PedestalManager.color("&cThe soul is not a seed — take it to the compass."));
      }
   }

   @EventHandler
   public void onSoulUse(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND
         && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
         && this.state.isEventRunning()) {
         Player p = event.getPlayer();
         ItemStack hand = p.getInventory().getItemInMainHand();
         int id = this.items.soulIdOf(hand);
         if (id != -1) {
            event.setCancelled(true);
            Location compass = this.state.compassLoc();
            if (compass != null && compass.getWorld() != null && compass.getWorld().equals(p.getWorld()) && compass.distance(p.getLocation()) <= 10.0) {
               this.returnSoul(p, id, hand, compass);
            } else {
               this.summonSoul(p, id, hand);
            }
         }
      }
   }

   private void returnSoul(Player p, int id, ItemStack hand, Location compass) {
      boolean raid = this.state.isFinalActive();
      if (!raid && this.state.isVillagerReturned(id)) {
         p.sendMessage(PedestalManager.color("&cThis villager has already been returned."));
      } else {
         Villager old = this.state.getVillager(id);
         if (old != null && this.state.isVillagerAlive(id)) {
            this.state.clearVillager(id);
            old.remove();
         }

         this.state.setVillagerAlive(id, false);
         this.items.spawnVillager(id, compass, this.state);
         hand.setAmount(hand.getAmount() - 1);
         this.state.clearSoulHolder(id);
         this.state.clearSoulLastPos(id);
         compass.getWorld().playSound(compass, Sound.ENTITY_VILLAGER_CELEBRATE, 2.0F, 1.0F);
         if (raid) {
            this.state.save();
            p.sendMessage(VillagerEventItems.villagerName(id).append(VillagerEventItems.text("&7 stands again.")));
         } else {
            this.state.incrementVillagersReturned();
            this.state.markVillagerReturned(id);
            if (this.state.villagersReturned() >= 3) {
               this.finalTriggerPending = true;
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                  this.finalTriggerPending = false;
                  this.manager.beginRaid();
               }, 60L);
            }
         }
      }
   }

   private void summonSoul(Player p, int id, ItemStack hand) {
      if (this.state.isFinalActive()) {
         p.sendMessage(PedestalManager.color("&cBring the soul to the compass — the village is under attack."));
      } else {
         if (this.state.isVillagerAlive(id) && this.state.getVillager(id) == null) {
            this.state.setVillagerAlive(id, false);
         }

         if (this.state.isVillagerAlive(id)) {
            p.sendMessage(PedestalManager.color("&cVillager " + id + " is already alive somewhere! Kill them first."));
         } else {
            this.items.spawnVillager(id, p.getLocation(), this.state);
            hand.setAmount(hand.getAmount() - 1);
            this.state.clearSoulHolder(id);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 2.0F, 0.8F);
         }
      }
   }

   @EventHandler
   public void onMerchantOpen(InventoryOpenEvent event) {
      if (this.state.isEventRunning() && event.getPlayer() instanceof Player p && event.getInventory() instanceof MerchantInventory inv) {
         if (inv.getMerchant() instanceof Villager v) {
            for (int var8 = 1; var8 <= 3; var8++) {
               if (v.getUniqueId().equals(this.state.villagerUuid(var8))) {
                  this.state.setTradingWith(p.getUniqueId(), var8);
                  return;
               }
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      this.state.clearTradingWith(event.getPlayer().getUniqueId());
   }

   @EventHandler
   public void onTradeClick(InventoryClickEvent event) {
      if (this.state.isEventRunning() && event.getWhoClicked() instanceof Player p && event.getView().getTopInventory() instanceof MerchantInventory inv) {
         Integer var11 = this.state.tradingWith(p.getUniqueId());
         ItemStack result = event.getCurrentItem();
         if (var11 != null && event.getRawSlot() == 2 && result != null && !result.getType().isAir()) {
            VillagerEventItems.TokenTrade trade = VillagerEventItems.TokenTrade.match(result);
            if (trade != null) {
               event.setCancelled(true);
               if (this.state.isTradeBought(var11, trade.key)) {
                  p.sendMessage(PedestalManager.color("&cThis trade has already been claimed!"));
                  p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               } else {
                  int have = this.countTokens(p, inv);
                  if (have < trade.cost) {
                     p.sendMessage(PedestalManager.color("&cYou need " + trade.cost + " Energy Tokens for this! You only have " + have + "."));
                     p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  } else {
                     this.removeTokens(p, inv, trade.cost);

                     for (ItemStack left : p.getInventory().addItem(new ItemStack[]{trade.build()}).values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), left);
                     }

                     this.state.setTradeBought(var11, trade.key);
                     this.state.save();
                     p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                     p.sendMessage(PedestalManager.color("&aYou bought &f" + trade.displayName + "&a for &6" + trade.cost + " Energy Tokens&a!"));
                  }
               }
            }
         }
      }
   }

   private int countTokens(Player p, MerchantInventory inv) {
      int n = 0;

      for (ItemStack item : p.getInventory().getContents()) {
         if (this.items.isEnergyToken(item)) {
            n += item.getAmount();
         }
      }

      for (int s = 0; s < 2; s++) {
         ItemStack item = inv.getItem(s);
         if (this.items.isEnergyToken(item)) {
            n += item.getAmount();
         }
      }

      return n;
   }

   private void removeTokens(Player p, MerchantInventory inv, int amount) {
      for (int s = 0; s < 2 && amount > 0; s++) {
         ItemStack item = inv.getItem(s);
         if (this.items.isEnergyToken(item)) {
            int take = Math.min(amount, item.getAmount());
            item.setAmount(item.getAmount() - take);
            inv.setItem(s, item.getAmount() <= 0 ? null : item);
            amount -= take;
         }
      }

      if (amount > 0) {
         ItemStack[] contents = p.getInventory().getContents();

         for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack item = contents[i];
            if (this.items.isEnergyToken(item)) {
               int take = Math.min(amount, item.getAmount());
               item.setAmount(item.getAmount() - take);
               if (item.getAmount() <= 0) {
                  contents[i] = null;
               }

               amount -= take;
            }
         }

         p.getInventory().setContents(contents);
      }
   }

   @EventHandler
   public void onConvertEnergy(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND
         && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
         && this.state.isEventRunning()) {
         Player p = event.getPlayer();
         ItemStack hand = p.getInventory().getItemInMainHand();
         if (this.items.isBottledEnergy(hand)) {
            event.setCancelled(true);
            hand.setAmount(hand.getAmount() - 1);

            for (ItemStack left : p.getInventory().addItem(new ItemStack[]{this.items.energyToken()}).values()) {
               p.getWorld().dropItemNaturally(p.getLocation(), left);
            }

            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
            p.sendMessage(PedestalManager.color("<##90EE90>⚗ Converted 1 &b&lBottled Energy &r<##90EE90>into an &a&lEnergy Token&r<##90EE90>. Irreversible."));
         }
      }
   }

   @EventHandler
   public void onJukebox(PlayerInteractEvent event) {
      Block block = event.getClickedBlock();
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && block.getType() == Material.JUKEBOX && block.getState() instanceof Jukebox jukebox) {
         Player var10 = event.getPlayer();
         Location top = block.getLocation().add(0.5, 0.6, 0.5);
         ItemStack record = jukebox.getRecord();
         if (record == null || record.getType() == Material.AIR) {
            ItemStack hand = event.getItem();
            if (hand != null && this.items.isVillageDisc(hand)) {
               event.setCancelled(true);
               if (this.claimTick(block.getLocation())) {
                  ItemStack in = hand.clone();
                  in.setAmount(1);
                  jukebox.setRecord(in);
                  jukebox.stopPlaying();
                  jukebox.update();
                  hand.setAmount(hand.getAmount() - 1);
                  if (!this.plugin.getConfig().getBoolean("villager-event.clue-audio", true)) {
                     this.revealCoords(var10);
                  } else {
                     String sound = this.plugin.getConfig().getString("villager-event.disc-song-key", "bliss:village_clue");
                     block.getWorld().playSound(top, sound, SoundCategory.RECORDS, 4.0F, 1.0F);
                     this.startDiscLoop(block, sound);
                  }
               }
            }
         } else if (event.getHand() == EquipmentSlot.HAND && this.items.isVillageDisc(record)) {
            event.setCancelled(true);
            if (this.claimTick(block.getLocation())) {
               ItemStack out = record.clone();
               jukebox.setRecord(null);
               jukebox.stopPlaying();
               jukebox.update();
               if (!var10.getInventory().addItem(new ItemStack[]{out}).isEmpty()) {
                  block.getWorld().dropItemNaturally(top, out);
               }

               this.stopDisc(block);
            }
         }
      }
   }

   private boolean claimTick(Location loc) {
      int now = Bukkit.getCurrentTick();
      Integer last = this.lastDiscTick.get(loc);
      if (last != null && last == now) {
         return false;
      }

      if (this.lastDiscTick.size() > 256) {
         this.lastDiscTick.clear();
      }

      this.lastDiscTick.put(loc, now);
      return true;
   }

   private void revealCoords(Player p) {
      Location c = this.state.compassLoc();
      if (c == null) {
         p.showTitle(
            Title.title(
               VillagerEventItems.text("&e&k████████"),
               Component.empty(),
               Times.times(Duration.ofMillis(400L), Duration.ofMillis(2500L), Duration.ofMillis(600L))
            )
         );
      } else {
         String coords = c.getBlockX() + "   " + c.getBlockY() + "   " + c.getBlockZ();
         int n = coords.length();

         for (int step = 0; step <= n; step++) {
            int shown = step;
            Bukkit.getScheduler()
               .runTaskLater(
                  this.plugin,
                  () -> {
                     Player online = Bukkit.getPlayer(p.getUniqueId());
                     if (online != null) {
                        boolean first = shown == 0;
                        boolean last = shown == n;
                        online.showTitle(
                           Title.title(
                              VillagerEventItems.text(frame(coords, shown)),
                              Component.empty(),
                              Times.times(Duration.ofMillis(first ? 300L : 0L), Duration.ofMillis(last ? 4000L : 2400L), Duration.ofMillis(last ? 1200L : 0L))
                           )
                        );
                     }
                  },
                  step * 11L
               );
         }
      }
   }

   private static String frame(String coords, int shown) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < coords.length(); i++) {
         char ch = coords.charAt(i);
         if (ch == ' ') {
            sb.append(' ');
         } else if (i < shown) {
            sb.append("&e").append(ch);
         } else if (i < shown + 4) {
            sb.append("&e&k").append(ch).append("&r");
         }
      }

      return sb.toString();
   }

   private void startDiscLoop(Block block, String sound) {
      Location loc = block.getLocation();
      this.stopDiscLoop(loc);
      this.discLoops.put(loc, Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            if (loc.getBlock().getState() instanceof Jukebox j && this.items.isVillageDisc(j.getRecord())) {
               loc.getWorld().playSound(loc.clone().add(0.5, 0.6, 0.5), sound, SoundCategory.RECORDS, 4.0F, 1.0F);
            } else {
               this.stopDiscLoop(loc);
            }
         }
      }, 406L, 406L));
   }

   private void stopDisc(Block block) {
      String sound = this.plugin.getConfig().getString("villager-event.disc-song-key", "bliss:village_clue");

      for (Player p : block.getWorld().getPlayers()) {
         if (p.getLocation().distanceSquared(block.getLocation()) <= 4096.0) {
            p.stopSound(sound, SoundCategory.RECORDS);
         }
      }

      this.stopDiscLoop(block.getLocation());
   }

   private void stopDiscLoop(Location loc) {
      BukkitTask t = this.discLoops.remove(loc);
      if (t != null) {
         t.cancel();
      }
   }
}
