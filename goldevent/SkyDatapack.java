package dev.xoperr.blissgems.goldevent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public final class SkyDatapack {
   private static final String NAME = "bliss_skies";
   private static final String BIOME = "data/minecraft/worldgen/biome/the_void.json";

   private SkyDatapack() {
   }

   public static void install(Plugin plugin) {
      if (plugin.getConfig().getBoolean("golden-dream.sky.datapack", true)) {
         try {
            ClassLoader server = Bukkit.getServer().getClass().getClassLoader();
            JsonObject biome = read(server, "data/minecraft/worldgen/biome/the_void.json");
            JsonObject version = read(server, "version.json");
            if (biome == null || version == null) {
               plugin.getLogger().warning("Golden Dream sky: couldn't read this server's vanilla data; the pocket keeps a normal sky.");
               return;
            }

            JsonObject effects = biome.has("effects") && biome.get("effects").isJsonObject() ? biome.getAsJsonObject("effects") : null;
            if (effects == null || !effects.has("sky_color")) {
               plugin.getLogger().warning("Golden Dream sky: this Minecraft version stores biome colours differently; datapack skipped.");
               return;
            }

            setIfPresent(effects, "sky_color", color(plugin, "pocket-sky-color", 15906891));
            setIfPresent(effects, "fog_color", color(plugin, "pocket-fog-color", 16769955));
            setIfPresent(effects, "water_color", color(plugin, "pocket-water-color", 15249482));
            setIfPresent(effects, "water_fog_color", color(plugin, "pocket-water-fog-color", 9067024));
            JsonObject pack = new JsonObject();
            JsonObject meta = new JsonObject();
            JsonObject pv = version.getAsJsonObject("pack_version");
            int format = pv.has("data") ? pv.get("data").getAsInt() : pv.get("data_major").getAsInt();
            meta.addProperty("pack_format", format);
            if (!pv.has("data")) {
               meta.addProperty("min_format", format);
               meta.addProperty("max_format", format);
            }

            meta.addProperty("description", "BlissGems: golden sky for the Golden Dream pocket (generated)");
            pack.add("pack", meta);
            World main = (World)Bukkit.getWorlds().get(0);
            File root = new File(new File(main.getWorldFolder(), "datapacks"), "bliss_skies");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            boolean changed = write(new File(root, "pack.mcmeta"), gson.toJson(pack))
               | write(new File(root, "data/minecraft/worldgen/biome/the_void.json"), gson.toJson(biome));
            if (changed) {
               plugin.getLogger()
                  .warning(
                     "Golden Dream sky: datapack 'bliss_skies' installed/updated in "
                        + main.getName()
                        + "/datapacks - restart the server once to apply it. Existing pocket chunks need /goldendream reset confirm."
                  );
            }
         } catch (Throwable t) {
            plugin.getLogger().warning("Golden Dream sky: datapack not installed (" + t.getMessage() + ")");
         }
      }
   }

   private static JsonObject read(ClassLoader cl, String path) throws Exception {
      try (InputStream in = cl.getResourceAsStream(path)) {
         return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
      }
   }

   private static void setIfPresent(JsonObject o, String key, int value) {
      if (o.has(key)) {
         o.addProperty(key, value);
      }
   }

   private static int color(Plugin plugin, String key, int fallback) {
      String s = plugin.getConfig().getString("golden-dream.sky." + key);
      if (s == null) {
         return fallback;
      }

      try {
         return Integer.parseInt(s.replace("#", ""), 16);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   private static boolean write(File f, String content) throws Exception {
      if (f.isFile() && new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).equals(content)) {
         return false;
      }

      f.getParentFile().mkdirs();
      Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
      return true;
   }
}
