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
        new BukkitRunnable() {
            Player player = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);
            public void run() {
                if (cmd.getName().equalsIgnoreCase("track")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("This command can only be used by a player!");
                        return;
                    }

                    if (args.length == 0) {
                        sender.sendMessage("Usage: /track <player>");
                        return;
                    }


                    if (target == null) {
                        sender.sendMessage("Player not found!");
                        return;
                    }

                    if (player.getWorld().getEnvironment() == World.Environment.NORMAL && target.getWorld().getEnvironment() == World.Environment.NORMAL) {
                        player.setCompassTarget(target.getLocation());
                        player.sendMessage("Compass is now pointing towards " + target.getName() + " in the overworld!");
                    } else if (player.getWorld().getEnvironment() == World.Environment.NETHER && target.getWorld().getEnvironment() == World.Environment.NETHER) {
                        ItemStack playersItem = player.getInventory().getItemInMainHand();

                        if (playersItem.getType() == Material.COMPASS) {
                            CompassMeta compassMeta = (CompassMeta) playersItem.getItemMeta();
                            compassMeta.setLodestone(target.getLocation());
                            compassMeta.setLodestoneTracked(true);
                            playersItem.setItemMeta(compassMeta);
                            player.sendMessage("Your compass is now tracking " + target.getName() + " in the nether!");
                        } else {
                            player.sendMessage("You need to hold a compass to use this command in the nether!");
                        }
                    } else {
                        player.sendMessage("The target is not in the same dimension as you!");
                    }
                }
            }
        }.runTaskTimer((Plugin)this, 0L, 0L);

        return true;
    }
}
