package nl.dutchcoding.blockparty.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.Game;
import nl.dutchcoding.blockparty.models.PlayerStats;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockPartyPlaceholders extends PlaceholderExpansion {
    
    private final BlockParty plugin;
    
    public BlockPartyPlaceholders(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "blockparty";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // Player stats placeholders
        if (params.equals("wins")) {
            PlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());
            return String.valueOf(stats.getWins());
        }
        
        if (params.equals("games_played")) {
            PlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());
            return String.valueOf(stats.getGamesPlayed());
        }
        
        if (params.equals("rounds_survived")) {
            PlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());
            return String.valueOf(stats.getRoundsSurvived());
        }
        
        if (params.equals("win_rate")) {
            PlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());
            if (stats.getGamesPlayed() == 0) {
                return "0.0";
            }
            double winRate = (double) stats.getWins() / stats.getGamesPlayed() * 100;
            return String.format("%.1f", winRate);
        }
        
        // Game state placeholders
        if (params.equals("in_game")) {
            return plugin.getGameManager().isPlayerInGame(player) ? "true" : "false";
        }
        
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            switch (params) {
                case "game_state":
                    return game.getState().name().toLowerCase();
                    
                case "game_players":
                    return String.valueOf(game.getPlayers().size());
                    
                case "game_alive_players":
                    return String.valueOf(game.getAlivePlayers().size());
                    
                case "game_max_players":
                    return String.valueOf(game.getArena().getMaxPlayers());
                    
                case "game_round":
                    return String.valueOf(game.getRound());
                    
                case "game_time_left":
                    return String.valueOf(game.getTimeLeft());
                    
                case "game_countdown":
                    return String.valueOf(game.getCountdown());
                    
                case "game_arena":
                    return game.getArena().getName();
                    
                case "game_selected_block":
                    return game.getSelectedBlock() != null ? 
                           formatBlockName(game.getSelectedBlock().name()) : "none";
                           
                case "is_alive":
                    return game.getAlivePlayers().contains(player.getUniqueId()) ? "true" : "false";
            }
        } else {
            // Return defaults when not in game
            switch (params) {
                case "game_state":
                    return "none";
                case "game_players":
                case "game_alive_players":
                case "game_max_players":
                case "game_round":
                case "game_time_left":
                case "game_countdown":
                    return "0";
                case "game_arena":
                case "game_selected_block":
                    return "none";
                case "is_alive":
                    return "false";
            }
        }
        
        // Global stats placeholders
        if (params.equals("total_active_games")) {
            return String.valueOf(plugin.getGameManager().getActiveGames().size());
        }
        
        if (params.equals("total_players_in_games")) {
            return String.valueOf(plugin.getGameManager().getActiveGames().values().stream()
                    .mapToInt(g -> g.getPlayers().size()).sum());
        }
        
        return null;
    }
    
    private String formatBlockName(String materialName) {
        return materialName.toLowerCase().replace("_", " ");
    }
}