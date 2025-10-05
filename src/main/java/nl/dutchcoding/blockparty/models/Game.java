package nl.dutchcoding.blockparty.models;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Game {
    
    private final Arena arena;
    private final List<UUID> players;
    private final List<UUID> alivePlayers;

    private GameState state;
    private int countdown;
    private int round;
    private int timeLeft;
    private Material selectedBlock;
    private int musicProgress; // Track music progress in seconds
    private boolean musicPlaying;
    
    public Game(Arena arena) {
        this.arena = arena;
        this.players = new ArrayList<>();
        this.alivePlayers = new ArrayList<>();

        this.state = GameState.WAITING;
        this.countdown = 30;
        this.round = 0;
        this.timeLeft = 0;
        this.musicProgress = 0;
        this.musicPlaying = false;
    }
    
    public void addPlayer(Player player) {
        if (players.size() >= arena.getMaxPlayers()) {
            return;
        }
        players.add(player.getUniqueId());
        alivePlayers.add(player.getUniqueId());
    }
    
    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        alivePlayers.remove(player.getUniqueId());
    }
    
    public boolean isPlayerInGame(Player player) {
        return players.contains(player.getUniqueId());
    }
    
    public boolean isPlayerAlive(Player player) {
        return alivePlayers.contains(player.getUniqueId());
    }
    
    public void eliminatePlayer(UUID playerId) {
        alivePlayers.remove(playerId);
    }
    
    public boolean canStart() {
        return players.size() >= arena.getMinPlayers() && state == GameState.WAITING;
    }
    
    public boolean hasWinner() {
        return alivePlayers.size() <= 1;
    }
    
    public UUID getWinner() {
        return alivePlayers.isEmpty() ? null : alivePlayers.get(0);
    }
    
    public int getRoundTime() {
        if (round <= 3) return 12;
        if (round <= 6) return 9;
        if (round <= 9) return 6;
        if (round <= 13) return 4;
        if (round <= 16) return 2;
        return 1;
    }
    
    public void nextRound() {
        this.round++;
        this.timeLeft = getRoundTime();
    }
    
    // Getters and setters
    public Arena getArena() {
        return arena;
    }
    
    public List<UUID> getPlayers() {
        return players;
    }
    
    public List<UUID> getAlivePlayers() {
        return alivePlayers;
    }
    
    public GameState getState() {
        return state;
    }
    
    public void setState(GameState state) {
        this.state = state;
    }
    
    public int getCountdown() {
        return countdown;
    }
    
    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }
    
    public int getRound() {
        return round;
    }
    
    public void setRound(int round) {
        this.round = round;
    }
    
    public int getTimeLeft() {
        return timeLeft;
    }
    
    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }
    
    public Material getSelectedBlock() {
        return selectedBlock;
    }
    
    public void setSelectedBlock(Material selectedBlock) {
        this.selectedBlock = selectedBlock;
    }
    
    public int getMusicProgress() {
        return musicProgress;
    }
    
    public void setMusicProgress(int musicProgress) {
        this.musicProgress = musicProgress;
    }
    
    public void addMusicProgress(int seconds) {
        this.musicProgress += seconds;
    }
    
    public boolean isMusicPlaying() {
        return musicPlaying;
    }
    
    public void setMusicPlaying(boolean musicPlaying) {
        this.musicPlaying = musicPlaying;
    }
    

    

    
    public enum GameState {
        WAITING, STARTING, PLAYING, ENDING
    }
}