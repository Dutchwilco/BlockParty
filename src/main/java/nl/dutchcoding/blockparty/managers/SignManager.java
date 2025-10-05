package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.Arena;
import nl.dutchcoding.blockparty.models.Game;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SignManager {
    
    private final BlockParty plugin;
    private final Map<Location, String> joinSigns;
    private File signsFile;
    private FileConfiguration signsConfig;
    
    public SignManager(BlockParty plugin) {
        this.plugin = plugin;
        this.joinSigns = new HashMap<>();
        loadSigns();
        startSignUpdater();
    }
    
    public void loadSigns() {
        signsFile = new File(plugin.getDataFolder(), "signs.yml");
        if (!signsFile.exists()) {
            try {
                signsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create signs.yml!");
                return;
            }
        }
        
        signsConfig = YamlConfiguration.loadConfiguration(signsFile);
        
        if (signsConfig.contains("signs")) {
            for (String key : signsConfig.getConfigurationSection("signs").getKeys(false)) {
                Location location = signsConfig.getLocation("signs." + key + ".location");
                String arena = signsConfig.getString("signs." + key + ".arena");
                if (location != null && arena != null) {
                    joinSigns.put(location, arena);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + joinSigns.size() + " join signs.");
    }
    
    public void saveSigns() {
        signsConfig.set("signs", null); // Clear existing data
        
        int i = 0;
        for (Map.Entry<Location, String> entry : joinSigns.entrySet()) {
            signsConfig.set("signs." + i + ".location", entry.getKey());
            signsConfig.set("signs." + i + ".arena", entry.getValue());
            i++;
        }
        
        try {
            signsConfig.save(signsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save signs.yml!");
        }
    }
    
    public void createSign(Location location, String arenaName) {
        joinSigns.put(location, arenaName);
        updateSign(location);
        saveSigns();
    }
    
    public void removeSign(Location location) {
        joinSigns.remove(location);
        saveSigns();
    }
    
    public String getArenaFromSign(Location location) {
        return joinSigns.get(location);
    }
    
    public boolean isJoinSign(Location location) {
        return joinSigns.containsKey(location);
    }
    
    private void startSignUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Location signLocation : joinSigns.keySet()) {
                    updateSign(signLocation);
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Update every second
    }
    
    private void updateSign(Location location) {
        Block block = location.getBlock();
        if (!(block.getState() instanceof Sign)) {
            joinSigns.remove(location);
            return;
        }
        
        Sign sign = (Sign) block.getState();
        String arenaName = joinSigns.get(location);
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            sign.setLine(0, "§c[BlockParty]");
            sign.setLine(1, "§cArena not found");
            sign.setLine(2, "§c" + arenaName);
            sign.setLine(3, "");
            sign.update();
            return;
        }
        
        Game game = plugin.getGameManager().getActiveGames().get(arenaName);
        String status;
        String playerCount;
        
        if (game == null) {
            status = "§aWaiting";
            playerCount = "0/" + arena.getMaxPlayers();
        } else {
            switch (game.getState()) {
                case WAITING:
                    status = "§aWaiting";
                    break;
                case STARTING:
                    status = "§eStarting";
                    break;
                case PLAYING:
                    status = "§6Playing";
                    break;
                case ENDING:
                    status = "§cEnding";
                    break;
                default:
                    status = "§7Unknown";
                    break;
            }
            playerCount = game.getPlayers().size() + "/" + arena.getMaxPlayers();
        }
        
        sign.setLine(0, "§b[BlockParty]");
        sign.setLine(1, status);
        sign.setLine(2, playerCount);
        sign.setLine(3, arena.getName());
        sign.update();
    }
}