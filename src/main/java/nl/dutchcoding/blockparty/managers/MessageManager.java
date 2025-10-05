package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageManager {
    
    private final BlockParty plugin;
    private FileConfiguration messages;
    
    public MessageManager(BlockParty plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public String getMessage(String key) {
        return messages.getString("messages." + key, "§cMessage not found: " + key)
                .replace("&", "§");
    }
    
    public String getTitle(String key) {
        return messages.getString("titles." + key + ".title", "")
                .replace("&", "§");
    }
    
    public String getSubtitle(String key) {
        return messages.getString("titles." + key + ".subtitle", "")
                .replace("&", "§");
    }
    
    public int getTitleFadeIn(String key) {
        return messages.getInt("titles." + key + ".fade-in", 10);
    }
    
    public int getTitleStay(String key) {
        return messages.getInt("titles." + key + ".stay", 70);
    }
    
    public int getTitleFadeOut(String key) {
        return messages.getInt("titles." + key + ".fade-out", 20);
    }

    public String getActionbar(String key) {
        return messages.getString("actionbar." + key, "§cActionbar not found: " + key)
                .replace("&", "§");
    }
}