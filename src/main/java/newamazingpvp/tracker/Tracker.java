package newamazingpvp.tracker;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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

public class Tracker extends JavaPlugin implements CommandExecutor, Listener {

    private final HashMap<UUID, UUID> trackingPlayers = new HashMap<>();
    private final HashMap<UUID, Location> lastPortalLocations = new HashMap<>();

    public void onEnable() {
        getCommand("track").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        startCompassUpdateTask();
    }

    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        lastPortalLocations.put(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        trackingPlayers.remove(event.getPlayer().getUniqueId());
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

            // Check if the player and target are in the same dimension
            if (player.getWorld() != target.getWorld()) {
                sender.sendMessage("The target is not in the same dimension as you!");
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
                        if (target != null) {
                            ItemStack compass = getCompassFromInventory(player);
                            if (compass != null) {
                                if (player.getWorld() == target.getWorld()) {
                                    updateCompass(compass, target.getLocation());
                                    String message = ChatColor.GREEN + "Tracking " + ChatColor.DARK_GREEN + ChatColor.BOLD + target.getName();
                                    TextComponent textComponent = new TextComponent(message);
                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, textComponent);
                                } else {
                                    Location portalLocation = lastPortalLocations.get(target.getUniqueId());
                                    if (portalLocation != null && player.getWorld() == portalLocation.getWorld()) {
                                        updateCompass(compass, portalLocation);
                                    }
                                    String message = ChatColor.GREEN + "Tracking " + ChatColor.DARK_GREEN + ChatColor.BOLD + target.getName();
                                    TextComponent textComponent = new TextComponent(message);
                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, textComponent);
                                }
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

    private void updateCompass(ItemStack compass, Location location) {
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        compassMeta.setLodestone(location);
        compassMeta.setLodestoneTracked(true);
        compass.setItemMeta(compassMeta);
    }
}
