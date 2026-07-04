package trap.towers.engine

import trap.towers.model.Cage
import trap.towers.model.Enemy
import trap.towers.model.Lure
import trap.towers.model.Position
import trap.towers.model.ScenarioConfig
import trap.towers.model.Snare
import trap.towers.model.Structure
import trap.towers.state.GameState
import trap.towers.state.Phase

fun resolveResolutionPhase(state: GameState): GameState {
    var current = state.copy(phase = Phase.RESOLUTION, tick = 0)

    // SPAWN PHASE (tick 0): first charge of every lure fires immediately
    val luresToFire = current.structures.filterIsInstance<Lure>().filter { it.chargesLeft > 0 }
    for (lure in luresToFire) {
        current = fireLureCharge(current, lure)
    }

    // TICK LOOP: continue while any lure has charges left or enemies remain on map
    var tickCounter = 1
    while (hasActiveLures(current) || current.enemies.isNotEmpty()) {
        if (current.shouldEndGame() && !hasActiveLures(current)) break
        current = processOneTick(current, tickCounter)
        tickCounter++

        if (tickCounter > 200) {
            // Safety valve: prevent infinite loops during development
            return current.copy(phase = Phase.PLANNING, round = state.round + 1)
        }
    }

    return current.copy(phase = Phase.PLANNING, round = state.round + 1)
}

private fun hasActiveLures(state: GameState): Boolean {
    return state.structures.any { it is Lure && it.chargesLeft > 0 }
}

// --- Tick processing (pure functional — no mutation of inputs) ---

fun processOneTick(state: GameState, tick: Int): GameState {
    var current = state

    // 1. FIRE LURE CHARGES whose cooldown hit zero
    val luresToFire = current.structures.filterIsInstance<Lure>().filter {
        it.fireCooldownRemaining == 0 && it.chargesLeft > 0
    }
    for (lure in luresToFire) {
        current = fireLureCharge(current, lure)
    }

    // 2. DECREMENT LURE TIMERS (for lures not just fired)
    val luresNotFired = current.structures.filterIsInstance<Lure>().filter { it.fireCooldownRemaining > 0 }
    for (lure in luresNotFired) {
        current = decrementLureTimer(current, lure)
    }

    // 3. DECREMENT ALL ROOTED ENEMIES' root timer (regardless of cage status)
    val rootedEnemies = current.enemies.filter { it.rootRemaining > 0 }.toList()
    for (enemy in rootedEnemies) {
        current = decrementRootTimer(current, enemy)
    }

    // 3b. PROCESS CAGE CAPTURE — for enemies still rooted AND on a cage tile
    val stillRootedOnCage = current.enemies.filter { it.rootRemaining > 0 && onCageTile(current, it) }.toList()
    for (enemy in stillRootedOnCage) {
        current = advanceCaptureProgress(current, enemy)
    }

    // Remove captured enemies and award currency
    val capturedEnemies = current.enemies.filter { isCaptured(it) }.toList()
    if (capturedEnemies.isNotEmpty()) {
        current = awardCurrencyForCaptures(current, capturedEnemies.size)
        current = removeEnemiesByIds(current, capturedEnemies.map { it.id })
    }

    // 4. DECREMENT SNARE COOLDOWNS
    val snaresToDecrement = current.structures.filterIsInstance<Snare>().filter { it.cooldownRemaining > 0 }
    for (snare in snaresToDecrement) {
        current = decrementSnareCooldown(current, snare)
    }

    // 5. MOVE UNROOTED ENEMIES (process high tileIndex first to avoid ordering issues)
    val unrootedEnemies = current.enemies.filter { it.rootRemaining == 0 && !isCaptured(it) }.toList()
    for (enemy in unrootedEnemies.sortedByDescending { it.tileIndex.value }) {
        if (!current.enemies.any { it.id == enemy.id }) continue // skip if captured/removed this tick

        current = moveEnemy(current, enemy)
    }

    // 6. CLEANUP: remove spent lures (charges drained and timer at zero)
    current = current.copy(
        structures = current.structures.filterNot { it is Lure && it.chargesLeft == 0 && it.fireCooldownRemaining == 0 }
    )

    return current.copy(tick = tick)
}

// --- Helpers (all pure — no mutation of inputs) ---

private fun fireLureCharge(state: GameState, lure: Lure): GameState {
    val newEnemyId = state.enemies.maxOfOrNull { it.id }?.plus(1) ?: 1
    val enemy = Enemy(
        id = newEnemyId,
        laneId = lure.position.laneId,
        tileIndex = trap.towers.model.TileIndex(ScenarioConfig.tilesPerLane - 1),
        cageCaptureProgress = 0
    )
    val updatedLure = Lure(lure.position, lure.chargesLeft - 1, ScenarioConfig.lureChargeInterval)
    return state.copy(enemies = state.enemies + enemy, structures = replaceStructure(state.structures, lure, updatedLure))
}

private fun decrementLureTimer(state: GameState, lure: Lure): GameState {
    val updated = Lure(lure.position, lure.chargesLeft, lure.fireCooldownRemaining - 1)
    return state.copy(structures = replaceStructure(state.structures, lure, updated))
}

