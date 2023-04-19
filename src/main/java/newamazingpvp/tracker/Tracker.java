package newamazingpvp.tracker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Tracker extends JavaPlugin implements CommandExecutor {

    public void onEnable() {
        getCommand("track").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("track")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                sender.sendMessage("Usage: /track <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage("Player not found!");
                return true;
            }

            if (player.getWorld() == target.getWorld()) {
                player.sendMessage("Compass is now pointing towards " + target.getName());
                ItemStack playersMainItem = player.getInventory().getItemInMainHand();
                ItemStack playersOffItem = player.getInventory().getItemInOffHand();
                if (playersMainItem.getType() == Material.COMPASS) {
                    new BukkitRunnable() {
                        public void run() {
                            CompassMeta compassMeta = (CompassMeta) playersMainItem.getItemMeta();
                            compassMeta.setLodestone(target.getLocation());
                            compassMeta.setLodestoneTracked(true);
                            playersMainItem.setItemMeta(compassMeta);
                        }
                    }.runTaskTimer((Plugin)this, 0L, 0L);
                } else if (playersOffItem.getType() == Material.COMPASS) {
                    new BukkitRunnable() {
                        public void run() {
                            CompassMeta compassMeta = (CompassMeta) playersOffItem.getItemMeta();
                            compassMeta.setLodestone(target.getLocation());
                            compassMeta.setLodestoneTracked(true);
                            playersOffItem.setItemMeta(compassMeta);
                        }
                    }.runTaskTimer((Plugin)this, 0L, 0L);
                } else {
                    player.sendMessage("You need to hold a compass to use this command in the nether!");
                }
            } else {
                player.sendMessage("The target is not in the same dimension as you!");
            }
            return true;
        }

        return false;
    }
}
