package nl.dutchcoding.blockparty;

import nl.dutchcoding.blockparty.commands.BlockPartyCommand;
import nl.dutchcoding.blockparty.listeners.PlayerListener;
import nl.dutchcoding.blockparty.listeners.SignListener;
import nl.dutchcoding.blockparty.managers.*;
import nl.dutchcoding.blockparty.placeholders.BlockPartyPlaceholders;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockParty extends JavaPlugin {

    private static BlockParty instance;
    
    private ConfigManager configManager;
    private MessageManager messageManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private SignManager signManager;
    private StatsManager statsManager;
    private ScoreboardManager scoreboardManager;
    private SoundManager soundManager;
    private InventoryManager inventoryManager;
    private RewardManager rewardManager;
    private LobbyItemManager lobbyItemManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.arenaManager = new ArenaManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.gameManager = new GameManager(this);
        this.signManager = new SignManager(this);
        this.statsManager = new StatsManager(this);
        this.soundManager = new SoundManager(this);
        this.inventoryManager = new InventoryManager();
        this.rewardManager = new RewardManager(this);
        this.lobbyItemManager = new LobbyItemManager(this);
        
        // Register commands
        getCommand("blockparty").setExecutor(new BlockPartyCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);

        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BlockPartyPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion registered!");
        }
        
        getLogger().info("BlockParty has been enabled!");
    }

    @Override
    public void onDisable() {
        // Stop all active games
        if (gameManager != null) {
            gameManager.stopAllGames();
        }
        
        // Save data
        if (arenaManager != null) {
            arenaManager.saveArenas();
        }
        if (statsManager != null) {
            statsManager.saveStats();
        }
        if (signManager != null) {
            signManager.saveSigns();
        }
        
        // Clear saved inventories on shutdown
        if (inventoryManager != null) {
            inventoryManager.clearAll();
        }
        
        // Clean up sound manager tasks
        if (soundManager != null) {
            soundManager.cleanup();
        }
        
        getLogger().info("BlockParty has been disabled!");
    }
    
    public static BlockParty getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public SignManager getSignManager() {
        return signManager;
    }
    
    public StatsManager getStatsManager() {
        return statsManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public LobbyItemManager getLobbyItemManager() {
        return lobbyItemManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public SoundManager getSoundManager() {
        return soundManager;
    }
    
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}