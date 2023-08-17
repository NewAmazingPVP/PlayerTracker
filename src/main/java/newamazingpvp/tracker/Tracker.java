package newamazingpvp.tracker;

import com.earth2me.essentials.EssentialsTimer;
import com.earth2me.essentials.IEssentials;
import com.github.sirblobman.combatlogx.api.ICombatLogX;
import com.github.sirblobman.combatlogx.api.manager.ICombatManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Tracker extends JavaPlugin implements CommandExecutor, Listener {

    private final HashMap<UUID, UUID> trackingPlayers = new HashMap<>();
    private final HashMap<UUID, Location> lastPortalLocations = new HashMap<>();
    private boolean logOffTracking;

    private final LocalDateTime serverStartTime = LocalDateTime.of(2023, 7, 27, 11, 30);

    public void onEnable() {
        if (!getDataFolder().exists()) {getDataFolder().mkdir();}
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


    @EventHandler
    public void playerServerAge(PlayerChatEvent event) {
        String message = event.getMessage().toLowerCase(); // Convert to lowercase
        List<String> phrasesToMatch = Arrays.asList(
                "how old is the server",
                "when did the server start",
                "how long has the server been",
                "what's the server's age",
                "since when has the server been running",
                "tell me the server's age",
                "from when has the server been active",
                "when was this server created",
                "how much time has the server been online",
                "when was the server initiated",
                "for how long has the server been live",
                "what is the server's founding date",
                "how much time has passed since the server's beginning",
                "when was this server brought into being",
                "when did the server first become active",
                "from what time has the server been up",
                "when did this server open up",
                "how old is server",
                "how long has the server been",
                "how old server",
                "how old is this server",
                "when did server start",
                "server runtime",
                "what is server age",
                "server age"
        );

        for (String phrase : phrasesToMatch) {
            if (message.contains(phrase)) {
                LocalDateTime currentTime = LocalDateTime.now();
                Duration duration = Duration.between(serverStartTime, currentTime);
                long days = duration.toDays();
                long hours = duration.toHoursPart();
                long minutes = duration.toMinutesPart();

                String uptimeMessage = String.format(
                        "The server started on" + ChatColor.AQUA + " 7/27/23 12:00pm est" + ChatColor.WHITE + " and has been up for" + ChatColor.GOLD + " %d days, %d hours, and %d minutes.",
                        days, hours, minutes
                );

                Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                    @Override
                    public void run() {
                        for(Player p : Bukkit.getServer().getOnlinePlayers()) {
                            p.sendMessage(uptimeMessage);
                        }
                    }
                }, 20);
                break;
            }
        }
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
                                int distance;
                                if (player.getWorld().getEnvironment() == target.getWorld().getEnvironment()) {
                                    distance = (int) player.getLocation().distance(target.getLocation());
                                } else {
                                    Location portalLocation = lastPortalLocations.get(target.getUniqueId());
                                    if (portalLocation != null && player.getWorld() == portalLocation.getWorld()) {
                                        distance = (int) player.getLocation().distance(portalLocation);
                                    } else {
                                        distance = -1;
                                    }
                                }
                                String message;
                                if (distance >= 0) {
                                    message = ChatColor.GREEN + "Tracking " + ChatColor.BOLD + target.getName() + " " + ChatColor.AQUA + distance + ChatColor.GREEN + " blocks away" ;
                                } else {
                                    message = ChatColor.RED + "Cannot measure the distance to the player because they are in a different dimension and haven't used a portal yet";
                                }
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
