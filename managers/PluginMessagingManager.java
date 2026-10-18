package dev.xoperr.blissgems.managers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class PluginMessagingManager implements PluginMessageListener {
   private final BlissGems plugin;
   public static final String ABILITY_CHANNEL = "blissgems:ability";
   public static final String GEM_DATA_CHANNEL = "blissgems:gemdata";

   public PluginMessagingManager(BlissGems plugin) {
      this.plugin = plugin;
      plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "blissgems:ability", this);
      plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "blissgems:gemdata");
   }

   public void onPluginMessageReceived(String channel, Player player, byte[] message) {
      if (channel.equals("blissgems:ability")) {
         ByteArrayDataInput in = ByteStreams.newDataInput(message);
         String abilityType = in.readUTF();
         if (!this.plugin.getGemManager().hasActiveGem(player)) {
            player.sendMessage("§c§lYou don't have a gem!");
         } else {
            int energy = this.plugin.getEnergyManager().getEnergy(player);
            if (energy <= 0) {
               String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy");
               if (msg != null && !msg.isEmpty()) {
                  player.sendMessage(msg);
               }
            } else {
               GemType gemType = this.plugin.getGemManager().getGemType(player);
               int tier = this.plugin.getGemManager().getGemTier(player);
               if (gemType != null) {
                  switch (abilityType) {
                     case "main":
                        this.handleMainAbility(player, gemType, tier);
                        break;
                     case "secondary":
                        if (tier < 2) {
                           player.sendMessage("§c§lSecondary abilities require Tier 2 gem!");
                           return;
                        }

                        this.handleSecondaryAbility(player, gemType, tier);
                        break;
                     case "tertiary":
                        if (tier < 2) {
                           player.sendMessage("§c§lTertiary abilities require Tier 2 gem!");
                           return;
                        }

                        this.handleTertiaryAbility(player, gemType, tier);
                  }
               }
            }
         }
      }
   }

   private void handleMainAbility(Player player, GemType gemType, int tier) {
      switch (gemType) {
         case ASTRA:
            this.plugin.getAstraAbilities().onRightClick(player, tier);
            break;
         case FIRE:
            this.plugin.getFireAbilities().onRightClick(player, tier);
            break;
         case FLUX:
            this.plugin.getFluxAbilities().onRightClick(player, tier);
            break;
         case LIFE:
            this.plugin.getLifeAbilities().onRightClick(player, tier);
            break;
         case PUFF:
            this.plugin.getPuffAbilities().onRightClick(player, tier);
            break;
         case SPEED:
            this.plugin.getSpeedAbilities().onRightClick(player, tier);
            break;
         case STRENGTH:
            this.plugin.getStrengthAbilities().onRightClick(player, tier);
            break;
         case WEALTH:
            this.plugin.getWealthAbilities().onRightClick(player, tier);
      }
   }

   private void handleSecondaryAbility(Player player, GemType gemType, int tier) {
      switch (gemType) {
         case ASTRA:
            this.plugin.getAstraAbilities().onRightClick(player, tier);
            break;
         case FIRE:
            this.plugin.getFireAbilities().onRightClick(player, tier);
            break;
         case FLUX:
            this.plugin.getFluxAbilities().onRightClick(player, tier);
            break;
         case LIFE:
            this.plugin.getLifeAbilities().onRightClick(player, tier);
            break;
         case PUFF:
            this.plugin.getPuffAbilities().onRightClick(player, tier);
            break;
         case SPEED:
            this.plugin.getSpeedAbilities().onRightClick(player, tier);
            break;
         case STRENGTH:
            this.plugin.getStrengthAbilities().onRightClick(player, tier);
            break;
         case WEALTH:
            this.plugin.getWealthAbilities().onRightClick(player, tier);
      }
   }

   private void handleTertiaryAbility(Player player, GemType gemType, int tier) {
      switch (gemType) {
         case WEALTH:
            this.plugin.getWealthAbilities().onRightClick(player, tier);
            break;
         default:
            player.sendMessage("§c§lThis gem doesn't have a tertiary ability!");
      }
   }

   public void sendGemData(Player player) {
      if (player.isOnline()) {
         GemType gemType = this.plugin.getGemManager().getGemType(player);
         int tier = this.plugin.getGemManager().getGemTier(player);
         int energy = this.plugin.getEnergyManager().getEnergy(player);
         ByteArrayDataOutput out = ByteStreams.newDataOutput();
         out.writeUTF(gemType != null ? gemType.name() : "NONE");
         out.writeInt(tier);
         out.writeInt(energy);
         player.sendPluginMessage(this.plugin, "blissgems:gemdata", out.toByteArray());
      }
   }

   public void shutdown() {
      this.plugin.getServer().getMessenger().unregisterIncomingPluginChannel(this.plugin, "blissgems:ability");
      this.plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(this.plugin, "blissgems:gemdata");
   }
}
