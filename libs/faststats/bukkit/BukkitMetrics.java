package dev.xoperr.blissgems.libs.faststats.bukkit;

import dev.xoperr.blissgems.libs.faststats.core.Metrics;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;

public sealed interface BukkitMetrics extends Metrics permits BukkitMetricsImpl {
   @Contract(pure = true)
   static BukkitMetrics.Factory factory() {
      return new BukkitMetricsImpl.Factory();
   }

   @Override
   void ready() throws IllegalPluginAccessException;

   interface Factory extends Metrics.Factory<Plugin, BukkitMetrics.Factory> {
      BukkitMetrics create(Plugin var1) throws IllegalStateException;
   }
}
