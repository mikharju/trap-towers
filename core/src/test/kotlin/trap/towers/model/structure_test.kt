package trap.towers.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StructureTest {

    @Test
    fun `lure has correct initial state`() {
        val lure = Lure(Position(LaneId(0), TileIndex(3)), chargesLeft = 3, fireCooldownRemaining = 2)
        assertEquals(3, lure.chargesLeft)
        assertEquals(2, lure.fireCooldownRemaining)
        assertIs<Lure>(lure)
    }

    @Test
    fun `snare has correct initial state`() {
        val snare = Snare(Position(LaneId(1), TileIndex(5)), cooldownRemaining = 2)
        assertEquals(2, snare.cooldownRemaining)
        assertIs<Snare>(snare)
    }

    @Test
    fun `cage has no mutable state`() {
        val cage = Cage(Position(LaneId(0), TileIndex(5)))
        assertIs<Cage>(cage)
    }

    @Test
    fun `structures share position via data class equality`() {
        val pos = Position(LaneId(0), TileIndex(3))
        val lure1 = Lure(pos, 2, 2)
        val lure2 = Lure(pos, 2, 2)
        assertEquals(lure1, lure2)
    }
}
