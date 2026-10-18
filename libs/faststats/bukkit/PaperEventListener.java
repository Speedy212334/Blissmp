package dev.xoperr.blissgems.libs.faststats.bukkit;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerPluginException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

record PaperEventListener(BukkitMetricsImpl metrics) implements Listener {
   @EventHandler(priority = EventPriority.MONITOR)
   public void onServerException(ServerExceptionEvent event) {
      if (event.getException() instanceof ServerPluginException exception) {
         if (exception.getResponsiblePlugin().equals(this.metrics.plugin())) {
            this.metrics.getErrorTracker().ifPresent(tracker -> tracker.trackError(exception));
         }
      }
   }
}
