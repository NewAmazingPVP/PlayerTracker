package newamazingpvp.tracker;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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

    public void onEnable() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }


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

            if (getCompassFromInventory(player) == null) {
                    sender.sendMessage(ChatColor.RED + "You need a compass in your inventory to use this command!");
                    return true;
            }

            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            long targetPlaytime = getPlaytime(target);
            long requiredPlaytimeTicks = 3 * 60 * 60 * 20;

            if (targetPlaytime < requiredPlaytimeTicks) {
                long remainingTicks = requiredPlaytimeTicks - targetPlaytime;
                long remainingSeconds = remainingTicks / 20;

                int remainingHours = (int) (remainingSeconds / 3600);
                int remainingMinutes = (int) ((remainingSeconds % 3600) / 60);
                int remainingSecondsLeft = (int) (remainingSeconds % 60);

                String remainingTimeMessage = ChatColor.RED + "Cannot track player because they have newbie protection for " +
                        ChatColor.YELLOW + remainingHours + " hours, " +
                        remainingMinutes + " minutes, " +
                        remainingSecondsLeft + " seconds.";

                sender.sendMessage(remainingTimeMessage);
                return true;
            }

            long targetDeathTime = getDeathTime(target);
            long requiredDeathTime = 15 * 60 * 20;

            if (targetDeathTime < requiredDeathTime) {
                long remainingTicks = requiredDeathTime - targetDeathTime;
                long remainingSeconds = remainingTicks / 20;

                int remainingMinutes = (int) ((remainingSeconds % 3600) / 60);
                int remainingSecondsLeft = (int) (remainingSeconds % 60);

                String remainingTimeMessage = ChatColor.RED + "Cannot track because they died recently and have death protection for " +
                        ChatColor.YELLOW + remainingMinutes + " minutes, " +
                        remainingSecondsLeft + " seconds.";

                sender.sendMessage(remainingTimeMessage);
                return true;
            }

            /*if(diamondBlockCount(player) > 0){
                ItemStack block = new ItemStack(Material.DIAMOND_BLOCK, 1);
                player.getInventory().removeItem(block);
                sender.sendMessage(ChatColor.GREEN + "1 diamond block taken to track player");
            } else {
                sender.sendMessage(ChatColor.RED + "You need a diamond block to track player");
                return true;
            }*/

            trackingPlayers.put(player.getUniqueId(), target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Compass is now pointing towards " + target.getName());
            return true;
        }

        return false;
    }

    public static int diamondBlockCount(Player player) {
        int diamondCount = 0;
        PlayerInventory inventory = player.getInventory();

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
                diamondCount += item.getAmount();
            }
        }

        return diamondCount;
    }

    private long getPlaytime(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE);
    }

    private long getDeathTime(Player player) {
        return player.getStatistic(Statistic.TIME_SINCE_DEATH );
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent e){
        Player player = e.getEntity();
        trackingPlayers.remove(player.getUniqueId());
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
                                String message = ChatColor.GREEN + "Tracking " + ChatColor.BOLD + target.getName();
                                TextComponent textComponent = new TextComponent(message);
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, textComponent);
                            } else {
                                setNormalCompass(compass);
                                player.setCompassTarget(generateRandomLocation(player));
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
        }.runTaskTimer(this, 0L, 20L);
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

}
