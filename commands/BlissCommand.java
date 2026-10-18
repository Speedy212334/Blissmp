package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.managers.AbilityBindingManager;
import dev.xoperr.blissgems.managers.GemLockManager;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.managers.SoulManager;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.pedestal.PedestalState;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.OraxenGemFixer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

public class BlissCommand implements CommandExecutor, TabCompleter {
   private final BlissGems plugin;
   private static final Set<String> TRANSFERABLE_ITEMS = Set.of("gem_trader", "gem_upgrader", "energy_bottle", "repair_kit");

   public BlissCommand(BlissGems plugin) {
      this.plugin = plugin;
   }

   private boolean requirePlayer(CommandSender sender) {
      if (sender instanceof Player) {
         return true;
      }

      sender.sendMessage("§cOnly players can use this command!");
      return false;
   }

   private boolean requireAdmin(CommandSender sender) {
      if (sender.hasPermission("blissgems.admin")) {
         return true;
      }

      sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      return false;
   }

   private void sendConfigMessageIfPresent(Player player, String key) {
      String msg = this.plugin.getConfigManager().getFormattedMessage(key);
      if (msg != null && !msg.isEmpty()) {
         player.sendMessage(msg);
      }
   }

   private int gemTierOf(GemRegistry registry, String oraxenId) {
      return registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
   }

   private static Boolean parseToggle(String value) {
      switch (value.toLowerCase()) {
         case "true":
         case "on":
         case "yes":
         case "1":
            return Boolean.TRUE;
         case "false":
         case "off":
         case "no":
         case "0":
            return Boolean.FALSE;
         default:
            return null;
      }
   }

