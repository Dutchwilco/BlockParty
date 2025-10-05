package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    
    private final BlockParty plugin;
    private FileConfiguration config;
    private FileConfiguration timersConfig;
    private FileConfiguration soundsConfig;
    private File configFile;
    private File timersFile;
    private File soundsFile;
    
    public ConfigManager(BlockParty plugin) {
        this.plugin = plugin;
        loadConfigs();
    }
    
    public void loadConfigs() {
        createConfigFiles();
        loadDefaultConfigs();
    }
    
    private void createConfigFiles() {
        // Main config
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Timers config
        timersFile = new File(plugin.getDataFolder(), "timers.yml");
        if (!timersFile.exists()) {
            plugin.saveResource("timers.yml", false);
        }
        timersConfig = YamlConfiguration.loadConfiguration(timersFile);
        
        // Sounds config
        soundsFile = new File(plugin.getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            plugin.saveResource("sounds.yml", false);
        }
        soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);
    }
    
    private void loadDefaultConfigs() {
        // Set default values if not present
        if (!config.contains("hub-location")) {
            config.set("hub-location", null);
        }
        
        if (!config.contains("countdown-time")) {
            config.set("countdown-time", 30);
        }
        
        if (!config.contains("blocks")) {
            config.set("blocks", java.util.Arrays.asList(
                "RED_WOOL", "BLUE_WOOL", "GREEN_WOOL", "YELLOW_WOOL", 
                "ORANGE_WOOL", "PURPLE_WOOL", "PINK_WOOL", "LIGHT_BLUE_WOOL",
                "LIME_WOOL", "MAGENTA_WOOL", "CYAN_WOOL", "WHITE_WOOL",
                "BLACK_WOOL", "GRAY_WOOL", "LIGHT_GRAY_WOOL", "BROWN_WOOL"
            ));
        }
        
        saveConfig();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml!");
        }
    }
    
    public void setHubLocation(Location location) {
        config.set("hub-location", location);
        saveConfig();
    }
    
    public Location getHubLocation() {
        return config.getLocation("hub-location");
    }
    
    public int getCountdownTime() {
        return config.getInt("countdown-time", 30);
    }
    
    public java.util.List<String> getGameBlocks() {
        return config.getStringList("blocks");
    }
    
    // Timer configurations
    public int getRoundTime(int round) {
        return timersConfig.getInt("round-times.round-" + getRoundGroup(round), getDefaultRoundTime(round));
    }
    
    private int getRoundGroup(int round) {
        if (round <= 3) return 1;
        if (round <= 6) return 4;
        if (round <= 9) return 7;
        if (round <= 13) return 10;
        if (round <= 16) return 14;
        return 17;
    }
    
    private int getDefaultRoundTime(int round) {
        if (round <= 3) return 12;
        if (round <= 6) return 9;
        if (round <= 9) return 6;
        if (round <= 13) return 4;
        if (round <= 16) return 2;
        return 1;
    }
    
    // Sound configurations
    public String getGameMusicSound() {
        return soundsConfig.getString("game-music", "MUSIC_DISC_BLOCKS");
    }
    
    public String getDeathSound() {
        return soundsConfig.getString("death-sound", "ENTITY_LIGHTNING_BOLT_THUNDER");
    }
    
    public String getWinSound() {
        return soundsConfig.getString("win-sound", "ENTITY_PLAYER_LEVELUP");
    }
    
    public String getCountdownSound() {
        return soundsConfig.getString("countdown-sound", "BLOCK_NOTE_BLOCK_PLING");
    }
    
    public String getStartSound() {
        return soundsConfig.getString("start-sound", "ENTITY_ENDER_DRAGON_GROWL");
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getTimersConfig() {
        return timersConfig;
    }
    
    public FileConfiguration getSoundsConfig() {
        return soundsConfig;
    }
}