package nl.dutchcoding.blockparty.listeners;

import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.Arena;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;

public class SignListener implements Listener {
    
    private final BlockParty plugin;
    
    public SignListener(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) return;
        
        if (!plugin.getSignManager().isJoinSign(block.getLocation())) return;
        
        String arenaName = plugin.getSignManager().getArenaFromSign(block.getLocation());
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("arena-not-found")
                .replace("{arena}", arenaName));
            return;
        }
        
        if (!event.getPlayer().hasPermission("blockparty.play")) {
            event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        plugin.getGameManager().joinGame(event.getPlayer(), arena);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (plugin.getSignManager().isJoinSign(block.getLocation())) {
            if (!event.getPlayer().hasPermission("blockparty.admin")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                return;
            }
            
            plugin.getSignManager().removeSign(block.getLocation());
            event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("sign-removed"));
        }
    }
}