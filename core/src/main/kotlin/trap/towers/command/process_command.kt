package trap.towers.command

import trap.towers.model.Cage
import trap.towers.model.Lure
import trap.towers.model.ScenarioConfig
import trap.towers.model.Snare
import trap.towers.state.GameState
import trap.towers.state.Phase

enum class PlacementResult { SUCCESS, INSUFFICIENT_CURRENCY, TILE_OCCUPIED, INVALID_PHASE, INVALID_POSITION }

fun processCommand(state: GameState, command: Command): Pair<GameState, PlacementResult> {
    return when (command) {
        is Command.EndTurn -> handleEndTurn(state)
        is Command.PlaceLure -> handlePlacement(state, command.position, ScenarioConfig.Costs.Lure) { Lure(command.position, 3, ScenarioConfig.lureChargeInterval) }
        is Command.PlaceSnare -> handlePlacement(state, command.position, ScenarioConfig.Costs.Snare) { Snare(command.position, ScenarioConfig.snareCooldownSteps) }
        is Command.PlaceCage -> handlePlacement(state, command.position, ScenarioConfig.Costs.Cage) { Cage(command.position) }
    }
}

private fun handleEndTurn(state: GameState): Pair<GameState, PlacementResult> {
    if (state.phase != Phase.PLANNING) return state to PlacementResult.INVALID_PHASE
    val newTick = if (state.tick == 0) 0 else state.tick
    return state.copy(
        phase = Phase.RESOLUTION,
        tick = newTick
    ) to PlacementResult.SUCCESS
}

private fun <T : trap.towers.model.Structure> handlePlacement(
    state: GameState,
    position: trap.towers.model.Position,
    cost: Int,
    factory: () -> T
): Pair<GameState, PlacementResult> {
    if (state.phase != Phase.PLANNING) return state to PlacementResult.INVALID_PHASE
    if (!isValidPosition(position)) return state to PlacementResult.INVALID_POSITION
    if (state.currency < cost) return state to PlacementResult.INSUFFICIENT_CURRENCY
    if (structureAt(state, position) != null) return state to PlacementResult.TILE_OCCUPIED

    val newStructure = factory()
    val newState = state.copy(
        currency = state.currency - cost,
        structures = state.structures + newStructure
    )
    return newState to PlacementResult.SUCCESS
}

private fun structureAt(state: GameState, position: trap.towers.model.Position): trap.towers.model.Structure? {
    return state.structures.firstOrNull { it.position == position }
}

private fun isValidPosition(position: trap.towers.model.Position): Boolean {
    return position.laneId.value in 0 until ScenarioConfig.laneCount &&
            position.tileIndex.value in 0 until ScenarioConfig.tilesPerLane
}
