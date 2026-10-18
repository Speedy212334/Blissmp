package dev.xoperr.blissgems.pedestal;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PedestalLayout {
   private final List<PedestalLayout.Block> open;
   private final List<PedestalLayout.Block> closed;

   private PedestalLayout(List<PedestalLayout.Block> open, List<PedestalLayout.Block> closed) {
      this.open = open;
      this.closed = closed;
   }

   public List<PedestalLayout.Block> open() {
      return this.open;
   }

   public List<PedestalLayout.Block> closed() {
      return this.closed;
   }

   public boolean hasOpen() {
      return !this.open.isEmpty();
   }

   public boolean hasClosed() {
      return !this.closed.isEmpty();
   }

   public static PedestalLayout load(BlissGems plugin) {
      File file = new File(plugin.getDataFolder(), "pedestal_layout.yml");
      if (!file.exists()) {
         try {
            plugin.saveResource("pedestal_layout.yml", false);
         } catch (IllegalArgumentException e) {
            return new PedestalLayout(Collections.emptyList(), Collections.emptyList());
         }
      }

      YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
      List<PedestalLayout.Block> open = parse(plugin, yml.getConfigurationSection("open"));
      List<PedestalLayout.Block> closed = parse(plugin, yml.getConfigurationSection("closed"));
      plugin.getLogger().info("pedestal_layout.yml loaded (" + open.size() + " open, " + closed.size() + " closed blocks).");
      return new PedestalLayout(open, closed);
   }

   private static List<PedestalLayout.Block> parse(BlissGems plugin, ConfigurationSection section) {
      List<PedestalLayout.Block> out = new ArrayList<>();
      if (section == null) {
         return out;
      }

      for (String key : section.getKeys(false)) {
         String[] parts = key.split(",");
         if (parts.length != 3) {
            plugin.getLogger().warning("pedestal_layout: bad offset key '" + key + "'");
         } else {
            String value = section.getString(key, "");
            if (value.isEmpty()) {
               plugin.getLogger().warning("pedestal_layout: empty value at '" + key + "'");
            } else {
               try {
                  int dx = Integer.parseInt(parts[0].trim());
                  int dy = Integer.parseInt(parts[1].trim());
                  int dz = Integer.parseInt(parts[2].trim());
                  out.add(new PedestalLayout.Block(dx, dy, dz, Bukkit.createBlockData(value)));
               } catch (NumberFormatException e) {
                  plugin.getLogger().warning("pedestal_layout: non-integer offset '" + key + "'");
               } catch (IllegalArgumentException e) {
                  plugin.getLogger().warning("pedestal_layout: bad block '" + value + "' at '" + key + "'");
               }
            }
         }
      }

      return out;
   }

   public record Block(int dx, int dy, int dz, BlockData data) {
   }
}
