package trap.towers.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import trap.towers.model.ScenarioConfig

class GameStateTest {

    @Test
    fun `initial state has correct defaults`() {
        val state = GameState.initial()
        assertEquals(1, state.round)
        assertEquals(Phase.PLANNING, state.phase)
        assertEquals(ScenarioConfig.initialCurrency, state.currency)
        assertEquals(ScenarioConfig.villageMaxHp, state.villageHp)
        assertTrue(state.structures.isEmpty())
        assertTrue(state.enemies.isEmpty())
        assertEquals(0, state.tick)
    }

    @Test
    fun `isLost returns false at start`() {
        val state = GameState.initial()
        assertFalse(state.isLost())
    }

    @Test
    fun `isLost returns true when villageHp is zero`() {
        val state = GameState.initial().copy(villageHp = 0)
        assertTrue(state.isLost())
    }

    @Test
    fun `isLost returns false when villageHp is positive`() {
        val state = GameState.initial().copy(villageHp = 1)
        assertFalse(state.isLost())
    }

    @Test
    fun `shouldEndGame is false during planning with no lures or enemies`() {
        val state = GameState.initial()
        // No active lures and no enemies, but phase is PLANNING — game should not end
        assertFalse(state.shouldEndGame())
    }

    @Test
    fun `shouldEndGame is true during resolution when no active lures and no enemies`() {
        val state = GameState.initial().copy(
            phase = Phase.RESOLUTION,
            structures = emptyList(),
            enemies = emptyList()
        )
        assertTrue(state.shouldEndGame())
    }

    @Test
    fun `shouldEndGame is false during resolution when lures still have charges`() {
        val state = GameState.initial().copy(
            phase = Phase.RESOLUTION,
            structures = listOf(trap.towers.model.Lure(
                trap.towers.model.Position(trap.towers.model.LaneId(0), trap.towers.model.TileIndex(3)),
                1, 2
            ))
        )
        assertFalse(state.shouldEndGame())
    }
}
