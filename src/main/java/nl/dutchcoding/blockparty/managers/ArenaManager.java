package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.Arena;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArenaManager {
    
    private final BlockParty plugin;
    private final Map<String, Arena> arenas;
    private File arenasFile;
    private FileConfiguration arenasConfig;
    
    public ArenaManager(BlockParty plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        loadArenas();
    }
    
    public void loadArenas() {
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            try {
                arenasFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create arenas.yml!");
                return;
            }
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        
        if (arenasConfig.contains("arenas")) {
            for (String name : arenasConfig.getConfigurationSection("arenas").getKeys(false)) {
                Map<String, Object> arenaData = arenasConfig.getConfigurationSection("arenas." + name).getValues(false);
                arenaData.put("name", name);
                Arena arena = new Arena(arenaData);
                arenas.put(name, arena);
            }
        }
        
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
    }
    
    public void saveArenas() {
        arenasConfig.set("arenas", null); // Clear existing data
        
        for (Arena arena : arenas.values()) {
            Map<String, Object> data = arena.serialize();
            data.remove("name"); // Don't save name in the data
            arenasConfig.set("arenas." + arena.getName(), data);
        }
        
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save arenas.yml!");
        }
    }
    
    public void addArena(Arena arena) {
        arenas.put(arena.getName(), arena);
    }
    
    public void removeArena(String name) {
        arenas.remove(name);
    }
    
    public Arena getArena(String name) {
        return arenas.get(name);
    }
    
    public Map<String, Arena> getArenas() {
        return arenas;
    }
    
    public List<String> getArenaNames() {
        return arenas.keySet().stream().collect(Collectors.toList());
    }
    
    public List<Arena> getSetupArenas() {
        return arenas.values().stream()
                .filter(Arena::isSetup)
                .collect(Collectors.toList());
    }
}