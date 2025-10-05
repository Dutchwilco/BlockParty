package nl.dutchcoding.blockparty.listeners;

import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.Game;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {
    
    private final BlockParty plugin;
    
    public PlayerListener(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getGameManager().isPlayerInGame(event.getPlayer())) {
            plugin.getGameManager().leaveGame(event.getPlayer());
        }
        // Clean up any saved inventory data if player disconnects without being in a game
        else if (plugin.getInventoryManager().hasSavedData(event.getPlayer())) {
            plugin.getInventoryManager().removeSavedData(event.getPlayer());
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            // Clear drops and keep inventory/exp
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            
            // Respawn player immediately
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.spigot().respawn();
                player.setHealth(20);
            }, 1L);
            
            // Eliminate player if game is active
            if (game.getState() == Game.GameState.PLAYING) {
                game.eliminatePlayer(player.getUniqueId());
                player.setGameMode(GameMode.SPECTATOR);

                // Send elimination message
                player.sendMessage(plugin.getMessageManager().getMessage("eliminated"));
                plugin.getGameManager().broadcastToGame(game, plugin.getMessageManager().getMessage("player-eliminated")
                    .replace("{player}", player.getName()));

                // Play death sound
                plugin.getSoundManager().playDeathSound(player);

                // Update stats
                plugin.getStatsManager().getPlayerStats(player.getUniqueId()).addLoss();
                plugin.getStatsManager().getPlayerStats(player.getUniqueId()).addRoundsSurvived(game.getRound());
            }
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Game game = plugin.getGameManager().getPlayerGame(event.getPlayer());
        if (game == null) return;

        // Check if player fell into void during game
        if (game.getState() == Game.GameState.PLAYING &&
            game.isPlayerAlive(event.getPlayer()) &&
            event.getTo().getY() < game.getArena().getPos1().getY() - 10) {

            game.eliminatePlayer(event.getPlayer().getUniqueId());
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
            event.getPlayer().teleport(game.getArena().getSpawnLocation());

            event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("eliminated"));
            plugin.getGameManager().broadcastToGame(game, plugin.getMessageManager().getMessage("player-eliminated")
                .replace("{player}", event.getPlayer().getName()));
            plugin.getSoundManager().playDeathSound(event.getPlayer());
            
            // Update stats
            plugin.getStatsManager().getPlayerStats(event.getPlayer().getUniqueId()).addLoss();
            plugin.getStatsManager().getPlayerStats(event.getPlayer().getUniqueId()).addRoundsSurvived(game.getRound());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!plugin.getGameManager().isPlayerInGame(player)) return;

        ItemStack item = event.getCurrentItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            if (item.getItemMeta().getLore().contains("BlockParty Block")) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Game victimGame = plugin.getGameManager().getPlayerGame(victim);
        Game attackerGame = plugin.getGameManager().getPlayerGame(attacker);

        // Disable PVP if either player is in a game (waiting or playing)
        if (victimGame != null || attackerGame != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getGameManager().isPlayerInGame(player)) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            if (item.getItemMeta().getLore().contains("BlockParty Block")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().isPlayerInGame(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().isPlayerInGame(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Check if player is in game and clicked lobby item
        if (plugin.getGameManager().isPlayerInGame(player)) {
            if (plugin.getLobbyItemManager().isLobbyItem(item)) {
                event.setCancelled(true);
                
                // Remove the lobby item from player's inventory
                plugin.getLobbyItemManager().removeLobbyItem(player);
                
                // Leave the game
                plugin.getGameManager().leaveGame(player);
                
                player.sendMessage(plugin.getMessageManager().getMessage("left-game"));
            }
        }
    }
}