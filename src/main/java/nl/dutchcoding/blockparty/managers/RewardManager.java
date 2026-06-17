package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class RewardManager {
    
    private final BlockParty plugin;
    
    public RewardManager(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    public void giveWinnerRewards(Player player) {
        if (!plugin.getConfigManager().getConfig().getBoolean("rewards.winner.enabled", true)) {
            return;
        }
        
        List<String> commands = plugin.getConfigManager().getConfig().getStringList("rewards.winner.commands");
        executeCommands(commands, player);
    }
    
    public void giveParticipationRewards(Player player) {
        if (!plugin.getConfigManager().getConfig().getBoolean("rewards.participation.enabled", true)) {
            return;
        }
        
        List<String> commands = plugin.getConfigManager().getConfig().getStringList("rewards.participation.commands");
        executeCommands(commands, player);
    }
    
    public void giveSurvivalRewards(Player player, int roundsSurvived) {
        if (!plugin.getConfigManager().getConfig().getBoolean("rewards.survival.enabled", true)) {
            return;
        }
        
        int minRounds = plugin.getConfigManager().getConfig().getInt("rewards.survival.min-rounds", 3);
        if (roundsSurvived < minRounds) {
            return;
        }
        
        List<String> commands = plugin.getConfigManager().getConfig().getStringList("rewards.survival.commands");
        
        // Execute commands for each round survived above minimum
        int rewardRounds = roundsSurvived - minRounds + 1;
        for (int i = 0; i < rewardRounds; i++) {
            executeCommands(commands, player);
        }
    }
    
    private void executeCommands(List<String> commands, Player player) {
        for (String command : commands) {
            String processedCommand = command.replace("{player}", player.getName())
                                           .replace("{uuid}", player.getUniqueId().toString())
                                           .replace("{displayname}", player.getDisplayName());
            
            // Execute as console command
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            });
        }
    }
}