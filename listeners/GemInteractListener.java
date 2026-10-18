package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.managers.FluxEnergyManager;
import dev.xoperr.blissgems.managers.GemLockManager;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.util.RayTraceResult;

public class GemInteractListener implements Listener {
   private final BlissGems plugin;
   private final Map<UUID, Long> traderCooldowns;
   private final Map<UUID, Long> clickDisabledMessageCooldowns = new HashMap<>();
   private static final long CLICK_DISABLED_MSG_INTERVAL = 30000L;
   private static final long TRADE_ABILITY_GRACE_MS = 500L;

   public GemInteractListener(BlissGems plugin) {
      this.plugin = plugin;
      this.traderCooldowns = new HashMap<>();
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      Action a = event.getAction();
      boolean isRight = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
      boolean isLeft = a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK;
      if (isRight || isLeft) {
         if (isLeft) {
            this.handleLeftClickAbility(event);
         } else {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null) {
               ItemStack mainHand = player.getInventory().getItemInMainHand();
               ItemStack offHand = player.getInventory().getItemInOffHand();
               String mainHandId = CustomItemManager.getIdByItem(mainHand);
               String offHandId = CustomItemManager.getIdByItem(offHand);
               boolean hasGemInMainHand = mainHandId != null && GemType.isGem(mainHandId);
               boolean hasGemInOffHand = offHandId != null && GemType.isGem(offHandId);
               boolean hasBundleInMainHand = mainHand != null && mainHand.getType() == Material.BUNDLE;
               boolean hasBundleInOffHand = offHand != null && offHand.getType() == Material.BUNDLE;
               if ((!hasGemInMainHand || !hasBundleInOffHand)
                  && (!hasGemInOffHand || !hasBundleInMainHand)
                  && (!hasGemInMainHand || item == null || item.getType() != Material.BUNDLE)
                  && (!hasGemInOffHand || item == null || item.getType() != Material.BUNDLE)) {
                  if (item == null || item.getType() != Material.BUNDLE || !hasGemInMainHand && !hasGemInOffHand) {
                     if (item != null && item.getType() == Material.BUNDLE && this.bundleContainsGem(item)) {
                        event.setCancelled(true);
                        player.sendMessage("§c§lThis bundle contains a gem! Remove the gem first before using it.");
                     } else {
                        String oraxenId = CustomItemManager.getIdByItem(item);
                        if (oraxenId != null) {
                           switch (oraxenId) {
                              case "energy_bottle":
                                 this.handleEnergyBottle(player, item, event);
                                 break;
                              case "gem_trader":
                                 this.handleGemTrader(player, item, event);
                           }

                           if (oraxenId.endsWith("_gem_t1") || oraxenId.endsWith("_gem_t2")) {
                              this.handleGemAbility(player, oraxenId, event);
                           }
                        }
                     }
                  } else {
                     event.setCancelled(true);
                     String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
                     if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                     }
                  }
               } else {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }
               }
            }
         }
      }
   }

   private void handleGemAbility(Player player, String oraxenId, PlayerInteractEvent event) {
      event.setCancelled(true);
      Long lastTrade = this.traderCooldowns.get(player.getUniqueId());
      if (lastTrade == null || System.currentTimeMillis() - lastTrade >= 500L) {
         AbilityBinding input = AbilityBinding.rightClick(player.isSneaking());
         this.dispatchBoundAbility(player, input);
      }
   }

   private void handleLeftClickAbility(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      String id = item != null ? CustomItemManager.getIdByItem(item) : null;
      boolean heldGem = id != null && (id.endsWith("_gem_t1") || id.endsWith("_gem_t2"));
      boolean offhandGold = this.plugin.getGoldGemManager() != null && this.plugin.getGoldGemManager().isGoldGem(player.getInventory().getItemInOffHand());
      if (heldGem || offhandGold && event.getAction() == Action.LEFT_CLICK_AIR) {
         if (player.isSneaking()) {
            if (this.plugin.getSoulManager() != null
               && System.currentTimeMillis() - this.plugin.getSoulManager().getLastCaptureTime(player.getUniqueId()) < 1000L) {
               return;
            }

            RayTraceResult trace = player.getWorld()
               .rayTraceEntities(
                  player.getEyeLocation(),
                  player.getEyeLocation().getDirection(),
                  4.5,
                  0.5,
                  e -> e instanceof LivingEntity && !(e instanceof Player) && !e.equals(player)
               );
            if (trace != null && trace.getHitEntity() != null) {
               return;
            }
         }

         AbilityBinding input = AbilityBinding.leftClick(player.isSneaking());
         AbilitySlot slot = this.plugin.getAbilityBindingManager() != null ? this.plugin.getAbilityBindingManager().getSlot(player, input) : null;
         if (slot != null) {
            boolean extraSlot = slot == AbilitySlot.QUINARY || slot == AbilitySlot.SENARY;
            if (!extraSlot || offhandGold || "gold_gem_t1".equals(id)) {
               event.setCancelled(true);
               if (this.gateAbility(player)) {
                  this.plugin.getBlissCommand().triggerSlot(player, slot);
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onGemHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         if (event.getCause() == DamageCause.ENTITY_ATTACK) {
            String id = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand());
            if (id != null && (id.endsWith("_gem_t1") || id.endsWith("_gem_t2"))) {
               boolean astra = id.startsWith("astra_gem");
               if (!astra || !player.isSneaking() || event.getEntity() instanceof Player) {
                  if (astra && !player.isSneaking() && event.getEntity() instanceof Player) {
                     if (this.gateAbility(player) && this.plugin.getAstraUnbounded() != null) {
                        this.plugin.getAstraUnbounded().start(player, (Player)event.getEntity());
                     }
                  } else {
                     AbilitySlot slot = this.plugin.getAbilityBindingManager() != null
                        ? this.plugin.getAbilityBindingManager().getSlot(player, AbilityBinding.leftClick(player.isSneaking()))
                        : null;
                     if (slot != null) {
                        if (slot != AbilitySlot.QUINARY && slot != AbilitySlot.SENARY || "gold_gem_t1".equals(id)) {
                           if (this.gateAbility(player)) {
                              this.plugin.getBlissCommand().triggerSlot(player, slot);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void dispatchBoundAbility(Player player, AbilityBinding input) {
      if (this.gateAbility(player)) {
         AbilitySlot slot = this.plugin.getAbilityBindingManager() != null ? this.plugin.getAbilityBindingManager().getSlot(player, input) : null;
         if (slot != null) {
            this.plugin.getBlissCommand().triggerSlot(player, slot);
         } else {
            String oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand());
            if (oraxenId == null || !oraxenId.endsWith("_gem_t1") && !oraxenId.endsWith("_gem_t2")) {
               oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInOffHand());
            }

            if (oraxenId != null) {
               int tier = oraxenId.endsWith("_gem_t2") ? 2 : 1;
               GemType gemType = GemType.fromOraxenId(oraxenId);
               if (gemType != null) {
                  switch (gemType) {
                     case ASTRA:
                        this.plugin.getAstraAbilities().onRightClick(player, tier);
                        return;
                     case FIRE:
                        this.plugin.getFireAbilities().onRightClick(player, tier);
                        return;
                     case FLUX:
                        this.plugin.getFluxAbilities().onRightClick(player, tier);
                        return;
                     case LIFE:
                        this.plugin.getLifeAbilities().onRightClick(player, tier);
                        return;
                     case PUFF:
                        this.plugin.getPuffAbilities().onRightClick(player, tier);
                        return;
                     case SPEED:
                        this.plugin.getSpeedAbilities().onRightClick(player, tier);
                        return;
                     case STRENGTH:
                        this.plugin.getStrengthAbilities().onRightClick(player, tier);
                        return;
                     case WEALTH:
                        this.plugin.getWealthAbilities().onRightClick(player, tier);
                        return;
                  }
               }
            }
         }
      }
   }

   private boolean gateAbility(Player player) {
      GemLockManager lockMgr = this.plugin.getGemLockManager();
      if (lockMgr != null && lockMgr.isLocked(player)) {
         int left = lockMgr.getRemainingSeconds(player.getUniqueId());
         player.sendMessage("§6ꐬ §c§oYour gem is locked! §7(" + left + "s)");
         return false;
      }

      if (this.plugin.getClickActivationManager().isClickActivationEnabled(player)) {
         int energy = this.plugin.getEnergyManager().getEnergy(player);
         if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy");
            if (msg != null && !msg.isEmpty()) {
               player.sendMessage(msg);
            }

            return false;
         } else {
            return true;
         }
      } else {
         long now = System.currentTimeMillis();
         Long lastSent = this.clickDisabledMessageCooldowns.get(player.getUniqueId());
         if (lastSent == null || now - lastSent >= 30000L) {
            this.clickDisabledMessageCooldowns.put(player.getUniqueId(), now);
            String msg = this.plugin.getConfigManager().getFormattedMessage("click-activation-disabled");
            if (msg != null && !msg.isEmpty()) {
               player.sendMessage(msg);
            }
         }

         return false;
      }
   }

   private void handleEnergyBottle(Player player, ItemStack item, PlayerInteractEvent event) {
      event.setCancelled(true);
      int currentEnergy = this.plugin.getEnergyManager().getEnergy(player);
      int maxEnergy = this.plugin.getConfigManager().getMaxEnergy();
      if (currentEnergy <= 0) {
         player.sendMessage("§c§oYour gem is §c§lBROKEN§c§o! Only a Restoration Ritual can reforge it.");
      } else if (currentEnergy < maxEnergy) {
         this.plugin.getEnergyManager().addEnergy(player, 1);
         if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
         } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
         } else {
            player.getInventory().setItemInMainHand(null);
         }

         if (this.plugin.getConfig().getBoolean("energy-bottle.play-effects", true)) {
            this.plugin.getConfigManager().playConfiguredEffect(player, "energy-bottle", Particle.HEART, 20, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
         }

         String msg;
         if ((msg = this.plugin.getConfigManager().getFormattedMessage("energy-bottle-consumed")) != null && !msg.isEmpty()) {
            player.sendMessage(msg);
         }
      } else {
         String msg2 = this.plugin.getConfigManager().getFormattedMessage("energy-already-max");
         if (msg2 != null && !msg2.isEmpty()) {
            player.sendMessage(msg2);
         } else {
            player.sendMessage("§c§oYou already have maximum energy!");
         }
      }
   }

   private void handleGemTrader(Player player, ItemStack traderItem, PlayerInteractEvent event) {
      event.setCancelled(true);
      long now = System.currentTimeMillis();
      Long lastUse = this.traderCooldowns.get(player.getUniqueId());
      int cooldownSeconds = this.plugin.getConfigManager().getTraderCooldown();
      long timeLeft;
      if (lastUse != null && (timeLeft = lastUse + cooldownSeconds * 1000L - now) > 0L) {
         int secondsLeft = (int)Math.ceil(timeLeft / 1000.0);
         String msg = this.plugin.getConfigManager().getFormattedMessage("trade-cooldown", "seconds", secondsLeft);
         if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
         }
      } else if (!this.plugin.getGemManager().hasActiveGem(player)) {
         String msg = this.plugin.getConfigManager().getFormattedMessage("no-gem");
         if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
         }
      } else {
         String currentGemId = this.plugin.getGemManager().getGemId(player);
         ArrayList<String> availableGems = new ArrayList<>(this.plugin.getGemManager().getAvailableGemIds());
         availableGems.remove(currentGemId);
         if (availableGems.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No other gem types available!");
         } else {
            String newGemId = availableGems.get((int)(Math.random() * availableGems.size()));
            if (this.plugin.getGemManager().replaceGem(player, newGemId)) {
               if (traderItem.getAmount() > 1) {
                  traderItem.setAmount(traderItem.getAmount() - 1);
               } else {
                  player.getInventory().setItemInMainHand(null);
               }

               this.traderCooldowns.put(player.getUniqueId(), now);
               if (this.plugin.getAchievementManager() != null) {
                  this.plugin.getAchievementManager().unlock(player, Achievement.TIME_FOR_A_CHANGE);
               }

               if (this.plugin.getConfigManager().shouldPlayTradeEffects()) {
                  this.plugin.getConfigManager().playConfiguredEffect(player, "trader", Particle.PORTAL, 30, Sound.ENTITY_ENDERMAN_TELEPORT);
               }

               String msg;
               if ((msg = this.plugin.getConfigManager().getFormattedMessage("trade-success", "gem", this.plugin.getGemManager().getGemDisplayName(newGemId)))
                     != null
                  && !msg.isEmpty()) {
                  player.sendMessage(msg);
               }
            } else {
               player.sendMessage(ChatColor.RED + "Failed to trade gem!");
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (FluxEnergyManager.isDeposit(event.getView().getTopInventory())) {
         if (this.plugin.getFluxEnergyManager() != null) {
            this.plugin.getFluxEnergyManager().onInventoryClick(event);
         }
      } else {
         if (event.getWhoClicked() instanceof Player player) {
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            if (clickedItem != null && cursorItem != null && !cursorItem.getType().isAir() && this.plugin.getFluxEnergyManager() != null) {
               String clickedGem = CustomItemManager.getIdByItem(clickedItem);
               if (clickedGem != null && clickedGem.startsWith("flux_gem")) {
                  int fuelVal = this.plugin.getFluxEnergyManager().getFuelWattValue(cursorItem.getType());
                  if (fuelVal > 0) {
                     event.setCancelled(true);
                     UUID uuid = player.getUniqueId();
                     double curWatts = this.plugin.getFluxEnergyManager().getWatts(uuid);
                     int maxWatts = this.plugin.getFluxEnergyManager().getMaxWatts();
                     if (curWatts >= maxWatts) {
                        player.sendMessage("§cYour Flux Gem is already at maximum capacity!");
                        return;
                     }

                     int cursorAmount = cursorItem.getAmount();
                     double needed = maxWatts - curWatts;
                     int countToUse = Math.min(cursorAmount, (int)Math.ceil(needed / fuelVal));
                     if (countToUse <= 0) {
                        countToUse = 1;
                     }

                     double added = Math.min(needed, (double)countToUse * fuelVal);
                     this.plugin.getFluxEnergyManager().addWatts(uuid, added);
                     cursorItem.setAmount(cursorAmount - countToUse);
                     player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
                     player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 1.0F, 1.2F);
                     player.sendMessage(
                        "§b⚡ Charged Flux Gem with §f"
                           + countToUse
                           + "x "
                           + cursorItem.getType().name()
                           + " §b(+§f"
                           + String.format("%,.0f", added)
                           + " §bwatts)!"
                     );
                     return;
                  }
               }
            }

            ItemStack hotbarItem = null;
            int hotbarSlot;
            if (event.getClick().toString().contains("NUMBER_KEY") && (hotbarSlot = event.getHotbarButton()) >= 0 && hotbarSlot < 9) {
               hotbarItem = player.getInventory().getItem(hotbarSlot);
            }

            PlayerInventory inv;
            boolean inventoryFull = (inv = player.getInventory()).firstEmpty() == -1;
            if (inventoryFull && event.getClickedInventory() == inv) {
               String clickedId = clickedItem != null ? CustomItemManager.getIdByItem(clickedItem) : null;
               if (clickedId != null && GemType.isGem(clickedId)) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-move-gem-full-inventory");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }

                  return;
               }

               String slotId;
               if (cursorItem != null
                  && cursorItem.getType() != Material.AIR
                  && clickedItem != null
                  && (slotId = CustomItemManager.getIdByItem(clickedItem)) != null
                  && GemType.isGem(slotId)) {
                  event.setCancelled(true);
                  player.sendMessage("§c§lYou cannot swap items with your gem when inventory is full!");
                  return;
               }
            }

            if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
               int slot;
               if (event.getClickedInventory() == event.getView().getTopInventory() && (slot = event.getRawSlot()) >= 1 && slot <= 4) {
                  String cursorId2;
                  if (cursorItem != null
                     && cursorItem.getType() != Material.AIR
                     && (cursorId2 = CustomItemManager.getIdByItem(cursorItem)) != null
                     && GemType.isGem(cursorId2)) {
                     event.setCancelled(true);
                     return;
                  }

                  String hotbarId;
                  if (hotbarItem != null && (hotbarId = CustomItemManager.getIdByItem(hotbarItem)) != null && GemType.isGem(hotbarId)) {
                     event.setCancelled(true);
                     return;
                  }
               }

               String clickedOraxenId;
               if (event.isShiftClick()
                  && event.getClickedInventory() == player.getInventory()
                  && clickedItem != null
                  && (clickedOraxenId = CustomItemManager.getIdByItem(clickedItem)) != null
                  && GemType.isGem(clickedOraxenId)) {
                  event.setCancelled(true);
                  return;
               }
            }

            boolean hasContainerOpen = event.getView().getTopInventory() != null && event.getView().getTopInventory().getHolder() != player;
            if (hasContainerOpen) {
               String offhandOraxenId;
               ItemStack offhandItem;
               if (event.getClick().toString().equals("SWAP_OFFHAND")
                  && (offhandItem = player.getInventory().getItemInOffHand()) != null
                  && (offhandOraxenId = CustomItemManager.getIdByItem(offhandItem)) != null
                  && GemType.isGem(offhandOraxenId)) {
                  event.setCancelled(true);
                  ComprehensiveGemProtectionListener.resyncOffhand(this.plugin, player);
                  String msg2 = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                  if (msg2 != null && !msg2.isEmpty()) {
                     player.sendMessage(msg2);
                  }

                  return;
               }

               String cursorOraxenId2;
               if (event.getClickedInventory() != player.getInventory()
                  && cursorItem != null
                  && (cursorOraxenId2 = CustomItemManager.getIdByItem(cursorItem)) != null
                  && GemType.isGem(cursorOraxenId2)) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }

                  return;
               }

               String clickedOraxenId;
               if (event.getClick().isShiftClick()
                  && event.getClickedInventory() == player.getInventory()
                  && clickedItem != null
                  && (clickedOraxenId = CustomItemManager.getIdByItem(clickedItem)) != null
                  && GemType.isGem(clickedOraxenId)) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }

                  return;
               }

               String hotbarOraxenId;
               if (event.getClickedInventory() != player.getInventory()
                  && hotbarItem != null
                  && (hotbarOraxenId = CustomItemManager.getIdByItem(hotbarItem)) != null
                  && GemType.isGem(hotbarOraxenId)) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }

                  return;
               }

               if (event.getClick().toString().equals("DOUBLE_CLICK")
                  && cursorItem != null
                  && (cursorOraxenId2 = CustomItemManager.getIdByItem(cursorItem)) != null
                  && GemType.isGem(cursorOraxenId2)) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }

                  return;
               }
            }

            boolean hasBundleInInventory = false;
            boolean hasGemInInventory = false;
            if (clickedItem != null) {
               if (clickedItem.getType() == Material.BUNDLE) {
                  hasBundleInInventory = true;
                  String cursorId = cursorItem != null ? CustomItemManager.getIdByItem(cursorItem) : null;
                  if (cursorId != null && GemType.isGem(cursorId)) {
                     event.setCancelled(true);
                     String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
                     if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                     }

                     return;
                  }
               }

               String clickedId;
               if ((clickedId = CustomItemManager.getIdByItem(clickedItem)) != null && GemType.isGem(clickedId)) {
                  hasGemInInventory = true;
                  if (cursorItem != null && cursorItem.getType() == Material.BUNDLE) {
                     event.setCancelled(true);
                     String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
                     if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                     }

                     return;
                  }
               }
            }

            if (cursorItem != null) {
               if (cursorItem.getType() == Material.BUNDLE) {
                  hasBundleInInventory = true;
               }

               String cursorId;
               if ((cursorId = CustomItemManager.getIdByItem(cursorItem)) != null && GemType.isGem(cursorId)) {
                  hasGemInInventory = true;
               }
            }

            if (hotbarItem != null) {
               if (hotbarItem.getType() == Material.BUNDLE && hasGemInInventory) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }

                  return;
               }

               String hotbarId = CustomItemManager.getIdByItem(hotbarItem);
               if (hotbarId != null && GemType.isGem(hotbarId) && hasBundleInInventory) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }

                  return;
               }
            }

            String cursorOraxenId;
            if (this.plugin.getConfigManager().isSingleGemOnly()
               && cursorItem != null
               && event.getClickedInventory() == player.getInventory()
               && (cursorOraxenId = CustomItemManager.getIdByItem(cursorItem)) != null
               && GemType.isGem(cursorOraxenId)
               && this.countGemsInInventory(player) > 0) {
               boolean isMovingOwnGem = false;
               String clickedOraxenId;
               if (clickedItem != null && (clickedOraxenId = CustomItemManager.getIdByItem(clickedItem)) != null && GemType.isGem(clickedOraxenId)) {
                  isMovingOwnGem = true;
               }

               if (!isMovingOwnGem) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("already-have-gem");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }

                  return;
               }
            }

            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.plugin.getGemManager().updateActiveGem(player), 1L);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onEntityPickupItem(EntityPickupItemEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         Item itemEntity = event.getItem();
         ItemStack item = itemEntity.getItemStack();
         String oraxenId = CustomItemManager.getIdByItem(item);
         if (this.plugin.getConfigManager().isSingleGemOnly()) {
            if (oraxenId != null && GemType.isGem(oraxenId) && this.countGemsInInventory(player) > 0 && player.getInventory().firstEmpty() == -1) {
               event.setCancelled(true);
            } else if (oraxenId != null && GemType.isGem(oraxenId)) {
               int currentGemCount = this.countGemsInInventory(player);
               if (currentGemCount > 0) {
                  event.setCancelled(true);
                  String msg = this.plugin.getConfigManager().getFormattedMessage("already-have-gem");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         ItemStack draggedItem = event.getOldCursor();
         String draggedId;
         if (draggedItem != null
            && (draggedId = CustomItemManager.getIdByItem(draggedItem)) != null
            && GemType.isGem(draggedId)
            && event.getInventory().getType() != InventoryType.PLAYER) {
            for (int slot : event.getRawSlots()) {
               if (slot < event.getView().getTopInventory().getSize()) {
                  event.setCancelled(true);
                  player.sendMessage("§c§lYou cannot move gems to other containers!");
                  return;
               }
            }
         }

         if (this.countGemsInInventory(player) > 0 && player.getInventory().firstEmpty() == -1) {
            for (int slot : event.getRawSlots()) {
               if (slot >= event.getView().getTopInventory().getSize()) {
                  event.setCancelled(true);
                  player.sendMessage("§c§lYour inventory is full! Cannot move items while carrying a gem.");
                  return;
               }
            }
         }
      }
   }

   private int countGemsInInventory(Player player) {
      int count = 0;

      for (ItemStack item : player.getInventory().getContents()) {
         if (item != null) {
            String oraxenId = CustomItemManager.getIdByItem(item);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
               count++;
            } else if (item.getType() == Material.BUNDLE && this.bundleContainsGem(item)) {
               count++;
            }
         }
      }

      return count;
   }

   private boolean bundleContainsGem(ItemStack bundle) {
      if (bundle != null && bundle.getType() == Material.BUNDLE) {
         BundleMeta bundleMeta;
         if (bundle.getItemMeta() instanceof BundleMeta && (bundleMeta = (BundleMeta)bundle.getItemMeta()).hasItems()) {
            for (ItemStack bundledItem : bundleMeta.getItems()) {
               String itemId;
               if (bundledItem != null && (itemId = CustomItemManager.getIdByItem(bundledItem)) != null && GemType.isGem(itemId)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @EventHandler
   public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
      Player player = event.getPlayer();
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof Allay) {
         String mainHandId = CustomItemManager.getIdByItem(mainHand);
         String offHandId = CustomItemManager.getIdByItem(offHand);
         if (mainHandId != null && GemType.isGem(mainHandId) || offHandId != null && GemType.isGem(offHandId)) {
            event.setCancelled(true);
            String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-place-gem-itemframe");
            if (msg != null && !msg.isEmpty()) {
               player.sendMessage(msg);
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player) {
         Player player = (Player)event.getPlayer();
         if (FluxEnergyManager.isDeposit(event.getInventory())) {
            if (this.plugin.getFluxEnergyManager() != null) {
               this.plugin.getFluxEnergyManager().onInventoryClose(event);
            }
         } else {
            if (event.getView().getTitle().equals("§6§lPockets")) {
               this.plugin.getWealthAbilities().savePocketsInventory(player.getUniqueId());
            }

            if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
               for (int i = 1; i <= 4; i++) {
                  ItemStack craftSlot = event.getView().getTopInventory().getItem(i);
                  String craftId;
                  if (craftSlot != null
                     && !craftSlot.getType().isAir()
                     && (craftId = CustomItemManager.getIdByItem(craftSlot)) != null
                     && GemType.isGem(craftId)) {
                     event.getView().getTopInventory().setItem(i, null);
                     this.forceGemIntoInventory(player, craftSlot);
                  }
               }
            }

            String cursorId;
            ItemStack cursorItem;
            if ((cursorItem = player.getItemOnCursor()) != null
               && cursorItem.getType() != Material.AIR
               && (cursorId = CustomItemManager.getIdByItem(cursorItem)) != null
               && GemType.isGem(cursorId)) {
               player.setItemOnCursor(null);
               this.forceGemIntoInventory(player, cursorItem);
            }

            this.enforceOneGemOnly(player);
         }
      }
   }

   private void enforceOneGemOnly(Player player) {
      if (this.plugin.getConfigManager().isSingleGemOnly()) {
         PlayerInventory inv = player.getInventory();
         boolean foundFirstGem = false;
         ArrayList<ItemStack> gemsToDrop = new ArrayList<>();

         for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
               String itemId = CustomItemManager.getIdByItem(item);
               if (itemId == null || !GemType.isGem(itemId)) {
                  List<ItemStack> extractedGems;
                  if (item.getType() == Material.BUNDLE && !(extractedGems = this.extractGemsFromBundle(item, i, inv)).isEmpty()) {
                     for (ItemStack extractedGem : extractedGems) {
                        if (!foundFirstGem) {
                           foundFirstGem = true;
                           this.forceGemIntoInventory(player, extractedGem);
                        } else {
                           gemsToDrop.add(extractedGem);
                        }
                     }
                  }
               } else if (!foundFirstGem) {
                  foundFirstGem = true;
               } else {
                  gemsToDrop.add(item.clone());
                  inv.setItem(i, null);
               }
            }
         }

         if (!gemsToDrop.isEmpty()) {
            for (ItemStack gemToDrop : gemsToDrop) {
               player.getWorld().dropItemNaturally(player.getLocation(), gemToDrop);
            }

            player.sendMessage("§c§lYou can only have one gem! Extra gems have been dropped.");
         }
      }
   }

   private List<ItemStack> extractGemsFromBundle(ItemStack bundle, int slotIndex, PlayerInventory inv) {
      ArrayList<ItemStack> extractedGems = new ArrayList<>();
      if (bundle != null && bundle.getType() == Material.BUNDLE) {
         BundleMeta bundleMeta;
         if (bundle.getItemMeta() instanceof BundleMeta && (bundleMeta = (BundleMeta)bundle.getItemMeta()).hasItems()) {
            ArrayList<ItemStack> remainingItems = new ArrayList<>();

            for (ItemStack bundledItem : bundleMeta.getItems()) {
               if (bundledItem != null) {
                  String itemId = CustomItemManager.getIdByItem(bundledItem);
                  if (itemId != null && GemType.isGem(itemId)) {
                     extractedGems.add(bundledItem.clone());
                  } else {
                     remainingItems.add(bundledItem);
                  }
               }
            }

            if (!extractedGems.isEmpty()) {
               bundleMeta.setItems(remainingItems);
               bundle.setItemMeta(bundleMeta);
               inv.setItem(slotIndex, bundle);
            }
         }

         return extractedGems;
      } else {
         return extractedGems;
      }
   }

   private void forceGemIntoInventory(Player player, ItemStack gem) {
      PlayerInventory inv = player.getInventory();
      int emptySlot = inv.firstEmpty();
      if (emptySlot != -1) {
         inv.setItem(emptySlot, gem);
      } else {
         for (int i = 9; i < 36; i++) {
            ItemStack slotItem = inv.getItem(i);
            String slotId;
            if (slotItem != null
               && slotItem.getType() != Material.AIR
               && ((slotId = CustomItemManager.getIdByItem(slotItem)) == null || !GemType.isGem(slotId))) {
               ItemStack replacedItem = slotItem.clone();
               inv.setItem(i, gem);
               player.getWorld().dropItemNaturally(player.getLocation(), replacedItem);
               String msg = this.plugin.getConfigManager().getFormattedMessage("gem-forced-into-inventory");
               if (msg != null && !msg.isEmpty()) {
                  player.sendMessage(msg);
               }

               return;
            }
         }

         for (int var12 = 0; var12 < 9; var12++) {
            ItemStack slotItem = inv.getItem(var12);
            String slotId;
            if (slotItem != null
               && slotItem.getType() != Material.AIR
               && ((slotId = CustomItemManager.getIdByItem(slotItem)) == null || !GemType.isGem(slotId))) {
               ItemStack replacedItem = slotItem.clone();
               inv.setItem(var12, gem);
               player.getWorld().dropItemNaturally(player.getLocation(), replacedItem);
               String msg = this.plugin.getConfigManager().getFormattedMessage("gem-forced-into-inventory");
               if (msg != null && !msg.isEmpty()) {
                  player.sendMessage(msg);
               }

               return;
            }
         }

         inv.setItem(0, gem);
         String msg = this.plugin.getConfigManager().getFormattedMessage("gem-forced-into-hotbar");
         if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
         }
      }
   }

   public void triggerAbilityViaCommand(Player player, boolean secondary) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      String oraxenId = CustomItemManager.getIdByItem(mainHand);
      boolean isGemItem = oraxenId != null
         && (GemType.isGem(oraxenId) || this.plugin.getGemRegistry() != null && this.plugin.getGemRegistry().isRegisteredGem(oraxenId));
      if (!isGemItem) {
         oraxenId = CustomItemManager.getIdByItem(offHand);
         isGemItem = oraxenId != null
            && (GemType.isGem(oraxenId) || this.plugin.getGemRegistry() != null && this.plugin.getGemRegistry().isRegisteredGem(oraxenId));
         if (!isGemItem) {
            player.sendMessage("§c§lYou must be holding a gem to use this command!");
            return;
         }
      }

      if (this.plugin.getEnergyManager().getEnergy(player) <= 0) {
         String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy");
         if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
         }
      } else {
         GemRegistryImpl registry = this.plugin.getGemRegistry();
         String gemId = registry != null ? registry.gemIdFromItemId(oraxenId) : null;
         int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
         GemDefinition def = gemId != null && registry != null ? registry.getGem(gemId) : null;
         boolean unlocksAllAtTier1 = def != null && def.getMaxTier() < 2;
         if (secondary && tier < 2 && !unlocksAllAtTier1) {
            player.sendMessage("§c§lSecondary abilities require Tier 2 gem!");
         } else {
            GemType gemType = GemType.fromOraxenId(oraxenId);
            if (gemType != null) {
               boolean wasSneaking = player.isSneaking();
               if (secondary && !wasSneaking) {
                  player.sendMessage("§e§oShift + use ability or right-click for secondary ability!");
               } else {
                  switch (gemType) {
                     case ASTRA:
                        this.plugin.getAstraAbilities().onRightClick(player, tier);
                        break;
                     case FIRE:
                        this.plugin.getFireAbilities().onRightClick(player, tier);
                        break;
                     case FLUX:
                        this.plugin.getFluxAbilities().onRightClick(player, tier);
                        break;
                     case LIFE:
                        this.plugin.getLifeAbilities().onRightClick(player, tier);
                        break;
                     case PUFF:
                        this.plugin.getPuffAbilities().onRightClick(player, tier);
                        break;
                     case SPEED:
                        this.plugin.getSpeedAbilities().onRightClick(player, tier);
                        break;
                     case STRENGTH:
                        this.plugin.getStrengthAbilities().onRightClick(player, tier);
                        break;
                     case WEALTH:
                        this.plugin.getWealthAbilities().onRightClick(player, tier);
                  }
               }
            } else {
               GemAbilityHandler handler;
               if (gemId != null && registry != null && (handler = registry.getAbilityHandler(gemId)) != null) {
                  if (secondary) {
                     handler.onSecondary(player, tier);
                  } else {
                     handler.onPrimary(player, tier);
                  }
               }
            }
         }
      }
   }
}
