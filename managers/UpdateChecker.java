package dev.xoperr.blissgems.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;

public class UpdateChecker {
   private static final String VERSIONS_URL = "https://api.modrinth.com/v2/project/%s/version";
   public static final String DEFAULT_PROJECT = "bliss-smp-plugin";
   private final BlissGems plugin;

   public UpdateChecker(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void checkAsync() {
      if (this.plugin.getConfig().getBoolean("auto-update.enabled", true)) {
         String slug = this.plugin.getConfig().getString("auto-update.modrinth-project", "bliss-smp-plugin").trim();
         if (slug.isEmpty()) {
            slug = "bliss-smp-plugin";
         }

         if (slug.endsWith("/")) {
            slug = slug.substring(0, slug.length() - 1);
         }

         slug = slug.substring(slug.lastIndexOf(47) + 1);
         String project = slug;
         boolean notifyOnly = this.plugin.getConfig().getBoolean("auto-update.notify-only", false);
         this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.run(project, notifyOnly));
      }
   }

   private void run(String slug, boolean notifyOnly) {
      try {
         JsonArray versions = this.fetchJson(String.format("https://api.modrinth.com/v2/project/%s/version", slug)).getAsJsonArray();
         JsonObject latest = pickLatest(versions);
         if (latest == null) {
            this.plugin.getLogger().warning("[AutoUpdate] No Paper/Spigot/Bukkit version found on Modrinth for '" + slug + "'.");
            return;
         }

         String remote = latest.get("version_number").getAsString();
         String current;
         if (compareVersions(remote, current = this.plugin.getDescription().getVersion()) <= 0) {
            this.plugin.getLogger().info("[AutoUpdate] Up to date (" + current + ").");
            return;
         }

         this.plugin.getLogger().info("[AutoUpdate] New version available: " + remote + " (current " + current + ").");
         if (notifyOnly) {
            return;
         }

         JsonObject file = this.primaryFile(latest);
         if (file == null) {
            this.plugin.getLogger().warning("[AutoUpdate] Latest version has no downloadable file.");
            return;
         }

         String sha512 = file.has("hashes") && file.getAsJsonObject("hashes").has("sha512") ? file.getAsJsonObject("hashes").get("sha512").getAsString() : null;
         this.downloadToUpdateFolder(file.get("url").getAsString(), remote, sha512);
      } catch (Exception ex) {
         this.plugin.getLogger().warning("[AutoUpdate] Update check failed: " + ex.getMessage());
      }
   }

   static JsonObject pickLatest(JsonArray versions) {
      JsonObject best = null;

      for (JsonElement el : versions) {
         JsonObject v = el.getAsJsonObject();
         if (targetsBukkit(v)
            && (!v.has("version_type") || "release".equals(v.get("version_type").getAsString()))
            && (best == null || compareVersions(v.get("version_number").getAsString(), best.get("version_number").getAsString()) > 0)) {
            best = v;
         }
      }

      return best;
   }

   private static boolean targetsBukkit(JsonObject version) {
      JsonArray loaders = version.getAsJsonArray("loaders");
      if (loaders == null) {
         return false;
      }

      for (JsonElement l : loaders) {
         switch (l.getAsString().toLowerCase()) {
            case "paper":
            case "spigot":
            case "bukkit":
            case "purpur":
            case "folia":
               return true;
         }
      }

      return false;
   }

   private JsonObject primaryFile(JsonObject version) {
      JsonArray files = version.getAsJsonArray("files");
      if (files != null && !files.isEmpty()) {
         for (JsonElement fe : files) {
            JsonObject f = fe.getAsJsonObject();
            if (f.has("primary") && f.get("primary").getAsBoolean()) {
               return f;
            }
         }

         return files.get(0).getAsJsonObject();
      } else {
         return null;
      }
   }

   private void downloadToUpdateFolder(String url, String version, String sha512) throws Exception {
      File runningJar = this.plugin.getPluginFile();
      File updateDir = new File(this.plugin.getDataFolder().getParentFile(), this.plugin.getServer().getUpdateFolder());
      if (!updateDir.exists() && !updateDir.mkdirs()) {
         this.plugin.getLogger().warning("[AutoUpdate] Could not create update folder " + updateDir);
      } else {
         File dest = new File(updateDir, runningJar.getName());
         File part = new File(updateDir, runningJar.getName() + ".part");
         MessageDigest digest = MessageDigest.getInstance("SHA-512");
         HttpURLConnection conn = this.open(url);

         try (
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(part);
         ) {
            byte[] buffer = new byte[8192];

            int read;
            while ((read = in.read(buffer)) != -1) {
               out.write(buffer, 0, read);
               digest.update(buffer, 0, read);
            }
         }

         if (sha512 != null && !HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(sha512)) {
            part.delete();
            this.plugin.getLogger().warning("[AutoUpdate] Download of " + version + " did not match Modrinth's checksum; discarded.");
         } else {
            Files.move(part.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.plugin
               .getLogger()
               .info(
                  "[AutoUpdate] Downloaded "
                     + version
                     + " to "
                     + this.plugin.getServer().getUpdateFolder()
                     + "/"
                     + dest.getName()
                     + " — it will be applied on the next server restart."
               );
         }
      }
   }

   private JsonElement fetchJson(String url) throws Exception {
      HttpURLConnection conn = this.open(url);
      if (conn.getResponseCode() != 200) {
         throw new IllegalStateException("HTTP " + conn.getResponseCode() + " from " + url);
      }

      try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader);
      }
   }

   private HttpURLConnection open(String url) throws Exception {
      HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
      conn.setRequestProperty("User-Agent", "BlissGems/" + this.plugin.getDescription().getVersion() + " (auto-updater)");
      conn.setConnectTimeout(8000);
      conn.setReadTimeout(15000);
      conn.setInstanceFollowRedirects(true);
      return conn;
   }

   static int compareVersions(String a, String b) {
      String[] pa = a.replaceAll("[^0-9.]", "").split("\\.");
      String[] pb = b.replaceAll("[^0-9.]", "").split("\\.");
      int len = Math.max(pa.length, pb.length);

      for (int i = 0; i < len; i++) {
         int x = i < pa.length && !pa[i].isEmpty() ? Integer.parseInt(pa[i]) : 0;
         int y = i < pb.length && !pb[i].isEmpty() ? Integer.parseInt(pb[i]) : 0;
         if (x != y) {
            return Integer.compare(x, y);
         }
      }

      return 0;
   }
}
