# Agent Guide — small game

**Conciseness is paramount.** Code and docs must be scannable, 
containing only what's needed. If you're unsure whether something 
belongs here, put it in "Maybe Keep" below so it can be reviewed 
or deleted easily.

## Priorities:

1. Code simple, easy to understand
2. Code and docs concise, quick to read
3. Functional, avoid mutability, where it makes sense
4. Hexagonal architecture, functional core behind interfaces, UIs, scenario loading, such in adapters

## Tests

- JUnit 5 + `kotlin.test.*`.
- Keep tests concise. Prove that complex or important logic works. Coverage is not a priority.

## Build & Run

Each UI variant is an independent Gradle module with its own `installDist`:

```sh
./gradlew :exploration-engine-ui-lanterna:installDist  # TUI mode (lanterna + jansi)
```

Run: `./adapter-ui-text/build/install/exploration-engine-ui-text/bin/exploration-engine-ui-text <scenario-file>`

JVM 21 required. No separate lint/typecheck — `compileKotlin` covers it.

## Structure

Multi-module Gradle (Kotlin 2.1.20, JUnit 5). Each UI variant has its own `Main.kt`.

| Path | Contents |
|---|---|
| `core/model/` | Area, Device, Player, World, Trigger, Item data classes |
| `core/state/` | Immutable GameState with win/lose check |
| `core/command/` | Sealed Command (Look/Move/Activate/TakeItem/DropItem/EquipItem/UnequipItem/Inventory), processCommand |
| `core/engine/` | GameEngineImpl, TriggerEngine |
| `core/port/` | Interfaces: GameEngine, ScenarioRepository; types: InputEvent, ViewData |
| `core/adapter/jsonloader/` | JsonScenarioRepository, JsonFileReader (scenario loading) |
| `core/adapter/storage/` | InMemoryGameStateStore |
| `core/scenario/` | `ScenarioFile.kt` (JSON entries), `ScenarioLoader.kt` (assembleGame) |
| `adapter-ui-lanterna/src/main/kotlin/exploration/cli/Main.kt` | LanternaUiAdapter entry point |

App module (`app/`) contains shared utilities (InMemoryGameStateStore, JsonScenarioRepository moved to core) and test resources. Each adapter-ui-* module is independently buildable with its own `MainKt`.

## Architecture

- Pure functional core: `processCommand(GameState, Command) -> GameState`. Imperative shell in Main.kt. State transitions via `copy(...)`.
- Put logic and decision making into core, keep UIs and adapters thin when it makes sense (example: command validation logic in core)

## Agent behavior

- Prefer Grep and Glob when finding files to read instead of find and other bash commands when possible
- Do not spawn multiple sub agents in parallel, one at a time only
- If there is need to use temporary files, put them inside project directory under temp directory. These include extracted library sources when finding out how a library works.

## Plans

- Plans for project/features in plans directory under project root
- Initial MVP plan: plans/TRAP_TD_INITIAL_PLAN.md

---

