package nl.dutchcoding.blockparty.managers;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager {

    private final Map<UUID, PlayerInventoryData> savedInventories = new HashMap<>();

    /**
     * Saves a player's current inventory, armor, gamemode, and other properties
     */
    public void savePlayerData(Player player) {
        PlayerInventoryData data = new PlayerInventoryData();
        
        // Save inventory contents
        data.inventoryContents = player.getInventory().getContents().clone();
        data.armorContents = player.getInventory().getArmorContents().clone();
        
        // Save player properties
        data.gameMode = player.getGameMode();
        data.health = player.getHealth();
        data.foodLevel = player.getFoodLevel();
        data.saturation = player.getSaturation();
        data.level = player.getLevel();
        data.exp = player.getExp();
        data.totalExperience = player.getTotalExperience();
        data.allowFlight = player.getAllowFlight();
        data.flying = player.isFlying();
        data.flySpeed = player.getFlySpeed();
        data.walkSpeed = player.getWalkSpeed();
        
        // Save potion effects
        data.potionEffects = player.getActivePotionEffects();
        
        savedInventories.put(player.getUniqueId(), data);
    }

    /**
     * Restores a player's saved inventory and properties
     */
    public boolean restorePlayerData(Player player) {
        PlayerInventoryData data = savedInventories.remove(player.getUniqueId());
        if (data == null) {
            return false;
        }

        // Clear current effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Restore inventory
        player.getInventory().setContents(data.inventoryContents);
        player.getInventory().setArmorContents(data.armorContents);
        
        // Restore player properties
        player.setGameMode(data.gameMode);
        player.setHealth(data.health);
        player.setFoodLevel(data.foodLevel);
        player.setSaturation(data.saturation);
        player.setLevel(data.level);
        player.setExp(data.exp);
        player.setTotalExperience(data.totalExperience);
        player.setAllowFlight(data.allowFlight);
        player.setFlying(data.flying);
        player.setFlySpeed(data.flySpeed);
        player.setWalkSpeed(data.walkSpeed);
        
        // Restore potion effects
        for (PotionEffect effect : data.potionEffects) {
            player.addPotionEffect(effect);
        }
        
        return true;
    }

    /**
     * Checks if a player has saved data
     */
    public boolean hasSavedData(Player player) {
        return savedInventories.containsKey(player.getUniqueId());
    }

    /**
     * Removes saved data for a player without restoring it
     */
    public void removeSavedData(Player player) {
        savedInventories.remove(player.getUniqueId());
    }

    /**
     * Clears all saved inventories (useful for plugin reload)
     */
    public void clearAll() {
        savedInventories.clear();
    }

    /**
     * Gets the number of saved inventories
     */
    public int getSavedCount() {
        return savedInventories.size();
    }

    /**
     * Data class to store player inventory and properties
     */
    private static class PlayerInventoryData {
        ItemStack[] inventoryContents;
        ItemStack[] armorContents;
        GameMode gameMode;
        double health;
        int foodLevel;
        float saturation;
        int level;
        float exp;
        int totalExperience;
        boolean allowFlight;
        boolean flying;
        float flySpeed;
        float walkSpeed;
        Collection<PotionEffect> potionEffects;
    }
}