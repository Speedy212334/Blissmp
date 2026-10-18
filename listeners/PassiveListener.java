package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.FluxAbilities;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import dev.xoperr.blissgems.abilities.WealthAbilities;
import dev.xoperr.blissgems.managers.FlowStateManager;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.Particle.DustOptions;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PassiveListener implements Listener {
   private final BlissGems plugin;
   private final Map<UUID, Integer> jumpsRemaining = new HashMap<>();

   public PassiveListener(BlissGems plugin) {
      this.plugin = plugin;
   }

   private boolean isHoldingPuffGem(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String oraxenId;
      return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("puff_gem");
   }

   @EventHandler
   public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player) {
         if (event.getEntity() instanceof LivingEntity victim) {
            if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.LIFE)) {
               if (this.canUsePassives(player)) {
                  if (Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(victim.getType())) {
                     int tier = this.plugin.getGemManager().getTierFor(player, GemType.LIFE);
                     double multiplier = this.plugin.getConfigManager().getUndeadDamageMultiplier(tier);
                     event.setDamage(event.getDamage() * multiplier);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPuffGemHitPlayer(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player) {
         if (event.getEntity() instanceof Player targetPlayer) {
            if (this.plugin.getTrustedPlayersManager() == null || !this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
               ItemStack mainHand = player.getInventory().getItemInMainHand();
               if (mainHand != null) {
                  String oraxenId = CustomItemManager.getIdByItem(mainHand);
                  if (oraxenId != null && oraxenId.contains("puff_gem")) {
                     if (this.canUsePassives(player)) {
                        String abilityKey = "puff-launch";
                        if (!this.plugin.getAbilityManager().isOnCooldown(player, abilityKey)) {
                           int tier = GemType.getTierFromOraxenId(oraxenId);
                           double launchVelocityValue = this.plugin.getConfigManager().getLaunchVelocity(tier);
                           Vector launchVelocity = new Vector(0.0, launchVelocityValue, 0.0);
                           targetPlayer.setVelocity(launchVelocity);
                           this.plugin.getAbilityManager().useAbility(player, abilityKey);
                           targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0F, 0.8F);
                           targetPlayer.getWorld().spawnParticle(Particle.CLOUD, targetPlayer.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                           targetPlayer.sendMessage("§b§oYou've been launched into the sky!");
                           player.sendMessage("§b§oYou launched " + targetPlayer.getName() + " into the sky!");
                           this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                              if (targetPlayer.isOnline()) {
                                 Vector slamVelocity = new Vector(0.0, -3.5, 0.0);
                                 targetPlayer.setVelocity(slamVelocity);
                                 targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_BREEZE_LAND, 1.0F, 0.5F);
                                 targetPlayer.sendMessage("§c§oYou're being slammed down!");
                                 this.plugin.getPuffAbilities().impactRingOnLand(targetPlayer);
                              }
                           }, 60L);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onFallDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (event.getCause() == DamageCause.FALL) {
            if (this.plugin.getPuffAbilities().hasFallDamageImmunity(player)) {
               event.setCancelled(true);
               this.plugin.getPuffAbilities().removeFallDamageImmunity(player);
            } else if (this.plugin.getAstraAbilities().hasDashFallImmunity(player)) {
               event.setCancelled(true);
            } else if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || this.isHoldingPuffGem(player)) {
               if (this.canUsePassives(player)) {
                  event.setCancelled(true);
                  if (this.plugin.getAchievementManager() != null) {
                     int prevented = (int)event.getDamage();
                     this.plugin.getAchievementManager().addProgress(player, Achievement.ALMOST_FELL_FROM_GRACE, prevented);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
         boolean hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || this.isHoldingPuffGem(player);
         boolean passivesActive = hasPuff && this.canUsePassives(player);
         if (passivesActive && player.isOnGround()) {
            this.jumpsRemaining.put(player.getUniqueId(), 2);
            if (!player.getAllowFlight()) {
               player.setAllowFlight(true);
            }
         } else if (!passivesActive && player.getAllowFlight() && player.getGameMode() == GameMode.SURVIVAL) {
            player.setAllowFlight(false);
            this.jumpsRemaining.remove(player.getUniqueId());
         }
      }
   }

   @EventHandler
   public void onTripleJumpFlight(PlayerToggleFlightEvent event) {
      Player player = event.getPlayer();
      if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
         boolean hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || this.isHoldingPuffGem(player);
         if (hasPuff) {
            if (this.canUsePassives(player)) {
               UUID uuid = player.getUniqueId();
               if (!FluxAbilities.isPlayerStunned(uuid) && !SpeedAbilities.isPlayerFrozen(uuid)) {
                  int remaining = this.jumpsRemaining.getOrDefault(uuid, 0);
                  if (remaining <= 0) {
                     event.setCancelled(true);
                  } else {
                     event.setCancelled(true);
                     player.setAllowFlight(false);
                     this.jumpsRemaining.put(uuid, remaining - 1);
                     int tier = this.plugin.getGemManager().getTierFor(player, GemType.PUFF);
                     String oraxenId;
                     if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand())) != null) {
                        tier = GemType.getTierFromOraxenId(oraxenId);
                     }

                     if (tier == 0) {
                        tier = 1;
                     }

                     double jumpVelocity = this.plugin.getConfigManager().getDoubleJumpVelocity(tier);
                     Vector velocity = player.getVelocity();
                     velocity.setY(jumpVelocity);
                     player.setVelocity(velocity);
                     player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0.05);
                     player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.5F, 1.5F);
                     if (remaining - 1 > 0) {
                        this.plugin
                           .getServer()
                           .getScheduler()
                           .runTaskLater(
                              this.plugin,
                              () -> {
                                 if (player.isOnline()
                                    && !player.isOnGround()
                                    && player.getGameMode() != GameMode.CREATIVE
                                    && player.getGameMode() != GameMode.SPECTATOR) {
                                    player.setAllowFlight(true);
                                 }
                              },
                              2L
                           );
                     }
                  }
               } else {
                  event.setCancelled(true);
               }
            }
         }
      }
   }

   @EventHandler
   public void onFarmlandTrample(PlayerInteractEvent event) {
      if (event.getAction() == Action.PHYSICAL) {
         if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.FARMLAND) {
            Player player = event.getPlayer();
            boolean hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || this.isHoldingPuffGem(player);
            if (hasPuff) {
               if (this.canUsePassives(player)) {
                  event.setCancelled(true);
               }
            }
         }
      }
   }

   @EventHandler
   public void onCrispBlockBreak(BlockBreakEvent event) {
      if (this.plugin.getFireAbilities() != null && this.plugin.getFireAbilities().isProtectedBlock(event.getBlock().getLocation())) {
         event.setCancelled(true);
         event.getPlayer().sendMessage("§c§oThis block is scorched and cannot be broken!");
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      boolean hasFireGem = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FIRE)
         || this.plugin.getGemManager().hasGemType(player, GemType.FIRE);
      boolean hasWealthGem = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH)
         || this.plugin.getGemManager().hasGemType(player, GemType.WEALTH)
         || this.isHoldingWealthGem(player);
      int wealthTier = hasWealthGem ? this.plugin.getGemManager().getTierFor(player, GemType.WEALTH) : 0;
      boolean canAutoSmelt = (hasFireGem || hasWealthGem && wealthTier >= 2) && this.plugin.getWealthAbilities().isAutoSmeltEnabled(player);
      ItemStack tool = player.getInventory().getItemInMainHand();
      boolean hasSilkTouch = tool != null && tool.hasItemMeta() && tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH);
      boolean willAutoSmelt = canAutoSmelt && this.canUsePassives(player) && !hasSilkTouch;
      if (event.getBlock().getType() == Material.ANCIENT_DEBRIS && hasWealthGem && this.canUsePassives(player)) {
         int tier = this.plugin.getGemManager().getTierFor(player, GemType.WEALTH);
         String tierKey = tier >= 2 ? "tier2" : "tier1";
         if (this.plugin.getConfig().getBoolean("passives.wealth." + tierKey + ".double-debris", true)) {
            Material dropMat = willAutoSmelt ? Material.NETHERITE_SCRAP : Material.ANCIENT_DEBRIS;
            ItemStack extra = new ItemStack(dropMat, 1);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), extra);
            DustOptions greenDust = new DustOptions(ParticleUtils.WEALTH_GREEN, 1.2F);
            player.getWorld().spawnParticle(Particle.DUST, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.0, greenDust, true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
            player.sendMessage("§a§oDouble Debris! Extra " + (willAutoSmelt ? "netherite scrap" : "ancient debris") + "!");
         }
      }

      if (willAutoSmelt) {
         Material blockType = event.getBlock().getType();
         ItemStack result = this.getSmeltedItem(blockType);
         if (result != null) {
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), result);
         }
      }
   }

   private ItemStack getSmeltedItem(Material blockType) {
      return switch (blockType) {
         case IRON_ORE, DEEPSLATE_IRON_ORE -> new ItemStack(Material.IRON_INGOT);
         case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> new ItemStack(Material.GOLD_INGOT);
         case COPPER_ORE, DEEPSLATE_COPPER_ORE -> new ItemStack(Material.COPPER_INGOT);
         case ANCIENT_DEBRIS -> new ItemStack(Material.NETHERITE_SCRAP);
         case SAND -> new ItemStack(Material.GLASS);
         case COBBLESTONE -> new ItemStack(Material.STONE);
         case RAW_IRON_BLOCK -> new ItemStack(Material.IRON_BLOCK);
         case RAW_GOLD_BLOCK -> new ItemStack(Material.GOLD_BLOCK);
         case RAW_COPPER_BLOCK -> new ItemStack(Material.COPPER_BLOCK);
         default -> null;
      };
   }

   @EventHandler
   public void onFoodLevelChange(FoodLevelChangeEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.LIFE)) {
            if (this.canUsePassives(player)) {
               int foodLevelBefore = player.getFoodLevel();
               int foodLevelAfter = event.getFoodLevel();
               if (foodLevelAfter > foodLevelBefore) {
                  int gain = foodLevelAfter - foodLevelBefore;
                  int tier = this.plugin.getGemManager().getTierFor(player, GemType.LIFE);
                  float saturationGain = gain * (float)this.plugin.getConfigManager().getSaturationMultiplier(tier);
                  player.setSaturation(player.getSaturation() + saturationGain);
               }
            }
         }
      }
   }

   @EventHandler
   public void onProjectileHit(ProjectileHitEvent event) {
      Projectile projectile = event.getEntity();
      if (projectile instanceof Arrow) {
         if (projectile.getShooter() instanceof Player player) {
            if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
               if (this.canUsePassives(player)) {
                  Entity hitEntity = event.getHitEntity();
                  if (hitEntity != null && hitEntity instanceof LivingEntity) {
                     double chance = this.plugin.getConfig().getDouble("passives.flux.shocking-chance", 0.25);
                     if (!(Math.random() > chance)) {
                        LivingEntity target = (LivingEntity)hitEntity;
                        int tier = this.plugin.getGemManager().getTierFor(player, GemType.FLUX);
                        double shockDamage = this.plugin.getConfigManager().getShockingArrowDamage(tier);
                        target.damage(shockDamage, player);
                        int stunDuration = this.plugin.getConfig().getInt("abilities.durations.flux-shocking-stun", 1) * 20;
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunDuration, 255, false, false));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, stunDuration, 255, false, false));
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0.0, 1.0, 0.0), 60, 0.7, 0.7, 0.7);
                        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
                        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0.0, 1.0, 0.0), 15, 0.3, 0.3, 0.3);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5F, 1.5F);
                        player.sendMessage("§b§oShocking Arrow hit!");
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onSculkShriekerActivate(BlockReceiveGameEvent event) {
      if (event.getEntity() instanceof Player player) {
         boolean inOffhand = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF);
         if (inOffhand || this.isHoldingPuffGem(player)) {
            if (this.canUsePassives(player)) {
               int tier = inOffhand
                  ? this.plugin.getGemManager().getTierFor(player, GemType.PUFF)
                  : GemType.getTierFromOraxenId(CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand()));
               if (this.plugin.getConfigManager().isSculkImmunity(tier)) {
                  Material blockType = event.getBlock().getType();
                  if (blockType == Material.SCULK_SHRIEKER || blockType == Material.SCULK_SENSOR || blockType == Material.CALIBRATED_SCULK_SENSOR) {
                     event.setCancelled(true);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      if (item.getType() == Material.GOLDEN_APPLE || item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
         if (this.plugin.getGemManager().hasGemType(player, GemType.LIFE)) {
            if (this.canUsePassives(player)) {
               int tier = this.plugin.getGemManager().getGemTier(player);
               int absorptionLevel = this.plugin.getConfigManager().getGoldenAppleAbsorptionLevel(tier);
               this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                  player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, absorptionLevel, true, true));
                  player.sendMessage("§a§oLife Gem enhanced your golden apple!");
               }, 1L);
            }
         }
      }
   }

   @EventHandler
   public void onFireballExplosion(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player) {
         Player damaged = (Player)event.getEntity();
         Entity damager = event.getDamager();
         Fireball fireball;
         if (damager instanceof Fireball
            && (fireball = (Fireball)damager).getShooter() instanceof Player
            && ((Player)fireball.getShooter()).getUniqueId().equals(damaged.getUniqueId())) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onEntityDeath(EntityDeathEvent event) {
      LivingEntity killed = event.getEntity();
      Player killer = killed.getKiller();
      if (killer != null) {
         boolean hasAstra = this.plugin.getGemManager().hasGemTypeInOffhand(killer, GemType.ASTRA) || this.isHoldingAstraGem(killer);
         if (hasAstra) {
            if (this.plugin.getEnergyManager().arePassivesActive(killer)) {
               this.plugin.getSoulManager().absorbSoul(killer, killed);
            }
         }
      }
   }

   @EventHandler
   public void onAstraSoulCapture(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         if (player.isSneaking()) {
            Entity target = event.getEntity();
            if (target instanceof LivingEntity && !(target instanceof Player)) {
               boolean hasAstra = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.ASTRA) || this.isHoldingAstraGem(player);
               if (hasAstra) {
                  if (this.canUsePassives(player)) {
                     LivingEntity mob = (LivingEntity)target;
                     if (this.plugin.getSoulManager().captureMob(player, mob)) {
                        event.setCancelled(true);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onAstraSoulCaptureInteract(PlayerInteractEntityEvent event) {
      Player player = event.getPlayer();
      if (player.isSneaking()) {
         Entity target = event.getRightClicked();
         if (target instanceof LivingEntity && !(target instanceof Player)) {
            boolean hasAstra = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.ASTRA) || this.isHoldingAstraGem(player);
            if (hasAstra) {
               if (this.canUsePassives(player)) {
                  LivingEntity mob = (LivingEntity)target;
                  if (this.plugin.getSoulManager().captureMob(player, mob)) {
                     event.setCancelled(true);
                  }
               }
            }
         }
      }
   }

   private boolean isHoldingAstraGem(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String oraxenId;
      return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("astra_gem");
   }

   private boolean isHoldingFluxGem(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String oraxenId;
      return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("flux_gem");
   }

   @EventHandler
   public void onChargedCreeperDamage(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player) {
         if (event.getDamager() instanceof Creeper) {
            Creeper creeper = (Creeper)event.getDamager();
            if (creeper.isPowered()) {
               Player player = (Player)event.getEntity();
               boolean hasFlux = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX) || this.isHoldingFluxGem(player);
               if (hasFlux) {
                  if (this.canUsePassives(player)) {
                     double reduction = this.plugin.getConfig().getDouble("passives.flux.charged-creeper-reduction", 1.0);
                     event.setDamage(event.getDamage() * (1.0 - reduction));
                     DustOptions cyan = new DustOptions(ParticleUtils.FLUX_CYAN, 1.2F);
                     player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.0, cyan, true);
                     player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.1);
                     player.sendMessage("§b⚡ §oFlux gem absorbed the charged creeper blast!");
                  }
               }
            }
         }
      }
   }

   private boolean isHoldingStrengthGem(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String oraxenId;
      return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("strength_gem");
   }

   @EventHandler
   public void onStrengthBloodthorns(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         if (event.getEntity() instanceof LivingEntity) {
            if (event.getEntity() instanceof Player) {
               Player victim = (Player)event.getEntity();
               if (this.plugin.getTrustedPlayersManager() != null && this.plugin.getTrustedPlayersManager().isTrusted(player, victim)) {
                  return;
               }
            }

            boolean hasStrength = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.STRENGTH) || this.isHoldingStrengthGem(player);
            if (hasStrength) {
               if (this.canUsePassives(player)) {
                  int tier = this.plugin.getGemManager().getTierFor(player, GemType.STRENGTH);
                  String oraxenId;
                  if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand())) != null) {
                     tier = GemType.getTierFromOraxenId(oraxenId);
                  }

                  if (tier == 0) {
                     tier = 1;
                  }

                  double bonusDamage;
                  if ((bonusDamage = this.plugin.getConfigManager().getBloodthornsMaxBonusDamage(tier) * (1.0 - player.getHealth() / player.getMaxHealth()))
                     > 0.1) {
                     event.setDamage(event.getDamage() + bonusDamage);
                     LivingEntity target = (LivingEntity)event.getEntity();
                     DustOptions redDust = new DustOptions(ParticleUtils.STRENGTH_RED, 1.0F);
                     int particleCount = Math.min((int)(bonusDamage * 3.0), 30);
                     target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), particleCount, 0.3, 0.3, 0.3, 0.0, redDust, true);
                     target.getWorld()
                        .spawnParticle(
                           Particle.DAMAGE_INDICATOR, target.getLocation().add(0.0, 1.0, 0.0), Math.min((int)(bonusDamage * 2.0), 15), 0.3, 0.3, 0.3
                        );
                  }
               }
            }
         }
      }
   }

   private boolean isHoldingWealthGem(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String oraxenId;
      return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("wealth_gem");
   }

   @EventHandler
   public void onWealthDurabilityChip(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity)event.getEntity();
            boolean hasWealth = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH) || this.isHoldingWealthGem(player);
            if (hasWealth) {
               if (this.canUsePassives(player)) {
                  int tier = this.plugin.getGemManager().getTierFor(player, GemType.WEALTH);
                  String oraxenId;
                  if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand())) != null) {
                     tier = GemType.getTierFromOraxenId(oraxenId);
                  }

                  if (tier == 0) {
                     tier = 1;
                  }

                  String tierKey = tier == 2 ? "tier2" : "tier1";
                  int extraDamage = this.plugin.getConfig().getInt("passives.wealth." + tierKey + ".durability-chip-extra-damage", 1);
                  if (target instanceof Player targetPlayer) {
                     if (this.plugin.getTrustedPlayersManager() == null || !this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                        ItemStack[] armor = targetPlayer.getInventory().getArmorContents();
                        boolean damaged = false;

                        for (int i = 0; i < armor.length; i++) {
                           if (armor[i] != null && !armor[i].getType().isAir() && armor[i].getItemMeta() instanceof Damageable) {
                              Damageable damageable = (Damageable)armor[i].getItemMeta();
                              damageable.setDamage(damageable.getDamage() + extraDamage);
                              armor[i].setItemMeta(damageable);
                              damaged = true;
                           }
                        }

                        if (damaged) {
                           targetPlayer.getInventory().setArmorContents(armor);
                           DustOptions greenDust = new DustOptions(ParticleUtils.WEALTH_GREEN, 1.0F);
                           targetPlayer.getWorld()
                              .spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 10, 0.3, 0.3, 0.3, 0.0, greenDust, true);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onWealthArmorMend(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         if (event.getEntity() instanceof LivingEntity) {
            if (event.getEntity() != player) {
               boolean hasWealth = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH) || this.isHoldingWealthGem(player);
               if (hasWealth) {
                  if (this.canUsePassives(player)) {
                     int tier = this.plugin.getGemManager().getTierFor(player, GemType.WEALTH);
                     String oraxenId;
                     if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand())) != null) {
                        tier = GemType.getTierFromOraxenId(oraxenId);
                     }

                     if (tier == 0) {
                        tier = 1;
                     }

                     String tierKey = tier == 2 ? "tier2" : "tier1";
                     int repairAmount = this.plugin.getConfig().getInt("passives.wealth." + tierKey + ".armor-mend-amount", 2);
                     ItemStack[] armor = player.getInventory().getArmorContents();
                     boolean repaired = false;

                     for (int i = 0; i < armor.length; i++) {
                        Damageable damageable;
                        int currentDamage;
                        if (armor[i] != null
                           && !armor[i].getType().isAir()
                           && armor[i].getItemMeta() instanceof Damageable
                           && (currentDamage = (damageable = (Damageable)armor[i].getItemMeta()).getDamage()) > 0) {
                           damageable.setDamage(Math.max(0, currentDamage - repairAmount));
                           armor[i].setItemMeta(damageable);
                           repaired = true;
                        }
                     }

                     if (repaired) {
                        player.getInventory().setArmorContents(armor);
                        DustOptions greenDust = new DustOptions(ParticleUtils.WEALTH_GREEN, 0.8F);
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 8, 0.3, 0.3, 0.3, 0.0, greenDust, true);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onWealthDoubleDebris(FurnaceExtractEvent event) {
      Player player = event.getPlayer();
      boolean hasWealth = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH) || this.isHoldingWealthGem(player);
      if (hasWealth) {
         if (this.canUsePassives(player)) {
            int tier = this.plugin.getGemManager().getTierFor(player, GemType.WEALTH);
            String oraxenId;
            if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand())) != null) {
               tier = GemType.getTierFromOraxenId(oraxenId);
            }

            if (tier == 0) {
               tier = 1;
            }

            String tierKey = tier == 2 ? "tier2" : "tier1";
            boolean enabled = this.plugin.getConfig().getBoolean("passives.wealth." + tierKey + ".double-debris", true);
            if (enabled) {
               if (event.getItemType() == Material.NETHERITE_SCRAP) {
                  int extracted = event.getItemAmount();
                  if (extracted > 0) {
                     player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.NETHERITE_SCRAP, extracted));
                     DustOptions greenDust = new DustOptions(ParticleUtils.WEALTH_GREEN, 1.2F);
                     player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 20, 0.3, 0.3, 0.3, 0.0, greenDust, true);
                     player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
                     player.sendMessage("§a§oDouble Debris! Extra netherite scrap!");
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onUnfortunateAttack(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         double attackFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.attack-fail-chance", 0.25);
         if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), attackFailChance)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4F, 1.0F);
            player.sendMessage("§c§oYou can't do that while Unfortunate!");
         }
      }
   }

   @EventHandler
   public void onUnfortunateBlockPlace(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      double blockPlaceFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.block-place-fail-chance", 0.25);
      if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), blockPlaceFailChance)) {
         event.setCancelled(true);
         player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4F, 1.0F);
         player.sendMessage("§c§oYou can't do that while Unfortunate!");
      }
   }

   @EventHandler
   public void onUnfortunateEat(PlayerItemConsumeEvent event) {
      Player player = event.getPlayer();
      double eatFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.eat-fail-chance", 0.25);
      if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), eatFailChance)) {
         event.setCancelled(true);
         player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4F, 1.0F);
         player.sendMessage("§c§oYou can't do that while Unfortunate!");
      }
   }

   @EventHandler
   public void onUnfortunateBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      double blockBreakFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.block-break-fail-chance", 0.25);
      if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), blockBreakFailChance)) {
         event.setCancelled(true);
         player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4F, 1.0F);
         player.sendMessage("§c§oYou can't do that while Unfortunate!");
      }
   }

   @EventHandler
   public void onUnfortunateBow(EntityShootBowEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         double bowFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.bow-fail-chance", 0.25);
         if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), bowFailChance)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4F, 1.0F);
            player.sendMessage("§c§oYou can't do that while Unfortunate!");
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onUnfortunateTotem(EntityResurrectEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         double totemFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.totem-fail-chance", 0.25);
         if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), totemFailChance)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 0.8F);
            player.sendMessage("§c§oYour Totem of Undying failed to activate due to Unfortunate!");
         }
      }
   }

   @EventHandler
   public void onItemLockUse(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      if (WealthAbilities.isItemLocked(player.getUniqueId())) {
         ItemStack heldItem = player.getInventory().getItemInMainHand();
         ItemStack lockedItem = WealthAbilities.getLockedItem(player.getUniqueId());
         if (lockedItem != null && heldItem != null) {
            if (heldItem.isSimilar(lockedItem)) {
               event.setCancelled(true);
               player.sendMessage("§c§oThis item is locked!");
            }
         }
      }
   }

   @EventHandler
   public void onItemLockSwitch(PlayerItemHeldEvent event) {
      Player player = event.getPlayer();
      if (WealthAbilities.isItemLocked(player.getUniqueId())) {
         ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
         ItemStack lockedItem = WealthAbilities.getLockedItem(player.getUniqueId());
         if (lockedItem != null && newItem != null) {
            if (newItem.isSimilar(lockedItem)) {
               player.sendMessage("§c§oWarning: This item is locked!");
            }
         }
      }
   }

   @EventHandler
   public void onStrengthChadHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         if (event.getEntity() instanceof LivingEntity) {
            boolean hasActiveChad = this.plugin.getStrengthAbilities() != null && this.plugin.getStrengthAbilities().getChadHitsRemaining(player) > 0;
            boolean hasStrength = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.STRENGTH)
               || this.isHoldingStrengthGem(player)
               || hasActiveChad;
            if (hasStrength) {
               if (this.canUsePassives(player)) {
                  int tier = this.plugin.getGemManager().getTierFor(player, GemType.STRENGTH);
                  String oraxenId;
                  if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand())) != null) {
                     tier = GemType.getTierFromOraxenId(oraxenId);
                  }

                  if (tier >= 2 || hasActiveChad) {
                     double bonusDamage = this.plugin.getStrengthAbilities().consumeChadBonus(player);
                     if (!(bonusDamage <= 0.0)) {
                        event.setDamage(event.getDamage() + bonusDamage);
                        LivingEntity target = (LivingEntity)event.getEntity();
                        DustOptions redDust = new DustOptions(ParticleUtils.STRENGTH_RED, 2.0F);
                        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 50, 0.8, 0.8, 0.8, 0.0, redDust, true);
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.3);
                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0, 1.0, 0.0), 10, 0.5, 0.5, 0.5);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onRichRushBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      if (WealthAbilities.hasRichRush(player.getUniqueId())) {
         if (!event.isCancelled()) {
            Material type = event.getBlock().getType();
            if (this.isOreBlock(type)) {
               ItemStack tool = player.getInventory().getItemInMainHand();
               if (tool == null || !tool.hasItemMeta() || !tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
                  if (type != Material.ANCIENT_DEBRIS) {
                     for (ItemStack drop : new ArrayList(event.getBlock().getDrops(player.getInventory().getItemInMainHand(), player))) {
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop.clone());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isOreBlock(Material type) {
      return type == Material.COAL_ORE
         || type == Material.DEEPSLATE_COAL_ORE
         || type == Material.IRON_ORE
         || type == Material.DEEPSLATE_IRON_ORE
         || type == Material.GOLD_ORE
         || type == Material.DEEPSLATE_GOLD_ORE
         || type == Material.NETHER_GOLD_ORE
         || type == Material.COPPER_ORE
         || type == Material.DEEPSLATE_COPPER_ORE
         || type == Material.DIAMOND_ORE
         || type == Material.DEEPSLATE_DIAMOND_ORE
         || type == Material.EMERALD_ORE
         || type == Material.DEEPSLATE_EMERALD_ORE
         || type == Material.LAPIS_ORE
         || type == Material.DEEPSLATE_LAPIS_ORE
         || type == Material.REDSTONE_ORE
         || type == Material.DEEPSLATE_REDSTONE_ORE
         || type == Material.NETHER_QUARTZ_ORE
         || type == Material.ANCIENT_DEBRIS;
   }

   @EventHandler
   public void onRichRushEntityDeath(EntityDeathEvent event) {
      if (!(event.getEntity() instanceof Player)) {
         Player killer = event.getEntity().getKiller();
         if (killer != null) {
            if (WealthAbilities.hasRichRush(killer.getUniqueId())) {
               EntityType entityType = event.getEntity().getType();
               if (entityType != EntityType.DONKEY && entityType != EntityType.MULE && entityType != EntityType.LLAMA && entityType != EntityType.TRADER_LLAMA) {
                  for (ItemStack drop : new ArrayList(event.getDrops())) {
                     if (this.isNaturalMobDrop(drop)) {
                        event.getDrops().add(drop.clone());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isNaturalMobDrop(ItemStack item) {
      if (item == null || item.getType().isAir()) {
         return false;
      }

      if (CustomItemManager.isUndroppable(item)) {
         return false;
      }

      String customId = CustomItemManager.getIdByItem(item);
      if (customId != null && this.plugin.getGemManager().isAnyGem(customId)) {
         return false;
      }

      Material type = item.getType();
      if (type.name().contains("HELMET")
         || type.name().contains("CHESTPLATE")
         || type.name().contains("LEGGINGS")
         || type.name().contains("BOOTS")
         || type.name().contains("SWORD")
         || type.name().contains("AXE")
         || type.name().contains("PICKAXE")
         || type.name().contains("SHOVEL")
         || type.name().contains("HOE")
         || type.name().contains("BOW")
         || type.name().contains("CROSSBOW")
         || type.name().contains("TRIDENT")
         || type.name().contains("SHIELD")) {
         return false;
      }

      if (!type.name().contains("SHULKER")
         && !type.name().contains("CHEST")
         && !type.name().contains("BARREL")
         && !type.name().contains("BUNDLE")
         && type != Material.SADDLE
         && !type.name().contains("HORSE_ARMOR")) {
         ItemMeta meta;
         if (!item.hasItemMeta() || !(meta = item.getItemMeta()).hasDisplayName() && !meta.hasLore()) {
            if (type == Material.ROTTEN_FLESH
               || type == Material.BONE
               || type == Material.SPIDER_EYE
               || type == Material.STRING
               || type == Material.GUNPOWDER
               || type == Material.ENDER_PEARL
               || type == Material.BLAZE_ROD
               || type == Material.GHAST_TEAR
               || type == Material.MAGMA_CREAM
               || type == Material.SLIME_BALL
               || type == Material.PRISMARINE_SHARD
               || type == Material.PRISMARINE_CRYSTALS
               || type == Material.RABBIT_HIDE
               || type == Material.RABBIT_FOOT
               || type == Material.PHANTOM_MEMBRANE
               || type == Material.NAUTILUS_SHELL) {
               return true;
            } else if (type == Material.BEEF
               || type == Material.PORKCHOP
               || type == Material.MUTTON
               || type == Material.CHICKEN
               || type == Material.RABBIT
               || type == Material.COD
               || type == Material.SALMON
               || type == Material.TROPICAL_FISH
               || type == Material.PUFFERFISH) {
               return true;
            } else if (type == Material.COOKED_BEEF
               || type == Material.COOKED_PORKCHOP
               || type == Material.COOKED_MUTTON
               || type == Material.COOKED_CHICKEN
               || type == Material.COOKED_RABBIT
               || type == Material.COOKED_COD
               || type == Material.COOKED_SALMON) {
               return true;
            } else if (type == Material.LEATHER || type == Material.FEATHER || type == Material.INK_SAC || type == Material.GLOW_INK_SAC) {
               return true;
            } else if (type == Material.NETHER_STAR
               || type == Material.WITHER_SKELETON_SKULL
               || type == Material.SKELETON_SKULL
               || type == Material.ZOMBIE_HEAD
               || type == Material.CREEPER_HEAD
               || type == Material.DRAGON_HEAD) {
               return true;
            } else if (type == Material.TOTEM_OF_UNDYING || type == Material.ELYTRA || type == Material.DRAGON_BREATH) {
               return true;
            } else if (type == Material.ARROW || type == Material.EGG || type == Material.SNOWBALL) {
               return true;
            } else if (type == Material.ECHO_SHARD || type == Material.SCULK_CATALYST) {
               return true;
            } else {
               return type == Material.SNIFFER_EGG
                  ? true
                  : type == Material.OCHRE_FROGLIGHT || type == Material.PEARLESCENT_FROGLIGHT || type == Material.VERDANT_FROGLIGHT;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @EventHandler
   public void onFluxFlowBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
         if (this.canUsePassives(player)) {
            this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.BLOCK_BREAK);
         }
      }
   }

   @EventHandler
   public void onFluxFlowAttack(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            if (this.canUsePassives(player)) {
               this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.ATTACK);
            }
         }
      }
   }

   @EventHandler
   public void onFluxFlowArrowShoot(ProjectileLaunchEvent event) {
      Projectile projectile = event.getEntity();
      if (projectile instanceof Arrow) {
         if (projectile.getShooter() instanceof Player player) {
            if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
               if (this.canUsePassives(player)) {
                  this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.ARROW_SHOOT);
               }
            }
         }
      }
   }

   @EventHandler
   public void onFluxFlowSprint(PlayerToggleSprintEvent event) {
      Player player = event.getPlayer();
      if (event.isSprinting()) {
         if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            if (this.canUsePassives(player)) {
               this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.SPRINT);
            }
         }
      }
   }

   @EventHandler
   public void onFluxFlowJump(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (!(event.getTo().getY() <= event.getFrom().getY())) {
         if (player.isOnGround()) {
            if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
               if (this.canUsePassives(player)) {
                  this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.JUMP);
               }
            }
         }
      }
   }

   private boolean canUsePassives(Player player) {
      if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(player)) {
         return false;
      } else {
         return !this.plugin.getEnergyManager().arePassivesActive(player)
            ? false
            : this.plugin.getRegionManager() == null || !this.plugin.getRegionManager().areGemsDisabled(player);
      }
   }
}
