package nl.dutchcoding.blockparty.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Arena implements ConfigurationSerializable {
    
    private final String name;
    private Location pos1;
    private Location pos2;
    private Location spawnLocation;
    private Location waitLobby;
    private int minPlayers;
    private int maxPlayers;
    
    public Arena(String name) {
        this.name = name;
        this.minPlayers = 2;
        this.maxPlayers = 16;
    }
    
    public Arena(Map<String, Object> map) {
        this.name = (String) map.get("name");
        this.pos1 = (Location) map.get("pos1");
        this.pos2 = (Location) map.get("pos2");
        this.spawnLocation = (Location) map.get("spawnLocation");
        this.waitLobby = (Location) map.get("waitLobby");
        this.minPlayers = (Integer) map.getOrDefault("minPlayers", 2);
        this.maxPlayers = (Integer) map.getOrDefault("maxPlayers", 16);
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("pos1", pos1);
        map.put("pos2", pos2);
        map.put("spawnLocation", spawnLocation);
        map.put("waitLobby", waitLobby);
        map.put("minPlayers", minPlayers);
        map.put("maxPlayers", maxPlayers);
        return map;
    }
    
    public boolean isSetup() {
        return pos1 != null && pos2 != null && spawnLocation != null && waitLobby != null;
    }
    
    public Set<Location> getFloorBlocks() {
        if (pos1 == null || pos2 == null) {
            return Set.of();
        }
        
        Set<Location> blocks = new java.util.HashSet<>();
        
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int y = Math.min(pos1.getBlockY(), pos2.getBlockY());
        
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                blocks.add(new Location(pos1.getWorld(), x, y, z));
            }
        }
        
        return blocks;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public Location getPos1() {
        return pos1;
    }
    
    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }
    
    public Location getPos2() {
        return pos2;
    }
    
    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }
    
    public Location getSpawnLocation() {
        return spawnLocation;
    }
    
    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }
    
    public Location getWaitLobby() {
        return waitLobby;
    }
    
    public void setWaitLobby(Location waitLobby) {
        this.waitLobby = waitLobby;
    }
    
    public int getMinPlayers() {
        return minPlayers;
    }
    
    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}