package nl.dutchcoding.blockparty.commands;

import nl.dutchcoding.blockparty.BlockParty;
import nl.dutchcoding.blockparty.models.Arena;
import nl.dutchcoding.blockparty.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockPartyCommand implements CommandExecutor, TabCompleter {
    
    private final BlockParty plugin;
    
    public BlockPartyCommand(BlockParty plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("blockparty.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "arena":
                handleArenaCommand(sender, args);
                break;
            case "join":
                handleJoinCommand(sender, args);
                break;
            case "leave":
                handleLeaveCommand(sender);
                break;
            case "forcestart":
                handleForceStartCommand(sender, args);
                break;
            case "sethub":
                handleSetHubCommand(sender);
                break;
            case "setsign":
                handleSetSignCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "stats":
                handleStatsCommand(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    private void handleArenaCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player-only"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("arena-usage"));
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "create":
                if (args.length < 3) {
                    player.sendMessage(plugin.getMessageManager().getMessage("arena-create-usage"));
                    return;
                }
                handleCreateArena(player, args[2]);
                break;
            case "pos1":
            case "pos2":
                if (args.length < 3) {
                    player.sendMessage(plugin.getMessageManager().getMessage("arena-pos-usage"));
                    return;
                }
                handleSetPosition(player, args[2], args[1].toLowerCase());
                break;
            case "setspawn":
                if (args.length < 3) {
                    player.sendMessage(plugin.getMessageManager().getMessage("arena-spawn-usage"));
                    return;
                }
                handleSetSpawn(player, args[2]);
                break;
            case "waitlobby":
                if (args.length < 3) {
                    player.sendMessage(plugin.getMessageManager().getMessage("arena-lobby-usage"));
                    return;
                }
                handleSetWaitLobby(player, args[2]);
                break;
            default:
                player.sendMessage(plugin.getMessageManager().getMessage("arena-usage"));
                break;
        }
    }
    
    private void handleCreateArena(Player player, String name) {
        if (plugin.getArenaManager().getArena(name) != null) {
            player.sendMessage(plugin.getMessageManager().getMessage("arena-already-exists")
                .replace("{arena}", name));
            return;
        }
        
        Arena arena = new Arena(name);
        plugin.getArenaManager().addArena(arena);
        player.sendMessage(plugin.getMessageManager().getMessage("arena-created")
            .replace("{arena}", name));
    }
    
    private void handleSetPosition(Player player, String arenaName, String posType) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("arena-not-found")
                .replace("{arena}", arenaName));
            return;
        }
        
        Location loc = player.getLocation();
        if (posType.equals("pos1")) {
            arena.setPos1(loc);
            player.sendMessage(plugin.getMessageManager().getMessage("arena-pos1-set")
                .replace("{arena}", arenaName));
        } else {
            arena.setPos2(loc);
            player.sendMessage(plugin.getMessageManager().getMessage("arena-pos2-set")
                .replace("{arena}", arenaName));
        }
        
        plugin.getArenaManager().saveArenas();
    }
    
    private void handleSetSpawn(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("arena-not-found")
                .replace("{arena}", arenaName));
            return;
        }
        
        arena.setSpawnLocation(player.getLocation());
        player.sendMessage(plugin.getMessageManager().getMessage("arena-spawn-set")
            .replace("{arena}", arenaName));
        plugin.getArenaManager().saveArenas();
    }
    
    private void handleSetWaitLobby(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("arena-not-found")
                .replace("{arena}", arenaName));
            return;
        }
        
        arena.setWaitLobby(player.getLocation());
        player.sendMessage(plugin.getMessageManager().getMessage("arena-lobby-set")
            .replace("{arena}", arenaName));
        plugin.getArenaManager().saveArenas();
    }
    
    private void handleSetHubCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player-only"));
            return;
        }
        
        Player player = (Player) sender;
        plugin.getConfigManager().setHubLocation(player.getLocation());
        player.sendMessage(plugin.getMessageManager().getMessage("hub-set"));
    }
    
    private void handleSetSignCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player-only"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("sign-usage"));
            return;
        }
        
        Player player = (Player) sender;
        String arenaName = args[1];
        
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("arena-not-found")
                .replace("{arena}", arenaName));
            return;
        }
        
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
            player.sendMessage(plugin.getMessageManager().getMessage("sign-not-found"));
            return;
        }
        
        plugin.getSignManager().createSign(targetBlock.getLocation(), arenaName);
        player.sendMessage(plugin.getMessageManager().getMessage("sign-created")
            .replace("{arena}", arenaName));
    }
    
    private void handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player-only"));
            return;
        }

        Player player = (Player) sender;
        Player target;

        if (args.length == 1) {
            target = player;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("player-not-found"));
                return;
            }
        }

        PlayerStats stats = plugin.getStatsManager().getPlayerStats(target.getUniqueId());
        player.sendMessage(plugin.getMessageManager().getMessage("stats-header").replace("{player}", target.getName()));
        player.sendMessage(plugin.getMessageManager().getMessage("stats-wins").replace("{wins}", String.valueOf(stats.getWins())));
        player.sendMessage(plugin.getMessageManager().getMessage("stats-losses").replace("{losses}", String.valueOf(stats.getLosses())));
        player.sendMessage(plugin.getMessageManager().getMessage("stats-games").replace("{games}", String.valueOf(stats.getGamesPlayed())));
        player.sendMessage(plugin.getMessageManager().getMessage("stats-rounds").replace("{rounds}", String.valueOf(stats.getRoundsSurvived())));
        player.sendMessage(plugin.getMessageManager().getMessage("stats-winrate").replace("{winrate}", String.format("%.2f", stats.getWinRate())));
    }
    
    private void handleJoinCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player-only"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("join-usage"));
            return;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("arena-not-found")
                .replace("{arena}", arenaName));
            return;
        }
        
        plugin.getGameManager().joinGame(player, arena);
    }
    
    private void handleLeaveCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player-only"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (!plugin.getGameManager().isPlayerInGame(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("not-in-game"));
            return;
        }
        
        plugin.getGameManager().leaveGame(player);
    }
    
    private void handleForceStartCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blockparty.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("forcestart-usage"));
            return;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("arena-not-found")
                .replace("{arena}", arenaName));
            return;
        }
        
        if (plugin.getGameManager().forceStartGame(arenaName)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("game-force-started")
                .replace("{arena}", arenaName));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("cannot-force-start")
                .replace("{arena}", arenaName));
        }
    }
    
    private void handleReloadCommand(CommandSender sender) {
        plugin.getConfigManager().loadConfigs();
        sender.sendMessage(plugin.getMessageManager().getMessage("plugin-reloaded"));
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6§lBlockParty Commands:");
        sender.sendMessage("§e/bp join <arena> §7- Join a game");
        sender.sendMessage("§e/bp leave §7- Leave current game");
        if (sender.hasPermission("blockparty.admin")) {
            sender.sendMessage("§e/bp forcestart <arena> §7- Force start a game");
            sender.sendMessage("§e/bp arena create <name> §7- Create a new arena");
            sender.sendMessage("§e/bp arena pos1|pos2 <arena> §7- Set floor positions");
            sender.sendMessage("§e/bp arena setspawn <arena> §7- Set arena spawn");
            sender.sendMessage("§e/bp arena waitlobby <arena> §7- Set waiting lobby");
            sender.sendMessage("§e/bp sethub §7- Set main hub spawn");
            sender.sendMessage("§e/bp setsign <arena> §7- Create join sign");
            sender.sendMessage("§e/bp reload §7- Reload configuration");
        }
        sender.sendMessage("§e/bp stats [player] §7- View player stats");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("join", "leave", "stats"));
            if (sender.hasPermission("blockparty.admin")) {
                completions.addAll(Arrays.asList("forcestart", "arena", "sethub", "setsign", "reload"));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            completions.addAll(plugin.getArenaManager().getArenaNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("forcestart")) {
            completions.addAll(plugin.getArenaManager().getArenaNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("arena")) {
            completions.addAll(Arrays.asList("create", "pos1", "pos2", "setspawn", "waitlobby"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setsign")) {
            completions.addAll(plugin.getArenaManager().getArenaNames());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("arena") && 
                  !args[1].equalsIgnoreCase("create")) {
            completions.addAll(plugin.getArenaManager().getArenaNames());
        }
        
        return completions;
    }
}