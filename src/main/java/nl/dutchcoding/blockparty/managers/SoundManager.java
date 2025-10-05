package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.Game;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SoundManager {
    
    private final BlockParty plugin;
    private final boolean itemsAdderEnabled;
    private final Map<String, BukkitTask> musicTasks; // Arena name -> music task
    private final Map<String, Set<UUID>> activeMusicPlayers; // Arena name -> players hearing music
    private final Map<String, Double> totalMusicProgress; // Arena name -> total cumulative time played
    private final Map<String, Long> roundStartTime; // Arena name -> when current round started
    
    public SoundManager(BlockParty plugin) {
        this.plugin = plugin;
        this.itemsAdderEnabled = plugin.getServer().getPluginManager().isPluginEnabled("ItemsAdder");
        this.musicTasks = new HashMap<>();
        this.activeMusicPlayers = new HashMap<>();
        this.totalMusicProgress = new HashMap<>();
        this.roundStartTime = new HashMap<>();
    }
    
    public void startGameMusic(Game game) {
        String arenaName = game.getArena().getName();
        
        game.setMusicPlaying(true);
        
        // Initialize progress tracking if new game
        if (!totalMusicProgress.containsKey(arenaName)) {
            totalMusicProgress.put(arenaName, 0.0);
        }
        
        // Record when this round started
        roundStartTime.put(arenaName, System.currentTimeMillis());
        
        // Initialize empty set for tracking music players (don't add players yet)
        activeMusicPlayers.put(arenaName, ConcurrentHashMap.newKeySet());
        
        String soundName = plugin.getConfigManager().getGameMusicSound();
        
        // Start continuous music playback
        startContinuousMusic(game, soundName);
        
        plugin.getLogger().info("Started continuous music for arena " + arenaName + 
                               " (Total progress: " + String.format("%.1f", totalMusicProgress.get(arenaName)) + "s)");
    }
    
    public void stopGameMusic(Game game) {
        String arenaName = game.getArena().getName();
        
        // Calculate and add the time this round played
        if (roundStartTime.containsKey(arenaName) && game.isMusicPlaying()) {
            long roundDuration = System.currentTimeMillis() - roundStartTime.get(arenaName);
            double secondsPlayed = roundDuration / 1000.0;
            
            // Add this round's duration to total progress
            totalMusicProgress.put(arenaName, 
                totalMusicProgress.getOrDefault(arenaName, 0.0) + secondsPlayed);
            
            plugin.getLogger().info("Round ended for arena " + arenaName + 
                                  ". Round duration: " + String.format("%.1f", secondsPlayed) + 
                                  "s, Total progress: " + String.format("%.1f", totalMusicProgress.get(arenaName)) + "s");
        }
        
        game.setMusicPlaying(false);
        
        // Cancel the music task
        BukkitTask musicTask = musicTasks.remove(arenaName);
        if (musicTask != null) {
            musicTask.cancel();
        }
        
        // Stop sounds for all players in the game
        Set<UUID> musicPlayers = activeMusicPlayers.get(arenaName);
        if (musicPlayers != null) {
            for (UUID playerId : musicPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    stopMusicForPlayer(player);
                }
            }
        }
        // IMPORTANT: clear tracking so a fresh game doesn't think players still have music
        if (musicPlayers != null) {
            musicPlayers.clear();
        }
        activeMusicPlayers.remove(arenaName);
        
        // Clean up round start time
        roundStartTime.remove(arenaName);
    }
    
    public void pauseGameMusic(Game game) {
        String arenaName = game.getArena().getName();
        
        // Update progress tracking and STOP the actual music during elimination
        if (roundStartTime.containsKey(arenaName) && game.isMusicPlaying()) {
            long roundDuration = System.currentTimeMillis() - roundStartTime.get(arenaName);
            double secondsPlayed = roundDuration / 1000.0;
            
            // Add this round's duration to total progress
            totalMusicProgress.put(arenaName, 
                totalMusicProgress.getOrDefault(arenaName, 0.0) + secondsPlayed);
            
            plugin.getLogger().info("Paused music for arena " + arenaName + 
                                  ". Round duration: " + String.format("%.1f", secondsPlayed) + 
                                  "s, Total progress: " + String.format("%.1f", totalMusicProgress.get(arenaName)) + "s");
        }
        
        // Mark as not playing (stops progress tracking)
        game.setMusicPlaying(false);
        
        // Stop the actual music during elimination phase
        BukkitTask musicTask = musicTasks.get(arenaName);
        if (musicTask != null) {
            musicTask.cancel();
            musicTasks.remove(arenaName);
        }
        
        // Stop music for all players in the game
        Set<UUID> musicPlayers = activeMusicPlayers.get(arenaName);
        if (musicPlayers != null) {
            for (UUID playerId : musicPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    stopMusicForPlayer(player);
                }
            }
        }
        // IMPORTANT: clear tracking so resume can re-play to players next round
        if (musicPlayers != null) {
            musicPlayers.clear();
        }
        activeMusicPlayers.remove(arenaName);
        
        // Clean up round start time
        roundStartTime.remove(arenaName);
    }
    
    public void resumeGameMusic(Game game) {
        String arenaName = game.getArena().getName();
        
        // Resume music playback and progress tracking
        game.setMusicPlaying(true);
        roundStartTime.put(arenaName, System.currentTimeMillis());
        
        // Restart continuous music for the new round
        String soundName = plugin.getConfigManager().getGameMusicSound();
        startContinuousMusic(game, soundName);
        
        plugin.getLogger().info("Resumed music for arena " + arenaName + 
                               " (Total progress: " + String.format("%.1f", totalMusicProgress.getOrDefault(arenaName, 0.0)) + "s)");
    }
    
    public void resetGameMusic(String arenaName) {
        // Reset progress for new game
        totalMusicProgress.put(arenaName, 0.0);
        plugin.getLogger().info("Reset music progress for arena " + arenaName);
    }
    
    private void startContinuousMusic(Game game, String soundName) {
        String arenaName = game.getArena().getName();
        
        // Cancel any existing music task first
        BukkitTask existingTask = musicTasks.get(arenaName);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Ensure we have the music players set for this arena
        Set<UUID> musicPlayers = activeMusicPlayers.get(arenaName);
        if (musicPlayers == null) {
            musicPlayers = ConcurrentHashMap.newKeySet();
            activeMusicPlayers.put(arenaName, musicPlayers);
        }
        
        // Start playing music immediately to all players (but only once per player)
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                // Only play music if this player isn't already hearing it
                if (!musicPlayers.contains(playerId)) {
                    playMusicToPlayer(player, soundName, 0.8f, 1.0f);
                    musicPlayers.add(playerId);
                    plugin.getLogger().info("Started music for player: " + player.getName());
                } else {
                    plugin.getLogger().info("Music already playing for player: " + player.getName() + ", skipping duplicate");
                }
            }
        }
        
        // Create a continuous music task that keeps music playing throughout the entire game
        // This task will NOT restart the music - it only manages new players
        BukkitTask musicTask = new BukkitRunnable() {
            private final double trackDuration = plugin.getConfigManager().getConfig().getDouble("music.track-duration", 180.0);
            private final long trackDurationTicks = Math.round(trackDuration * 20);
            private long tickCounter = 0;
            
            @Override
            public void run() {
                if (!game.isMusicPlaying() || game.getAlivePlayers().isEmpty()) {
                    cancel();
                    musicTasks.remove(arenaName);
                    return;
                }

                // Only restart music if the track duration is reached AND the game is still going
                // This handles very long games that exceed the track duration
                if (tickCounter > 0 && tickCounter % trackDurationTicks == 0) {
                    plugin.getLogger().info("Music track completed, restarting for long game in arena " + arenaName);
                    Set<UUID> currentMusicPlayers = activeMusicPlayers.get(arenaName);
                    if (currentMusicPlayers != null) {
                        for (UUID playerId : game.getAlivePlayers()) {
                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                playMusicToPlayer(player, soundName, 0.8f, 1.0f);
                            }
                        }
                    }
                }

                // Add music for any new players who joined during gameplay
                Set<UUID> currentMusicPlayers = activeMusicPlayers.get(arenaName);
                if (currentMusicPlayers != null) {
                    for (UUID playerId : game.getAlivePlayers()) {
                        if (!currentMusicPlayers.contains(playerId)) {
                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                playMusicToPlayer(player, soundName, 0.8f, 1.0f);
                                currentMusicPlayers.add(playerId);
                                plugin.getLogger().info("Added music for new player: " + player.getName());
                            }
                        }
                    }
                }
                
                tickCounter++;
            }
        }.runTaskTimer(plugin, 20, 20); // Check every second
        
        musicTasks.put(arenaName, musicTask);
    }
    
    public void playDeathSound(Player player) {
        String soundName = plugin.getConfigManager().getDeathSound();
        playSound(player, soundName, 1.0f, 1.0f);
    }
    
    public void playWinSound(Player player) {
        String soundName = plugin.getConfigManager().getWinSound();
        playSound(player, soundName, 1.0f, 1.0f);
    }
    
    public void playCountdownSound(Player player) {
        String soundName = plugin.getConfigManager().getCountdownSound();
        playSound(player, soundName, 1.0f, 1.0f);
    }
    
    public void playStartSound(Player player) {
        String soundName = plugin.getConfigManager().getStartSound();
        playSound(player, soundName, 1.0f, 1.0f);
    }
    
    private void playSound(Player player, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.isEmpty()) {
            return;
        }
        
        // Try ItemsAdder custom sound first
        if (itemsAdderEnabled && soundName.contains(":")) {
            try {
                Class<?> customSoundClass = Class.forName("dev.lone.itemsadder.api.CustomSound");
                customSoundClass.getMethod("playToPlayer", Player.class, String.class, float.class, float.class)
                    .invoke(null, player, soundName, volume, pitch);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to play ItemsAdder sound: " + soundName);
            }
        }
        
        // Fallback to vanilla sounds - play at player's location for regular sounds
        try {
            Sound bukkitSound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), bukkitSound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name: " + soundName);
            
            // Ultimate fallback
            switch (soundName.toLowerCase()) {
                case "death":
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, volume, pitch);
                    break;
                case "win":
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, volume, pitch);
                    break;
                case "countdown":
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch);
                    break;
                case "start":
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, volume, pitch);
                    break;
            }
        }
    }
    
    private void playMusicToPlayer(Player player, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.isEmpty()) {
            return;
        }

        // Try ItemsAdder custom sound first
        if (itemsAdderEnabled && soundName.contains(":")) {
            try {
                Class<?> customSoundClass = Class.forName("dev.lone.itemsadder.api.CustomSound");
                customSoundClass.getMethod("playToPlayer", Player.class, String.class, float.class, float.class)
                    .invoke(null, player, soundName, volume, pitch);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to play ItemsAdder sound: " + soundName);
            }
        }

        // Fallback to Bukkit API (Spigot compatible)
        try {
            Sound bukkitSound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), bukkitSound, SoundCategory.RECORDS, volume, pitch);
        } catch (IllegalArgumentException e) {
            // If custom sound string or invalid, use fallback music
            player.playSound(player.getLocation(), Sound.MUSIC_DISC_BLOCKS, SoundCategory.RECORDS, volume, pitch);
        }
    }
    
    private void stopMusicForPlayer(Player player) {
        // Stop sounds using Bukkit API (Spigot compatible)
        try {
            player.stopSound(SoundCategory.RECORDS);
        } catch (Exception e) {
            // Ultimate fallback - stop all sounds if category doesn't work
            try {
                player.stopSound(SoundCategory.MASTER);
            } catch (Exception ex) {
                // Some older versions might not have stopAllSounds, just log
                plugin.getLogger().warning("Could not stop sounds for player: " + player.getName());
            }
        }
    }

    public void cleanup() {
        // Cancel all music tasks when plugin disables
        for (BukkitTask task : musicTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        musicTasks.clear();
        
        // Stop music for all tracked players
        for (Set<UUID> players : activeMusicPlayers.values()) {
            for (UUID playerId : players) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    stopMusicForPlayer(player);
                }
            }
        }
        activeMusicPlayers.clear();
        totalMusicProgress.clear();
        roundStartTime.clear();
    }
    
    // Utility method to get current music progress for an arena
    public double getMusicProgress(String arenaName) {
        return totalMusicProgress.getOrDefault(arenaName, 0.0);
    }
    
    // Utility method to format progress time for display
    public String getFormattedProgress(String arenaName) {
        double progress = getMusicProgress(arenaName);
        int minutes = (int) (progress / 60);
        int seconds = (int) (progress % 60);
        return String.format("%d:%02d", minutes, seconds);
    }
}