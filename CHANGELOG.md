# Changelog

All notable changes are recorded here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning
follows [SemVer](https://semver.org/).

## [Unreleased]

### Added

- Full Brazilian-rules game engine (mandatory capture, lei da maioria
  quantity rule, flying king, captured-as-blockers, no mid-chain
  promotion, no same-turn capture after promotion).
- Alpha-beta agent with iterative deepening, ε-greedy exploration, and
  a Zobrist-keyed transposition table.
- TD(0) learner that updates a shared linear evaluator after every game.
- Win-to-unlock progression: five wins at level N unlock level N+1.
  Levels are unbounded; tuning saturates past level 12.
- Compose UI: home screen, game screen with sliding move animations,
  multi-step capture input, rules screen, settings screen.
- Hilt-injected persistence: Room for level scores and the shared
  weight snapshot; DataStore for user preferences.
- Opt-in background self-play training via WorkManager (charging + idle).
- Two-player same-device mode.
- Sound effects using Android's built-in system UI sounds (no bundled
  audio assets, opt-in).
- English and Brazilian Portuguese localization.
- 35+ JUnit tests across the engine, AI, TD learner, and ViewModels.

### Repo

- Pre-commit hook (`.githooks/pre-commit`) that blocks commits whose
  staged additions contain forbidden identifiers.
- GitHub Actions workflows for CI on pushes/PRs and tagged releases.
- `RELEASING.md` with end-to-end release instructions.