private fun decrementSnareCooldown(state: GameState, snare: Snare): GameState {
    val updated = Snare(snare.position, snare.cooldownRemaining - 1)
    return state.copy(structures = replaceStructure(state.structures, snare, updated))
}

private fun onCageTile(state: GameState, enemy: Enemy): Boolean {
    return structuresAt(state, enemy.toPosition()).any { it is Cage }
}

private fun decrementRootTimer(state: GameState, enemy: Enemy): GameState {
    val updated = Enemy(enemy.id, enemy.laneId, enemy.tileIndex, enemy.movingInward, enemy.rootRemaining - 1, enemy.cageCaptureProgress)
    return state.copy(enemies = replaceEnemy(state.enemies, enemy, updated))
}

private fun advanceCaptureProgress(state: GameState, enemy: Enemy): GameState {
    val progress = if (onCageTile(state, enemy)) enemy.cageCaptureProgress + 1 else enemy.cageCaptureProgress
    val updated = Enemy(enemy.id, enemy.laneId, enemy.tileIndex, enemy.movingInward, enemy.rootRemaining, progress)
    return state.copy(enemies = replaceEnemy(state.enemies, enemy, updated))
}

private fun isCaptured(enemy: Enemy): Boolean {
    return enemy.cageCaptureProgress >= ScenarioConfig.snareCooldownSteps
}

private fun moveEnemy(state: GameState, enemy: Enemy): GameState {
    var current = state

    if (enemy.movingInward) {
        val newTileIdx = enemy.tileIndex.value - 1
        if (newTileIdx < 0) return current // shouldn't happen but safety check

        val newPos = Position(enemy.laneId, trap.towers.model.TileIndex(newTileIdx))
        val structuresAtNewPos = structuresAt(current, newPos)
        val readySnare = structuresAtNewPos.filterIsInstance<Snare>().firstOrNull { it.cooldownRemaining == 0 }

        if (readySnare != null) {
            // Root the enemy and put snare on cooldown
            val updatedEnemy = Enemy(enemy.id, enemy.laneId, newPos.tileIndex, true, ScenarioConfig.snareCooldownSteps, 0)
            val updatedSnare = Snare(readySnare.position, ScenarioConfig.snareCooldownSteps)
            current = current.copy(
                enemies = replaceEnemy(current.enemies, enemy, updatedEnemy),
                structures = replaceStructure(current.structures, readySnare, updatedSnare)
            )
        } else {
            val lureAtPos = structuresAtNewPos.filterIsInstance<Lure>().firstOrNull()
            if (lureAtPos != null) {
                // Eat the bait: destroy lure, reverse direction
                val updatedEnemy = Enemy(enemy.id, enemy.laneId, newPos.tileIndex, false, 0, 0)
                current = current.copy(
                    enemies = replaceEnemy(current.enemies, enemy, updatedEnemy),
                    structures = removeStructureFromList(current.structures, lureAtPos)
                )
            } else {
                val updatedEnemy = Enemy(enemy.id, enemy.laneId, newPos.tileIndex, true, 0, 0)
                current = current.copy(enemies = replaceEnemy(current.enemies, enemy, updatedEnemy))
            }
        }
    } else {
        // Moving outward
        val newTileIdx = enemy.tileIndex.value + 1
        if (newTileIdx >= ScenarioConfig.tilesPerLane - 1) {
            // LEAK! Enemy reached exit
            val updatedEnemy = Enemy(enemy.id, enemy.laneId, trap.towers.model.TileIndex(newTileIdx), false, 0, 0)
            current = current.copy(
                villageHp = current.villageHp - ScenarioConfig.leakDamage,
                enemies = replaceEnemy(current.enemies, enemy, updatedEnemy).filter { it.id != enemy.id }
            )
        } else {
            val updatedEnemy = Enemy(enemy.id, enemy.laneId, trap.towers.model.TileIndex(newTileIdx), false, 0, 0)
            current = current.copy(enemies = replaceEnemy(current.enemies, enemy, updatedEnemy))
        }
    }

    return current
}

private fun awardCurrencyForCaptures(state: GameState, count: Int): GameState {
    return state.copy(currency = state.currency + (count * ScenarioConfig.captureReward))
}

private fun removeEnemiesByIds(state: GameState, ids: List<Int>): GameState {
    val idSet = ids.toSet()
    return state.copy(enemies = state.enemies.filter { it.id !in idSet })
}

private fun structuresAt(state: GameState, position: Position): List<Structure> {
    return state.structures.filter { it.position == position }
}

private fun removeStructureFromList(structures: List<Structure>, target: Structure): List<Structure> {
    return structures.filter { !sameStructure(it, target) }
}

private fun replaceStructure(structures: List<Structure>, old: Structure, new: Structure): List<Structure> {
    return structures.map { if (sameStructure(it, old)) new else it }
}

private fun replaceEnemy(enemies: List<Enemy>, old: Enemy, new: Enemy): List<Enemy> {
    return enemies.map { if (it.id == old.id) new else it }
}

// Identity comparison for structures — match by type and position
private fun sameStructure(a: Structure, b: Structure): Boolean {
    return a::class == b::class && a.position == b.position
}
