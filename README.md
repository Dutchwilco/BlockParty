# BlockParty

A comprehensive BlockParty minigame plugin for Minecraft servers where players must run to the correct colored block before time runs out.

## Description

BlockParty is a fast-paced minigame where players compete in an arena filled with colored blocks. As the game progresses, the floor changes colors, and players must quickly move to the matching block color before the time expires. The last player standing wins!

## Features

- **Arena Management**: Create and manage multiple game arenas with custom positions and spawn locations
- **Lobby System**: Dedicated waiting lobbies for each arena
- **Configurable Timers**: Progressive round timers that get faster as the game advances
- **Customizable Blocks**: Choose which blocks to use for the game floor
- **Scoreboard Integration**: Real-time scoreboard updates for waiting and in-game states
- **Stats Tracking**: Track player wins, losses, games played, rounds survived, and win rates
- **Sound Support**: Custom sound integration with ItemsAdder
- **Music Playback**: Continuous music track support with configurable duration
- **Join Signs**: Create interactive signs for easy arena joining
- **No Player Collision**: Players can't push each other during games
- **Inventory Management**: Automatic inventory saving and restoration
- **Message Customization**: Fully customizable messages and titles
- **Hub System**: Configurable hub location for players to return after games

## Installation

1. Download the latest release JAR file
2. Place the JAR file in your server's `plugins` folder
3. Restart your server or run `/reload`
4. Configure arenas and settings as needed

### Dependencies

- **Paper 1.21.4** or higher (recommended)
- **ItemsAdder** (optional, for custom sounds and music)
- **PlaceholderAPI** (optional, for placeholder support)
- **Economy plugin** (optional, for reward commands)

## Configuration

### Main Configuration (`config.yml`)

- `hub-location`: Set the main hub spawn location
- `countdown-time`: Time before games start (default: 30 seconds)
- `music.track-duration`: Duration of music tracks for continuous playbook (default: 180 seconds)
- `blocks`: List of available blocks for the game floor
- `scoreboard`: Customize waiting, in-game, and winner scoreboards
- `rewards`: Configure win, participation, and survival rewards

### Messages Configuration (`messages.yml`)

- Customize all in-game messages, titles, and action bars
- Support for color codes and placeholders

### Timers Configuration (`timers.yml`)

- Configure round times that progressively decrease:
  - Rounds 1-3: 12 seconds
  - Rounds 4-6: 9 seconds
  - Rounds 7-9: 6 seconds
  - Rounds 10-13: 4 seconds
  - Rounds 14-16: 2 seconds
  - Rounds 17+: 1 second

## Commands

### Main Command: `/blockparty` (alias: `/bp`)

Requires `blockparty.admin` permission

#### Arena Management
- `/bp arena create <name>` - Create a new arena
- `/bp arena pos1 <arena>` - Set position 1 of the arena floor
- `/bp arena pos2 <arena>` - Set position 2 of the arena floor
- `/bp arena setspawn <arena>` - Set the spawn location for the arena
- `/bp arena waitlobby <arena>` - Set the waiting lobby location

#### Game Management
- `/bp join <arena>` - Join a specific arena
- `/bp forcestart <arena>` - Force start a game in an arena

#### Other
- `/bp setsign <arena>` - Create a join sign for an arena
- `/bp hub` - Set the main hub location
- `/bp stats <player>` - View player statistics
- `/bp reload` - Reload the plugin configuration

## Permissions

- `blockparty.admin` - Access to all BlockParty commands (default: op)
- `blockparty.play` - Permission to join BlockParty games (default: true)

## Setup Guide

1. **Create an Arena**:
   ```
   /bp arena create myarena
   ```

2. **Set Arena Positions**:
   - Stand at one corner of the arena floor and run `/bp arena pos1 myarena`
   - Stand at the opposite corner and run `/bp arena pos2 myarena`

3. **Set Spawn and Lobby**:
   ```
   /bp arena setspawn myarena
   /bp arena waitlobby myarena
   ```

4. **Set Hub Location** (optional):
   ```
   /bp hub
   ```

5. **Create Join Signs** (optional):
   - Place a sign and look at it
   - Run `/bp setsign myarena`

## Rewards System

Configure rewards in `config.yml`:

```yaml
rewards:
  # Winner rewards (given to the last player standing)
  winner:
    commands:
      - "eco give {player} 100"
      - "give {player} diamond 5"
      - "broadcast &a{player} won BlockParty!"
    enabled: true
  
  # Participation rewards (given to all players who participate)
  participation:
    commands:
      - "eco give {player} 10"
      - "give {player} emerald 1"
    enabled: true
  
  # Round survival rewards (given per round survived)
  survival:
    commands:
      - "eco give {player} 5"
    enabled: true
    min-rounds: 3
```

Available placeholders in reward commands:
- `{player}` - Player name
- `{uuid}` - Player UUID
- `{displayname}` - Player display name

## PlaceholderAPI Integration

When PlaceholderAPI is installed, the following placeholders are available:

### Player Statistics
- `%blockparty_wins%` - Player's total wins
- `%blockparty_games_played%` - Total games played
- `%blockparty_rounds_survived%` - Total rounds survived
- `%blockparty_win_rate%` - Win rate percentage

### Game Status
- `%blockparty_in_game%` - Whether player is in a game (true/false)
- `%blockparty_game_state%` - Current game state (waiting/playing/ending)
- `%blockparty_game_players%` - Total players in current game
- `%blockparty_game_alive_players%` - Alive players in current game
- `%blockparty_game_max_players%` - Maximum players for current arena
- `%blockparty_game_round%` - Current round number
- `%blockparty_game_time_left%` - Time left in current round
- `%blockparty_game_countdown%` - Countdown time before game starts
- `%blockparty_game_arena%` - Current arena name
- `%blockparty_game_selected_block%` - Currently selected block
- `%blockparty_is_alive%` - Whether player is alive in current game

### Global Statistics
- `%blockparty_total_active_games%` - Total active games across all arenas
- `%blockparty_total_players_in_games%` - Total players currently in games

## Game Rules

- Players join an arena and wait in the lobby
- When enough players join, a countdown starts
- The game begins, and players are teleported to the arena
- Colored blocks appear on the floor
- Players must stand on the correct color block before time runs out
- Incorrect blocks or floor blocks cause elimination
- Rounds get progressively faster
- Last player standing wins and receives rewards!

## Support

If you need help or have questions:
- Check the configuration files for examples
- Ensure all arena positions are set correctly
- Verify that the floor area is clear of obstructions

## Version

Current Version: 1.0.0

Compatible with Paper 1.21.4+</content>
<parameter name="path">README.md
