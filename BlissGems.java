package dev.xoperr.blissgems;

import dev.xoperr.blissgems.abilities.AstraAbilities;
import dev.xoperr.blissgems.abilities.AstraUnbounded;
import dev.xoperr.blissgems.abilities.FireAbilities;
import dev.xoperr.blissgems.abilities.FluxAbilities;
import dev.xoperr.blissgems.abilities.GoldAbilities;
import dev.xoperr.blissgems.abilities.LifeAbilities;
import dev.xoperr.blissgems.abilities.PuffAbilities;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import dev.xoperr.blissgems.abilities.StrengthAbilities;
import dev.xoperr.blissgems.abilities.WealthAbilities;
import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.commands.BlissCommand;
import dev.xoperr.blissgems.commands.FixGemsCommand;
import dev.xoperr.blissgems.commands.FixHeartsCommand;
import dev.xoperr.blissgems.commands.FixedHeartsCommand;
import dev.xoperr.blissgems.commands.NewsCommand;
import dev.xoperr.blissgems.core.api.enchant.AutoEnchantAPI;
import dev.xoperr.blissgems.core.api.particle.ParticleAPI;
import dev.xoperr.blissgems.core.api.protection.GemProtectionAPI;
import dev.xoperr.blissgems.core.api.region.RegionAPI;
import dev.xoperr.blissgems.core.api.text.InventoryTextAPI;
import dev.xoperr.blissgems.core.managers.AutoEnchantManager;
import dev.xoperr.blissgems.core.managers.ParticleManager;
import dev.xoperr.blissgems.core.managers.ProtectionManager;
import dev.xoperr.blissgems.core.managers.RegionManager;
import dev.xoperr.blissgems.core.managers.TextManager;
import dev.xoperr.blissgems.goldevent.AlternateCommand;
import dev.xoperr.blissgems.goldevent.FragmentCoreRitual;
import dev.xoperr.blissgems.goldevent.GoldenDream;
import dev.xoperr.blissgems.goldevent.GoldenDreamCommand;
import dev.xoperr.blissgems.libs.faststats.bukkit.BukkitMetrics;
import dev.xoperr.blissgems.libs.faststats.core.Metrics;
import dev.xoperr.blissgems.libs.faststats.core.data.Metric;
import dev.xoperr.blissgems.listeners.AstraPhasingListener;
import dev.xoperr.blissgems.listeners.AutoEnchantListener;
import dev.xoperr.blissgems.listeners.BrokenGemDamageListener;
import dev.xoperr.blissgems.listeners.BrokenMythicListener;
import dev.xoperr.blissgems.listeners.ComprehensiveGemProtectionListener;
import dev.xoperr.blissgems.listeners.DropSwapGuard;
import dev.xoperr.blissgems.listeners.EnchantLimiterListener;
import dev.xoperr.blissgems.listeners.GaleCloudListener;
import dev.xoperr.blissgems.listeners.GemCraftingGuard;
import dev.xoperr.blissgems.listeners.GemIntegrityListener;
import dev.xoperr.blissgems.listeners.GemInteractListener;
import dev.xoperr.blissgems.listeners.GoldSummonListener;
import dev.xoperr.blissgems.listeners.ItemOwnershipListener;
import dev.xoperr.blissgems.listeners.KillTrackingListener;
import dev.xoperr.blissgems.listeners.MaceVillagerListener;
import dev.xoperr.blissgems.listeners.PassiveListener;
import dev.xoperr.blissgems.listeners.PlayerDeathListener;
import dev.xoperr.blissgems.listeners.PlayerJoinListener;
import dev.xoperr.blissgems.listeners.PrismaticEdgeListener;
import dev.xoperr.blissgems.listeners.RepairKitListener;
import dev.xoperr.blissgems.listeners.RestorationBookListener;
import dev.xoperr.blissgems.listeners.ReviveBeaconListener;
import dev.xoperr.blissgems.listeners.RitualCleanupListener;
import dev.xoperr.blissgems.listeners.StunListener;
import dev.xoperr.blissgems.listeners.SwapHandAbilityListener;
import dev.xoperr.blissgems.listeners.TeleportListener;
import dev.xoperr.blissgems.listeners.UpgraderListener;
import dev.xoperr.blissgems.listeners.VillagerTradeListener;
import dev.xoperr.blissgems.managers.AbilityBindingManager;
import dev.xoperr.blissgems.managers.AbilityBossBarManager;
import dev.xoperr.blissgems.managers.AbilityManager;
import dev.xoperr.blissgems.managers.AchievementManager;
import dev.xoperr.blissgems.managers.ClickActivationManager;
import dev.xoperr.blissgems.managers.ControlsMenu;
import dev.xoperr.blissgems.managers.CooldownDisplayManager;
import dev.xoperr.blissgems.managers.CriticalHitManager;
import dev.xoperr.blissgems.managers.EnchantLimiterManager;
import dev.xoperr.blissgems.managers.EndSkyVisualManager;
import dev.xoperr.blissgems.managers.EnergyManager;
import dev.xoperr.blissgems.managers.EnhancedGuiManager;
import dev.xoperr.blissgems.managers.FlowStateManager;
import dev.xoperr.blissgems.managers.FluxEnergyManager;
import dev.xoperr.blissgems.managers.GemLockManager;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.managers.GemMenu;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.managers.GemRitualManager;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.managers.GoldHarvestCeremony;
import dev.xoperr.blissgems.managers.MaceVillagerManager;
import dev.xoperr.blissgems.managers.MythicWorldEventManager;
import dev.xoperr.blissgems.managers.PassiveManager;
import dev.xoperr.blissgems.managers.PluginMessagingManager;
import dev.xoperr.blissgems.managers.RecipeManager;
import dev.xoperr.blissgems.managers.RepairKitManager;
import dev.xoperr.blissgems.managers.ReviveBeaconManager;
import dev.xoperr.blissgems.managers.SoulManager;
import dev.xoperr.blissgems.managers.SpawnBeaconManager;
import dev.xoperr.blissgems.managers.StatsManager;
import dev.xoperr.blissgems.managers.TrustedPlayersManager;
import dev.xoperr.blissgems.managers.UpdateChecker;
import dev.xoperr.blissgems.pedestal.PedestalListener;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.utils.ConfigManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.villagerevent.VillagerEventCommand;
import dev.xoperr.blissgems.villagerevent.VillagerEventListener;
import dev.xoperr.blissgems.villagerevent.VillagerEventManager;
import java.io.File;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class BlissGems extends JavaPlugin implements BlissGemsAPI {
   private ConfigManager configManager;
   private BlissCommand blissCommand;
   private GemRegistryImpl gemRegistry;
   private EnergyManager energyManager;
   private GemManager gemManager;
   private AbilityManager abilityManager;
   private AbilityBossBarManager abilityBossBarManager;
   private AbilityBindingManager abilityBindingManager;
   private PassiveManager passiveManager;
   private ClickActivationManager clickActivationManager;
   private TrustedPlayersManager trustedPlayersManager;
   private CooldownDisplayManager cooldownDisplayManager;
   private EnhancedGuiManager enhancedGuiManager;
   private StatsManager statsManager;
   private RecipeManager recipeManager;
   private RepairKitManager repairKitManager;
   private ReviveBeaconManager reviveBeaconManager;
   private SoulManager soulManager;
   private GoldGemManager goldGemManager;
   private GoldAbilities goldAbilities;
   private GoldHarvestCeremony goldHarvestCeremony;
   private FlowStateManager flowStateManager;
   private GemLockManager gemLockManager;
   private CriticalHitManager criticalHitManager;
   private PluginMessagingManager pluginMessagingManager;
   private AstraAbilities astraAbilities;
   private AstraUnbounded astraUnbounded;
   private ControlsMenu controlsMenu;
   private FireAbilities fireAbilities;
   private FluxAbilities fluxAbilities;
   private FluxEnergyManager fluxEnergyManager;
   private LifeAbilities lifeAbilities;
   private PuffAbilities puffAbilities;
   private SpeedAbilities speedAbilities;
   private StrengthAbilities strengthAbilities;
   private WealthAbilities wealthAbilities;
   private ItemOwnershipListener itemOwnershipListener;
   private ProtectionManager protectionManager;
   private ParticleManager particleManager;
   private TextManager textManager;
   private AutoEnchantManager autoEnchantManager;
   private RegionManager regionManager;
   private AchievementManager achievementManager;
   private GemRitualManager gemRitualManager;
   private MaceVillagerManager maceVillagerManager;
   private EnchantLimiterManager enchantLimiterManager;
   private MythicWorldEventManager mythicWorldEventManager;
   private SpawnBeaconManager spawnBeaconManager;
   private EndSkyVisualManager endSkyVisualManager;
   private PedestalManager pedestalManager;
   private VillagerEventManager villagerEventManager;
   private FragmentCoreRitual fragmentCoreRitual;
   private GoldenDream goldenDream;
   private Metrics metrics;
   private GemIntegrityListener gemIntegrity;
   private GemMenu gemMenu;

   public void onEnable() {
      this.saveDefaultConfig();

      try {
         CustomItemManager.initialize(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: CustomItemManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.protectionManager = new ProtectionManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ProtectionManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.particleManager = new ParticleManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ParticleManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.textManager = new TextManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: TextManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.autoEnchantManager = new AutoEnchantManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AutoEnchantManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.regionManager = new RegionManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: RegionManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         GemProtectionAPI.initialize(this.protectionManager);
         ParticleAPI.initialize(this.particleManager);
         InventoryTextAPI.initialize(this.textManager);
         AutoEnchantAPI.initialize(this.autoEnchantManager);
         RegionAPI.initialize(this.regionManager);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: Core APIs ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.configManager = new ConfigManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ConfigManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.energyManager = new EnergyManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: EnergyManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.gemManager = new GemManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: GemManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.abilityManager = new AbilityManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AbilityManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.abilityBindingManager = new AbilityBindingManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AbilityBindingManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.passiveManager = new PassiveManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: PassiveManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.clickActivationManager = new ClickActivationManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ClickActivationManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.trustedPlayersManager = new TrustedPlayersManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: TrustedPlayersManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.repairKitManager = new RepairKitManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: RepairKitManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.reviveBeaconManager = new ReviveBeaconManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ReviveBeaconManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.soulManager = new SoulManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: SoulManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.flowStateManager = new FlowStateManager(this);
         this.gemLockManager = new GemLockManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: FlowStateManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.criticalHitManager = new CriticalHitManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: CriticalHitManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.pluginMessagingManager = new PluginMessagingManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: PluginMessagingManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.astraAbilities = new AstraAbilities(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AstraAbilities ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.fireAbilities = new FireAbilities(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: FireAbilities ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.fluxEnergyManager = new FluxEnergyManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: FluxEnergyManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.fluxAbilities = new FluxAbilities(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: FluxAbilities ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.lifeAbilities = new LifeAbilities(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: LifeAbilities ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.puffAbilities = new PuffAbilities(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: PuffAbilities ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.speedAbilities = new SpeedAbilities(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: SpeedAbilities ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.strengthAbilities = new StrengthAbilities(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: StrengthAbilities ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.wealthAbilities = new WealthAbilities(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: WealthAbilities ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.goldGemManager = new GoldGemManager(this);
         this.goldAbilities = new GoldAbilities(this);
         this.goldHarvestCeremony = new GoldHarvestCeremony(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: GoldGem ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.gemRegistry = new GemRegistryImpl(this);
         this.registerBuiltInGems();
         this.getServer().getServicesManager().register(BlissGemsAPI.class, this, this, ServicePriority.Normal);
         this.getLogger().info("BlissGems Addon API registered via ServicesManager");
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: GemRegistry/API ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.cooldownDisplayManager = new CooldownDisplayManager(this);
         this.abilityBossBarManager = new AbilityBossBarManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: CooldownDisplayManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.statsManager = new StatsManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: StatsManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.achievementManager = new AchievementManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AchievementManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.enhancedGuiManager = new EnhancedGuiManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: EnhancedGuiManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.recipeManager = new RecipeManager(this);
         this.gemRitualManager = new GemRitualManager(this);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: RecipeManager/GemRitualManager ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         if (this.recipeManager != null) {
            this.recipeManager.registerRecipes();
         }
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: Recipe Registration ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.checkAutoStartSmp();
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: SMP Auto-Start Check ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.spawnBeaconManager = new SpawnBeaconManager(this);
         this.maceVillagerManager = new MaceVillagerManager(this);
         this.enchantLimiterManager = new EnchantLimiterManager(this);
         this.mythicWorldEventManager = new MythicWorldEventManager(this);
         this.endSkyVisualManager = new EndSkyVisualManager(this);
         this.pedestalManager = new PedestalManager(this);
         this.getServer().getPluginManager().registerEvents(new PedestalListener(this, this.pedestalManager), this);
         this.getServer().getPluginManager().registerEvents(this.pedestalManager.revive(), this);
         this.villagerEventManager = new VillagerEventManager(this);
         this.getServer().getPluginManager().registerEvents(new VillagerEventListener(this, this.villagerEventManager), this);
         this.fragmentCoreRitual = new FragmentCoreRitual(this);
         this.getServer().getPluginManager().registerEvents(this.fragmentCoreRitual, this);
         this.fragmentCoreRitual.start();
         this.goldenDream = new GoldenDream(this, this.fragmentCoreRitual);
         GoldenDreamCommand goldenDreamCommand = new GoldenDreamCommand(this.goldenDream);
         this.getCommand("goldendream").setExecutor(goldenDreamCommand);
         this.getCommand("goldendream").setTabCompleter(goldenDreamCommand);
         this.getCommand("alternate").setExecutor(new AlternateCommand(this.goldenDream));
         VillagerEventCommand villagerEventCommand = new VillagerEventCommand(this.villagerEventManager);
         this.getCommand("blissevent").setExecutor(villagerEventCommand);
         this.getCommand("blissevent").setTabCompleter(villagerEventCommand);
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: World/Villager/Beacon Managers ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.registerListeners();
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: Listener Registration ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      try {
         this.registerCommands();
      } catch (Exception e) {
         this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: Command Registration ===");
         this.getLogger().severe(e.getMessage());
         e.printStackTrace();
      }

      if (this.getConfig().getBoolean("send-anonymous-metrics", true)) {
         try {
            this.metrics = BukkitMetrics.factory().token("33b82f6ed2f61ee4be22345da22fbf24").addMetric(Metric.number("active_gem_players", () -> {
               int count = 0;

               for (Player p : this.getServer().getOnlinePlayers()) {
                  if (this.gemManager != null && this.gemManager.hasGemInOffhand(p)) {
                     count++;
                  }
               }

               return count;
            })).create(this);
            this.metrics.ready();
         } catch (Exception e) {
            this.getLogger().warning("FastStats metrics failed to initialize: " + e.getMessage());
         }
      }

      this.getLogger().info("BlissGems has been enabled!");
      this.getLogger().info("Version: " + this.getDescription().getVersion());
      this.getLogger().info("Using custom item system with vanilla Minecraft items");
      new UpdateChecker(this).checkAsync();
   }

   public File getPluginFile() {
      return this.getFile();
   }

   public void onDisable() {
      if (this.gemIntegrity != null) {
         this.gemIntegrity.save();
      }

      if (this.particleManager != null) {
         this.particleManager.cleanup();
      }

      if (this.textManager != null) {
         this.textManager.cleanup();
      }

      if (this.autoEnchantManager != null) {
         this.autoEnchantManager.cleanup();
      }

      if (this.cooldownDisplayManager != null) {
         this.cooldownDisplayManager.stop();
      }

      if (this.abilityBossBarManager != null) {
         this.abilityBossBarManager.stop();
      }

      if (this.itemOwnershipListener != null) {
         this.itemOwnershipListener.stop();
      }

      if (this.repairKitManager != null) {
         this.repairKitManager.cleanup();
      }

      if (this.reviveBeaconManager != null) {
         this.reviveBeaconManager.cleanup();
      }

      if (this.goldHarvestCeremony != null) {
         this.goldHarvestCeremony.cleanup();
      }

      if (this.mythicWorldEventManager != null) {
         this.mythicWorldEventManager.cleanup();
      }

      if (this.endSkyVisualManager != null) {
         this.endSkyVisualManager.cleanup();
      }

      if (this.goldenDream != null) {
         this.goldenDream.shutdown();
      }

      if (this.astraUnbounded != null) {
         this.astraUnbounded.shutdown();
      }

      if (this.fragmentCoreRitual != null) {
         this.fragmentCoreRitual.stop();
      }

      if (this.villagerEventManager != null) {
         this.villagerEventManager.shutdown();
      }

      if (this.pedestalManager != null) {
         this.pedestalManager.shutdown();
      }

      if (this.pluginMessagingManager != null) {
         this.pluginMessagingManager.shutdown();
      }

      if (this.recipeManager != null) {
         this.recipeManager.unregisterRecipes();
      }

      if (this.fluxEnergyManager != null) {
         this.fluxEnergyManager.stop();
      }

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (this.astraAbilities != null) {
            this.astraAbilities.cleanup(player);
         }

         if (this.fireAbilities != null) {
            this.fireAbilities.cleanup(player);
         }

         if (this.fluxAbilities != null) {
            this.fluxAbilities.cleanup(player);
         }

         if (this.strengthAbilities != null) {
            this.strengthAbilities.cleanup(player);
         }

         if (this.lifeAbilities != null) {
            this.lifeAbilities.cleanup(player);
         }
      }

      if (this.metrics != null) {
         this.metrics.shutdown();
      }

      this.energyManager.saveAll();
      if (this.wealthAbilities != null) {
         this.wealthAbilities.saveAllPockets();
      }

      if (this.abilityManager != null) {
         this.abilityManager.saveAllCooldowns();
      }

      if (this.achievementManager != null) {
         this.achievementManager.saveAll();
      }

      this.getLogger().info("BlissGems has been disabled!");
   }

   private void checkAutoStartSmp() {
      if (this.configManager != null && !this.configManager.isSmpStarted()) {
         File playerDataFolder = new File(this.getDataFolder(), "playerdata");
         if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            int threshold = this.configManager.getSmpAutoStartThreshold();
            int count = 0;
            File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
               for (File file : files) {
                  YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
                  if (data.getBoolean("received-first-gem", false)) {
                     if (++count >= threshold) {
                        this.configManager.setSmpStarted(true);
                        this.getLogger().info("SMP auto-started! Found " + count + " players with gems (threshold: " + threshold + ").");
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private void registerListeners() {
      PlayerDeathListener deathListener = new PlayerDeathListener(this);
      this.getServer().getPluginManager().registerEvents(deathListener, this);
      this.getServer().getPluginManager().registerEvents(new GemCraftingGuard(), this);
      this.gemMenu = new GemMenu(this);
      this.getServer().getPluginManager().registerEvents(this.gemMenu, this);
      this.getServer().getScheduler().runTask(this, deathListener::validateDroppableOnDeathConfig);
      this.getServer().getPluginManager().registerEvents(new ComprehensiveGemProtectionListener(this), this);
      this.gemIntegrity = new GemIntegrityListener(this);
      this.getServer().getPluginManager().registerEvents(this.gemIntegrity, this);
      this.gemIntegrity.start();
      this.getServer().getPluginManager().registerEvents(new GemInteractListener(this), this);
      this.getServer().getPluginManager().registerEvents(new UpgraderListener(this), this);
      this.getServer().getPluginManager().registerEvents(new PassiveListener(this), this);
      this.getServer().getPluginManager().registerEvents(new AstraPhasingListener(this), this);
      this.astraUnbounded = new AstraUnbounded(this);
      this.getServer().getPluginManager().registerEvents(this.astraUnbounded, this);
      this.controlsMenu = new ControlsMenu(this);
      this.getServer().getPluginManager().registerEvents(this.controlsMenu, this);
      this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
      this.getServer().getPluginManager().registerEvents(new AutoEnchantListener(this), this);
      this.getServer().getPluginManager().registerEvents(new StunListener(this), this);
      this.getServer().getPluginManager().registerEvents(new RepairKitListener(this), this);
      this.getServer().getPluginManager().registerEvents(new ReviveBeaconListener(this), this);
      this.getServer().getPluginManager().registerEvents(new KillTrackingListener(this), this);
      this.getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
      this.getServer().getPluginManager().registerEvents(new VillagerTradeListener(this), this);
      this.getServer().getPluginManager().registerEvents(new SwapHandAbilityListener(this), this);
      this.getServer().getPluginManager().registerEvents(new RitualCleanupListener(this), this);
      this.getServer().getPluginManager().registerEvents(new BrokenGemDamageListener(this), this);
      this.getServer().getPluginManager().registerEvents(new GaleCloudListener(this), this);
      this.getServer().getPluginManager().registerEvents(new RestorationBookListener(this), this);
      this.getServer().getPluginManager().registerEvents(new PrismaticEdgeListener(this), this);
      if (this.goldAbilities != null) {
         this.getServer().getPluginManager().registerEvents(this.goldAbilities, this);
      }

      if (this.goldHarvestCeremony != null) {
         this.getServer().getPluginManager().registerEvents(this.goldHarvestCeremony, this);
      }

      this.getServer().getPluginManager().registerEvents(new GoldSummonListener(this), this);
      DropSwapGuard dropSwapGuard = new DropSwapGuard(this);
      this.getServer().getPluginManager().registerEvents(dropSwapGuard, this);
      dropSwapGuard.start();
      int sweptRitualGems = GemRitualManager.sweepAll(this.getServer());
      if (sweptRitualGems > 0) {
         this.getLogger().info("Swept " + sweptRitualGems + " orphaned ritual gem display(s) on enable.");
      }

      this.itemOwnershipListener = new ItemOwnershipListener(this);
      this.getServer().getPluginManager().registerEvents(this.itemOwnershipListener, this);
      this.itemOwnershipListener.start();
      this.getServer().getPluginManager().registerEvents(this.enhancedGuiManager, this);
      if (this.maceVillagerManager != null) {
         this.getServer().getPluginManager().registerEvents(new MaceVillagerListener(this, this.maceVillagerManager), this);
      }

      if (this.enchantLimiterManager != null) {
         this.getServer().getPluginManager().registerEvents(new EnchantLimiterListener(this, this.enchantLimiterManager), this);
      }

      if (this.mythicWorldEventManager != null) {
         this.getServer().getPluginManager().registerEvents(new BrokenMythicListener(this, this.mythicWorldEventManager), this);
      }
   }

   private void registerCommands() {
      this.blissCommand = new BlissCommand(this);
      this.getCommand("bliss").setExecutor(this.blissCommand);
      this.getCommand("bliss").setTabCompleter(this.blissCommand);
      NewsCommand newsCommand = new NewsCommand(this);

      for (String name : new String[]{"startcharging", "stopcharging"}) {
         if (this.getCommand(name) != null) {
            boolean start = name.equals("startcharging");
            this.getCommand(name).setExecutor((sender, command, label, args) -> {
               if (sender instanceof Player p) {
                  if (this.fluxEnergyManager != null) {
                     this.fluxEnergyManager.setCharging(p, start);
                  }

                  return true;
               } else {
                  sender.sendMessage("Player only.");
                  return true;
               }
            });
         }
      }

      this.getServer().getPluginManager().registerEvents(new Listener() {
         @EventHandler
         public void onJoin(PlayerJoinEvent event) {
            if (BlissGems.this.fluxEnergyManager != null) {
               BlissGems.this.fluxEnergyManager.onJoin(event.getPlayer());
            }

            BlissGems.this.refreshGoldLater(event.getPlayer(), 40L);
         }

         @EventHandler
         public void onRespawn(PlayerRespawnEvent event) {
            BlissGems.this.refreshGoldLater(event.getPlayer(), 5L);
         }
      }, this);
      if (this.getCommand("news") != null) {
         this.getCommand("news").setExecutor(newsCommand);
      }

      FixHeartsCommand fixHearts = new FixHeartsCommand(this);
      if (this.getCommand("fixhearts") != null) {
         this.getCommand("fixhearts").setExecutor(fixHearts);
         this.getCommand("fixhearts").setTabCompleter(fixHearts);
      }

      FixedHeartsCommand fixedHearts = new FixedHeartsCommand(this);
      if (this.getCommand("fixedhearts") != null) {
         this.getCommand("fixedhearts").setExecutor(fixedHearts);
         this.getCommand("fixedhearts").setTabCompleter(fixedHearts);
      }

      FixGemsCommand fixGems = new FixGemsCommand(this);
      if (this.getCommand("fixgems") != null) {
         this.getCommand("fixgems").setExecutor(fixGems);
         this.getCommand("fixgems").setTabCompleter(fixGems);
      }

      if (this.getCommand("conduction") != null) {
         this.getCommand("conduction").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player player)) {
               sender.sendMessage("§cOnly players can use this command!");
               return true;
            } else {
               if (this.gemManager.getGemType(player) != GemType.FLUX) {
                  player.sendMessage("§c§lYou need the Flux Gem to use Conduction!");
                  return true;
               }

               if (this.fluxAbilities != null) {
                  this.fluxAbilities.conduction(player);
               }

               return true;
            }
         });
      }
   }

   public BlissCommand getBlissCommand() {
      return this.blissCommand;
   }

   @Override
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Override
   public EnergyManager getEnergyManager() {
      return this.energyManager;
   }

   @Override
   public GemManager getGemManager() {
      return this.gemManager;
   }

   @Override
   public AbilityManager getAbilityManager() {
      return this.abilityManager;
   }

   public AbilityBindingManager getAbilityBindingManager() {
      return this.abilityBindingManager;
   }

   public AbilityBossBarManager getAbilityBossBarManager() {
      return this.abilityBossBarManager;
   }

   public PassiveManager getPassiveManager() {
      return this.passiveManager;
   }

   public RecipeManager getRecipeManager() {
      return this.recipeManager;
   }

   public GemRitualManager getGemRitualManager() {
      return this.gemRitualManager;
   }

   void refreshGoldLater(Player player, long delay) {
      this.getServer().getScheduler().runTaskLater(this, () -> {
         if (player.isOnline() && !player.isDead() && this.goldGemManager != null && this.goldGemManager.holdsGoldGem(player)) {
            this.goldGemManager.refreshGoldItem(player);
         }
      }, delay);
   }

   public ControlsMenu getControlsMenu() {
      return this.controlsMenu;
   }

   public AstraUnbounded getAstraUnbounded() {
      return this.astraUnbounded;
   }

   public AstraAbilities getAstraAbilities() {
      return this.astraAbilities;
   }

   public FireAbilities getFireAbilities() {
      return this.fireAbilities;
   }

   public FluxAbilities getFluxAbilities() {
      return this.fluxAbilities;
   }

   public LifeAbilities getLifeAbilities() {
      return this.lifeAbilities;
   }

   public PuffAbilities getPuffAbilities() {
      return this.puffAbilities;
   }

   public SpeedAbilities getSpeedAbilities() {
      return this.speedAbilities;
   }

   public GemMenu getGemMenu() {
      return this.gemMenu;
   }

   public StrengthAbilities getStrengthAbilities() {
      return this.strengthAbilities;
   }

   public WealthAbilities getWealthAbilities() {
      return this.wealthAbilities;
   }

   public RepairKitManager getRepairKitManager() {
      return this.repairKitManager;
   }

   public ReviveBeaconManager getReviveBeaconManager() {
      return this.reviveBeaconManager;
   }

   public GoldGemManager getGoldGemManager() {
      return this.goldGemManager;
   }

   public GoldAbilities getGoldAbilities() {
      return this.goldAbilities;
   }

   public GoldHarvestCeremony getGoldHarvestCeremony() {
      return this.goldHarvestCeremony;
   }

   public SoulManager getSoulManager() {
      return this.soulManager;
   }

   public GemLockManager getGemLockManager() {
      return this.gemLockManager;
   }

   public FlowStateManager getFlowStateManager() {
      return this.flowStateManager;
   }

   public CriticalHitManager getCriticalHitManager() {
      return this.criticalHitManager;
   }

   public ClickActivationManager getClickActivationManager() {
      return this.clickActivationManager;
   }

   @Override
   public TrustedPlayersManager getTrustedPlayersManager() {
      return this.trustedPlayersManager;
   }

   public EnhancedGuiManager getEnhancedGuiManager() {
      return this.enhancedGuiManager;
   }

   public StatsManager getStatsManager() {
      return this.statsManager;
   }

   public AchievementManager getAchievementManager() {
      return this.achievementManager;
   }

   public PluginMessagingManager getPluginMessagingManager() {
      return this.pluginMessagingManager;
   }

   public FluxEnergyManager getFluxEnergyManager() {
      return this.fluxEnergyManager;
   }

   public ProtectionManager getProtectionManager() {
      return this.protectionManager;
   }

   public ParticleManager getParticleManager() {
      return this.particleManager;
   }

   public TextManager getTextManager() {
      return this.textManager;
   }

   public AutoEnchantManager getAutoEnchantManager() {
      return this.autoEnchantManager;
   }

   public RegionManager getRegionManager() {
      return this.regionManager;
   }

   public GemRegistryImpl getGemRegistry() {
      return this.gemRegistry;
   }

   @Override
   public boolean playerHasGem(Player player, String gemId) {
      GemManager.ActiveGem gem = this.gemManager.getActiveGem(player);
      return gem == null ? false : gemId.equals(gem.getGemId());
   }

   public MaceVillagerManager getMaceVillagerManager() {
      return this.maceVillagerManager;
   }

   public EnchantLimiterManager getEnchantLimiterManager() {
      return this.enchantLimiterManager;
   }

   public MythicWorldEventManager getMythicWorldEventManager() {
      return this.mythicWorldEventManager;
   }

   public SpawnBeaconManager getSpawnBeaconManager() {
      return this.spawnBeaconManager;
   }

   public EndSkyVisualManager getEndSkyVisualManager() {
      return this.endSkyVisualManager;
   }

   public PedestalManager getPedestalManager() {
      return this.pedestalManager;
   }

   public GoldenDream getGoldenDream() {
      return this.goldenDream;
   }

   public VillagerEventManager getVillagerEventManager() {
      return this.villagerEventManager;
   }

   private void registerBuiltInGems() {
      for (GemType type : GemType.values()) {
         GemDefinition def = new GemDefinition.Builder(type.getId())
            .displayName(type.getDisplayName())
            .description(type.getDescription())
            .color(type.getColor())
            .plugin("BlissGems")
            .maxTier(2)
            .build();
         this.gemRegistry.registerGem(def);
      }

      this.gemRegistry
         .registerGem(
            new GemDefinition.Builder("gold")
               .displayName("Gold")
               .description("Watch the lines of reality fray as eight souls become one")
               .color("§6")
               .plugin("BlissGems")
               .maxTier(1)
               .material(CustomItemManager.GEM_MATERIAL)
               .t1CustomModelData(1009)
               .build()
         );
      if (this.astraAbilities != null) {
         this.gemRegistry.registerAbilities("astra", this.astraAbilities);
      }

      if (this.fireAbilities != null) {
         this.gemRegistry.registerAbilities("fire", this.fireAbilities);
      }

      if (this.fluxAbilities != null) {
         this.gemRegistry.registerAbilities("flux", this.fluxAbilities);
      }

      if (this.lifeAbilities != null) {
         this.gemRegistry.registerAbilities("life", this.lifeAbilities);
      }

      if (this.puffAbilities != null) {
         this.gemRegistry.registerAbilities("puff", this.puffAbilities);
      }

      if (this.speedAbilities != null) {
         this.gemRegistry.registerAbilities("speed", this.speedAbilities);
      }

      if (this.strengthAbilities != null) {
         this.gemRegistry.registerAbilities("strength", this.strengthAbilities);
      }

      if (this.wealthAbilities != null) {
         this.gemRegistry.registerAbilities("wealth", this.wealthAbilities);
      }

      if (this.goldAbilities != null) {
         this.gemRegistry.registerAbilities("gold", this.goldAbilities);
      }

      if (this.passiveManager != null) {
         this.passiveManager.registerBuiltInHandlers(this.gemRegistry);
      }

      this.gemRegistry
         .registerCooldowns(
            "astra",
            List.of(
               new CooldownEntry("astra-daggers", "Daggers"),
               new CooldownEntry("astra-projection", "Projection"),
               new CooldownEntry("astra-drift", "Drift"),
               new CooldownEntry("astra-void", "Nullify")
            )
         );
      this.gemRegistry
         .registerCooldowns(
            "fire",
            List.of(
               new CooldownEntry("fire-fireball", "Fireball"),
               new CooldownEntry("fire-campfire", "Campfire"),
               new CooldownEntry("fire-crisp", "Crisp"),
               new CooldownEntry("fire-meteor-shower", "Meteor")
            )
         );
      this.gemRegistry
         .registerCooldowns(
            "flux",
            List.of(
               new CooldownEntry("flux-beam", "Beam"),
               new CooldownEntry("flux-ground", "Ground"),
               new CooldownEntry("flux-flashbang", "Flash"),
               new CooldownEntry("flux-kinetic-burst", "Kinetic")
            )
         );
      this.gemRegistry
         .registerCooldowns(
            "life",
            List.of(
               new CooldownEntry("life-drainer", "Drainer"),
               new CooldownEntry("life-circle-of-life", "Circle"),
               new CooldownEntry("life-vitality-vortex", "Vortex"),
               new CooldownEntry("life-heart-lock", "Lock")
            )
         );
      this.gemRegistry
         .registerCooldowns(
            "puff",
            List.of(new CooldownEntry("puff-dash", "Dash"), new CooldownEntry("puff-breezy-bash", "Bash"), new CooldownEntry("puff-group-bash", "Group"))
         );
      this.gemRegistry
         .registerCooldowns(
            "speed",
            List.of(new CooldownEntry("speed-blur", "Blur"), new CooldownEntry("speed-storm", "Storm"), new CooldownEntry("speed-terminal", "Terminal"))
         );
      this.gemRegistry
         .registerCooldowns(
            "strength",
            List.of(
               new CooldownEntry("strength-nullify", "Nullify"),
               new CooldownEntry("strength-frailer", "Frailer"),
               new CooldownEntry("strength-shadow-stalker", "Stalker")
            )
         );
      this.gemRegistry
         .registerCooldowns(
            "wealth",
            List.of(
               new CooldownEntry("wealth-unfortunate", "Unfortunate"),
               new CooldownEntry("wealth-rich-rush", "Rush"),
               new CooldownEntry("wealth-item-lock", "Lock"),
               new CooldownEntry("wealth-amplification", "Amplify")
            )
         );
      this.gemRegistry.registerCooldowns("gold", List.of(new CooldownEntry("gold-beam", "Beam")));
   }
}
