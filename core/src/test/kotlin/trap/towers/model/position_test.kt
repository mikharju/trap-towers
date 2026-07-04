package trap.towers.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PositionTest {

    @Test
    fun `base returns tile index 0`() {
        val pos = Position.base(LaneId(0))
        assertEquals(0, pos.tileIndex.value)
    }

    @Test
    fun `outermost returns last tile index on lane`() {
        val pos = Position.outermost(LaneId(1))
        assertEquals(ScenarioConfig.tilesPerLane - 1, pos.tileIndex.value)
    }

    @Test
    fun `laneNumber is one-based`() {
        assertEquals(1, LaneId(0).toPosition(TileIndex(3)).laneNumber)
        assertEquals(2, LaneId(1).toPosition(TileIndex(3)).laneNumber)
        assertEquals(3, LaneId(2).toPosition(TileIndex(3)).laneNumber)
    }

    @Test
    fun `invalid lane id throws`() {
        assertFailsWith<IllegalArgumentException> { LaneId(99) }
    }

    @Test
    fun `invalid tile index throws`() {
        assertFailsWith<IllegalArgumentException> { TileIndex(99) }
    }

    private fun LaneId.toPosition(ti: TileIndex) = Position(this, ti)
}
