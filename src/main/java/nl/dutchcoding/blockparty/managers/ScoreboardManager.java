package nl.dutchcoding.blockparty.managers;

import nl.dutchcoding.blockparty.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;
import java.util.Map;

public class ScoreboardManager {

    private final BlockParty plugin;

    public ScoreboardManager(BlockParty plugin) {
        this.plugin = plugin;
    }

    public void setWaitingScoreboard(Player player, int currentPlayers, int maxPlayers) {
        Map<String, String> placeholders = Map.of(
            "%players%", currentPlayers + "/" + maxPlayers
        );
        setScoreboard(player, "waiting", placeholders);
    }

    public void setIngameScoreboard(Player player, int round, int time, int currentPlayers, int maxPlayers) {
        Map<String, String> placeholders = Map.of(
            "%round%", String.valueOf(round),
            "%time%", String.valueOf(time),
            "%players%", currentPlayers + "/" + maxPlayers
        );
        setScoreboard(player, "ingame", placeholders);
    }

    public void setWinnerScoreboard(Player player, String winnerName) {
        Map<String, String> placeholders = Map.of(
            "%winner%", winnerName
        );
        setScoreboard(player, "winner", placeholders);
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private void setScoreboard(Player player, String type, Map<String, String> placeholders) {
        FileConfiguration config = plugin.getConfig();
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("scoreboard." + type + ".title", "[BlockParty]"));
        List<String> lines = config.getStringList("scoreboard." + type + ".lines");

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("blockparty", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        for (String line : lines) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                line = line.replace(entry.getKey(), entry.getValue());
            }
            line = ChatColor.translateAlternateColorCodes('&', line);
            objective.getScore(line).setScore(score--);
        }

        player.setScoreboard(scoreboard);
        // Ensure no player collision while in a BlockParty game
        if (plugin.getGameManager().isPlayerInGame(player)) {
            applyNoCollision(player, scoreboard);
        }
    }

    private void applyNoCollision(Player player, Scoreboard scoreboard) {
        Team team = scoreboard.getTeam("bp_nocollide");
        if (team == null) {
            team = scoreboard.registerNewTeam("bp_nocollide");
        }
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        String entry = player.getName();
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
    }
}