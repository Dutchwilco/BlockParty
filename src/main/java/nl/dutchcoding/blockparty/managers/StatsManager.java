package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.PlayerStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {
    
    private final BlockParty plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private File statsFile;
    private FileConfiguration statsConfig;
    
    public StatsManager(BlockParty plugin) {
        this.plugin = plugin;
        this.playerStats = new HashMap<>();
        loadStats();
    }
    
    public void loadStats() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create stats.yml!");
                return;
            }
        }
        
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        
        if (statsConfig.contains("stats")) {
            for (String uuidString : statsConfig.getConfigurationSection("stats").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    Map<String, Object> data = statsConfig.getConfigurationSection("stats." + uuidString).getValues(false);
                    data.put("playerId", uuidString);
                    PlayerStats stats = new PlayerStats(data);
                    playerStats.put(playerId, stats);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in stats: " + uuidString);
                }
            }
        }
        
        plugin.getLogger().info("Loaded stats for " + playerStats.size() + " players.");
    }
    
    public void saveStats() {
        statsConfig.set("stats", null); // Clear existing data
        
        for (PlayerStats stats : playerStats.values()) {
            Map<String, Object> data = stats.serialize();
            String uuidString = data.remove("playerId").toString();
            statsConfig.set("stats." + uuidString, data);
        }
        
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats.yml!");
        }
    }
    
    public PlayerStats getPlayerStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, PlayerStats::new);
    }
    
    public Map<UUID, PlayerStats> getAllStats() {
        return playerStats;
    }
}