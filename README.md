# Blue Checkers

Brazilian-rules checkers for Android. The opponent learns from every game,
so the AI you face after 200 games is meaningfully stronger than the one
you faced on day one.

Built with Kotlin, Jetpack Compose, Material 3, Room, Hilt, and WorkManager.
The game engine and the AI are pure Kotlin (zero Android dependencies) so
they're fully covered by JVM unit tests.

## Features

- Player vs computer (white plays the human; black plays the AI).
- Two-player same-device mode (both sides played from the same screen).
- Progressive difficulty: each level requires **five wins to unlock the
  next**. Levels keep counting up forever; the AI's tuning saturates around
  level 12 — past that the level number tracks perseverance, not search depth.
- On-device reinforcement learning. TD(0) updates a small linear evaluator
  after every completed game (player or self-play). Weights persist across
  sessions, so the AI keeps every game it has ever played.
- Optional background self-play training while the device is charging and
  idle (WorkManager, opt-in).
- Multi-step capture input: tap each landing in a multi-jump so equal-length
  chains with different captured pieces are picked unambiguously.
- Smooth move animations along the full capture path.
- Configurable settings: animations, capture hints, undo (auto-disabled at
  your highest unlocked level so the score stays fair), CBD vs FMJD-64
  draw counters, sound effects, self-play.
- English and Brazilian Portuguese (pt-BR).

## Rules implemented

Brazilian draughts (Damas Brasileiras), following FMJD-64 and CBD:

- 8×8 board, 12 men per side, dark squares only.
- White moves first. Long dark diagonal on each player's left.
- Men move one square diagonally forward and capture forward or backward.
- Kings (damas) fly any number of empty squares along a diagonal and
  capture at a distance, landing on any empty square beyond the captured
  piece.
- Captures are mandatory. Among available capture sequences, the player
  must pick one that captures the maximum number of pieces (quantity
  only — kings and men count the same).
- Captured pieces remain on the board as inert blockers until the chain
  ends; no piece may be jumped twice in one chain.
- A man does not promote when it merely passes through the last row
  mid-chain. Promotion happens only when the chain ends with the piece
  stopping on the promotion row; a newly promoted king may not capture
  on the same turn.
- Default draws: three-fold repetition, or 20 consecutive king-only plies
  with no capture and no man movement (CBD rule). FMJD-64 tiered draw
  counters (15/30/60) are available in Settings.

A `Rules` screen in the app explains the same rules to players.

## Difficulty

A "level" is a `(search depth, ε exploration, time budget)` tuple. The
trained weight snapshot is shared across all levels.

| Level | Depth | ε    | Time budget |
| ----- | ----- | ---- | ----------- |
| 0     | 1     | 1.00 | 100 ms      |
| 1     | 2     | 0.35 | 200 ms      |
| 2     | 3     | 0.20 | 350 ms      |
| 3     | 3     | 0.10 | 500 ms      |
| 4     | 4     | 0.05 | 700 ms      |
| 5     | 5     | 0.02 | 900 ms      |
| 6     | 6     | 0.00 | 1200 ms     |
| 7     | 7     | 0.00 | 1500 ms     |
| 8     | 8     | 0.00 | 2000 ms     |
| 9     | 9     | 0.00 | 2500 ms     |
| 10    | 9     | 0.00 | 2800 ms     |
| 11    | 10    | 0.00 | 3000 ms     |
| 12+   | 10    | 0.00 | 3000 ms     |

Past level 12 the tuning saturates because alpha-beta with a linear
evaluator runs out of practical depth on a phone — even a transposition
table buys only a couple more plies.

## Architecture

```
app/src/main/kotlin/dev/canem/bluecheckers/
├── game/              # pure Kotlin rules engine (no Android deps)
├── ai/                # agents, evaluator, TD learner, Zobrist hash
├── data/              # Room entities, DAOs, repositories, DataStore prefs
├── training/          # SelfPlayWorker + scheduler
├── ui/
│   ├── home/          # HomeScreen + HomeViewModel
│   ├── game/          # GameScreen, BoardCanvas, GameViewModel, SoundPlayer
│   ├── rules/         # RulesScreen
│   ├── settings/      # SettingsScreen + SettingsViewModel
│   ├── navigation/    # BlueNavGraph + Routes
│   └── theme/         # Material 3 colors + typography
├── di/                # Hilt modules
├── BlueApp.kt         # Hilt entry point, WorkManager Configuration.Provider
└── MainActivity.kt
```

## Build

```sh
./scripts/setup.sh         # one-time: points git at the in-repo hooks
./gradlew :app:assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk` and
can be sideloaded directly to a device.

For a signed release build, see [RELEASING.md](RELEASING.md).

## Tests

```sh
./gradlew :app:testDebugUnitTest
```

35+ JUnit tests cover the rules engine (capture chains, mandatory
captures, lei da maioria, promotion edge cases, draws), the AI (alpha-beta
search, agent vs random sanity check), the TD learner (weight update
direction), and the ViewModels (tap input flow, 2P mode, resign).

## Requirements

- Android 10+ (API 29).
- ~70 MB on-device for the debug build (smaller for release after R8).

## Repository hygiene

The pre-commit hook in `.githooks/pre-commit` is generic: it reads a
list of patterns from `.git/identity-block-patterns` (a file inside
your local `.git/` directory, never tracked by git) and refuses any
commit whose staged additions contain those words.

After cloning, run:

```sh
./scripts/setup.sh
```

This installs the hook and creates an example patterns file. Edit
that file to add words you want to keep out of commits — your name,
your employer, project codenames, etc. Your patterns stay private to
your clone; the committed hook learns nothing about them.

## License

[Apache 2.0](LICENSE).
