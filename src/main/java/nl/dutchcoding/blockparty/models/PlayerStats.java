package nl.dutchcoding.blockparty.models;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats implements ConfigurationSerializable {
    
    private final UUID playerId;
    private int wins;
    private int losses;
    private int gamesPlayed;
    private int roundsSurvived;
    
    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
        this.wins = 0;
        this.losses = 0;
        this.gamesPlayed = 0;
        this.roundsSurvived = 0;
    }
    
    public PlayerStats(Map<String, Object> map) {
        this.playerId = UUID.fromString((String) map.get("playerId"));
        this.wins = (Integer) map.getOrDefault("wins", 0);
        this.losses = (Integer) map.getOrDefault("losses", 0);
        this.gamesPlayed = (Integer) map.getOrDefault("gamesPlayed", 0);
        this.roundsSurvived = (Integer) map.getOrDefault("roundsSurvived", 0);
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId.toString());
        map.put("wins", wins);
        map.put("losses", losses);
        map.put("gamesPlayed", gamesPlayed);
        map.put("roundsSurvived", roundsSurvived);
        return map;
    }
    
    public void addWin() {
        this.wins++;
        this.gamesPlayed++;
    }
    
    public void addLoss() {
        this.losses++;
        this.gamesPlayed++;
    }
    
    public void addRoundsSurvived(int rounds) {
        this.roundsSurvived += rounds;
    }
    
    public double getWinRate() {
        if (gamesPlayed == 0) return 0.0;
        return (double) wins / gamesPlayed * 100;
    }
    
    // Getters
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getWins() {
        return wins;
    }
    
    public int getLosses() {
        return losses;
    }
    
    public int getGamesPlayed() {
        return gamesPlayed;
    }
    
    public int getRoundsSurvived() {
        return roundsSurvived;
    }
}