# Agent Guide — small game

**Conciseness is paramount.** Code and docs must be scannable, 
containing only what's needed. If you're unsure whether something 
belongs here, put it in "Maybe Keep" below so it can be reviewed 
or deleted easily.

## Priorities:

1. Code simple, easy to understand
2. Code and docs concise, quick to read
3. Functional, avoid mutability, where it makes sense
4. Hexagonal architecture, pure functional core behind interfaces, UIs, scenario loading in adapters

## Tests

- JUnit 5 + `kotlin.test.*`.
- Keep tests concise. Prove that complex or important logic works. Coverage is not a priority.

## Build & Run

Each UI variant is an independent Gradle module with its own `installDist`:

```sh
./gradlew :<ui-module>:installDist  # TUI mode (lanterna + jansi)
```

Run: `<ui-module>/build/install/<ui-module>/bin/<ui-module> <scenario-file>`

JVM 21 required. No separate lint/typecheck — `compileKotlin` covers it.

### JDK selection across environments

The project targets JVM 21 but other versions may be on PATH (e.g., a newer JDK in the dev container). Gradle picks the right one via two mechanisms:

- **`org.gradle.java.installations.paths`** in `gradle.properties` lists directories to search. Gradle walks these looking for the matching toolchain version (`jvmToolchain(21)`), regardless of what's on PATH.
- **Foojay-resolver-convention plugin** (configured in `settings.gradle.kts`) extends this: if no directory contains JDK 21, it auto-downloads from Maven Central.

This means the same project compiles identically inside distrobox/sbx and on Bazzite host without per-env config.

## Structure

Multi-module Gradle (Kotlin 2.1.20, JUnit 5). Each UI variant has its own `Main.kt`.

| Path | Contents |
|---|---|
| `core/model/` | Data classes describing game entities and state (per plans directory) |
| `core/state/` | Immutable GameState with win/lose check |
| `core/command/` | Sealed Command hierarchy for player actions; `processCommand(GameState, Command) -> GameState` |
| `core/engine/` | Game logic implementations: game engine, trigger/event systems |
| `core/port/` | Interfaces (`ScenarioRepository`, `GameStateStore`) and shared types (`InputEvent`, `ViewData`) |
| `core/scenario/` | Pure data classes for scenario file schema (JSON entries) |
| `adapter-scenario/src/main/kotlin/...` | JSON scenario loading adapter (depends on core ports + kotlinx.serialization) |
| `adapter-storage/src/main/kotlin/...` | Concrete GameStateStore implementations (in-memory, disk-backed, etc.) |
| `<ui-module>/src/main/kotlin/trap/towers/cli/Main.kt` | UI adapter entry point: imperative shell that wires everything together |

## Architecture — Hexagonal / Functional Core

The project follows hexagonal architecture with a **pure functional engine** at its center:

- `core/` contains only data classes and pure functions. No file I/O, no serialization imports, 
  no UI dependencies. Unit-testable without mocking anything.
- Adapters depend on core's ports. They implement interfaces (ScenarioRepository, GameStateStore) 
  with concrete logic that touches the outside world.

### The functional engine loop

The engine takes an input state and a command, returns a new state:

```
scenario loader ──► assemble initial GameState
storage reads previous state ──┐
                                ▼
                      commands ► processCommand(state, cmd) ► new GameState
                                │                                    │
                                │ saves full state (incl. secrets)     │ derives player-visible view
                                ▼                                    ▼
                           storage                              UI render
```

- `processCommand(GameState, Command) -> GameState` is the single entry point to game logic. 
  State transitions happen via `copy(...)` — no mutable fields in core.
- **Storage gets full state**: everything the engine produces persists between turns so the next load resolves correctly (enemy positions, hidden triggers, etc.).
- **UI view is derived from full state**: secrets are stripped/abstracted before rendering so the player only sees what their character knows.
- Main.kt wires it all together: read scenario or resume from storage → loop calling processCommand() and deriving a view for display.

### Adding new game data / features

1. Add data class to `core/model/`. Must be immutable (`val`).
2. Update scenario schema in `core/scenario/` if it's part of the JSON file; update `adapter-scenario` if needed.
3. If it changes GameState, update `core/state/` and dependent logic in `core/engine/` or `core/command/`.

### Adding a new UI variant

1. Create module (e.g., `adapter-ui-web/`).
2. Add build.gradle.kts with deps on core + adapter-scenario + adapter-storage.
3. Implement needed ports (ScenarioRepository, GameStateStore).
4. Write Main.kt that wires adapters and runs the game loop.

## Agent behavior

- Prefer Grep and Glob when finding files to read instead of find and other bash commands when possible
- Do not spawn multiple sub agents in parallel, one at a time only
- If there is need to use temporary files, put them inside project directory under temp directory. These include extracted library sources when out how a library works.

## Plans

- Game-specific design decisions (entities, mechanics, rules) live in the `plans/` directory.
- Core module packages mirror what's described there — if a game concept has no home in core/, add it to the right package.
- Initial plan: plans/TRAP_TD_INITIAL_PLAN.md

---
