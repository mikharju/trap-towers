# Trap TD — MVP Project Plan

## Concept

A turn-based tower defense where the objective is to **capture** enemies alive, not kill them. Lanes radiate outward from a central base to the edges of the map. Enemies do not spawn on a timer — they are *summoned* by the player's own Lures. Placing a Lure is simultaneously an offensive resource (it's what brings enemies into play at all) and a targeting tool (it determines which lane(s) they appear in). The player alternates between a planning turn (placing traps and lures) and a resolution turn (enemies spawn, move, and are either captured or escape). Currency is earned per capture. Enemies that reach the base uncaptured damage a "village" meter; if it hits zero, the player loses.

This is a genre reframe of standard TD in two ways: capture-not-kill, and player-triggered-not-timer-triggered spawning. The MVP should prove this core loop is satisfying before anything else is added.

## Map Layout

- Central base tile, with a fixed number of lanes (e.g., 3–4) radiating outward from it to the map edges, like spokes on a wheel.
- Enemies always move inward along a lane, from edge toward base.
- Each lane is a simple line of tiles — no branching, no open-grid pathfinding.

## Turn Structure

The game proceeds in discrete rounds, each with two phases:

1. **Planning phase (player turn)**
   - Player places Snares and Cages anywhere on lane tiles.
   - Player places one or more Lures, each consuming currency and having a limited number of charges.
   - Placing a Lure **on a lane tile** (not the center) causes enemies to spawn only in that lane.
   - Placing a Lure **on the central base tile** causes enemies to spawn across any/all lanes.
   - Player ends the turn when satisfied with the setup.
2. **Resolution phase (enemy turn)**
   - For each active Lure charge, an enemy spawns at the outer edge of the targeted lane(s) and begins moving toward the base.
   - Enemies move, get rooted by Snares, and get captured by Cages (or escape/reach base) automatically — no player input during this phase.
   - Phase ends when all Lure charges for the round are spent and all spawned enemies have either been captured or reached the base.
3. Control returns to the player for the next Planning phase.

This makes each round a discrete, resolvable puzzle: "given my currency and trap layout, where do I place lures to spawn enemies into a setup that captures them?"

## Structures (MVP scope: exactly these three)

- **Lure**
  - Placed on any tile (lane tile or the central base tile).
  - Has a fixed number of charges; each charge spawns one enemy during the resolution phase, then the Lure is spent/removed.
  - Placement location determines spawn scope:
    - On a lane tile → enemies spawn only in that lane.
    - On the central base tile → enemies spawn in a randomly chosen lane (or all lanes, for MVP simplicity — pick one and treat the other as a Phase 2 variant).
- **Snare**
  - Placed on a lane tile.
  - Any enemy that moves onto the tile is rooted/heavily slowed for a fixed duration (measured in resolution-phase movement steps, not real time, since the game is turn-based).
  - Has a cooldown before it can trigger again within the same resolution phase.
- **Cage**
  - Placed on a lane tile.
  - While an enemy on this tile is rooted (by an active Snare effect), the cage's capture meter fills each step.
  - If the enemy's root expires before the meter is full, progress is lost and the enemy resumes moving (the "escaped" case — mechanically and visually distinct from never being trapped at all).
  - Meter full → enemy is removed, player gains currency.

Intended combo: **Lure (spawn) → Snare (root) → Cage (capture)**, placed in sequence along a lane.

## Enemies (MVP scope: one basic type)

- Fixed lane, fixed base speed, moves one step per resolution tick toward the base.
- Has a "rooted" state (cannot move, vulnerable to Cage capture).
- No resistances or special behavior yet — all enemies react identically to traps.
- Only exist because a Lure summoned them — there is no ambient/timer-based spawning at all.

## Resources & Win/Loss

- **Currency**: earned only from successful captures. Spent on placing Lures, Snares, and Cages.
- **Village meter**: starts full, decreases by a fixed amount each time an enemy reaches the base uncaptured. Zero = game over.
- **Win condition**: survive a fixed number of rounds (e.g., 5–10) with village meter above zero. Since the player controls when/how enemies spawn via Lures, the round counter (not enemy count) is what drives pacing.

## Scope Boundaries for MVP (explicitly excluded for now)

- No open-grid pathfinding — fixed radial lanes only.
- No enemy variety, resistances, or flying/armored types.
- No trap upgrades or tech tree.
- No captured-creature meta systems (selling, breeding, zoo, etc.).
- No multiplayer.
- No real-time elements anywhere — resolution phase can auto-step or be advanced manually one tick at a time, whichever is simpler to implement first.
- Minimal/placeholder art and UI — functionality over presentation.

## Definition of Done for MVP

- Player can place Lures, Snares, and Cages during a planning phase.
- Lure placement location correctly determines single-lane vs. any-lane spawning.
- Resolution phase correctly spawns enemies per Lure charge, moves them, and resolves rooting/capture/escape.
- Currency is awarded on capture and spendable on structures.
- Village meter decreases on leak and triggers a loss state at zero.
- Player can win by surviving a fixed number of rounds.
- A full round (plan → resolve → plan again) is playable start to finish in a single session without crashes.
