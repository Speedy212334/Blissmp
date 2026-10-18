package dev.xoperr.blissgems.goldevent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.WeatherType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class DreamSky {
   private static long goldenTime = 12600L;
   private static long memoryTime = 12900L;
   private static long stormTime = 18000L;
   public static final long DOME_DAY = 80036L;
   public static final long GLOOM_DAY = 80037L;
   public static final long STORM_DAY = 80038L;
   public static final long GLITCH_DAY = 80039L;
   private static boolean signals = true;
   private static String pocketFog = "minecraft:fog_mesa";
   private static String goldenFog = "minecraft:fog_mesa";
   private static String memoryFog = "minecraft:fog_basalt_deltas";
   private static String stormFog = "minecraft:fog_the_end";
   private static final Map<UUID, String> FOG_NOW = new HashMap<>();

   private static long at(long day, long timeOfDay) {
      return signals ? day * 24000L + timeOfDay : timeOfDay;
   }

   private DreamSky() {
   }

   private static void setFog(Player p, String fogId) {
      String now = FOG_NOW.get(p.getUniqueId());
      if (fogId == null || !fogId.equals(now)) {
         if (now != null) {
            BedrockBridge.clearFog(p, null);
         }

         if (fogId != null && !fogId.isBlank()) {
            FOG_NOW.put(p.getUniqueId(), fogId);
            BedrockBridge.fog(p, fogId);
         } else {
            FOG_NOW.remove(p.getUniqueId());
         }
      }
   }

   public static void load(Plugin plugin) {
      ConfigurationSection c = plugin.getConfig().getConfigurationSection("golden-dream.sky");
      if (c != null) {
         goldenTime = c.getLong("golden-hour-time", goldenTime);
         signals = c.getBoolean("shader-signals", true);
         memoryTime = c.getLong("memory-time", memoryTime);
         stormTime = c.getLong("storm-time", stormTime);
         pocketFog = c.getString("bedrock-fog.pocket", pocketFog);
         goldenFog = c.getString("bedrock-fog.golden", goldenFog);
         memoryFog = c.getString("bedrock-fog.memory", memoryFog);
         stormFog = c.getString("bedrock-fog.storm", stormFog);
      }
   }

   public static void golden(Player p) {
      p.setPlayerTime(at(80036L, goldenTime), false);
      setFog(p, goldenFog);
   }

   public static void glitch(Player p) {
      if (signals) {
         p.setPlayerTime(1920942000L, false);
      }
   }

   public static void domeFade(Player p, double f) {
      long tod = Math.round(Math.max(0.0, Math.min(1.0, f)) * 1000.0);
      p.setPlayerTime(at(80036L, tod), false);
      setFog(p, goldenFog);
   }

   public static void pocket(Player p) {
      p.resetPlayerTime();
      p.setPlayerWeather(WeatherType.CLEAR);
      setFog(p, pocketFog);
   }

   public static void memory(Player p, boolean storm) {
      if (storm) {
         p.setPlayerTime(at(80038L, stormTime), false);
         p.setPlayerWeather(WeatherType.DOWNFALL);
         setFog(p, stormFog);
      } else {
         p.setPlayerTime(at(80037L, memoryTime), false);
         p.setPlayerWeather(WeatherType.CLEAR);
         setFog(p, memoryFog);
      }
   }

   public static void clear(Player p) {
      p.resetPlayerWeather();
      setFog(p, null);
   }

   public static void reset(Player p) {
      p.resetPlayerTime();
      clear(p);
   }

   public static void restore(Player p, long offset, boolean relative) {
      p.setPlayerTime(offset, relative);
      clear(p);
   }

   public static void closeEyes(Player p, int ticks) {
      p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0, false, false, false));
      BedrockBridge.fade(p, 0, 0, 0, 0.5F, Math.max(0.0F, ticks / 20.0F - 1.0F), 0.5F);
   }

   public static void forget(UUID id) {
      FOG_NOW.remove(id);
   }

   public static void openEyes(Player p) {
      p.removePotionEffect(PotionEffectType.BLINDNESS);
   }
}
