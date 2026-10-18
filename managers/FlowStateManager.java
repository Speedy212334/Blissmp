package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FlowStateManager {
   private final BlissGems plugin;
   private final Map<UUID, FlowStateManager.FlowStateData> playerFlowStates = new HashMap<>();
   private static final int ACTION_TIMEOUT_MS = 3000;
   private static final int MAX_FLOW_LEVEL = 5;

   public FlowStateManager(BlissGems plugin) {
      this.plugin = plugin;
   }

   public void registerAction(Player player, FlowStateManager.ActionType actionType) {
      UUID uuid = player.getUniqueId();
      FlowStateManager.FlowStateData flowData = this.playerFlowStates.computeIfAbsent(uuid, k -> new FlowStateManager.FlowStateData());
      long currentTime = System.currentTimeMillis();
      if (flowData.getLastActionType() == actionType) {
         long timeSinceLastAction = currentTime - flowData.getLastActionTime();
         if (timeSinceLastAction <= 3000L) {
            int newLevel = Math.min(flowData.getFlowLevel() + 1, 5);
            flowData.setFlowLevel(newLevel);
            this.applyFlowStateEffects(player, newLevel);
            if (newLevel > flowData.getFlowLevel()) {
               player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 10 + newLevel * 2, 0.3, 0.5, 0.3, 0.05);
               if (newLevel == 5) {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
                  player.sendMessage("§b§l⚡ MAX FLOW STATE!");
               } else if (newLevel % 2 == 0) {
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 1.5F + newLevel * 0.2F);
               }
            }
         } else {
            flowData.setFlowLevel(1);
            this.applyFlowStateEffects(player, 1);
         }
      } else {
         flowData.setFlowLevel(1);
         this.applyFlowStateEffects(player, 1);
      }

      flowData.setLastActionType(actionType);
      flowData.setLastActionTime(currentTime);
   }

   private void applyFlowStateEffects(Player player, int level) {
      if (level > 0) {
         int maxSpeedLevel = this.plugin.getConfig().getInt("passives.flux.max-flow-speed-level", 0);
         int speedLevel = Math.min((level + 1) / 2, maxSpeedLevel);
         player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, speedLevel, false, false));
         if (level >= 3) {
            int hasteLevel = level >= 4 ? 1 : 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, hasteLevel, false, false));
         }

         if (level >= 3) {
            player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 0.5, 0.0), 5, 0.3, 0.3, 0.3, 0.01);
         }
      }
   }

   public int getFlowLevel(Player player) {
      FlowStateManager.FlowStateData data = this.playerFlowStates.get(player.getUniqueId());
      if (data == null) {
         return 0;
      }

      long timeSinceLastAction = System.currentTimeMillis() - data.getLastActionTime();
      return timeSinceLastAction > 3000L ? 0 : data.getFlowLevel();
   }

   public void resetFlowState(Player player) {
      this.playerFlowStates.remove(player.getUniqueId());
   }

   public void clearFlowState(UUID playerId) {
      this.playerFlowStates.remove(playerId);
   }

   public enum ActionType {
      BLOCK_BREAK,
      ARROW_SHOOT,
      ATTACK,
      SPRINT,
      JUMP;
   }

   private static class FlowStateData {
      private FlowStateManager.ActionType lastActionType = null;
      private long lastActionTime = 0L;
      private int flowLevel = 0;

      public FlowStateManager.ActionType getLastActionType() {
         return this.lastActionType;
      }

      public void setLastActionType(FlowStateManager.ActionType lastActionType) {
         this.lastActionType = lastActionType;
      }

      public long getLastActionTime() {
         return this.lastActionTime;
      }

      public void setLastActionTime(long lastActionTime) {
         this.lastActionTime = lastActionTime;
      }

      public int getFlowLevel() {
         return this.flowLevel;
      }

      public void setFlowLevel(int flowLevel) {
         this.flowLevel = flowLevel;
      }
   }
}
