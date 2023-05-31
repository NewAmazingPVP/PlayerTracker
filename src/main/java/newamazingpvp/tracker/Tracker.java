package newamazingpvp.tracker;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Tracker extends JavaPlugin implements CommandExecutor, Listener {

    private final HashMap<UUID, UUID> trackingPlayers = new HashMap<>();
    private final HashMap<UUID, Location> lastPortalLocations = new HashMap<>();
    private boolean logOffTracking;
    private FileConfiguration config;

    public void onEnable() {
        config = getConfig();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();

                FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);

                configuration.set("log_off_tracking.enabled", true);
                configuration.set("portal_tracking.enabled", true);
                configuration.set("give_compass_with_command.enabled", false);
                configuration.set("give_compass_with_command.enabled", false);
                configuration.set("tracking_message", "&aTracking &2&l{target_name}");

                configuration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logOffTracking = getConfig().getBoolean("log_off_tracking.enabled");



        Objects.requireNonNull(getCommand("track")).setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        compassUpdate();
    }

    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        lastPortalLocations.put(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!config.getBoolean("logoff_tracking")){
            trackingPlayers.remove(event.getPlayer().getUniqueId());
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("playerTracker.track")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

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

            if (!config.getBoolean("noCompass_tracking"))
                if (getCompassFromInventory(player) == null) {
                    if (config.getBoolean("giveCompass")){
                        player.getInventory().addItem(new ItemStack(Material.COMPASS));
                    } else {
                        sender.sendMessage(ChatColor.RED + "You need a compass in your inventory to use this command!");
                        return true;
                    }
            }

            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            // Check if the player and target are in the same dimension
            if (!config.getBoolean("multiDimensional_tracking")){
                if (player.getWorld() != target.getWorld()) {
                    sender.sendMessage(ChatColor.RED + "The target is not in the same dimension as you!");
                    return true;
                }
            }

            trackingPlayers.put(player.getUniqueId(), target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Compass is now pointing towards " + target.getName());
            return true;
        }

        return false;
    }

    private void compassUpdate() {
        new BukkitRunnable() {
            public void run() {
                for (UUID playerUUID : trackingPlayers.keySet()) {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        Player target = Bukkit.getPlayer(trackingPlayers.get(playerUUID));
                        ItemStack compass = getCompassFromInventory(player);
                        if (compass != null) {
                            if (target != null) {
                                if (player.getWorld().getEnvironment() == World.Environment.NORMAL && target.getWorld().getEnvironment() == World.Environment.NORMAL) {
                                    setNormalCompass(compass);
                                    player.setCompassTarget(target.getLocation());
                                } else if (player.getWorld() == target.getWorld()) {
                                    setLodestoneCompass(compass, target.getLocation());
                                } else {
                                    Location portalLocation = lastPortalLocations.get(target.getUniqueId());
                                    if (portalLocation != null && player.getWorld() == portalLocation.getWorld()) {
                                        setLodestoneCompass(compass, portalLocation);
                                    }
                                }
                                String message = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(config.getString("tracking_message"))).replace("{target_name}", target.getName());
                                TextComponent textComponent = new TextComponent(message);
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, textComponent);
                            } else {
                                if (!config.getBoolean("logoff_tracking")){
                                    setNormalCompass(compass);
                                    player.setCompassTarget(generateRandomLocation(player));
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 0L); // Update interval for normal compasses

        new BukkitRunnable() {
            public void run() {
                for (UUID playerUUID : trackingPlayers.keySet()) {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        ItemStack compass = getCompassFromInventory(player);
                        if (compass != null) {
                            CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
                            assert compassMeta != null;
                            boolean isLodestone = compassMeta.isLodestoneTracked();
                            if (isLodestone) {
                                Player target = Bukkit.getPlayer(trackingPlayers.get(playerUUID));
                                if (target != null && player.getWorld() == target.getWorld()) {
                                    setLodestoneCompass(compass, target.getLocation());
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, config.getLong("lodestoneCompass_updateInterval"));
    }



    private Location generateRandomLocation(Player player) {
        int offsetX = (int) (Math.random() * 201) - 100;
        int offsetZ = (int) (Math.random() * 201) - 100;
        Location playerLocation = player.getLocation();
        int x = playerLocation.getBlockX() + offsetX;
        int z = playerLocation.getBlockZ() + offsetZ;
        return new Location(player.getWorld(), x, 64, z);
    }

    private ItemStack getCompassFromInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                return item;
            }
        }
        return null;
    }

    private void setNormalCompass(ItemStack compass) {
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        assert compassMeta != null;
        if (compassMeta.isLodestoneTracked()) {
            compassMeta.setLodestone(null);
            compassMeta.setLodestoneTracked(false);
            compass.setItemMeta(compassMeta);
        }
    }
    private void setLodestoneCompass(ItemStack compass, Location location) {
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        assert compassMeta != null;
        compassMeta.setLodestone(location);
        compassMeta.setLodestoneTracked(true);
        compass.setItemMeta(compassMeta);
    }

    public FileConfiguration getConfig() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            FileConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
            return configuration;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