   private void clearGemsFromInventory(Player target) {
      PlayerInventory inv = target.getInventory();

      for (int i = 0; i < inv.getSize(); i++) {
         ItemStack item = inv.getItem(i);
         if (item != null && this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(item))) {
            inv.setItem(i, null);
         }
      }
   }

   private static List<String> onlinePlayerNames() {
      return Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         if (sender instanceof Player) {
            this.plugin.getGemMenu().open((Player)sender);
         } else {
            this.sendHelp(sender);
         }

         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "give":
               this.handleGive(sender, args);
               break;
            case "reroll":
               this.handleReroll(sender, args);
               break;
            case "giveitem":
               this.handleGiveItem(sender, args);
               break;
            case "transfer":
               this.handleTransfer(sender, args);
               break;
            case "energy":
               this.handleEnergy(sender, args);
               break;
            case "withdraw":
               this.handleWithdraw(sender, args);
               break;
            case "info":
               this.handleInfo(sender, args);
               break;
            case "whoowns":
               this.handleWhoOwns(sender, args);
               break;
            case "reload":
               this.handleReload(sender, args);
               break;
            case "pockets":
               this.handlePockets(sender, args);
               break;
            case "amplify":
               this.handleAmplify(sender, args);
               break;
            case "toggle_click":
               this.handleToggleClick(sender, args);
               break;
            case "ability:main":
               this.handleAbilityMain(sender, args);
               break;
            case "ability:secondary":
               this.handleAbilitySecondary(sender, args);
               break;
            case "ability:tertiary":
               this.handleAbilityTertiary(sender, args);
               break;
            case "ability:quaternary":
               this.handleAbilityQuaternary(sender, args);
               break;
            case "ability:quinary":
               this.handleExtraSlot(sender, AbilitySlot.QUINARY);
               break;
            case "ability:senary":
               this.handleExtraSlot(sender, AbilitySlot.SENARY);
               break;
            case "trust":
               this.handleTrust(sender, args);
               break;
            case "untrust":
               this.handleUntrust(sender, args);
               break;
            case "trusted":
               this.handleTrustedList(sender, args);
               break;
            case "bannable":
               this.handleBannable(sender, args);
               break;
            case "oraxen":
               this.handleOraxen(sender, args);
               break;
            case "autosmelt":
               this.handleAutoSmelt(sender, args);
               break;
            case "conduction":
               this.handleConduction(sender, args);
               break;
            case "charge":
               this.handleCharge(sender, args);
               break;
            case "setwatts":
               this.handleSetWatts(sender, args);
               break;
            case "getwatts":
               this.handleGetWatts(sender, args);
               break;
            case "stats":
               this.handleStats(sender, args);
               break;
            case "release":
               this.handleReleaseSouls(sender, args);
               break;
            case "souls":
               this.handleSoulsInfo(sender, args);
               break;
            case "achievements":
               this.handleAchievements(sender, args);
               break;
            case "normalise":
            case "normalize":
               this.handleNormalise(sender, args);
               break;
            case "smp":
               this.handleSmp(sender, args);
               break;
            case "pedestal":
               this.handlePedestal(sender, args);
               break;
            case "deps":
               this.handleDeps(sender);
               break;
            case "shaders":
            case "shader":
               this.handleShaders(sender);
               break;
            case "unbounded":
               this.handleUnbounded(sender, args);
               break;
            case "cooldowns":
               this.handleListCooldowns(sender, args);
               break;
            case "clearcds":
               this.handleClearCooldowns(sender, args);
               break;
            case "nocdtoggle":
               this.handleNoCooldownToggle(sender, args);
               break;
            case "ability":
               if (args.length > 1) {
                  String sub = args[1].toLowerCase();
                  switch (sub) {
                     case "main":
                     case "primary":
                        this.handleAbilityMain(sender, args);
                        return true;
                     case "secondary":
                        this.handleAbilitySecondary(sender, args);
                        return true;
                     case "tertiary":
                        this.handleAbilityTertiary(sender, args);
                        return true;
                     case "quaternary":
                        this.handleAbilityQuaternary(sender, args);
                        return true;
                     case "quinary":
                        this.handleExtraSlot(sender, AbilitySlot.QUINARY);
                        return true;
                     case "senary":
                        this.handleExtraSlot(sender, AbilitySlot.SENARY);
                        return true;
                     case "set":
                     case "bind":
                        String[] shifted = new String[Math.max(1, args.length - 1)];
                        shifted[0] = "set_ability";

                        for (int i = 2; i < args.length; i++) {
                           shifted[i - 1] = args[i];
                        }

                        this.handleSetAbility(sender, shifted);
                        return true;
                     case "reset":
                        this.handleSetAbility(sender, new String[]{"set_ability", "reset"});
                        return true;
                     case "list":
                        this.handleAbilityBindingsList(sender, args);
                        return true;
                  }
               }

               if (sender instanceof Player && this.plugin.getControlsMenu() != null) {
                  this.plugin.getControlsMenu().open((Player)sender);
               } else {
                  this.handleAbilityBindingsList(sender, args);
               }
               break;
            case "primary":
            case "main":
               this.handleAbilityMain(sender, args);
               break;
            case "secondary":
               this.handleAbilitySecondary(sender, args);
               break;
            case "tertiary":
               this.handleAbilityTertiary(sender, args);
               break;
            case "quaternary":
               this.handleAbilityQuaternary(sender, args);
               break;
            case "quinary":
               this.handleExtraSlot(sender, AbilitySlot.QUINARY);
               break;
            case "senary":
               this.handleExtraSlot(sender, AbilitySlot.SENARY);
               break;
            case "viewer":
            case "gemviewer":
            case "gui":
            case "admin":
               if (!(sender instanceof Player)) {
                  sender.sendMessage("§cOnly players can open the admin panel!");
               } else if (!sender.hasPermission("blissgems.admin")) {
                  this.plugin.getConfigManager().sendFormattedMessage(sender, "no-permission");
               } else {
                  this.plugin.getEnhancedGuiManager().openMainMenu((Player)sender);
               }
               break;
            case "menu":
               if (sender instanceof Player) {
                  this.plugin.getEnhancedGuiManager().openMainMenu((Player)sender);
               } else {
                  sender.sendMessage("§cOnly players can open the gem viewer!");
               }
               break;
            case "set_ability":
               this.handleSetAbility(sender, args);
               break;
            case "goldgem":
               this.handleGoldGem(sender, args);
               break;
            case "goldcycle":
               this.handleGoldCycle(sender);
               break;
            case "gold":
               this.handleGold(sender, args);
               break;
            case "goldarmor":
            case "goldarmour":
               this.handleGoldArmorToggle(sender);
               break;
            case "enchantlimit":
               this.handleEnchantLimit(sender, args);
               break;
            case "spawnvillager":
               this.handleSpawnVillager(sender, args);
               break;
            case "news":
               new NewsCommand(this.plugin).onCommand(sender, command, label, args);
               break;
            default:
               this.sendHelp(sender);
         }

         return true;
      }
   }

   private void handleEnchantLimit(CommandSender sender, String[] args) {
      if (this.requireAdmin(sender)) {
         if (args.length >= 3) {
            String enchant = args[1].toLowerCase();

            int maxLevel;
            try {
               maxLevel = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
               sender.sendMessage(ChatColor.RED + "Invalid number for maxLevel: " + args[2]);
               return;
            }

            if (this.plugin.getEnchantLimiterManager() != null) {
               this.plugin.getEnchantLimiterManager().setLimit(enchant, maxLevel);
               sender.sendMessage(
                  ChatColor.GREEN + "Set enchantment limit for " + ChatColor.YELLOW + enchant + ChatColor.GREEN + " to " + ChatColor.YELLOW + maxLevel
               );
            }
         } else {
            sender.sendMessage(ChatColor.GOLD + "=== Enchantment Limits ===");
            if (this.plugin.getEnchantLimiterManager() != null) {
               for (Entry<String, Integer> e : this.plugin.getEnchantLimiterManager().getAllLimits().entrySet()) {
                  sender.sendMessage(ChatColor.YELLOW + e.getKey() + ": " + ChatColor.WHITE + e.getValue());
               }
            }

            sender.sendMessage(ChatColor.GRAY + "Usage: /bliss enchantlimit <enchantment> <maxLevel>");
         }
      }
   }

   private void handleSpawnVillager(CommandSender sender, String[] args) {
      if (this.requireAdmin(sender)) {
         if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command.");
         } else if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bliss spawnvillager <mace1|mace2|mace3|energy>");
         } else {
            String typeKey = args[1].toLowerCase();
            String type;
            if (typeKey.contains("1") || typeKey.equalsIgnoreCase("mace1")) {
               type = "mace_1";
            } else if (typeKey.contains("2") || typeKey.equalsIgnoreCase("mace2")) {
               type = "mace_2";
            } else if (!typeKey.contains("3") && !typeKey.equalsIgnoreCase("mace3")) {
               if (!typeKey.contains("energy") && !typeKey.equalsIgnoreCase("energygames")) {
                  player.sendMessage(ChatColor.RED + "Unknown villager type. Options: mace1, mace2, mace3, energy");
                  return;
               }

               type = "energy_games";
            } else {
               type = "mace_3";
            }

            if (this.plugin.getMaceVillagerManager() != null) {
               this.plugin.getMaceVillagerManager().spawnCustomVillager(player.getLocation(), type);
               player.sendMessage(ChatColor.GREEN + "Spawned custom merchant: " + ChatColor.YELLOW + type);
            }
         }
      }
   }

   private void handleGive(CommandSender sender, String[] args) {
      if (this.requireAdmin(sender)) {
         if (args.length < 3) {
            sender.sendMessage("§cUsage: /bliss give <player> <gem_type> [tier]");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
            } else {
               String gemIdArg = args[2].toLowerCase();
               String resolvedGemId = null;
               String resolvedDisplayName = null;

               for (GemType type : GemType.values()) {
                  if (type.getId().equalsIgnoreCase(gemIdArg) || type.getDisplayName().equalsIgnoreCase(gemIdArg)) {
                     resolvedGemId = type.getId();
                     resolvedDisplayName = type.getDisplayName();
                     break;
                  }
               }

               GemDefinition def;
               GemRegistryImpl registry;
               if (resolvedGemId == null && (registry = this.plugin.getGemRegistry()) != null && (def = registry.getGem(gemIdArg)) != null) {
                  resolvedGemId = def.getId();
                  resolvedDisplayName = def.getDisplayName();
               }

               if (resolvedGemId == null) {
                  sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-gem-type"));
               } else {
                  int tier = 1;
                  if (args.length >= 4) {
                     try {
                        tier = Integer.parseInt(args[3]);
                        if (tier < 1 || tier > 2) {
                           sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier"));
                           return;
                        }
                     } catch (NumberFormatException e) {
                        sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier"));
                        return;
                     }
                  }

                  GemRegistryImpl tierRegistry;
                  GemDefinition tierDef = (tierRegistry = this.plugin.getGemRegistry()) != null ? tierRegistry.getGem(resolvedGemId) : null;
                  if (tierDef != null && tier > tierDef.getMaxTier()) {
                     tier = tierDef.getMaxTier();
                     sender.sendMessage("§e" + resolvedDisplayName + " has no Tier 2 - giving Tier " + tier + " instead.");
                     if ("gold".equals(resolvedGemId)) {
                        sender.sendMessage(
                           "§7The Gold Gem's secondary abilities come from the tier of the soul it channels: §f/bliss goldgem fill <player> <soul> 2§7."
                        );
                     }
                  }

                  this.clearGemsFromInventory(target);
                  if (this.plugin.getGemManager().giveGem(target, resolvedGemId, tier)) {
                     sender.sendMessage(
                        this.plugin.getConfigManager().getFormattedMessage("gem-given", "player", target.getName(), "gem", resolvedDisplayName, "tier", tier)
                     );
                  } else {
                     sender.sendMessage("§cFailed to give gem!");
                  }
               }
            }
         }
      }
   }

   private void handleGoldGem(CommandSender sender, String[] args) {
      if (this.requireAdmin(sender)) {
         if (args.length < 3) {
            this.sendGoldGemUsage(sender);
         } else {
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
            } else {
               switch (args[1].toLowerCase()) {
                  case "fill":
                     this.handleGoldGemFill(sender, target, args);
                     break;
                  case "remove":
                     this.handleGoldGemRemove(sender, target, args);
                     break;
                  case "clear":
                     int removed = this.plugin.getGoldGemManager().clearSouls(target);
                     sender.sendMessage(
                        removed > 0
                           ? "§aEmptied " + target.getName() + "'s Gold Gem (" + removed + " soul(s) removed)."
                           : "§e" + target.getName() + "'s Gold Gem is already dormant."
                     );
                     break;
                  case "list":
                     this.handleGoldGemList(sender, target);
                     break;
                  default:
                     this.sendGoldGemUsage(sender);
               }
            }
         }
      }
   }

   private void sendGoldGemUsage(CommandSender sender) {
      sender.sendMessage("§cUsage:");
      sender.sendMessage("§7/bliss goldgem fill <player> <soulType> <tier>");
      sender.sendMessage("§7/bliss goldgem remove <player> <soulType>");
      sender.sendMessage("§7/bliss goldgem clear <player>");
      sender.sendMessage("§7/bliss goldgem list <player>");
   }

   private void handleGold(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         GoldGemManager gold = this.plugin.getGoldGemManager();
         if (gold != null && gold.holdsGoldGem(player)) {
            String sub = args.length > 1 ? args[1].toLowerCase() : "souls";
            switch (sub) {
               case "cycle":
               case "next":
                  this.handleGoldCycle(sender);
                  break;
               case "beam":
                  if (!this.blockedByGemLock(player)) {
                     this.plugin.getGoldAbilities().onTertiary(player, 1);
                  }
                  break;
               case "armor":
               case "armour":
               case "trims":
                  this.handleGoldArmorToggle(sender);
                  break;
               case "souls":
               case "menu":
                  this.plugin.getGoldAbilities().openSoulMenu(player);
                  break;
               default:
                  player.sendMessage(
                     "§6/bliss gold §7[souls|cycle|beam|armor] §8- pick the channelled soul, cycle it, fire the Sundering Beam, toggle gold trims"
                  );
            }
         } else {
            player.sendMessage("§cYou are not carrying the Gold Gem.");
         }
      }
   }

   private void handleGoldCycle(CommandSender sender) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         GoldGemManager gold = this.plugin.getGoldGemManager();
         if (gold != null && gold.holdsGoldGem(player)) {
            String next = gold.cycleActive(player);
            if (next == null) {
               player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            } else {
               player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.4F);
               player.sendMessage(
                  this.plugin.getConfigManager().getMessage("gold-soul-selected").replace("{gem}", this.plugin.getGemManager().getGemDisplayName(next))
               );
            }
         } else {
            player.sendMessage("§cYou are not carrying the Gold Gem.");
         }
      }
   }

   private void handleGoldArmorToggle(CommandSender sender) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         GoldGemManager gold = this.plugin.getGoldGemManager();
         if (gold != null && gold.holdsGoldGem(player)) {
            boolean enabled = gold.toggleTrims(player);
            player.sendMessage(enabled ? "§6Gold armour trims §aenabled§6." : "§7Gold armour trims §cdisabled§7. Your original trims are restored.");
         } else {
            player.sendMessage("§cYou are not carrying the Gold Gem.");
         }
      }
   }

   private String resolveGemIdArg(CommandSender sender, String arg) {
      String gemIdArg = arg.toLowerCase();

      for (GemType type : GemType.values()) {
         if (type.getId().equalsIgnoreCase(gemIdArg) || type.getDisplayName().equalsIgnoreCase(gemIdArg)) {
            return type.getId();
         }
      }

      GemRegistryImpl registry = this.plugin.getGemRegistry();
      GemDefinition def;
      if (registry != null && (def = registry.getGem(gemIdArg)) != null) {
         return def.getId();
      }

      sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-gem-type"));
      return null;
   }

   private void handleGoldGemFill(CommandSender sender, Player target, String[] args) {
      if (args.length < 5) {
         sender.sendMessage("§cUsage: /bliss goldgem fill <player> <soulType> <tier>");
      } else {
         String resolvedGemId = this.resolveGemIdArg(sender, args[3]);
         if (resolvedGemId != null) {
            int tier;
            try {
               tier = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier"));
               return;
            }

            if (tier >= 1 && tier <= 2) {
               this.plugin.getGoldGemManager().fillSoul(target, resolvedGemId, tier);
               sender.sendMessage(
                  "§aFilled "
                     + target.getName()
                     + "'s Gold Gem with a Tier "
                     + tier
                     + " "
                     + this.plugin.getGemManager().getGemDisplayName(resolvedGemId)
                     + " §asoul."
               );
            } else {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier"));
            }
         }
      }
   }

   private void handleGoldGemRemove(CommandSender sender, Player target, String[] args) {
      if (args.length < 4) {
         sender.sendMessage("§cUsage: /bliss goldgem remove <player> <soulType>");
      } else {
         String resolvedGemId = this.resolveGemIdArg(sender, args[3]);
         if (resolvedGemId != null) {
            String display = this.plugin.getGemManager().getGemDisplayName(resolvedGemId);
            if (this.plugin.getGoldGemManager().removeSoul(target, resolvedGemId)) {
               sender.sendMessage("§aRemoved the " + display + " §asoul from " + target.getName() + "'s Gold Gem.");
            } else {
               sender.sendMessage("§c" + target.getName() + "'s Gold Gem has no " + display + " §csoul.");
            }
         }
      }
   }

   private void handleGoldGemList(CommandSender sender, Player target) {
      Map<String, GoldGemManager.Harvest> souls = this.plugin.getGoldGemManager().getHarvested(target.getUniqueId());
      if (souls.isEmpty()) {
         sender.sendMessage("§e" + target.getName() + "'s Gold Gem is dormant.");
      } else {
         String active = this.plugin.getGoldGemManager().getActive(target.getUniqueId());
         sender.sendMessage("§6§l" + target.getName() + "'s harvested souls §8(" + souls.size() + "/8)");

         for (Entry<String, GoldGemManager.Harvest> soul : souls.entrySet()) {
            sender.sendMessage(
               "§7- "
                  + this.plugin.getGemManager().getGemDisplayName(soul.getKey())
                  + " §8(T"
                  + soul.getValue().tier()
                  + ")"
                  + (soul.getKey().equals(active) ? " §6§l[active]" : "")
            );
         }
      }
   }

   private void handleReroll(CommandSender sender, String[] args) {
      if (this.requireAdmin(sender)) {
         if (args.length < 2) {
            sender.sendMessage("§cUsage: /bliss reroll <player> [tier]");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
            } else {
               String randomGem = this.getRandomEnabledGem();
               if (randomGem == null) {
                  sender.sendMessage("§cNo gems are enabled in the config!");
               } else {
                  int tier = 1;
                  if (args.length >= 3) {
                     try {
                        tier = Integer.parseInt(args[2]);
                        if (tier < 1 || tier > 2) {
                           sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier"));
                           return;
                        }
                     } catch (NumberFormatException e) {
                        sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier"));
                        return;
                     }
                  }

                  this.clearGemsFromInventory(target);
                  ItemStack offGem = target.getInventory().getItemInOffHand();
                  if (offGem != null && this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(offGem))) {
                     target.getInventory().setItemInOffHand(null);
                  }

                  sender.sendMessage("§d§lInitiating gem ritual for " + target.getName() + "...");
                  target.sendMessage("§d§l§nGEM REROLL RITUAL");
                  target.sendMessage("§7§oThe ancient powers are choosing your fate...");
                  int finalTier = tier;
                  this.plugin.getGemRitualManager().performGemRitual(target, randomGem, false, finalTier);
                  this.plugin
                     .getServer()
                     .getScheduler()
                     .runTaskLater(
                        this.plugin,
                        () -> {
                           if (this.plugin.getGemManager().giveGemToOffhand(target, randomGem, finalTier)) {
                              String gemName = this.plugin.getGemManager().getGemDisplayName(randomGem);
                              String gemColor = this.plugin.getGemManager().getGemColorCode(randomGem);
                              String msg = this.plugin
                                 .getConfigManager()
                                 .getFormattedMessage("gem-rerolled", "player", target.getName(), "gem", gemName, "tier", finalTier);
                              if (msg != null && !msg.isEmpty()) {
                                 sender.sendMessage(msg);
                              } else {
                                 sender.sendMessage("§aRerolled " + target.getName() + "'s gem to " + gemColor + gemName + " §a(Tier " + finalTier + ")!");
                              }

                              String targetMsg = this.plugin.getConfigManager().getFormattedMessage("gem-rerolled-received", "gem", gemName, "tier", finalTier);
                              if (targetMsg != null && !targetMsg.isEmpty()) {
                                 target.sendMessage(targetMsg);
                              } else {
                                 target.sendMessage("§d§l» §fYour gem has been chosen: " + gemColor + "§l" + gemName + " §f(Tier " + finalTier + ")§d§l «");
                              }
                           } else {
                              sender.sendMessage("§cFailed to reroll gem!");
                           }
                        },
                        20L
                     );
               }
            }
         }
      }
   }

   private void handleGiveItem(CommandSender sender, String[] args) {
      if (this.requireAdmin(sender)) {
         if (args.length < 3) {
            sender.sendMessage("§cUsage: /bliss giveitem <player> <item_id> [amount]");
            sender.sendMessage("§7Available items:");
            sender.sendMessage("§b  - energy_bottle");
            sender.sendMessage("§b  - repair_kit");
            sender.sendMessage("§b  - gem_trader");
            sender.sendMessage("§b  - gem_fragment");
            sender.sendMessage("§b  - gem_upgrader §7(universal - works for all gems)");
            sender.sendMessage("§b  - restoration_book §7(revives a Broken gem)");
            sender.sendMessage("§b  - prismatic_edge §7(legendary sword)");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
            } else {
               String itemId = args[2].toLowerCase();
               int amount = 1;
               if (args.length >= 4) {
                  try {
                     amount = Integer.parseInt(args[3]);
                     if (amount < 1 || amount > 64) {
                        sender.sendMessage("§cAmount must be between 1 and 64!");
                        return;
                     }
                  } catch (NumberFormatException e) {
                     sender.sendMessage("§cInvalid amount!");
                     return;
                  }
               }

               ItemStack item;
               if ((item = CustomItemManager.getItemById(itemId)) == null) {
                  sender.sendMessage("§cInvalid item ID: " + itemId);
                  sender.sendMessage("§7Available items: energy_bottle, repair_kit, gem_trader, gem_fragment, gem_upgrader, restoration_book, prismatic_edge");
               } else {
                  item.setAmount(amount);
                  target.getInventory().addItem(new ItemStack[]{item});
                  ItemMeta itemMeta = item.getItemMeta();
                  String itemName = itemMeta != null ? itemMeta.getDisplayName() : itemId;
                  sender.sendMessage("§aGave " + amount + "x " + itemName + " §ato " + target.getName() + "!");
                  target.sendMessage("§aYou received " + amount + "x " + itemName + "§a!");
               }
            }
         }
      }
   }

   private void handleTransfer(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("§cOnly players can transfer items!");
      } else if (args.length < 3) {
         player.sendMessage("§cUsage: /bliss transfer <player> <item> [amount]");
         player.sendMessage("§7Transferable: gem_trader, gem_upgrader, energy_bottle, repair_kit");
      } else {
         Player target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
         } else if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou can't transfer items to yourself!");
         } else {
            String itemId = args[2].toLowerCase();
            if (!TRANSFERABLE_ITEMS.contains(itemId)) {
               player.sendMessage("§cYou can only transfer: §fgem_trader, gem_upgrader, energy_bottle, repair_kit");
            } else {
               int amount = 1;
               if (args.length >= 4) {
                  try {
                     amount = Integer.parseInt(args[3]);
                  } catch (NumberFormatException e) {
                     player.sendMessage("§cInvalid amount!");
                     return;
                  }

                  if (amount < 1 || amount > 64) {
                     player.sendMessage("§cAmount must be between 1 and 64!");
                     return;
                  }
               }

               int have;
               if ((have = this.countCustomItem(player, itemId)) < amount) {
                  player.sendMessage("§cYou only have §f" + have + "§c of that item.");
               } else {
                  this.removeCustomItem(player, itemId, amount);
                  ItemStack give = CustomItemManager.getItemById(itemId);
                  if (give == null) {
                     player.sendMessage("§cInvalid item.");
                  } else {
                     give.setAmount(amount);
                     HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(new ItemStack[]{give});
                     int notDelivered = 0;

                     for (ItemStack left : leftover.values()) {
                        if (left != null) {
                           notDelivered += left.getAmount();
                        }
                     }

                     if (notDelivered > 0) {
                        ItemStack refund = CustomItemManager.getItemById(itemId);
                        refund.setAmount(notDelivered);
                        player.getInventory().addItem(new ItemStack[]{refund});
                     }

                     int delivered;
                     if ((delivered = amount - notDelivered) <= 0) {
                        player.sendMessage("§c" + target.getName() + "'s inventory is full, nothing was transferred.");
                     } else {
                        String itemName = give.getItemMeta() != null ? give.getItemMeta().getDisplayName() : itemId;
                        player.sendMessage("§aTransferred §f" + delivered + "x " + itemName + " §ato " + target.getName() + "!");
                        target.sendMessage("§a" + player.getName() + " §atransferred you §f" + delivered + "x " + itemName + "§a!");
                        if (notDelivered > 0) {
                           player.sendMessage("§e" + notDelivered + " couldn't fit and were returned to you.");
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private int countCustomItem(Player player, String itemId) {
      int total = 0;

      for (ItemStack item : player.getInventory().getContents()) {
         if (item != null && itemId.equals(CustomItemManager.getIdByItem(item))) {
            total += item.getAmount();
         }
      }

      return total;
   }

   private void removeCustomItem(Player player, String itemId, int amount) {
      int remaining = amount;
      ItemStack[] contents = player.getInventory().getContents();

      for (int i = 0; i < contents.length && remaining > 0; i++) {
         ItemStack item = contents[i];
         if (item != null && itemId.equals(CustomItemManager.getIdByItem(item))) {
            int take = Math.min(remaining, item.getAmount());
            int left = item.getAmount() - take;
            if (left <= 0) {
               player.getInventory().setItem(i, null);
            } else {
               item.setAmount(left);
            }

            remaining -= take;
         }
      }
   }

   private void handleEnergy(CommandSender sender, String[] args) {
      if (args.length == 1) {
         if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can check their energy!");
         } else {
            int energy = this.plugin.getEnergyManager().getEnergy(player);
            EnergyState state = EnergyState.fromEnergy(energy);
            String energyBar = this.getEnergyBar(energy);
            this.plugin.getConfigManager().sendFormattedMessage(player, "energy-info-header");
            this.plugin.getConfigManager().sendFormattedMessage(player, "energy-info-line1", "energyBar", energyBar, "energy", energy);
            this.plugin.getConfigManager().sendFormattedMessage(player, "energy-info-line2", "state", state.getDisplayName());
         }
      } else if (this.requireAdmin(sender)) {
         if (args.length < 4) {
            sender.sendMessage("§cUsage: /bliss energy <player> <set/add/remove> <amount>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
            } else {
               String action = args[2].toLowerCase();

               int amount;
               try {
                  amount = Integer.parseInt(args[3]);
               } catch (NumberFormatException e) {
                  sender.sendMessage("§cInvalid amount!");
                  return;
               }

               switch (action) {
                  case "set":
                     this.plugin.getEnergyManager().setEnergy(target, amount);
                     sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-set", "player", target.getName(), "amount", amount));
                     break;
                  case "add":
                     this.plugin.getEnergyManager().addEnergy(target, amount);
                     sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-added", "player", target.getName(), "amount", amount));
                     break;
                  case "remove":
                     int currentEnergy = this.plugin.getEnergyManager().getEnergy(target);
                     if (currentEnergy <= 0) {
                        sender.sendMessage("§c" + target.getName() + " already has 0 energy!");
                     } else {
                        this.plugin.getEnergyManager().removeEnergy(target, amount);
                        sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-removed", "player", target.getName(), "amount", amount));
                     }
                     break;
                  default:
                     sender.sendMessage("§cUsage: /bliss energy <player> <set/add/remove> <amount>");
               }
            }
         }
      }
   }

   private String getEnergyBar(int energy) {
      StringBuilder bar = new StringBuilder();

      for (int i = 1; i <= 10; i++) {
         if (i <= energy) {
            bar.append("§a█");
         } else {
            bar.append("§7█");
         }
      }

      return bar.toString();
   }

   private void handleWithdraw(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         int currentEnergy = this.plugin.getEnergyManager().getEnergy(player);
         if (currentEnergy <= 1) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "not-enough-energy");
         } else {
            this.plugin.getEnergyManager().removeEnergy(player, 1);
            ItemStack bottle = CustomItemManager.getItemById("energy_bottle");
            if (bottle != null) {
               player.getInventory().addItem(new ItemStack[]{bottle});
               this.plugin.getConfigManager().sendFormattedMessage(player, "energy-withdrawn");
            } else {
               this.plugin.getConfigManager().sendFormattedMessage(player, "energy-bottle-failed");
            }
         }
      }
   }

   private void handleWhoOwns(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("§cOnly players can use this command!");
      } else if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage("§cYou don't have permission to use this command!");
      } else {
         ItemStack held = player.getInventory().getItemInMainHand();
         if (held != null && held.getType() != Material.AIR) {
            UUID owner = CustomItemManager.getOwner(held);
            if (owner == null) {
               player.sendMessage("§eThat item has no ownership stamp.");
            } else {
               OfflinePlayer op = this.plugin.getServer().getOfflinePlayer(owner);
               String name = op.getName() != null ? op.getName() : "unknown";
               player.sendMessage("§d§lOwner: §f" + name + " §7(" + owner + ")");
            }
         } else {
            player.sendMessage("§cHold an item in your main hand.");
         }
      }
   }

   private void handleInfo(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.plugin.getGemManager().hasActiveGem(player)) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "no-active-gem");
         } else {
            GemType gemType = this.plugin.getGemManager().getGemType(player);
            int tier = this.plugin.getGemManager().getGemTier(player);
            int energy = this.plugin.getEnergyManager().getEnergy(player);
            EnergyState state = this.plugin.getEnergyManager().getEnergyState(player);
            this.plugin
               .getConfigManager()
               .sendFormattedMessage(player, "gem-info", "gem", gemType.getDisplayName(), "tier", tier, "energy", energy, "state", state.getDisplayName());
         }
      }
   }

   private void handleReload(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         this.plugin.getConfigManager().sendFormattedMessage(sender, "no-permission");
      } else {
         this.plugin.getConfigManager().reload();
         this.plugin.getConfigManager().sendFormattedMessage(sender, "config-reloaded");
      }
   }

   private void handlePockets(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.plugin.getGemManager().hasGemType(player, GemType.WEALTH)) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "requires-wealth-gem-pockets");
         } else {
            int tier = this.plugin.getGemManager().getGemTier(player, GemType.WEALTH);
            if (tier < 2) {
               this.plugin.getConfigManager().sendFormattedMessage(player, "requires-wealth-t2-pockets");
            } else {
               this.plugin.getWealthAbilities().pockets(player);
            }
         }
      }
   }

   private void handleAmplify(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.plugin.getGemManager().hasGemType(player, GemType.WEALTH)) {
            player.sendMessage("§cYou need a Wealth gem to use Amplification!");
         } else {
            int tier = this.plugin.getGemManager().getGemTier(player, GemType.WEALTH);
            if (tier < 2) {
               player.sendMessage("§cAmplification requires Tier 2 Wealth gem!");
            } else {
               this.plugin.getWealthAbilities().amplification(player);
            }
         }
      }
   }

   private void handleToggleClick(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         boolean newState = this.plugin.getClickActivationManager().toggleClickActivation(player);
         if (newState) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "click-activation-enabled");
         } else {
            this.plugin.getConfigManager().sendFormattedMessage(player, "click-activation-disabled");
         }
      }
   }

   private String findGemInHand(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      GemRegistryImpl registry = this.plugin.getGemRegistry();
      String oraxenId = CustomItemManager.getIdByItem(mainHand);
      if (oraxenId == null || !GemType.isGem(oraxenId) && (registry == null || !registry.isRegisteredGem(oraxenId))) {
         oraxenId = CustomItemManager.getIdByItem(offHand);
         return oraxenId == null || !GemType.isGem(oraxenId) && (registry == null || !registry.isRegisteredGem(oraxenId)) ? null : oraxenId;
      } else {
         return oraxenId;
      }
   }

   private boolean unlocksAllAtTier1(String oraxenId) {
      GemRegistryImpl registry = this.plugin.getGemRegistry();
      if (registry == null) {
         return false;
      }

      String gemId = registry.gemIdFromItemId(oraxenId);
      GemDefinition def = gemId != null ? registry.getGem(gemId) : null;
      return def != null && def.getMaxTier() < 2;
   }

   private boolean blockedByGemLock(Player player) {
      GemLockManager mgr = this.plugin.getGemLockManager();
      if (mgr != null && mgr.isLocked(player)) {
         int left = mgr.getRemainingSeconds(player.getUniqueId());
         player.sendMessage("§6ꐬ §c§oYour gem is locked! §7(" + left + "s)");
         return true;
      } else {
         return false;
      }
   }

   private void handleAbilityMain(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.blockedByGemLock(player)) {
            String oraxenId = this.findGemInHand(player);
            if (oraxenId == null) {
               this.plugin.getConfigManager().sendFormattedMessage(player, "must-hold-gem");
            } else {
               int energy = this.plugin.getEnergyManager().getEnergy(player);
               if (energy <= 0) {
                  this.plugin.getConfigManager().sendFormattedMessage(player, "no-energy");
               } else {
                  GemRegistryImpl registry = this.plugin.getGemRegistry();
                  int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
                  GemType gemType = GemType.fromOraxenId(oraxenId);
                  if (gemType != null) {
                     switch (gemType) {
                        case ASTRA:
                           this.plugin.getAstraAbilities().astralDaggers(player);
                           break;
                        case FIRE:
                           this.plugin.getFireAbilities().chargedFireball(player);
                           break;
                        case FLUX:
                           this.plugin.getFluxAbilities().fluxBeam(player);
                           break;
                        case LIFE:
                           this.plugin.getLifeAbilities().heartDrainer(player);
                           break;
                        case PUFF:
                           this.plugin.getPuffAbilities().dash(player);
                           break;
                        case SPEED:
                           this.plugin.getSpeedAbilities().onRightClick(player, tier);
                           break;
                        case STRENGTH:
                           this.plugin.getStrengthAbilities().chadStrength(player);
                           break;
                        case WEALTH:
                           this.plugin.getWealthAbilities().unfortunate(player);
                     }
                  } else {
                     if (registry != null) {
                        String gemId = registry.gemIdFromItemId(oraxenId);
                        GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
                        if (handler != null) {
                           handler.onPrimary(player, tier);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void handleAbilitySecondary(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.blockedByGemLock(player)) {
            String oraxenId = this.findGemInHand(player);
            if (oraxenId == null) {
               this.plugin.getConfigManager().sendFormattedMessage(player, "must-hold-gem");
            } else {
               GemRegistryImpl registry = this.plugin.getGemRegistry();
               int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
               if (tier < 2 && !this.unlocksAllAtTier1(oraxenId)) {
                  String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }
               } else {
                  int energy = this.plugin.getEnergyManager().getEnergy(player);
                  if (energy <= 0) {
                     String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy");
                     if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                     }
                  } else {
                     GemType gemType = GemType.fromOraxenId(oraxenId);
                     if (gemType != null) {
                        switch (gemType) {
                           case ASTRA:
                              this.plugin.getAstraAbilities().astralProjection(player);
                              break;
                           case FIRE:
                              this.plugin.getFireAbilities().cozyCampfire(player);
                              break;
                           case FLUX:
                              this.plugin.getFluxAbilities().ground(player);
                              break;
                           case LIFE:
                              this.plugin.getLifeAbilities().circleOfLife(player);
                              break;
                           case PUFF:
                              this.plugin.getPuffAbilities().breezyBash(player);
                              break;
                           case SPEED:
                              this.plugin.getSpeedAbilities().speedStorm(player);
                              break;
                           case STRENGTH:
                              this.plugin.getStrengthAbilities().frailer(player);
                              break;
                           case WEALTH:
                              this.plugin.getWealthAbilities().richRush(player);
                        }
                     } else {
                        if (registry != null) {
                           String gemId = registry.gemIdFromItemId(oraxenId);
                           GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
                           if (handler != null) {
                              handler.onSecondary(player, tier);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void triggerPrimary(Player player) {
      this.handleAbilityMain(player, new String[0]);
   }

   public void triggerSecondary(Player player) {
      this.handleAbilitySecondary(player, new String[0]);
   }

   public void triggerTertiary(Player player) {
      this.handleAbilityTertiary(player, new String[0]);
   }

   public void triggerQuaternary(Player player) {
      this.handleAbilityQuaternary(player, new String[0]);
   }

   public void triggerQuinary(Player player) {
      this.handleExtraSlot(player, AbilitySlot.QUINARY);
   }

   public void triggerSenary(Player player) {
      this.handleExtraSlot(player, AbilitySlot.SENARY);
   }

   private void handleExtraSlot(CommandSender sender, AbilitySlot slot) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("§cOnly players can use this command!");
      } else if (!this.blockedByGemLock(player)) {
         GoldGemManager goldMgr = this.plugin.getGoldGemManager();
         if (goldMgr == null || !goldMgr.isGoldGem(player.getInventory().getItemInMainHand()) && !goldMgr.isGoldGem(player.getInventory().getItemInOffHand())) {
            String oraxenId = this.findGemInHand(player);
            if (oraxenId == null) {
               String msg = this.plugin.getConfigManager().getFormattedMessage("must-hold-gem");
               if (msg != null && !msg.isEmpty()) {
                  player.sendMessage(msg);
               }
            } else {
               GemRegistryImpl registry = this.plugin.getGemRegistry();
               if (registry != null) {
                  int tier = registry.tierFromItemId(oraxenId);
                  if (tier < 2 && !this.unlocksAllAtTier1(oraxenId)) {
                     String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2");
                     if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                     }
                  } else {
                     int energy = this.plugin.getEnergyManager().getEnergy(player);
                     if (energy <= 0) {
                        String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy");
                        if (msg != null && !msg.isEmpty()) {
                           player.sendMessage(msg);
                        }
                     } else {
                        String gemId = registry.gemIdFromItemId(oraxenId);
                        GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
                        if (handler != null) {
                           if (slot == AbilitySlot.QUINARY) {
                              handler.onQuinary(player, tier);
                           } else {
                              handler.onSenary(player, tier);
                           }
                        }
                     }
                  }
               }
            }
         } else {
            if (slot == AbilitySlot.QUINARY) {
               this.plugin.getGoldAbilities().onTertiary(player, 1);
            } else {
               this.plugin.getGoldAbilities().openSoulMenu(player);
            }
         }
      }
   }

   public boolean triggerSlot(Player player, AbilitySlot slot) {
      if (slot == null) {
         return false;
      }

      switch (slot) {
         case PRIMARY:
            this.triggerPrimary(player);
            return true;
         case SECONDARY:
            this.triggerSecondary(player);
            return true;
         case TERTIARY:
            this.triggerTertiary(player);
            return true;
         case QUATERNARY:
            this.triggerQuaternary(player);
            return true;
         case QUINARY:
            this.triggerQuinary(player);
            return true;
         case SENARY:
            this.triggerSenary(player);
            return true;
         default:
            return false;
      }
   }

   private void handleAbilityTertiary(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.blockedByGemLock(player)) {
            String oraxenId = this.findGemInHand(player);
            if (oraxenId == null) {
               String msg = this.plugin.getConfigManager().getFormattedMessage("must-hold-gem");
               if (msg != null && !msg.isEmpty()) {
                  player.sendMessage(msg);
               }
            } else {
               GemRegistryImpl registry = this.plugin.getGemRegistry();
               int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
               if (tier < 2 && !this.unlocksAllAtTier1(oraxenId)) {
                  String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }
               } else {
                  int energy = this.plugin.getEnergyManager().getEnergy(player);
                  if (energy <= 0) {
                     String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy");
                     if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                     }
                  } else {
                     GemType gemType = GemType.fromOraxenId(oraxenId);
                     if (gemType != null) {
                        switch (gemType) {
                           case ASTRA:
                              this.plugin.getAstraAbilities().activateDimensionalDrift(player);
                              break;
                           case FIRE:
                              this.plugin.getFireAbilities().crisp(player);
                              break;
                           case FLUX:
                              this.plugin.getFluxAbilities().flashbang(player);
                              break;
                           case LIFE:
                              this.plugin.getLifeAbilities().vitalityVortex(player);
                              break;
                           case PUFF:
                              this.plugin.getPuffAbilities().groupBreezyBash(player);
                              break;
                           case SPEED:
                              this.plugin.getSpeedAbilities().activateTerminalVelocity(player);
                              break;
                           case STRENGTH:
                              this.plugin.getStrengthAbilities().shadowStalker(player);
                              break;
                           case WEALTH:
                              this.plugin.getWealthAbilities().itemLock(player);
                              break;
                           default:
                              player.sendMessage("§c§oNo tertiary ability for your gem type!");
                        }
                     } else {
                        if (registry != null) {
                           String gemId = registry.gemIdFromItemId(oraxenId);
                           if ("auratus".equals(gemId)) {
                              this.plugin.getGemLockManager().castGemLock(player);
                              return;
                           }

                           GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
                           if (handler != null) {
                              handler.onTertiary(player, tier);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void handleAbilityQuaternary(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.blockedByGemLock(player)) {
            String oraxenId = this.findGemInHand(player);
            if (oraxenId == null) {
               String msg = this.plugin.getConfigManager().getFormattedMessage("must-hold-gem");
               if (msg != null && !msg.isEmpty()) {
                  player.sendMessage(msg);
               }
            } else {
               GemRegistryImpl registry = this.plugin.getGemRegistry();
               int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
               if (tier < 2 && !this.unlocksAllAtTier1(oraxenId)) {
                  String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2");
                  if (msg != null && !msg.isEmpty()) {
                     player.sendMessage(msg);
                  }
               } else {
                  int energy = this.plugin.getEnergyManager().getEnergy(player);
                  if (energy <= 0) {
                     String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy");
                     if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                     }
                  } else {
                     GemType gemType = GemType.fromOraxenId(oraxenId);
                     if (gemType != null) {
                        switch (gemType) {
                           case ASTRA:
                              this.plugin.getAstraAbilities().activateDimensionalVoid(player);
                              break;
                           case FIRE:
                              this.plugin.getFireAbilities().meteorShower(player);
                              break;
                           case FLUX:
                              this.plugin.getFluxAbilities().kineticBurst(player);
                              break;
                           case LIFE:
                              this.plugin.getLifeAbilities().heartLock(player);
                              break;
                           case PUFF:
                              this.plugin.getPuffAbilities().updraft(player);
                              break;
                           case SPEED:
                              this.plugin.getSpeedAbilities().lightningBackstab(player);
                              break;
                           case STRENGTH:
                              this.plugin.getStrengthAbilities().nullify(player);
                              break;
                           case WEALTH:
                              this.plugin.getWealthAbilities().amplification(player);
                              break;
                           default:
                              player.sendMessage("§c§oNo quaternary ability for your gem type!");
                        }
                     } else {
                        if (registry != null) {
                           String gemId = registry.gemIdFromItemId(oraxenId);
                           GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
                           if (handler != null) {
                              handler.onQuaternary(player, tier);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void handleAbilityBindingsList(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         AbilityBindingManager mgr = this.plugin.getAbilityBindingManager();
         if (mgr == null) {
            player.sendMessage("§cBinding system unavailable.");
         } else {
            EnumMap<AbilityBinding, AbilitySlot> map = mgr.getAll(player);
            EnumMap<AbilityBinding, AbilitySlot> defaults = mgr.getDefaults(player);
            player.sendMessage("§d§l⚡ Your Ability Bindings");

            for (AbilitySlot abilitySlot : AbilitySlot.values()) {
               AbilityBinding boundInput = null;

               for (Entry<AbilityBinding, AbilitySlot> e : map.entrySet()) {
                  if (e.getValue() == abilitySlot) {
                     boundInput = e.getKey();
                     break;
                  }
               }

               String inputLabel = boundInput != null ? "§f" + boundInput.getDisplayName() : "§8unbound";
               AbilityBinding defaultInput = null;

               for (Entry<AbilityBinding, AbilitySlot> e : defaults.entrySet()) {
                  if (e.getValue() == abilitySlot) {
                     defaultInput = e.getKey();
                     break;
                  }
               }

               String defaultLabel = defaultInput != null ? defaultInput.getDisplayName() : "unbound";
               player.sendMessage("§7" + abilitySlot.getDisplayName() + " §8→ " + inputLabel + " §8(default: §7" + defaultLabel + "§8)");
            }

            player.sendMessage("");
            player.sendMessage("§d§l⚡ Available Inputs");

            for (Enum enum_ : AbilityBinding.values()) {
               AbilitySlot s = map.get(enum_);
               String suffix = s != null ? " §8(§7" + s.getDisplayName() + "§8)" : "";
               player.sendMessage("§f• §7" + ((AbilityBinding)enum_).getId() + " §8- §f" + ((AbilityBinding)enum_).getDisplayName() + suffix);
            }

            player.sendMessage("");
            player.sendMessage("§7Open the controls menu with §f/bliss ability");
            player.sendMessage("§7Change with §f/bliss ability set <slot> <input>§7, unbind with §f<slot> none");
            player.sendMessage("§7Reset defaults with §f/bliss ability reset");
         }
      }
   }

   private void handleSetAbility(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         AbilityBindingManager mgr = this.plugin.getAbilityBindingManager();
         if (mgr == null) {
            player.sendMessage("§cBinding system unavailable.");
         } else if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            mgr.resetToDefaults(player);
            player.sendMessage("§aAbility bindings reset to defaults.");
         } else if (args.length < 3) {
            player.sendMessage("§cUsage: §f/bliss ability set <slot> <input>");
            player.sendMessage("§7Slots: §fprimary, secondary, tertiary, quaternary, quinary, senary");
            player.sendMessage("§7Inputs: §fright_click, shift_right_click, left_click, shift_left_click, swap_hand, shift_swap_hand, none");
            player.sendMessage("§7See current bindings with §f/bliss ability");
         } else {
            AbilitySlot slot = AbilitySlot.fromId(args[1]);
            if (slot == null) {
               player.sendMessage("§cUnknown slot: §f" + args[1] + "§c. Valid: primary, secondary, tertiary, quaternary, quinary, senary.");
            } else {
               String inputId = args[2];
               if (!inputId.equalsIgnoreCase("none") && !inputId.equalsIgnoreCase("unbind")) {
                  AbilityBinding input = AbilityBinding.fromId(inputId);
                  if (input == null) {
                     player.sendMessage("§cUnknown input: §f" + inputId + "§c. See §f/bliss ability§c for the list.");
                  } else {
                     mgr.setBinding(player, input, slot);
                     player.sendMessage("§aBound §f" + input.getDisplayName() + "§a → §l" + slot.getDisplayName() + "§a.");
                  }
               } else {
                  EnumMap<AbilityBinding, AbilitySlot> map = mgr.getAll(player);

                  for (Entry<AbilityBinding, AbilitySlot> e : map.entrySet()) {
                     if (e.getValue() == slot) {
                        mgr.unbind(player, e.getKey());
                     }
                  }

                  player.sendMessage("§aUnbound §l" + slot.getDisplayName() + "§a.");
               }
            }
         }
      }
   }

   private void handleTrust(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         if (args.length < 2) {
            sender.sendMessage("§cUsage: /bliss trust <player>");
         } else {
            Player player = (Player)sender;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
            } else if (player.getUniqueId().equals(target.getUniqueId())) {
               player.sendMessage("§cYou already trust yourself!");
            } else {
               this.plugin.getTrustedPlayersManager().addTrustedPlayer(player, target);
               player.sendMessage("§aYou now trust §l" + target.getName() + "§r§a! Your gem abilities will not harm them.");
               target.sendMessage("§a" + player.getName() + " §anow trusts you! Their gem abilities will not harm you.");
            }
         }
      }
   }

   private void handleUntrust(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         if (args.length < 2) {
            sender.sendMessage("§cUsage: /bliss untrust <player>");
         } else {
            Player player = (Player)sender;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
            } else {
               boolean removed = this.plugin.getTrustedPlayersManager().removeTrustedPlayer(player, target);
               if (removed) {
                  player.sendMessage("§cYou no longer trust §l" + target.getName() + "§r§c! Your gem abilities can now harm them.");
                  target.sendMessage("§c" + player.getName() + " §cno longer trusts you! Their gem abilities can now harm you.");
               } else {
                  player.sendMessage("§cYou were not trusting " + target.getName() + "!");
               }
            }
         }
      }
   }

   private void handleTrustedList(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         Set<UUID> trusted = this.plugin.getTrustedPlayersManager().getTrustedPlayers(player);
         if (trusted.isEmpty()) {
            player.sendMessage("§7You have no trusted players. Use §b/bliss trust <player>§7 to add someone.");
         } else {
            player.sendMessage("§5§lTrusted Players (" + trusted.size() + "):");

            for (UUID uuid : trusted) {
               OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
               String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
               String status = offlinePlayer.isOnline() ? "§a[Online]" : "§7[Offline]";
               player.sendMessage("§8 - §b" + name + " " + status);
            }
         }
      }
   }

   private void handleBannable(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      } else if (args.length < 2) {
         sender.sendMessage("§cUsage: /bliss bannable <true/false>");
         sender.sendMessage("§7Current status: " + (this.plugin.getConfigManager().isBanOnZeroEnergyEnabled() ? "§aEnabled" : "§cDisabled"));
      } else {
         String value = args[1].toLowerCase();
         boolean enable;
         if (!value.equals("true") && !value.equals("on") && !value.equals("yes") && !value.equals("1")) {
            if (!value.equals("false") && !value.equals("off") && !value.equals("no") && !value.equals("0")) {
               sender.sendMessage("§cInvalid value! Use true or false.");
               return;
            }

            enable = false;
         } else {
            enable = true;
         }

         this.plugin.getConfigManager().setBanOnZeroEnergy(enable);
         if (enable) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("ban-enabled");
            if (msg != null && !msg.isEmpty()) {
               sender.sendMessage(msg);
            } else {
               sender.sendMessage("§aBan-on-zero-energy has been enabled!");
            }
         } else {
            String msg = this.plugin.getConfigManager().getFormattedMessage("ban-disabled");
            if (msg != null && !msg.isEmpty()) {
               sender.sendMessage(msg);
            } else {
               sender.sendMessage("§cBan-on-zero-energy has been disabled!");
            }
         }
      }
   }

   private void handleOraxen(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      } else if (args.length < 2) {
         sender.sendMessage("§cUsage: /bliss oraxen <true/false>");
         sender.sendMessage(
            "§7Auto-replace legacy gems with Oraxen items on join: " + (OraxenGemFixer.isFixOnJoinEnabled(this.plugin) ? "§aEnabled" : "§cDisabled")
         );
      } else {
         String value = args[1].toLowerCase();
         boolean enable;
         if (!value.equals("true") && !value.equals("on") && !value.equals("yes") && !value.equals("1")) {
            if (!value.equals("false") && !value.equals("off") && !value.equals("no") && !value.equals("0")) {
               sender.sendMessage("§cInvalid value! Use true or false.");
               return;
            }

            enable = false;
         } else {
            enable = true;
         }

         OraxenGemFixer.setFixOnJoinEnabled(this.plugin, enable);
         if (enable) {
            sender.sendMessage("§aLegacy gems will now be replaced with Oraxen items when players join!");
         } else {
            sender.sendMessage("§cAutomatic gem replacement on join has been disabled!");
         }
      }
   }

   private void handleAutoSmelt(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!player.hasPermission("blissgems.autosmelt")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
         } else {
            boolean hasFire = this.plugin.getGemManager().hasGemType(player, GemType.FIRE);
            boolean hasWealthT2 = this.plugin.getGemManager().hasGemType(player, GemType.WEALTH)
               && this.plugin.getGemManager().getGemTier(player, GemType.WEALTH) >= 2;
            if (!hasFire && !hasWealthT2) {
               player.sendMessage("§c§lYou need the Fire Gem or Wealth Gem (Tier 2) to use auto-smelt!");
            } else {
               boolean currentState = this.plugin.getWealthAbilities().isAutoSmeltEnabled(player);
               this.plugin.getWealthAbilities().setAutoSmelt(player, !currentState);
               if (!currentState) {
                  player.sendMessage("§a§lAuto-Smelt enabled! Ores will now be automatically smelted when mined.");
               } else {
                  player.sendMessage("§c§lAuto-Smelt disabled!");
               }
            }
         }
      }
   }

   private void handleConduction(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.plugin.getGemManager().hasGemType(player, GemType.FLUX)) {
            player.sendMessage("§c§lYou need the Flux Gem to use Conduction!");
         } else {
            this.plugin.getFluxAbilities().conduction(player);
         }
      }
   }

   private void handleCharge(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.plugin.getGemManager().hasGemType(player, GemType.FLUX)) {
            player.sendMessage("§c§lYou need the Flux Gem to use the Watt Deposit!");
         } else {
            if (this.plugin.getFluxEnergyManager() != null) {
               this.plugin.getFluxEnergyManager().openChargingStation(player);
            }
         }
      }
   }

   private void handleSetWatts(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage("§cYou don't have permission to use this command!");
      } else if (args.length < 3) {
         sender.sendMessage("§cUsage: /bliss setwatts <player> <amount>");
      } else {
         Player target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            sender.sendMessage("§cPlayer not found!");
         } else {
            double amount;
            try {
               amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
               sender.sendMessage("§cInvalid number: " + args[2]);
               return;
            }

            if (this.plugin.getFluxEnergyManager() != null) {
               this.plugin.getFluxEnergyManager().setWatts(target.getUniqueId(), amount);
               sender.sendMessage("§aSet §e" + target.getName() + "§a's Flux Gem energy to §b" + String.format("%,.0f", amount) + " Watts§a.");
               target.sendMessage("§b\ud83d\udd2e §aAn administrator updated your Flux Gem energy to §b" + String.format("%,.0f", amount) + " Watts§a.");
            }
         }
      }
   }

   private void handleGetWatts(CommandSender sender, String[] args) {
      Player target;
      if (args.length > 1) {
         if (!sender.hasPermission("blissgems.admin") && !sender.getName().equalsIgnoreCase(args[1])) {
            sender.sendMessage("§cYou don't have permission to view other players' energy!");
            return;
         }

         target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
         }
      } else {
         if (!this.requirePlayer(sender)) {
            return;
         }

         target = (Player)sender;
      }

      if (this.plugin.getFluxEnergyManager() != null) {
         double watts = this.plugin.getFluxEnergyManager().getWatts(target.getUniqueId());
         int maxWatts = this.plugin.getFluxEnergyManager().getMaxWatts();
         double beam = this.plugin.getFluxEnergyManager().getBeamCharge(target.getUniqueId());
         double percent = watts / maxWatts * 100.0;
         sender.sendMessage("§b\ud83d\udd2e §6§lFlux Energy Status for §e" + target.getName() + "§6:");
         sender.sendMessage(
            "§f  Battery: §b" + String.format("%,.0f", watts) + " §7/ §b" + String.format("%,d", maxWatts) + " W §a(" + String.format("%.2f%%", percent) + ")"
         );
         sender.sendMessage("§f  Beam Charge: §e" + String.format("%.1f%%", beam));
         sender.sendMessage("§f  Charging State: " + (this.plugin.getFluxEnergyManager().isCharging(target) ? "§aCharging" : "§cIdle"));
      }
   }

   private void handleStats(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         StatsCommand statsCommand = new StatsCommand(this.plugin);
         statsCommand.execute(player, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
      }
   }

   private void handleReleaseSouls(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.plugin.getGemManager().hasGemType(player, GemType.ASTRA)) {
            player.sendMessage("§c§oOnly Astra gem holders can release captured souls!");
         } else if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            player.sendMessage("§c§oYour gem energy is too low to release souls!");
         } else {
            this.plugin.getSoulManager().releaseAllSouls(player);
         }
      }
   }

   private void handleSoulsInfo(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.plugin.getGemManager().hasGemType(player, GemType.ASTRA)) {
            player.sendMessage("§c§oOnly Astra gem holders can view captured souls!");
         } else {
            List<SoulManager.CapturedMob> souls = this.plugin.getSoulManager().getCapturedSouls(player);
            int count = souls.size();
            int max = 2;
            player.sendMessage("§d§lCaptured Souls (" + count + "/" + max + "):");
            if (count == 0) {
               player.sendMessage("§7  No souls captured. Sneak + hit a mob to capture it.");
            } else {
               for (int i = 0; i < souls.size(); i++) {
                  player.sendMessage("§d  " + (i + 1) + ". §f" + souls.get(i).getDisplayName());
               }

               player.sendMessage("§7  Use §d/bliss release§7 to release all captured souls.");
            }
         }
      }
   }

   private void handleNormalise(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      } else {
         int count = 0;

         for (Player online : Bukkit.getOnlinePlayers()) {
            AttributeInstance attackSpeed = online.getAttribute(Attributes.attackSpeed());
            if (attackSpeed != null) {
               double original = attackSpeed.getBaseValue();
               attackSpeed.setBaseValue(1024.0);
               attackSpeed.setBaseValue(original);
            }

            count++;
         }

         sender.sendMessage("§a§lAttack cooldown reset for " + count + " online player(s)!");

         for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§e§oAttack cooldowns have been normalized by an admin.");
         }
      }
   }

   private void handlePedestal(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      } else {
         PedestalManager pedestal = this.plugin.getPedestalManager();
         if (pedestal == null) {
            sender.sendMessage("§cThe pedestal system failed to load.");
         } else {
            String sub = args.length < 2 ? "" : args[1].toLowerCase();
            switch (sub) {
               case "set":
                  if (!(sender instanceof Player)) {
                     sender.sendMessage("§cOnly players can set the pedestal.");
                     return;
                  }

                  pedestal.setPedestal(((Player)sender).getLocation().clone().add(0.0, -1.0, 0.0));
                  sender.sendMessage(
                     PedestalManager.color("&aPedestal location set (center is one block below you). Run /bliss pedestal activate to build it.")
                  );
                  return;
               case "activate":
                  if (!pedestal.activate()) {
                     sender.sendMessage(PedestalManager.color("&cPedestal location not set. Run /bliss pedestal set first."));
                     return;
                  }

                  Bukkit.broadcastMessage(PedestalManager.color("&6 The Pedestal Is Now &b&lOpen"));
                  return;
               case "deactivate":
                  pedestal.deactivate();
                  Bukkit.broadcastMessage(PedestalManager.color("&6 The Pedestal Has Closed &c&l"));
                  return;
               case "status":
                  PedestalState st = pedestal.state();
                  Location main = st.mainLoc();
                  sender.sendMessage(
                     PedestalManager.color(
                        "&7Location: &f"
                           + (main == null ? "not set" : main.getWorld().getName() + " " + main.getBlockX() + " " + main.getBlockY() + " " + main.getBlockZ())
                     )
                  );
                  sender.sendMessage(
                     PedestalManager.color(
                        "&7Active: &f"
                           + st.active()
                           + " &7Beacon: &f"
                           + st.beacon()
                           + " &7Ritual: &f"
                           + st.ritual()
                           + " &7Count: &f"
                           + st.count()
                           + "/5 &7Durability: &f"
                           + st.durability()
                           + "/3"
                     )
                  );
                  return;
               case "export":
                  Location main = pedestal.state().mainLoc();
                  if (main == null) {
                     sender.sendMessage(PedestalManager.color("&cPedestal location not set."));
                     return;
                  }

                  sender.sendMessage(
                     PedestalManager.color(
                        "&7pedestal.fixed: &fworld: "
                           + main.getWorld().getName()
                           + ", x: "
                           + main.getBlockX()
                           + ", y: "
                           + main.getBlockY()
                           + ", z: "
                           + main.getBlockZ()
                     )
                  );
                  return;
               default:
                  sender.sendMessage("§cUsage: /bliss pedestal <set|activate|deactivate|status|export>");
            }
         }
      }
   }

   private void handleSmp(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      } else if (args.length >= 2 && args[1].equalsIgnoreCase("start")) {
         if (this.plugin.getConfigManager().isSmpStarted()) {
            this.plugin.getConfigManager().sendFormattedMessage(sender, "smp-already-started");
         } else {
            this.plugin.getConfigManager().setSmpStarted(true);
            this.plugin.getConfigManager().sendFormattedMessage(sender, "smp-started");

            for (Player online : Bukkit.getOnlinePlayers()) {
               String randomGem;
               if (!this.hasReceivedFirstGem(online) && (randomGem = this.getRandomEnabledGem()) != null) {
                  String finalGem = randomGem;
                  Player target = online;
                  target.sendMessage("");
                  target.sendMessage("§d§l§m                                                  ");
                  target.sendMessage("§d§lWELCOME TO BLISSGEMS!");
                  target.sendMessage("");
                  target.sendMessage("§7§oThe ancient gem ritual begins...");
                  target.sendMessage("§7§oYour destiny is being forged...");
                  target.sendMessage("§d§l§m                                                  ");
                  target.sendMessage("");
                  this.plugin.getGemRitualManager().performGemRitual(target, finalGem, true, 1);
                  this.plugin
                     .getServer()
                     .getScheduler()
                     .runTaskLater(
                        this.plugin,
                        () -> {
                           if (target.isOnline() && this.plugin.getGemManager().giveGem(target, finalGem, 1)) {
                              this.markFirstGemReceived(target);
                              String gemName = this.plugin.getGemManager().getGemDisplayName(finalGem);
                              String welcomeMsg = this.plugin.getConfigManager().getFormattedMessage("first-gem-received", "gem", gemName);
                              if (welcomeMsg != null && !welcomeMsg.isEmpty()) {
                                 target.sendMessage(welcomeMsg);
                              } else {
                                 target.sendMessage("");
                                 target.sendMessage(
                                    "§d§l» §fYour gem has been chosen: " + this.plugin.getGemManager().getGemColorCode(finalGem) + "§l" + gemName + "§d§l «"
                                 );
                                 target.sendMessage("");
                              }

                              this.plugin.getLogger().info("SMP Start: Gave " + target.getName() + " their first gem: " + gemName);
                           }
                        },
                        20L
                     );
               }
            }
         }
      } else {
         sender.sendMessage("§cUsage: /bliss smp start");
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

   private String getRandomEnabledGem() {
      List<String> enabledGems = this.plugin.getGemManager().getAvailableGemIds();
      return enabledGems.isEmpty() ? null : enabledGems.get(new Random().nextInt(enabledGems.size()));
   }

   private void handleListCooldowns(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      } else if (args.length < 2) {
         sender.sendMessage("§cUsage: /bliss cooldowns <player>");
      } else {
         Player target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
         } else {
            long now = System.currentTimeMillis();
            TreeMap<String, String> lines = new TreeMap<>();

            for (Entry<String, Long> e : this.plugin.getAbilityManager().getCooldownsFor(target.getUniqueId()).entrySet()) {
               long left = (long)Math.ceil((e.getValue() - now) / 1000.0);
               if (left > 0L) {
                  lines.put(e.getKey(), "cooldown " + left);
               }
            }

            for (Entry<String, Long> e : this.plugin.getAbilityManager().getActiveAbilitiesFor(target.getUniqueId()).entrySet()) {
               long left = (long)Math.ceil((e.getValue() - now) / 1000.0);
               if (left > 0L) {
                  lines.merge(e.getKey(), "active " + left, (a, b) -> a + " " + b);
               }
            }

            sender.sendMessage("§d" + target.getName() + " §7cooldowns: §f" + lines.size());

            for (Entry<String, String> e : lines.entrySet()) {
               sender.sendMessage("§7" + e.getKey() + " §f" + e.getValue());
            }
         }
      }
   }

   private void handleClearCooldowns(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      } else if (args.length < 2) {
         sender.sendMessage("§cUsage: /bliss clearcds <player|all>");
      } else if (!args[1].equalsIgnoreCase("all")) {
         Player target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
         } else {
            this.plugin.getAbilityManager().clearCooldowns(target);
            sender.sendMessage("§aCleared all ability cooldowns for §l" + target.getName() + "§a!");
         }
      } else {
         int count = 0;

         for (Player online : Bukkit.getOnlinePlayers()) {
            this.plugin.getAbilityManager().clearCooldowns(online);
            count++;
         }

         sender.sendMessage("§a§lCleared all ability cooldowns for " + count + " online player(s)!");
      }
   }

   private void handleNoCooldownToggle(CommandSender sender, String[] args) {
      if (!sender.hasPermission("blissgems.admin")) {
         sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission"));
      } else {
         if (args.length < 2) {
            if (!(sender instanceof Player target)) {
               sender.sendMessage("§cUsage: /bliss nocdtoggle <player>");
               return;
            }
         } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found"));
               return;
            }
         }

         boolean enabled = this.plugin.getAbilityManager().toggleNoCooldown(target);
         if (enabled) {
            sender.sendMessage("§a§lNo-cooldown §aENABLED for §l" + target.getName() + "§a!");
            if (target != sender) {
               target.sendMessage("§a§oYour ability cooldowns have been disabled.");
            }
         } else {
            sender.sendMessage("§c§lNo-cooldown §cDISABLED for §l" + target.getName() + "§c!");
            if (target != sender) {
               target.sendMessage("§c§oYour ability cooldowns are back to normal.");
            }
         }
      }
   }

   private void handleAchievements(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         Set<Achievement> unlocked = this.plugin.getAchievementManager().getUnlocked(player);
         int total = Achievement.values().length;
         int unlockedCount = unlocked.size();
         player.sendMessage("§6§l⭐ Achievements (" + unlockedCount + "/" + total + ")");
         player.sendMessage("");

         for (Achievement achievement : Achievement.values()) {
            boolean isUnlocked = unlocked.contains(achievement);
            int progress = this.plugin.getAchievementManager().getProgress(player, achievement);
            int target = achievement.getTargetProgress();
            if (isUnlocked) {
               player.sendMessage("§a✔ §e" + achievement.getDisplayName() + " §7- " + achievement.getDescription());
            } else if (target > 1 && progress > 0) {
               player.sendMessage("§8✘ §7" + achievement.getDisplayName() + " §8- " + achievement.getDescription() + " §e(" + progress + "/" + target + ")");
            } else {
               player.sendMessage("§8✘ §7" + achievement.getDisplayName() + " §8- " + achievement.getDescription());
            }
         }
      }
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage("§5§lBlissGems Commands:");
      sender.sendMessage("§7/bliss give <player> <gem_type> [tier] §8- Give a gem");
      sender.sendMessage("§7/bliss reroll <player> [tier] §8- Give a random gem");
      sender.sendMessage("§7/bliss giveitem <player> <item_id> [amount] §8- Give special items");
      sender.sendMessage("§7/bliss energy <player> <set/add/remove> <amount> §8- Manage energy");
      sender.sendMessage("§7/bliss withdraw §8- Extract energy into bottle");
      sender.sendMessage("§7/bliss info §8- Show your gem info");
      sender.sendMessage("§7/bliss pockets §8- Open personal inventory (Wealth T2)");
      sender.sendMessage("§7/bliss amplify §8- Amplify potion effects (Wealth T2)");
      sender.sendMessage("§7/bliss autosmelt §8- Toggle auto-smelting (Wealth T2)");
      sender.sendMessage("§7/bliss conduction §8- Teleport to nearest copper block (Flux)");
      sender.sendMessage("§7/bliss toggle_click §8- Toggle click activation on/off");
      sender.sendMessage("§7/bliss ability:main §8- Trigger primary ability");
      sender.sendMessage("§7/bliss ability:secondary §8- Trigger secondary ability (T2)");
      sender.sendMessage("§7/bliss trust <player> §8- Trust player (prevent friendly fire)");
      sender.sendMessage("§7/bliss untrust <player> §8- Untrust player");
      sender.sendMessage("§7/bliss trusted §8- List trusted players");
      sender.sendMessage("§7/bliss souls §8- View captured souls (Astra)");
      sender.sendMessage("§7/bliss release §8- Release captured souls (Astra)");
      sender.sendMessage("§7/bliss achievements §8- View your achievements");
      sender.sendMessage("§7/bliss stats [top|me|gems] §8- View server stats");
      sender.sendMessage("§7/bliss bannable <true/false> §8- Toggle ban on 0 energy (Admin)");
      sender.sendMessage("§7/bliss smp start §8- Start the SMP and distribute gems (Admin)");
      sender.sendMessage("§7/bliss normalise §8- Reset attack cooldowns for all players (Admin)");
      sender.sendMessage("§7/bliss clearcds <player|all> §8- Clear ability cooldowns (Admin)");
      sender.sendMessage("§7/bliss nocdtoggle <player> §8- Toggle no ability cooldowns (Admin)");
      sender.sendMessage("§7/bliss goldgem fill <player> <soulType> <tier> §8- Add a harvested soul (Admin)");
      sender.sendMessage("§7/bliss goldgem remove <player> <soulType> §8- Take a soul back out (Admin)");
      sender.sendMessage("§7/bliss goldgem clear <player> §8- Empty a Gold Gem (Admin)");
      sender.sendMessage("§7/bliss goldgem list <player> §8- List harvested souls (Admin)");
      sender.sendMessage("§7/bliss ability §8- Show your ability keybinds");
      sender.sendMessage("§7/bliss ability §8- Customize your ability controls");
      sender.sendMessage("§7/bliss reload §8- Reload config");
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      ArrayList<String> completions = new ArrayList<>();
      if (args.length == 1) {
         completions.addAll(
            Arrays.asList(
               "admin",
               "give",
               "reroll",
               "giveitem",
               "transfer",
               "energy",
               "withdraw",
               "info",
               "pockets",
               "amplify",
               "autosmelt",
               "conduction",
               "charge",
               "setwatts",
               "getwatts",
               "reload",
               "toggle_click",
               "ability:main",
               "ability:secondary",
               "ability:tertiary",
               "ability:quaternary",
               "primary",
               "secondary",
               "tertiary",
               "quaternary",
               "quinary",
               "senary",
               "viewer",
               "gemviewer",
               "gui",
               "trust",
               "untrust",
               "trusted",
               "stats",
               "achievements",
               "bannable",
               "oraxen",
               "souls",
               "release",
               "normalise",
               "normalize",
               "smp",
               "cooldowns",
               "clearcds",
               "nocdtoggle",
               "goldgem",
               "goldcycle",
               "goldarmor",
               "ability",
               "set_ability",
               "enchantlimit",
               "spawnvillager",
               "news",
               "pedestal",
               "deps",
               "unbounded",
               "gold",
               "shaders"
            )
         );
      } else if (args.length == 2) {
         if (args[0].equalsIgnoreCase("gold")) {
            return Arrays.asList("souls", "cycle", "beam", "armor");
         }

         if (args[0].equalsIgnoreCase("unbounded")) {
            return Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
         }

         if (args[0].equalsIgnoreCase("ability")) {
            return Arrays.asList("main", "secondary", "tertiary", "quaternary", "quinary", "senary", "set", "reset", "list");
         }

         if (args[0].equalsIgnoreCase("spawnvillager")) {
            return Arrays.asList("mace1", "mace2", "mace3", "energy");
         }

         if (args[0].equalsIgnoreCase("enchantlimit")) {
            return Arrays.asList("density", "breach", "wind_burst", "sharpness", "protection", "unbreaking", "mending");
         }

         if (args[0].equalsIgnoreCase("nocdtoggle") || args[0].equalsIgnoreCase("setwatts") || args[0].equalsIgnoreCase("getwatts")) {
            return Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
         }

         if (args[0].equalsIgnoreCase("clearcds")) {
            List<String> targets = Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
            targets.add(0, "all");
            return targets;
         }

         if (args[0].equalsIgnoreCase("give")
            || args[0].equalsIgnoreCase("reroll")
            || args[0].equalsIgnoreCase("giveitem")
            || args[0].equalsIgnoreCase("transfer")
            || args[0].equalsIgnoreCase("energy")
            || args[0].equalsIgnoreCase("trust")
            || args[0].equalsIgnoreCase("untrust")) {
            return Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
         }

         if (args[0].equalsIgnoreCase("stats")) {
            return Arrays.asList("top", "me", "gems");
         }

         if (args[0].equalsIgnoreCase("bannable") || args[0].equalsIgnoreCase("oraxen")) {
            return Arrays.asList("true", "false");
         }

         if (args[0].equalsIgnoreCase("smp")) {
            return Arrays.asList("start");
         }

         if (args[0].equalsIgnoreCase("pedestal")) {
            return Arrays.asList("set", "activate", "deactivate", "status", "export");
         }

         if (args[0].equalsIgnoreCase("goldgem")) {
            return Arrays.asList("fill", "remove", "clear", "list");
         }

         if (args[0].equalsIgnoreCase("set_ability")) {
            List<String> slots = Arrays.stream(AbilitySlot.values()).map(AbilitySlot::getId).collect(Collectors.toList());
            slots.add("reset");
            return slots;
         }
      } else if (args.length == 3) {
         if (args[0].equalsIgnoreCase("setwatts")) {
            return Arrays.asList("100000", "500000", "1000000", "2000000");
         }

         if (args[0].equalsIgnoreCase("goldgem")) {
            return Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
         }

         if (args[0].equalsIgnoreCase("give")) {
            List<String> gemIds = Arrays.stream(GemType.values()).map(GemType::getId).collect(Collectors.toList());
            GemRegistryImpl registry = this.plugin.getGemRegistry();
            if (registry != null) {
               for (GemDefinition def : registry.getAllGems()) {
                  if (!gemIds.contains(def.getId())) {
                     gemIds.add(def.getId());
                  }
               }
            }

            return gemIds;
         }

         if (args[0].equalsIgnoreCase("giveitem")) {
            ArrayList<String> items = new ArrayList<>();
            items.add("energy_bottle");
            items.add("repair_kit");
            items.add("gem_trader");
            items.add("gem_fragment");
            items.add("gem_upgrader");
            items.add("restoration_book");
            items.add("prismatic_edge");
            return items;
         }

         if (args[0].equalsIgnoreCase("transfer")) {
            return new ArrayList<>(TRANSFERABLE_ITEMS);
         }

         if (args[0].equalsIgnoreCase("energy")) {
            return Arrays.asList("set", "add", "remove");
         }

         if (args[0].equalsIgnoreCase("set_ability")) {
            List<String> inputs = Arrays.stream(AbilityBinding.values()).map(AbilityBinding::getId).collect(Collectors.toList());
            inputs.add("none");
            return inputs;
         }
      } else if (args.length == 3) {
         if (args[0].equalsIgnoreCase("reroll")) {
            return Arrays.asList("1", "2");
         }
      } else if (args.length == 4) {
         if (args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("1", "2");
         }

         if (args[0].equalsIgnoreCase("giveitem") || args[0].equalsIgnoreCase("transfer")) {
            return Arrays.asList("1", "8", "16", "32", "64");
         }

         if (args[0].equalsIgnoreCase("goldgem") && (args[1].equalsIgnoreCase("fill") || args[1].equalsIgnoreCase("remove"))) {
            return Arrays.stream(GemType.values()).map(GemType::getId).collect(Collectors.toList());
         }
      } else if (args.length == 5 && args[0].equalsIgnoreCase("goldgem") && args[1].equalsIgnoreCase("fill")) {
         return Arrays.asList("1", "2");
      }

      return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
   }

   private void handleUnbounded(CommandSender sender, String[] args) {
      if (this.requirePlayer(sender)) {
         Player player = (Player)sender;
         if (!this.plugin.getGemManager().hasGemType(player, GemType.ASTRA)) {
            player.sendMessage("§5\ud83d\udd2e §cYou need the Astra gem to use Unbounded.");
         } else {
            double range = this.plugin.getConfig().getDouble("abilities.astra-unbounded.range", 12.0);
            Player target = null;
            if (args.length > 1) {
               target = Bukkit.getPlayerExact(args[1]);
               if (target == null || target.getWorld() != player.getWorld() || target.getLocation().distance(player.getLocation()) > range) {
                  player.sendMessage("§5\ud83d\udd2e §c" + args[1] + " is not within " + (int)range + " blocks.");
                  return;
               }
            } else {
               RayTraceResult hit = player.getWorld()
                  .rayTraceEntities(
                     player.getEyeLocation(),
                     player.getEyeLocation().getDirection(),
                     range,
                     0.6,
                     e -> e instanceof Player && !e.equals(player) && ((Player)e).getGameMode() != GameMode.SPECTATOR
                  );
               if (hit != null) {
                  target = (Player)hit.getHitEntity();
               }

               if (target == null) {
                  player.sendMessage("§5\ud83d\udd2e §cLook at a player within " + (int)range + " blocks, or name one: §f/bliss unbounded <player>");
                  return;
               }
            }

            if (!target.equals(player)) {
               if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(player)) {
                  player.sendMessage("§6ꐬ §c§oYour gem is locked!");
               } else {
                  if (this.plugin.getAstraUnbounded() != null) {
                     this.plugin.getAstraUnbounded().start(player, target);
                  }
               }
            }
         }
      }
   }

   private void handleShaders(CommandSender sender) {
      String url = this.plugin
         .getConfig()
         .getString("golden-dream.sky.shaderpack-url", "https://github.com/ItzJustNett/blissgems/releases/latest/download/BlissGems-Shaders.zip");
      sender.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + "BlissGems shaderpack" + ChatColor.GRAY + " (optional, Java + Iris or OptiFine)");
      sender.sendMessage(ChatColor.GRAY + "GoldenDome in the golden dream, GloomHaze in the memory world, plain Minecraft everywhere else.");
      TextComponent link = new TextComponent("" + ChatColor.AQUA + ChatColor.UNDERLINE + "Download BlissGems-Shaders.zip");
      link.setClickEvent(new ClickEvent(Action.OPEN_URL, url));
      sender.spigot().sendMessage(link);
      sender.sendMessage(
         ChatColor.GRAY
            + "Put the zip in "
            + ChatColor.WHITE
            + ".minecraft/shaderpacks"
            + ChatColor.GRAY
            + ", then pick "
            + ChatColor.WHITE
            + "BlissGems-Shaders"
            + ChatColor.GRAY
            + " in Video Settings > Shader Packs."
      );
   }

   private void handleDeps(CommandSender sender) {
      sender.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + "BlissGems optional plugins" + ChatColor.GRAY + " (none are required)");
      String[][] deps = new String[][]{
         {"Citizens", "NPC memories in the Golden Dream (/goldendream memory npc)"},
         {"Geyser-Spigot", "Bedrock players: dream fog and screen fades (skies/time/weather work without it)"},
         {"WorldGuard", "region flags that block abilities"},
         {"ProtocolLib", "extra packet effects"}
      };

      for (String[] d : deps) {
         boolean on = Bukkit.getPluginManager().isPluginEnabled(d[0]);
         sender.sendMessage(
            (on ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✖ ")
               + ChatColor.WHITE
               + d[0]
               + ChatColor.GRAY
               + " — "
               + d[1]
               + (on ? "" : ChatColor.DARK_GRAY + " (not installed)")
         );
      }

      sender.sendMessage(ChatColor.GRAY + "Resource pack: models and sounds (bliss:*) for rituals; without it everything still runs, with vanilla looks.");
   }
}
