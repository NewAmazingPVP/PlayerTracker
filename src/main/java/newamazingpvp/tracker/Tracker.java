package newamazingpvp.tracker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.UUID;

public class Tracker extends JavaPlugin implements CommandExecutor {

    private final HashMap<UUID, UUID> trackingPlayers = new HashMap<>();

    public void onEnable() {
        getCommand("track").setExecutor(this);
        startCompassUpdateTask();
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

            trackingPlayers.put(player.getUniqueId(), target.getUniqueId());
            player.sendMessage("Compass is now pointing towards " + target.getName());
            return true;
        }

        return false;
    }

    private void startCompassUpdateTask() {
        new BukkitRunnable() {
            public void run() {
                for (UUID playerUUID : trackingPlayers.keySet()) {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        Player target = Bukkit.getPlayer(trackingPlayers.get(playerUUID));
                        if (target != null && player.getWorld() == target.getWorld()) {
                            ItemStack compass = getCompassFromInventory(player);
                            if (compass != null) {
                                updateCompass(compass, target);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer((Plugin) this, 0L, 0L);
    }

    private ItemStack getCompassFromInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                return item;
            }
        }
        return null;
    }

    private void updateCompass(ItemStack compass, Player target) {
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        compassMeta.setLodestone(target.getLocation());
        compassMeta.setLodestoneTracked(true);
        compass.setItemMeta(compassMeta);
    }
}
