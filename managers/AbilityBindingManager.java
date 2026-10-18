package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class AbilityBindingManager {
   private final BlissGems plugin;
   private final Map<UUID, EnumMap<AbilityBinding, AbilitySlot>> cache = new ConcurrentHashMap<>();
   private static final EnumMap<AbilityBinding, AbilitySlot> DEFAULTS = new EnumMap<>(AbilityBinding.class);
   private static final EnumMap<AbilityBinding, AbilitySlot> BEDROCK_DEFAULTS = new EnumMap<>(AbilityBinding.class);
   private static final String BEDROCK_NAME_PREFIX = ".";
   private static final int BINDINGS_VERSION = 2;

   public static boolean isBedrock(Player player) {
      return player != null && player.getName().startsWith(".");
   }

   private EnumMap<AbilityBinding, AbilitySlot> defaultsFor(Player player) {
      boolean bedrock = isBedrock(player);
      EnumMap<AbilityBinding, AbilitySlot> configured = this.readConfigDefaults(bedrock ? "ability-bindings.bedrock-defaults" : "ability-bindings.defaults");
      return configured != null ? configured : new EnumMap<>(bedrock ? BEDROCK_DEFAULTS : DEFAULTS);
   }

   private EnumMap<AbilityBinding, AbilitySlot> readConfigDefaults(String path) {
      if (!this.plugin.getConfig().isConfigurationSection(path)) {
         return null;
      }

      EnumMap<AbilityBinding, AbilitySlot> map = new EnumMap<>(AbilityBinding.class);

      for (String key : this.plugin.getConfig().getConfigurationSection(path).getKeys(false)) {
         AbilityBinding input = AbilityBinding.fromId(key);
         String slotId = this.plugin.getConfig().getString(path + "." + key);
         AbilitySlot slot = AbilitySlot.fromId(slotId);
         if (input == null) {
            this.plugin.getLogger().warning("[AbilityBindings] " + path + ": unknown input '" + key + "' - ignored.");
         } else if (slot == null) {
            if (slotId == null || !slotId.equalsIgnoreCase("none") && !slotId.isEmpty()) {
               this.plugin.getLogger().warning("[AbilityBindings] " + path + "." + key + ": unknown slot '" + slotId + "' - ignored.");
            }
         } else {
            map.entrySet().removeIf(e -> e.getValue() == slot);
            map.put(input, slot);
         }
      }

      if (map.get(AbilityBinding.SWAP_HAND) == AbilitySlot.TERTIARY && map.get(AbilityBinding.LEFT_CLICK) == AbilitySlot.QUINARY) {
         return null;
      } else {
         return map.isEmpty() ? null : map;
      }
   }

   public AbilityBindingManager(BlissGems plugin) {
      this.plugin = plugin;
   }

   public AbilitySlot getSlot(Player player, AbilityBinding input) {
      return this.getOrLoad(player).get(input);
   }

   public EnumMap<AbilityBinding, AbilitySlot> getAll(Player player) {
      return new EnumMap<>(this.getOrLoad(player));
   }

   public void setBinding(Player player, AbilityBinding input, AbilitySlot slot) {
      EnumMap<AbilityBinding, AbilitySlot> map = this.getOrLoad(player);
      map.entrySet().removeIf(e -> e.getValue() == slot && e.getKey() != input);
      map.put(input, slot);
      this.save(player.getUniqueId(), map);
   }

   public void unbind(Player player, AbilityBinding input) {
      EnumMap<AbilityBinding, AbilitySlot> map = this.getOrLoad(player);
      map.remove(input);
      this.save(player.getUniqueId(), map);
   }

   public EnumMap<AbilityBinding, AbilitySlot> getDefaults(Player player) {
      return this.defaultsFor(player);
   }

   public void resetToDefaults(Player player) {
      EnumMap<AbilityBinding, AbilitySlot> map = this.defaultsFor(player);
      this.cache.put(player.getUniqueId(), map);
      this.save(player.getUniqueId(), map);
   }

   public void clearCache(UUID id) {
      this.cache.remove(id);
   }

   private EnumMap<AbilityBinding, AbilitySlot> getOrLoad(Player player) {
      UUID id = player.getUniqueId();
      EnumMap<AbilityBinding, AbilitySlot> map = this.cache.get(id);
      if (map != null) {
         return map;
      }

      map = this.load(player);
      this.cache.put(id, map);
      return map;
   }

   private EnumMap<AbilityBinding, AbilitySlot> load(Player player) {
      File f = this.playerFile(player.getUniqueId());
      if (!f.exists()) {
         return this.defaultsFor(player);
      }

      YamlConfiguration data = YamlConfiguration.loadConfiguration(f);
      if (!data.contains("ability-bindings")) {
         return this.defaultsFor(player);
      }

      if (data.getInt("ability-bindings-version", 1) < 2) {
         return this.migrate(player, data);
      }

      EnumMap<AbilityBinding, AbilitySlot> map = new EnumMap<>(AbilityBinding.class);
      Map<String, Object> raw = data.getConfigurationSection("ability-bindings").getValues(false);

      for (Entry<String, Object> e : raw.entrySet()) {
         AbilityBinding b = AbilityBinding.fromId(e.getKey());
         AbilitySlot s = AbilitySlot.fromId(String.valueOf(e.getValue()));
         if (b != null && s != null) {
            map.put(b, s);
         }
      }

      return map;
   }

   private EnumMap<AbilityBinding, AbilitySlot> migrate(Player player, YamlConfiguration data) {
      EnumMap<AbilityBinding, AbilitySlot> old = new EnumMap<>(AbilityBinding.class);

      for (Entry<String, Object> e : data.getConfigurationSection("ability-bindings").getValues(false).entrySet()) {
         AbilityBinding b = AbilityBinding.fromId(e.getKey());
         AbilitySlot s = AbilitySlot.fromId(String.valueOf(e.getValue()));
         if (b != null && s != null) {
            old.put(b, s);
         }
      }

      EnumMap<AbilityBinding, AbilitySlot> map = new EnumMap<>(AbilityBinding.class);
      if (old.containsKey(AbilityBinding.RIGHT_CLICK)) {
         map.put(AbilityBinding.RIGHT_CLICK, old.get(AbilityBinding.RIGHT_CLICK));
      }

      if (old.containsKey(AbilityBinding.SHIFT_RIGHT_CLICK)) {
         map.put(AbilityBinding.SHIFT_RIGHT_CLICK, old.get(AbilityBinding.SHIFT_RIGHT_CLICK));
      }

      if (old.containsKey(AbilityBinding.SWAP_HAND)) {
         map.put(AbilityBinding.LEFT_CLICK, old.get(AbilityBinding.SWAP_HAND));
      }

      if (old.containsKey(AbilityBinding.SHIFT_SWAP_HAND)) {
         map.put(AbilityBinding.SHIFT_LEFT_CLICK, old.get(AbilityBinding.SHIFT_SWAP_HAND));
      }

      if (map.isEmpty()) {
         map = this.defaultsFor(player);
      }

      this.save(player.getUniqueId(), map);
      return map;
   }

   private void save(UUID id, EnumMap<AbilityBinding, AbilitySlot> map) {
      File f = this.playerFile(id);
      YamlConfiguration data = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
      data.set("ability-bindings", null);
      HashMap<String, String> out = new HashMap<>();

      for (Entry<AbilityBinding, AbilitySlot> e : map.entrySet()) {
         out.put(e.getKey().getId(), e.getValue().getId());
      }

      data.createSection("ability-bindings", out);
      data.set("ability-bindings-version", 2);

      try {
         data.save(f);
      } catch (IOException e) {
         this.plugin.getLogger().warning("[AbilityBindings] Failed to save for " + id + ": " + e.getMessage());
      }
   }

   private File playerFile(UUID id) {
      File folder = new File(this.plugin.getDataFolder(), "playerdata");
      if (!folder.exists()) {
         folder.mkdirs();
      }

      return new File(folder, id + ".yml");
   }

   static {
      DEFAULTS.put(AbilityBinding.RIGHT_CLICK, AbilitySlot.PRIMARY);
      DEFAULTS.put(AbilityBinding.SHIFT_RIGHT_CLICK, AbilitySlot.SECONDARY);
      DEFAULTS.put(AbilityBinding.LEFT_CLICK, AbilitySlot.TERTIARY);
      DEFAULTS.put(AbilityBinding.SHIFT_LEFT_CLICK, AbilitySlot.QUATERNARY);
      BEDROCK_DEFAULTS.put(AbilityBinding.RIGHT_CLICK, AbilitySlot.PRIMARY);
      BEDROCK_DEFAULTS.put(AbilityBinding.SHIFT_RIGHT_CLICK, AbilitySlot.SECONDARY);
      BEDROCK_DEFAULTS.put(AbilityBinding.LEFT_CLICK, AbilitySlot.TERTIARY);
      BEDROCK_DEFAULTS.put(AbilityBinding.SHIFT_LEFT_CLICK, AbilitySlot.QUATERNARY);
   }
}
