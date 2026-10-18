package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class CooldownDisplayManager {
   private static final String GOLD_GEM_GLYPH = "\ue020";
   private static final String GOLD_BEAM_GLYPH = "\ue021";
   private final BlissGems plugin;
   private BukkitTask displayTask;
   private static final Map<GemType, List<String[]>> GEM_ABILITIES = new HashMap<>();
   private static final Key HUD_FONT = Key.key("bliss", "hud");

   public CooldownDisplayManager(BlissGems plugin) {
      this.plugin = plugin;
      this.startDisplayTask();
   }

   private void startDisplayTask() {
      this.displayTask = this.plugin
         .getServer()
         .getScheduler()
         .runTaskTimer(
            this.plugin,
            () -> {
               for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                  if (this.plugin.getGemManager().hasActiveGem(player)
                     && this.isHoldingGem(player)
                     && !this.plugin.getFireAbilities().isCharging(player)
                     && !this.plugin.getFluxAbilities().isCharging(player)
                     && !this.plugin.getStrengthAbilities().isTrackingActive(player)) {
                     if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(player)) {
                        int left = this.plugin.getGemLockManager().getRemainingSeconds(player.getUniqueId());
                        String locked = "§6ꐬ §c§lGEM LOCKED §8| §7" + left + "s";
                        this.sendActionBar(player, locked);
                     } else {
                        int energy = this.plugin.getEnergyManager().getEnergy(player);
                        Object cooldownDisplay = this.buildCooldownDisplay(player);
                        if (energy == 0) {
                           String brokenPrefix = "§c§lBROKEN §8| ";
                           cooldownDisplay = !((String)cooldownDisplay).isEmpty() ? brokenPrefix + (String)cooldownDisplay : "§c§lBROKEN";
                        }

                        if (!((String)cooldownDisplay).isEmpty()) {
                           this.sendActionBar(player, (String)cooldownDisplay);
                        }
                     }
                  }
               }
            },
            0L,
            10L
         );
   }

   private void sendActionBar(Player player, String legacy) {
      TextComponent out = Component.empty();
      StringBuilder run = new StringBuilder();
      boolean runGlyph = false;
      TextColor color = null;
      boolean bold = false;
      boolean italic = false;
      boolean strike = false;
      boolean under = false;
      boolean obf = false;

      for (int i = 0; i < legacy.length(); i++) {
         char ch = legacy.charAt(i);
         if (ch == 167 && i + 1 < legacy.length()) {
            if (run.length() > 0) {
               out = (TextComponent)out.append(this.styleRun(run.toString(), runGlyph, color, bold, italic, strike, under, obf));
               run.setLength(0);
            }

            char code = Character.toLowerCase(legacy.charAt(++i));
            switch (code) {
               case '0':
               case '1':
               case '2':
               case '3':
               case '4':
               case '5':
               case '6':
               case '7':
               case '8':
               case '9':
               case 'a':
               case 'b':
               case 'c':
               case 'd':
               case 'e':
               case 'f':
                  color = legacyColor(code);
                  obf = false;
                  under = false;
                  strike = false;
                  italic = false;
                  bold = false;
               case ':':
               case ';':
               case '<':
               case '=':
               case '>':
               case '?':
               case '@':
               case 'A':
               case 'B':
               case 'C':
               case 'D':
               case 'E':
               case 'F':
               case 'G':
               case 'H':
               case 'I':
               case 'J':
               case 'K':
               case 'L':
               case 'M':
               case 'N':
               case 'O':
               case 'P':
               case 'Q':
               case 'R':
               case 'S':
               case 'T':
               case 'U':
               case 'V':
               case 'W':
               case 'X':
               case 'Y':
               case 'Z':
               case '[':
               case '\\':
               case ']':
               case '^':
               case '_':
               case '`':
               case 'g':
               case 'h':
               case 'i':
               case 'j':
               case 'p':
               case 'q':
               default:
                  break;
               case 'k':
                  obf = true;
                  break;
               case 'l':
                  bold = true;
                  break;
               case 'm':
                  strike = true;
                  break;
               case 'n':
                  under = true;
                  break;
               case 'o':
                  italic = true;
                  break;
               case 'r':
                  color = null;
                  obf = false;
                  under = false;
                  strike = false;
                  italic = false;
                  bold = false;
            }
         } else {
            boolean glyph = isGlyph(ch);
            if (run.length() > 0 && glyph != runGlyph) {
               out = (TextComponent)out.append(this.styleRun(run.toString(), runGlyph, color, bold, italic, strike, under, obf));
               run.setLength(0);
            }

            runGlyph = glyph;
            run.append(ch);
         }
      }

      if (run.length() > 0) {
         out = (TextComponent)out.append(this.styleRun(run.toString(), runGlyph, color, bold, italic, strike, under, obf));
      }

      player.sendActionBar(out);
   }

   private static boolean isGlyph(char ch) {
      return ch < ' ' || ch > '~';
   }

   private Component styleRun(String text, boolean glyph, TextColor color, boolean bold, boolean italic, boolean strike, boolean under, boolean obf) {
      Component c = Component.text(text);
      if (color != null) {
         c = c.color(color);
      }

      if (bold) {
         c = c.decorate(TextDecoration.BOLD);
      }

      if (italic) {
         c = c.decorate(TextDecoration.ITALIC);
      }

      if (strike) {
         c = c.decorate(TextDecoration.STRIKETHROUGH);
      }

      if (under) {
         c = c.decorate(TextDecoration.UNDERLINED);
      }

      if (obf) {
         c = c.decorate(TextDecoration.OBFUSCATED);
      }

      if (!glyph) {
         c = c.font(HUD_FONT);
      }

      return c;
   }

   private static TextColor legacyColor(char code) {
      return switch (code) {
         case '0' -> TextColor.color(0);
         case '1' -> TextColor.color(170);
         case '2' -> TextColor.color(43520);
         case '3' -> TextColor.color(43690);
         case '4' -> TextColor.color(11141120);
         case '5' -> TextColor.color(11141290);
         case '6' -> TextColor.color(16755200);
         case '7' -> TextColor.color(11184810);
         case '8' -> TextColor.color(5592405);
         case '9' -> TextColor.color(5592575);
         default -> TextColor.color(16777215);
         case 'a' -> TextColor.color(5635925);
         case 'b' -> TextColor.color(5636095);
         case 'c' -> TextColor.color(16733525);
         case 'd' -> TextColor.color(16733695);
         case 'e' -> TextColor.color(16777045);
      };
   }

   private boolean isHoldingGem(Player player) {
      return this.getCurrentGemInfo(player) != null;
   }

   private CooldownDisplayManager.GemInfo getCurrentGemInfo(Player player) {
      GemRegistryImpl registry = this.plugin.getGemRegistry();
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String oraxenId2;
      if (mainHand != null && (oraxenId2 = CustomItemManager.getIdByItem(mainHand)) != null) {
         if (GemType.isGem(oraxenId2)) {
            GemType type = GemType.fromOraxenId(oraxenId2);
            int tier = GemType.getTierFromOraxenId(oraxenId2);
            String gemId = type != null ? type.getId() : null;
            return new CooldownDisplayManager.GemInfo(type, gemId, tier, "✋");
         }

         if (registry != null && registry.isRegisteredGem(oraxenId2)) {
            String gemId = registry.gemIdFromItemId(oraxenId2);
            int tier = registry.tierFromItemId(oraxenId2);
            return new CooldownDisplayManager.GemInfo(null, gemId, tier, "✋");
         }
      }

      String oraxenId;
      ItemStack offHand;
      if ((offHand = player.getInventory().getItemInOffHand()) != null && (oraxenId = CustomItemManager.getIdByItem(offHand)) != null) {
         if (GemType.isGem(oraxenId)) {
            GemType type = GemType.fromOraxenId(oraxenId);
            int tier = GemType.getTierFromOraxenId(oraxenId);
            String gemId = type != null ? type.getId() : null;
            return new CooldownDisplayManager.GemInfo(type, gemId, tier, "\ud83d\udee1");
         }

         if (registry != null && registry.isRegisteredGem(oraxenId)) {
            String gemId = registry.gemIdFromItemId(oraxenId);
            int tier = registry.tierFromItemId(oraxenId);
            return new CooldownDisplayManager.GemInfo(null, gemId, tier, "\ud83d\udee1");
         }
      }

      return null;
   }

   private String buildCooldownDisplay(Player player) {
      StringBuilder display = new StringBuilder();
      AbilityManager abilityManager = this.plugin.getAbilityManager();
      CooldownDisplayManager.GemInfo gemInfo = this.getCurrentGemInfo(player);
      if (gemInfo == null) {
         return "";
      }

      GemType gemType = gemInfo.type;
      int tier = gemInfo.tier;
      if ("gold".equals(gemInfo.gemId)) {
         return this.buildGoldDisplay(player);
      }

      if (gemType == null) {
         return "";
      }

      String gemColor = gemType.getColor();
      List<String[]> abilities = GEM_ABILITIES.get(gemType);
      if (abilities == null) {
         return "";
      }

      if (gemType == GemType.ASTRA) {
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
         if (tier == 2) {
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            String driftStatus = abilities.size() > 2 ? this.formatCompactStatus(player, abilities.get(2)[0]) : "•";
            String voidStatus = abilities.size() > 3 ? this.formatCompactStatus(player, abilities.get(3)[0]) : "•";
            if (!"•".equals(driftStatus) && !"•".equals(voidStatus)) {
               display.append(" §5(").append(driftStatus).append("§7|").append(voidStatus).append(") ");
            } else if (!"•".equals(driftStatus)) {
               display.append(" §5(").append(driftStatus).append(") ");
            } else if (!"•".equals(voidStatus)) {
               display.append(" §5(").append(voidStatus).append(") ");
            } else {
               display.append("  §5(✦)  ");
            }

            display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
         }

         return display.toString();
      } else if (gemType == GemType.FLUX) {
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
         if (tier == 2) {
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            int remainingFlash = abilityManager.getRemainingCooldown(player, abilities.get(2)[0]);
            int remainingKinetic = abilityManager.getRemainingCooldown(player, abilities.get(3)[0]);
            if (remainingFlash > 0 && remainingKinetic > 0) {
               display.append(" §b(").append(remainingFlash).append("s§7|").append(remainingKinetic).append("s) ");
            } else if (remainingFlash > 0) {
               display.append(" §b(").append(remainingFlash).append("s) ");
            } else if (remainingKinetic > 0) {
               display.append(" §b(").append(remainingKinetic).append("s) ");
            } else {
               display.append("  §b(⚡)  ");
            }

            display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
         }

         return display.toString();
      } else if (gemType == GemType.LIFE) {
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
         if (tier == 2) {
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            String vortexStatus = this.formatCompactStatus(player, abilities.get(2)[0]);
            String lockStatus = this.formatCompactStatus(player, abilities.get(3)[0]);
            if (!"•".equals(vortexStatus) && !"•".equals(lockStatus)) {
               display.append(" §d(").append(vortexStatus).append("§7|").append(lockStatus).append(") ");
            } else if (!"•".equals(vortexStatus)) {
               display.append(" §d(").append(vortexStatus).append(") ");
            } else if (!"•".equals(lockStatus)) {
               display.append(" §d(").append(lockStatus).append(") ");
            } else {
               display.append("  §d(❤)  ");
            }

            display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
         }

         return display.toString();
      } else if (gemType == GemType.PUFF) {
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
         if (tier == 2) {
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            int remainingGroup = abilityManager.getRemainingCooldown(player, abilities.get(2)[0]);
            if (remainingGroup > 0) {
               display.append(" §f(").append(remainingGroup).append("s) ");
            } else {
               display.append("  §f(✨)  ");
            }

            display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
         }

         return display.toString();
      } else if (gemType == GemType.SPEED) {
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         int blurCharges = this.plugin.getSpeedAbilities().getBlurCharges(player.getUniqueId());
         display.append(ability1Icon).append(" ");
         if (blurCharges > 0) {
            int maxCharges = this.plugin.getConfig().getInt("abilities.blur.strikes", 5);
            display.append("§e(").append(blurCharges).append("/").append(maxCharges).append(")");
         } else {
            display.append(this.readyOrSeconds(player, abilities.get(0)[0]));
         }

         if (tier == 2 && abilities.size() > 1) {
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            String tertiaryStatus = this.formatCompactStatus(player, abilities.get(2)[0]);
            if (!"•".equals(tertiaryStatus)) {
               display.append(" §a(").append(tertiaryStatus).append(") ");
            } else {
               display.append("  §a(⚡)  ");
            }

            display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
         }

         return display.toString();
      } else if (gemType == GemType.WEALTH) {
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
         if (tier == 2 && abilities.size() > 1) {
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            String lockStatus = abilities.size() > 2 ? this.formatCompactStatus(player, abilities.get(2)[0]) : "•";
            String ampStatus = abilities.size() > 3 ? this.formatCompactStatus(player, abilities.get(3)[0]) : "•";
            if (!"•".equals(lockStatus) && !"•".equals(ampStatus)) {
               display.append(" §e(").append(lockStatus).append("§7|").append(ampStatus).append(") ");
            } else if (!"•".equals(lockStatus)) {
               display.append(" §e(").append(lockStatus).append(") ");
            } else if (!"•".equals(ampStatus)) {
               display.append(" §e(").append(ampStatus).append(") ");
            } else {
               display.append("  §e(\ud83d\udcb0)  ");
            }

            display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
         }

         return display.toString();
      } else if (gemType == GemType.STRENGTH) {
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
         if (tier == 2 && abilities.size() > 1) {
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            String tertiaryStatus = abilities.size() > 2 ? this.formatCompactStatus(player, abilities.get(2)[0]) : "•";
            if (!"•".equals(tertiaryStatus)) {
               display.append(" §6(").append(tertiaryStatus).append(") ");
            } else {
               display.append("  §6(\ud83d\udcaa)  ");
            }

            display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
         }

         return display.toString();
      } else if (gemType == GemType.ASTRA) {
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
         if (tier == 2 && abilities.size() > 1) {
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            display.append(" ").append(gemColor).append("| ").append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
            if (abilities.size() > 2) {
               display.append(" ").append(gemColor).append("| ").append(gemColor).append("Drift: ").append(this.readyOrSeconds(player, abilities.get(2)[0]));
            }

            if (abilities.size() > 3) {
               display.append(" ").append(gemColor).append("| ").append(gemColor).append("Nullify: ").append(this.readyOrSeconds(player, abilities.get(3)[0]));
            }
         }

         return display.toString();
      } else {
         String[] ability1 = abilities.get(0);
         String ability1Key = ability1[0];
         String ability1Icon = this.getAbilityIcon(gemType, 0);
         display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, ability1Key));
         if (tier == 2 && abilities.size() > 1) {
            String[] ability2 = abilities.get(1);
            String ability2Key = ability2[0];
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            display.append(" ").append(gemColor).append("| ").append(ability2Icon).append(" ").append(this.readyOrSeconds(player, ability2Key));

            for (int i = 2; i < abilities.size(); i++) {
               String[] ability = abilities.get(i);
               String abilityKeyStr = ability[0];
               display.append(" ").append(gemColor).append("| ").append(gemColor).append("✦ ").append(this.readyOrSeconds(player, abilityKeyStr));
            }
         }

         return display.toString();
      }
   }

   private String buildGoldDisplay(Player player) {
      GoldGemManager gold = this.plugin.getGoldGemManager();
      if (gold == null) {
         return "";
      }

      AbilityManager abilityManager = this.plugin.getAbilityManager();
      Map<String, GoldGemManager.Harvest> souls = gold.getHarvested(player.getUniqueId());
      StringBuilder display = new StringBuilder();
      display.append("§f").append("\ue020").append(" §6").append(souls.size()).append("§7/").append(8);
      int beam = abilityManager.getRemainingCooldown(player, "gold-beam");
      display.append(" §6(§f").append("\ue021").append(" ").append(this.readyOrSeconds(beam)).append("§6)");
      if (souls.isEmpty()) {
         return display.append(" §8| §7no soul").toString();
      }

      display.append(" §8|");
      String active = gold.getActive(player.getUniqueId());

      for (Entry<String, GoldGemManager.Harvest> entry : souls.entrySet()) {
         GemType soul = GemManager.builtInType(entry.getKey());
         List<String[]> abilities = soul != null ? GEM_ABILITIES.get(soul) : null;
         if (abilities != null && !abilities.isEmpty()) {
            boolean channelled = entry.getKey().equals(active);
            if (!channelled) {
               display.append(" §f")
                  .append(this.getAbilityIcon(soul, 0))
                  .append(" §8")
                  .append(this.shortCooldown(abilityManager.getRemainingCooldown(player, abilities.get(0)[0])));
            } else {
               int usable = entry.getValue().tier() >= 2 ? Math.min(abilities.size(), 4) : 1;
               display.append(" ").append(this.getGemColor(soul)).append("«");

               for (int i = 0; i < usable; i++) {
                  display.append("§f")
                     .append(this.getAbilityIcon(soul, i))
                     .append(" ")
                     .append(this.readyOrSeconds(abilityManager.getRemainingCooldown(player, abilities.get(i)[0])));
                  if (i < usable - 1) {
                     display.append(" ");
                  }
               }

               display.append(this.getGemColor(soul)).append("»");
            }
         }
      }

      return display.toString();
   }

   private String shortCooldown(int remaining) {
      return remaining > 0 ? "§c" + remaining + "s" : "§a•";
   }

   private String shortCooldown(Player player, String abilityKey) {
      if (this.plugin.getAbilityManager().isAbilityActive(player, abilityKey)) {
         return "§eAct";
      }

      int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, abilityKey);
      return remaining > 0 ? "§c" + remaining + "s" : "§a•";
   }

   private String formatCompactStatus(Player player, String abilityKey) {
      if (this.plugin.getAbilityManager().isAbilityActive(player, abilityKey)) {
         return "Act";
      }

      int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, abilityKey);
      return remaining > 0 ? remaining + "s" : "•";
   }

   private String readyOrSeconds(int remaining) {
      return remaining > 0 ? "§c" + remaining + "s" : "§aReady";
   }

   private String readyOrSeconds(Player player, String abilityKey) {
      if (this.plugin.getAbilityManager().isAbilityActive(player, abilityKey)) {
         return "§eActive";
      }

      int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, abilityKey);
      return remaining > 0 ? "§c" + remaining + "s" : "§aReady";
   }

   private String getCustomGemIcon(GemType gemType) {
      return switch (gemType) {
         case ASTRA -> "\ue000";
         case FIRE -> "\ue001";
         case FLUX -> "\ue002";
         case LIFE -> "\ue003";
         case PUFF -> "\ue004";
         case SPEED -> "\ue005";
         case STRENGTH -> "\ue006";
         case WEALTH -> "\ue007";
      };
   }

   private String getAbilityIcon(GemType gemType, int abilityIndex) {
      return switch (gemType) {
         case ASTRA -> abilityIndex == 0 ? "\ue010" : (abilityIndex == 1 ? "\ue011" : (abilityIndex == 2 ? "\ue010" : "\ue011"));
         case FIRE -> abilityIndex == 0 ? "\ue012" : "\ue013";
         case FLUX -> abilityIndex == 0 ? "\ue014" : "\ue015";
         case LIFE -> abilityIndex == 0 ? "\ue016" : "\ue017";
         case PUFF -> abilityIndex == 0 ? "\ue018" : "\ue019";
         case SPEED -> abilityIndex == 0 ? "\ue01a" : "\ue01b";
         case STRENGTH -> abilityIndex == 0 ? "\ue01c" : "\ue01d";
         case WEALTH -> abilityIndex == 0 ? "\ue01e" : "\ue01f";
      };
   }

   private String getGemIcon(GemType gemType) {
      switch (gemType) {
         case ASTRA:
            return "✦";
         case FIRE:
            return "\ud83d\udd25";
         case FLUX:
            return "⚡";
         case LIFE:
            return "❤";
         case PUFF:
            return "\ud83d\udca8";
         case SPEED:
            return "⚡";
         case STRENGTH:
            return "⚔";
         case WEALTH:
            return "\ud83d\udcb0";
         default:
            return "◆";
      }
   }

   private String getGemColor(GemType gemType) {
      switch (gemType) {
         case ASTRA:
            return "§5";
         case FIRE:
            return "§c";
         case FLUX:
            return "§b";
         case LIFE:
            return "§d";
         case PUFF:
            return "§f";
         case SPEED:
            return "§a";
         case STRENGTH:
            return "§6";
         case WEALTH:
            return "§e";
         default:
            return "§7";
      }
   }

   private String getEnergyBar(int energy) {
      StringBuilder bar = new StringBuilder();
      String icon;
      String color;
      if (energy >= 8) {
         color = "§d§l";
         icon = "●";
      } else if (energy >= 5) {
         color = "§b";
         icon = "●";
      } else if (energy >= 3) {
         color = "§e";
         icon = "●";
      } else if (energy >= 2) {
         color = "§6";
         icon = "●";
      } else if (energy == 1) {
         color = "§4";
         icon = "○";
      } else {
         color = "§c§l";
         icon = "○";
      }

      bar.append(color);

      for (int i = 0; i < 10; i++) {
         if (i < energy) {
            bar.append(icon);
         } else {
            bar.append("§8○");
         }
      }

      bar.append(" §7(").append(energy).append("/10)");
      return bar.toString();
   }

   private String getCooldownBar(int remaining, int total, String abilityName) {
      double percentage = 1.0 - (double)remaining / total;
      int bars = (int)(percentage * 10.0);
      StringBuilder bar = new StringBuilder();
      String color = remaining <= 3 ? "§a" : (remaining <= 10 ? "§e" : (remaining <= 30 ? "§6" : "§c"));
      bar.append(color).append(abilityName).append(" ");
      bar.append("§7[");

      for (int i = 0; i < 10; i++) {
         if (i < bars) {
            bar.append("§a▰");
         } else {
            bar.append("§8▱");
         }
      }

      bar.append("§7] ").append(color).append(remaining).append("s");
      return bar.toString();
   }

   private String buildAddonCooldownDisplay(Player player, CooldownDisplayManager.GemInfo gemInfo) {
      if (gemInfo.gemId == null) {
         return "";
      }

      GemRegistryImpl registry = this.plugin.getGemRegistry();
      if (registry == null) {
         return "";
      }

      List<CooldownEntry> entries = registry.getCooldownEntries(gemInfo.gemId);
      if (entries != null && !entries.isEmpty()) {
         GemDefinition def = registry.getGem(gemInfo.gemId);
         String gemColor = def != null ? def.getColor() : "§7";
         AbilityManager abilityManager = this.plugin.getAbilityManager();
         StringBuilder display = new StringBuilder();
         boolean first = true;

         for (CooldownEntry entry : entries) {
            if (!first) {
               display.append(" ").append(gemColor).append("| ");
            }

            first = false;
            int remaining = abilityManager.getRemainingCooldown(player, entry.getAbilityKey());
            display.append(gemColor).append(entry.getDisplayName()).append(" ");
            if (remaining > 0) {
               display.append("§c").append(remaining).append("s");
            } else {
               display.append("§aReady");
            }
         }

         return display.toString();
      } else {
         return "";
      }
   }

   public void stop() {
      if (this.displayTask != null) {
         this.displayTask.cancel();
      }
   }

   static {
      GEM_ABILITIES.put(
         GemType.FIRE,
         Arrays.asList(
            new String[]{"fire-fireball", "Fireball"},
            new String[]{"fire-campfire", "Campfire"},
            new String[]{"fire-crisp", "Crisp"},
            new String[]{"fire-meteor-shower", "Meteor"}
         )
      );
      GEM_ABILITIES.put(
         GemType.ASTRA,
         Arrays.asList(
            new String[]{"astra-daggers", "Daggers"},
            new String[]{"astra-projection", "Projection"},
            new String[]{"astra-drift", "Drift"},
            new String[]{"astra-void", "Nullify"}
         )
      );
      GEM_ABILITIES.put(
         GemType.LIFE,
         Arrays.asList(
            new String[]{"life-drainer", "Drainer"},
            new String[]{"life-circle-of-life", "Circle"},
            new String[]{"life-vitality-vortex", "Vortex"},
            new String[]{"life-heart-lock", "Lock"}
         )
      );
      GEM_ABILITIES.put(
         GemType.FLUX,
         Arrays.asList(
            new String[]{"flux-beam", "Beam"},
            new String[]{"flux-ground", "Ground"},
            new String[]{"flux-flashbang", "Flash"},
            new String[]{"flux-kinetic-burst", "Kinetic"}
         )
      );
      GEM_ABILITIES.put(
         GemType.PUFF, Arrays.asList(new String[]{"puff-dash", "Dash"}, new String[]{"puff-breezy-bash", "Bash"}, new String[]{"puff-group-bash", "Group"})
      );
      GEM_ABILITIES.put(
         GemType.SPEED, Arrays.asList(new String[]{"speed-blur", "Blur"}, new String[]{"speed-storm", "Storm"}, new String[]{"speed-terminal", "Terminal"})
      );
      GEM_ABILITIES.put(
         GemType.STRENGTH,
         Arrays.asList(
            new String[]{"strength-chad", "Chad"},
            new String[]{"strength-frailer", "Frailer"},
            new String[]{"strength-shadow-stalker", "Stalker"},
            new String[]{"strength-nullify", "Nullify"}
         )
      );
      GEM_ABILITIES.put(
         GemType.WEALTH,
         Arrays.asList(
            new String[]{"wealth-unfortunate", "Unfortunate"},
            new String[]{"wealth-rich-rush", "Rush"},
            new String[]{"wealth-item-lock", "Lock"},
            new String[]{"wealth-amplification", "Amplify"}
         )
      );
   }

   private static class GemInfo {
      final GemType type;
      final String gemId;
      final int tier;
      final String location;

      GemInfo(GemType type, String gemId, int tier, String location) {
         this.type = type;
         this.gemId = gemId;
         this.tier = tier;
         this.location = location;
      }
   }
}
