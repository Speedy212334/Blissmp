package dev.xoperr.blissgems.goldevent;

import java.awt.Color;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BedrockBridge {
   private static Boolean present;

   private BedrockBridge() {
   }

   public static boolean available() {
      if (present == null) {
         boolean ok = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;

         try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
         } catch (Throwable t) {
            ok = false;
         }

         present = ok;
      }

      return present;
   }

   private static Object connection(UUID id) {
      if (!available()) {
         return null;
      }

      try {
         Object api = Class.forName("org.geysermc.geyser.api.GeyserApi").getMethod("api").invoke(null);
         return MemoryNpcs.call(api, "connectionByUuid", id);
      } catch (Throwable t) {
         return null;
      }
   }

   public static boolean isBedrock(Player p) {
      return p != null && connection(p.getUniqueId()) != null;
   }

   private static Object camera(Player p) throws Exception {
      Object c = p == null ? null : connection(p.getUniqueId());
      return c == null ? null : MemoryNpcs.call(c, "camera");
   }

   public static void fog(Player p, String fogId) {
      if (fogId != null && !fogId.isBlank()) {
         try {
            Object cam = camera(p);
            if (cam != null) {
               MemoryNpcs.call(cam, "sendFog", new String[]{fogId});
            }
         } catch (Throwable var3) {
         }
      }
   }

   public static void clearFog(Player p, String fogId) {
      try {
         Object cam = camera(p);
         if (cam != null) {
            MemoryNpcs.call(cam, "removeFog", fogId == null ? new String[0] : new String[]{fogId});
         }
      } catch (Throwable var3) {
      }
   }

   public static boolean fade(Player p, int r, int g, int b, float in, float hold, float out) {
      try {
         Object cam = camera(p);
         if (cam == null) {
            return false;
         }

         Class<?> fadeClass = Class.forName("org.geysermc.geyser.api.bedrock.camera.CameraFade");
         Object builder = fadeClass.getMethod("builder").invoke(null);
         builder = MemoryNpcs.call(builder, "color", new Color(r, g, b));
         builder = MemoryNpcs.call(builder, "fadeInSeconds", in);
         builder = MemoryNpcs.call(builder, "fadeHoldSeconds", hold);
         builder = MemoryNpcs.call(builder, "fadeOutSeconds", out);
         MemoryNpcs.call(cam, "sendCameraFade", MemoryNpcs.call(builder, "build"));
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }
}
