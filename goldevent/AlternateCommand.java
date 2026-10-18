package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AlternateCommand implements CommandExecutor {
   private static final long COOLDOWN_MS = 120000L;
   private final GoldenDream dream;
   private final Map<UUID, Long> next = new HashMap<>();

   public AlternateCommand(GoldenDream dream) {
      this.dream = dream;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player p) {
         if (!this.dream.roster().isMemory(p.getUniqueId()) && !p.hasPermission("blissgems.admin")) {
            p.sendMessage(PedestalManager.color("&cOnly memories can do that."));
            return true;
         } else {
            long now = System.currentTimeMillis();
            Long until = this.next.get(p.getUniqueId());
            if (until != null && until > now) {
               p.sendMessage(PedestalManager.color("&7Wait &6" + ((until - now) / 1000L + 1L) + "s&7."));
               return true;
            } else {
               this.next.put(p.getUniqueId(), now + 120000L);
               p.getWorld().playSound(p.getLocation(), "bliss:alternate", SoundCategory.MASTER, 1.0F, 1.0F);
               return true;
            }
         }
      } else {
         sender.sendMessage("In-game only.");
         return true;
      }
   }
}
