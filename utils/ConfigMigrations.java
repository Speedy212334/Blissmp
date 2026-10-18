package dev.xoperr.blissgems.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.configuration.ConfigurationSection;

public final class ConfigMigrations {
   public static final String KEY = "config-migrations";
   static final Map<String, List<ConfigMigrations.Change>> ALL = new LinkedHashMap<>();

   private ConfigMigrations() {
   }

   public static List<String> apply(ConfigurationSection cfg) {
      List<String> done = new ArrayList<>(cfg.getStringList("config-migrations"));
      List<String> changed = new ArrayList<>();

      for (Entry<String, List<ConfigMigrations.Change>> m : ALL.entrySet()) {
         if (!done.contains(m.getKey())) {
            for (ConfigMigrations.Change c : m.getValue()) {
               if (cfg.isSet(c.path()) && cfg.get(c.path()) instanceof Number n && !(Math.abs(n.doubleValue() - c.oldDefault()) > 1.0E-9)) {
                  boolean whole = n instanceof Integer || n instanceof Long;
                  cfg.set(c.path(), whole ? (int)Math.round(c.newDefault()) : c.newDefault());
                  changed.add(c.path());
               }
            }

            done.add(m.getKey());
         }
      }

      cfg.set("config-migrations", done);
      return changed;
   }

   static {
      ALL.put(
         "2026-10-buff-fire-flux-speed",
         List.of(
            new ConfigMigrations.Change("abilities.cooldowns.fire-fireball", 45.0, 35.0),
            new ConfigMigrations.Change("abilities.damage.fire-campfire", 2.0, 3.0),
            new ConfigMigrations.Change("abilities.fire-campfire.radius", 4.0, 5.0),
            new ConfigMigrations.Change("abilities.fire-campfire.heal-amount", 0.4, 1.0),
            new ConfigMigrations.Change("abilities.cooldowns.fire-campfire", 120.0, 90.0),
            new ConfigMigrations.Change("abilities.cooldowns.fire-crisp", 90.0, 60.0),
            new ConfigMigrations.Change("abilities.durations.fire-crisp", 15.0, 20.0),
            new ConfigMigrations.Change("abilities.damage.fire-meteor-shower", 8.0, 10.0),
            new ConfigMigrations.Change("abilities.cooldowns.fire-meteor-shower", 120.0, 90.0),
            new ConfigMigrations.Change("abilities.durations.fire-meteor-shower", 8.0, 10.0),
            new ConfigMigrations.Change("abilities.cooldowns.flux-beam", 240.0, 150.0),
            new ConfigMigrations.Change("abilities.damage.flux-beam-base", 10.0, 14.0),
            new ConfigMigrations.Change("abilities.durations.flux-ground-freeze", 3.0, 4.0),
            new ConfigMigrations.Change("abilities.durations.flux-flashbang", 5.0, 6.0),
            new ConfigMigrations.Change("abilities.flux-kinetic-burst.knockback", 2.0, 2.6),
            new ConfigMigrations.Change("abilities.flux-kinetic-burst.radius", 5.0, 6.0),
            new ConfigMigrations.Change("passives.flux.shocking-chance", 0.15, 0.25),
            new ConfigMigrations.Change("passives.flux.tier2.shocking-arrow-damage", 3.0, 4.0),
            new ConfigMigrations.Change("abilities.cooldowns.speed-storm", 135.0, 90.0),
            new ConfigMigrations.Change("abilities.speed-storm.damage", 2.0, 3.0),
            new ConfigMigrations.Change("abilities.durations.speed-storm", 10.0, 12.0),
            new ConfigMigrations.Change("abilities.damage.blur", 2.5, 3.5),
            new ConfigMigrations.Change("abilities.durations.terminal-velocity", 10.0, 15.0),
            new ConfigMigrations.Change("abilities.cooldowns.speed-terminal", 60.0, 50.0),
            new ConfigMigrations.Change("abilities.cooldowns.speed-gale-clouds", 60.0, 45.0)
         )
      );
   }

   record Change(String path, double oldDefault, double newDefault) {
   }
}
