package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LobbyItemManager {

    private final BlockParty plugin;
    private boolean enabled;
    private Material material;
    private int slot;
    private String name;
    private List<String> lore;

    public LobbyItemManager(BlockParty plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("lobby-item.enabled", true);
        
        String materialName = plugin.getConfig().getString("lobby-item.material", "RED_BED");
        try {
            this.material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid lobby item material: " + materialName + ", defaulting to RED_BED");
            this.material = Material.RED_BED;
        }
        
        this.slot = plugin.getConfig().getInt("lobby-item.slot", 8);
        this.name = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("lobby-item.name", "&cReturn to Lobby"));
        
        this.lore = new ArrayList<>();
        List<String> configLore = plugin.getConfig().getStringList("lobby-item.lore");
        for (String line : configLore) {
            this.lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    public void giveLobbyItem(Player player) {
        if (!enabled) return;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        player.getInventory().setItem(slot, item);
    }

    public void removeLobbyItem(Player player) {
        if (!enabled) return;
        player.getInventory().setItem(slot, null);
    }

    public boolean isLobbyItem(ItemStack item) {
        if (!enabled || item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals(name) && 
               item.getType() == material &&
               meta.hasLore() && 
               meta.getLore().equals(lore);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
