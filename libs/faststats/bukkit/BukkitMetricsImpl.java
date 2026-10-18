package dev.xoperr.blissgems.libs.faststats.bukkit;

import com.google.gson.JsonObject;
import dev.xoperr.blissgems.libs.faststats.core.SimpleMetrics;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

final class BukkitMetricsImpl extends SimpleMetrics implements BukkitMetrics {
   private final Plugin plugin;
   private final String pluginVersion;
   private final String minecraftVersion;
   private final String serverType;

   @Async.Schedule
   @Contract(mutates = "io")
   private BukkitMetricsImpl(BukkitMetricsImpl.Factory factory, Plugin plugin, Path config) throws IllegalStateException {
      super(factory, config);
      this.plugin = plugin;
      Server server = plugin.getServer();
      this.pluginVersion = this.<String>tryOrEmpty(() -> plugin.getPluginMeta().getVersion()).orElseGet(() -> plugin.getDescription().getVersion());
      this.minecraftVersion = this.<String>tryOrEmpty(() -> server.getMinecraftVersion())
         .or(() -> this.tryOrEmpty(() -> server.getBukkitVersion().split("-", 2)[0]))
         .orElseGet(() -> server.getVersion().split("\\(MC: |\\)", 3)[1]);
      this.serverType = server.getName();
      this.startSubmitting();
   }

   Plugin plugin() {
      return this.plugin;
   }

   private boolean checkOnlineMode() {
      Server server = this.plugin.getServer();
      return this.<Boolean>tryOrEmpty(() -> server.getServerConfig().isProxyOnlineMode())
         .or(() -> this.tryOrEmpty(this::isProxyOnlineMode))
         .orElseGet(server::getOnlineMode);
   }

   private boolean isProxyOnlineMode() {
      Server server = this.plugin.getServer();
      ConfigurationSection proxies = server.spigot().getPaperConfig().getConfigurationSection("proxies");
      if (proxies == null) {
         return false;
      }

      if (proxies.getBoolean("velocity.enabled") && proxies.getBoolean("velocity.online-mode")) {
         return true;
      }

      ConfigurationSection settings = server.spigot().getSpigotConfig().getConfigurationSection("settings");
      return settings == null ? false : settings.getBoolean("bungeecord") && proxies.getBoolean("bungee-cord.online-mode");
   }

   @Override
   protected void appendDefaultData(JsonObject metrics) {
      metrics.addProperty("minecraft_version", this.minecraftVersion);
      metrics.addProperty("online_mode", this.checkOnlineMode());
      metrics.addProperty("player_count", this.getPlayerCount());
      metrics.addProperty("plugin_version", this.pluginVersion);
      metrics.addProperty("server_type", this.serverType);
   }

   private int getPlayerCount() {
      try {
         return this.plugin.getServer().getOnlinePlayers().size();
      } catch (Throwable t) {
         this.error("Failed to get player count", t);
         return 0;
      }
   }

   @Override
   protected void printError(String message, @Nullable Throwable throwable) {
      this.plugin.getLogger().log(Level.SEVERE, message, throwable);
   }

   @Override
   protected void printInfo(String message) {
      this.plugin.getLogger().info(message);
   }

   @Override
   protected void printWarning(String message) {
      this.plugin.getLogger().warning(message);
   }

   @Override
   public void ready() {
      if (this.getErrorTracker().isPresent()) {
         try {
            Class.forName("com.destroystokyo.paper.event.server.ServerExceptionEvent");
            this.plugin.getServer().getPluginManager().registerEvents(new PaperEventListener(this), this.plugin);
         } catch (ClassNotFoundException var2) {
         }
      }
   }

   private <T> Optional<T> tryOrEmpty(Supplier<T> supplier) {
      try {
         return Optional.of(supplier.get());
      } catch (NoSuchMethodError | Exception e) {
         return Optional.empty();
      }
   }

   static final class Factory extends SimpleMetrics.Factory<Plugin, BukkitMetrics.Factory> implements BukkitMetrics.Factory {
      @Override
      public BukkitMetrics create(Plugin plugin) throws IllegalStateException {
         Path dataFolder = getPluginsFolder(plugin).resolve("faststats");
         Path config = dataFolder.resolve("config.properties");
         return new BukkitMetricsImpl(this, plugin, config);
      }

      private static Path getPluginsFolder(Plugin plugin) {
         try {
            return plugin.getServer().getPluginsFolder().toPath();
         } catch (NoSuchMethodError e) {
            return plugin.getDataFolder().getParentFile().toPath();
         }
      }
   }
}
