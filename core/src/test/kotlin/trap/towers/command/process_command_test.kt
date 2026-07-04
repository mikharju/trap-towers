package trap.towers.command

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import trap.towers.model.LaneId
import trap.towers.model.Position
import trap.towers.model.ScenarioConfig
import trap.towers.model.TileIndex
import trap.towers.state.GameState
import trap.towers.state.Phase

class ProcessCommandTest {

    private fun pos(lane: Int, tile: Int) = Position(LaneId(lane), TileIndex(tile))

    @Test
    fun `place lure deducts cost and adds structure`() {
        val state = GameState.initial()
        val (newState, result) = processCommand(state, Command.PlaceLure(pos(0, 3)))

        assertEquals(PlacementResult.SUCCESS, result)
        assertEquals(state.currency - ScenarioConfig.Costs.Lure, newState.currency)
        assertEquals(1, newState.structures.count { it is trap.towers.model.Lure })
    }

    @Test
    fun `place snare deducts cost and adds structure`() {
        val state = GameState.initial()
        val (newState, result) = processCommand(state, Command.PlaceSnare(pos(0, 5)))

        assertEquals(PlacementResult.SUCCESS, result)
        assertEquals(state.currency - ScenarioConfig.Costs.Snare, newState.currency)
        assertTrue(newState.structures.any { it is trap.towers.model.Snare && it.position == pos(0, 5) })
    }

    @Test
    fun `place cage deducts cost and adds structure`() {
        val state = GameState.initial()
        val (newState, result) = processCommand(state, Command.PlaceCage(pos(1, 4)))

        assertEquals(PlacementResult.SUCCESS, result)
        assertEquals(state.currency - ScenarioConfig.Costs.Cage, newState.currency)
        assertTrue(newState.structures.any { it is trap.towers.model.Cage && it.position == pos(1, 4) })
    }

    @Test
    fun `placement fails with insufficient currency`() {
        val state = GameState.initial().copy(currency = 0)
        val (_, result) = processCommand(state, Command.PlaceLure(pos(0, 3)))
        assertEquals(PlacementResult.INSUFFICIENT_CURRENCY, result)
    }

    @Test
    fun `placement fails on occupied tile`() {
        val state = GameState.initial().copy(
            structures = listOf(trap.towers.model.Lure(pos(0, 3), 1, 2))
        )
        val (_, result) = processCommand(state, Command.PlaceSnare(pos(0, 3)))
        assertEquals(PlacementResult.TILE_OCCUPIED, result)
    }

    @Test
    fun `placement fails during resolution phase`() {
        val state = GameState.initial().copy(phase = Phase.RESOLUTION)
        val (_, result) = processCommand(state, Command.PlaceLure(pos(0, 3)))
        assertEquals(PlacementResult.INVALID_PHASE, result)
    }

    @Test
    fun `end turn transitions to resolution phase`() {
        val state = GameState.initial()
        val (newState, result) = processCommand(state, Command.EndTurn)

        assertEquals(PlacementResult.SUCCESS, result)
        assertEquals(Phase.RESOLUTION, newState.phase)
    }

    @Test
    fun `end turn fails during resolution phase`() {
        val state = GameState.initial().copy(phase = Phase.RESOLUTION)
        val (_, result) = processCommand(state, Command.EndTurn)
        assertEquals(PlacementResult.INVALID_PHASE, result)
    }
}
