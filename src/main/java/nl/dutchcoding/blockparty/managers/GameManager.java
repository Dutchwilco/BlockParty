package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.Arena;
import nl.dutchcoding.blockparty.models.Game;
import nl.dutchcoding.blockparty.models.PlayerStats;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager {

    private final BlockParty plugin;
    private final Map<String, Game> activeGames;
    private final Map<UUID, String> playerArenas;
    private final List<Material> gameBlocks;
    private final ScoreboardManager scoreboardManager;
    
    public GameManager(BlockParty plugin) {
        this.plugin = plugin;
        this.activeGames = new HashMap<>();
        this.playerArenas = new HashMap<>();
        this.gameBlocks = new ArrayList<>();
        this.scoreboardManager = plugin.getScoreboardManager();

        loadGameBlocks();
    }
    
    private void loadGameBlocks() {
        gameBlocks.clear();
        List<String> blockNames = plugin.getConfigManager().getGameBlocks();
        for (String blockName : blockNames) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                gameBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid block material: " + blockName);
            }
        }
        
        if (gameBlocks.isEmpty()) {
            // Fallback blocks
            gameBlocks.addAll(Arrays.asList(
                Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, 
                Material.YELLOW_WOOL, Material.ORANGE_WOOL, Material.PURPLE_WOOL
            ));
        }
    }
    
    public boolean joinGame(Player player, Arena arena) {
        if (!arena.isSetup()) {
            player.sendMessage(plugin.getMessageManager().getMessage("arena-not-setup"));
            return false;
        }

        if (isPlayerInGame(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("already-in-game"));
            return false;
        }

        Game game = activeGames.computeIfAbsent(arena.getName(), k -> new Game(arena));

        if (game.getPlayers().size() >= arena.getMaxPlayers()) {
            player.sendMessage(plugin.getMessageManager().getMessage("game-full"));
            return false;
        }

        game.addPlayer(player);
        playerArenas.put(player.getUniqueId(), arena.getName());

        // Save player's current inventory and state (remove any existing saved data first)
        if (plugin.getInventoryManager().hasSavedData(player)) {
            plugin.getInventoryManager().removeSavedData(player);
        }
        plugin.getInventoryManager().savePlayerData(player);

        // Teleport to waiting lobby
        player.teleport(arena.getWaitLobby());
        player.setGameMode(GameMode.ADVENTURE);
        player.setCollidable(false);

        // Clear inventory
        player.getInventory().clear();

        // Give lobby item
        plugin.getLobbyItemManager().giveLobbyItem(player);

        // Set waiting scoreboard for all players in game
        updateWaitingScoreboards(game);

        // Send join message
        broadcastToGame(game, plugin.getMessageManager().getMessage("player-joined")
            .replace("{player}", player.getName())
            .replace("{current}", String.valueOf(game.getPlayers().size()))
            .replace("{max}", String.valueOf(arena.getMaxPlayers())));

        // Start countdown if minimum players reached
        if (game.canStart() && game.getState() == Game.GameState.WAITING) {
            startCountdown(game);
        }

        return true;
    }
    
    public void leaveGame(Player player) {
        String arenaName = playerArenas.get(player.getUniqueId());
        if (arenaName == null) return;
        
        Game game = activeGames.get(arenaName);
        if (game == null) return;
        
        game.removePlayer(player);
        playerArenas.remove(player.getUniqueId());

        // Remove scoreboard
        scoreboardManager.removeScoreboard(player);

        // Restore player
        restorePlayer(player);

        // Update scoreboards for remaining players if game still active
        if (!game.getPlayers().isEmpty()) {
            if (game.getState() == Game.GameState.PLAYING) {
                updateIngameScoreboards(game);
            } else {
                updateWaitingScoreboards(game);
            }
        }

        // Send leave message
        broadcastToGame(game, plugin.getMessageManager().getMessage("player-left")
            .replace("{player}", player.getName())
            .replace("{current}", String.valueOf(game.getPlayers().size()))
            .replace("{max}", String.valueOf(game.getArena().getMaxPlayers())));

        // Check if game should end
        if (game.getPlayers().isEmpty()) {
            endGame(game);
        } else if (game.getState() == Game.GameState.PLAYING && game.hasWinner()) {
            endGame(game);
        }
    }
    
    private void startCountdown(Game game) {
        game.setState(Game.GameState.STARTING);
        game.setCountdown(plugin.getConfigManager().getCountdownTime());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() != Game.GameState.STARTING) {
                    cancel();
                    return;
                }
                
                int countdown = game.getCountdown();
                
                if (game.getPlayers().size() < game.getArena().getMinPlayers()) {
                    game.setState(Game.GameState.WAITING);
                    broadcastToGame(game, plugin.getMessageManager().getMessage("not-enough-players"));
                    cancel();
                    return;
                }
                
                if (countdown <= 0) {
                    startGame(game);
                    cancel();
                    return;
                }
                
                if (countdown <= 10 || countdown % 10 == 0) {
                    broadcastToGame(game, plugin.getMessageManager().getMessage("countdown")
                        .replace("{time}", String.valueOf(countdown)));
                    
                    // Play sound
                    for (UUID playerId : game.getPlayers()) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            plugin.getSoundManager().playCountdownSound(player);
                        }
                    }
                }
                
                game.setCountdown(countdown - 1);
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    
    private void startGame(Game game) {
        game.setState(Game.GameState.PLAYING);
        game.setRound(0);

        // Teleport players to arena
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Remove lobby item when game starts
                plugin.getLobbyItemManager().removeLobbyItem(player);
                
                player.teleport(game.getArena().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        // Generate floor
        generateFloor(game);

        // Set in-game scoreboards
        updateIngameScoreboards(game);

        broadcastToGame(game, plugin.getMessageManager().getMessage("game-started"));

        // Reset music progress for new game
        plugin.getSoundManager().resetGameMusic(game.getArena().getName());

        // Start game loop
        startGameLoop(game);

        // Music will start at the beginning of each round
    }
    
    private void startGameLoop(Game game) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() != Game.GameState.PLAYING) {
                    cancel();
                    return;
                }
                
                if (game.hasWinner()) {
                    endGame(game);
                    cancel();
                    return;
                }
                
                startNewRound(game);
            }
        }.runTaskLater(plugin, 20 * 3); // 3 second delay between rounds
    }
    
    private void startNewRound(Game game) {
        game.nextRound();

        // Select random block
        Material selectedBlock = gameBlocks.get(new Random().nextInt(gameBlocks.size()));
        game.setSelectedBlock(selectedBlock);

        // Regenerate floor
        generateFloor(game);

        // Give selected block to players
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                giveSelectedBlock(player, selectedBlock);
            }
        }

        // Resume music for new round
        plugin.getSoundManager().resumeGameMusic(game);

        // Update scoreboards with new round
        updateIngameScoreboards(game);

        // Send round info
        String title = plugin.getMessageManager().getTitle("round-start")
            .replace("{round}", String.valueOf(game.getRound()));
        String subtitle = plugin.getMessageManager().getSubtitle("round-start")
            .replace("{block}", formatBlockName(selectedBlock))
            .replace("{time}", String.valueOf(game.getRoundTime()));

        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendTitle(title, subtitle,
                    plugin.getMessageManager().getTitleFadeIn("round-start"),
                    plugin.getMessageManager().getTitleStay("round-start"),
                    plugin.getMessageManager().getTitleFadeOut("round-start"));

                // Start playing music (this will be handled by the game-wide music system)
            }
        }



        // Start round timer
        startRoundTimer(game);
    }
    
    private void startRoundTimer(Game game) {
        game.setTimeLeft(game.getRoundTime());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() != Game.GameState.PLAYING) {
                    cancel();
                    return;
                }
                
                int timeLeft = game.getTimeLeft();
                
                if (timeLeft <= 0) {
                    // Round ended - pause music and remove wrong blocks
                    plugin.getSoundManager().pauseGameMusic(game);
                    removeWrongBlocks(game);

                    // Check for eliminations
                    checkEliminations(game);
                    
                    // Continue game loop
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            startGameLoop(game);
                        }
                    }.runTaskLater(plugin, 20 * 2);
                    
                    cancel();
                    return;
                }
                
                // Update actionbar timer
                String timerMessage = plugin.getMessageManager().getActionbar("timer")
                    .replace("{time}", String.valueOf(timeLeft));

                for (UUID playerId : game.getAlivePlayers()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(timerMessage));
                        // Update scoreboard time
                        scoreboardManager.setIngameScoreboard(player, game.getRound(), timeLeft, game.getAlivePlayers().size(), game.getArena().getMaxPlayers());
                    }
                }

                game.setTimeLeft(timeLeft - 1);
            }
        }.runTaskTimer(plugin, 20, 20);
    }
    
    private void generateFloor(Game game) {
        Set<Location> floorBlocks = game.getArena().getFloorBlocks();
        
        for (Location loc : floorBlocks) {
            Material randomBlock = gameBlocks.get(new Random().nextInt(gameBlocks.size()));
            loc.getBlock().setType(randomBlock);
        }
    }
    
    private void removeWrongBlocks(Game game) {
        Set<Location> floorBlocks = game.getArena().getFloorBlocks();
        
        for (Location loc : floorBlocks) {
            if (loc.getBlock().getType() != game.getSelectedBlock()) {
                loc.getBlock().setType(Material.AIR);
            }
        }
    }
    
    private void checkEliminations(Game game) {
        // Players will now fall naturally with gravity when their blocks disappear
        // The PlayerListener will handle elimination when they fall below the arena
        updateIngameScoreboards(game);
    }
    
    private void endGame(Game game) {
        game.setState(Game.GameState.ENDING);
        
        // Stop music completely when game ends
        plugin.getSoundManager().stopGameMusic(game);

        UUID winnerId = game.getWinner();
        if (winnerId != null) {
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                broadcastToGame(game, plugin.getMessageManager().getMessage("game-won")
                    .replace("{winner}", winner.getName()));

                plugin.getSoundManager().playWinSound(winner);

                // Reset floor to one color
                resetFloorToOneColor(game);

                // Launch firework
                launchWinFirework(winner);

                // Update winner stats
                PlayerStats stats = plugin.getStatsManager().getPlayerStats(winnerId);
                stats.addWin();
                stats.addRoundsSurvived(game.getRound());

                // Give winner rewards
                plugin.getRewardManager().giveWinnerRewards(winner);

                // Show winner scoreboard to all players
                for (UUID playerId : game.getPlayers()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        scoreboardManager.setWinnerScoreboard(player, winner.getName());
                    }
                }

                // Schedule return to hub after 5 seconds
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        returnPlayersToHub(game);
                        activeGames.remove(game.getArena().getName());
                    }
                }.runTaskLater(plugin, 20 * 5);
            }
        } else {
            broadcastToGame(game, plugin.getMessageManager().getMessage("game-draw"));
            returnPlayersToHub(game);
            activeGames.remove(game.getArena().getName());
        }
    }
    
    private void giveSelectedBlock(Player player, Material block) {
        // Clear inventory before giving the block
        player.getInventory().clear();

        ItemStack item = new ItemStack(block);
        var meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + formatBlockName(block));
        meta.setLore(Arrays.asList("BlockParty Block"));
        item.setItemMeta(meta);
        for (int i = 0; i < 10; i++) {
            player.getInventory().setItem(i, item.clone());
        }
    }
    
    private void restorePlayer(Player player) {
        // Try to restore saved inventory and player data
        boolean restored = plugin.getInventoryManager().restorePlayerData(player);

        if (!restored) {
            // Fallback to default restoration if no saved data
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.setFoodLevel(20);
        }

        // Re-enable collision
        player.setCollidable(true);

        Location hub = plugin.getConfigManager().getHubLocation();
        if (hub != null) {
            player.teleport(hub);
        }
    }
    
    private void updateWaitingScoreboards(Game game) {
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                scoreboardManager.setWaitingScoreboard(player, game.getPlayers().size(), game.getArena().getMaxPlayers());
            }
        }
    }

    private void updateIngameScoreboards(Game game) {
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                scoreboardManager.setIngameScoreboard(player, game.getRound(), game.getTimeLeft(), game.getAlivePlayers().size(), game.getArena().getMaxPlayers());
            }
        }
    }

    public void broadcastToGame(Game game, String message) {
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
    
    private String formatBlockName(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }
    
    public boolean isPlayerInGame(Player player) {
        return playerArenas.containsKey(player.getUniqueId());
    }
    
    public Game getPlayerGame(Player player) {
        String arenaName = playerArenas.get(player.getUniqueId());
        return arenaName != null ? activeGames.get(arenaName) : null;
    }
    
    public void stopAllGames() {
        for (Game game : new ArrayList<>(activeGames.values())) {
            endGame(game);
        }
    }
    
    public Map<String, Game> getActiveGames() {
        return activeGames;
    }

    private void resetFloorToOneColor(Game game) {
        Set<Location> floorBlocks = game.getArena().getFloorBlocks();
        Material color = game.getSelectedBlock(); // Use the last selected block as the color

        for (Location loc : floorBlocks) {
            loc.getBlock().setType(color);
        }
    }

    private void launchWinFirework(Player winner) {
        Location loc = winner.getLocation();
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 5) {
                    cancel();
                    return;
                }
                Firework firework = loc.getWorld().spawn(loc, Firework.class);
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.PURPLE)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withTrail()
                    .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
                count++;
            }
        }.runTaskTimer(plugin, 0, 20); // Every second for 5 seconds
    }

    private void returnPlayersToHub(Game game) {
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Give participation rewards
                plugin.getRewardManager().giveParticipationRewards(player);
                
                // Give survival rewards based on rounds survived
                PlayerStats stats = plugin.getStatsManager().getPlayerStats(playerId);
                plugin.getRewardManager().giveSurvivalRewards(player, game.getRound());
                
                scoreboardManager.removeScoreboard(player);
                restorePlayer(player);
                playerArenas.remove(playerId);
            }
        }
    }
    
    public boolean forceStartGame(String arenaName) {
        Game game = activeGames.get(arenaName);
        if (game == null) {
            return false;
        }
        
        if (game.getState() != Game.GameState.WAITING && game.getState() != Game.GameState.STARTING) {
            return false;
        }
        
        if (game.getPlayers().size() < 2) {
            return false; // Need at least 2 players
        }
        
        // Cancel any existing countdown
        game.setState(Game.GameState.STARTING);
        game.setCountdown(0);
        
        // Start the game immediately
        new BukkitRunnable() {
            @Override
            public void run() {
                startGame(game);
            }
        }.runTaskLater(plugin, 1);
        
        return true;
    }
}