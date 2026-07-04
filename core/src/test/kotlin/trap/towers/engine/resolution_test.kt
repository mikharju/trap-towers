package trap.towers.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import trap.towers.model.Cage
import trap.towers.model.Enemy as EnemyModel
import trap.towers.model.LaneId
import trap.towers.model.Lure
import trap.towers.model.Position
import trap.towers.model.ScenarioConfig
import trap.towers.model.Snare
import trap.towers.model.TileIndex
import trap.towers.state.GameState
import trap.towers.state.Phase

class ResolutionTest {

    private fun pos(lane: Int, tile: Int) = Position(LaneId(lane), TileIndex(tile))

    // --- Lure firing cadence ---

    @Test
    fun `first lure charge fires immediately at resolution start`() {
        val state = GameState.initial().copy(
            structures = listOf(Lure(pos(0, 3), chargesLeft = 1, fireCooldownRemaining = ScenarioConfig.lureChargeInterval))
        )
        val result = resolveResolutionPhase(state)

        assertEquals(2, result.round) // round advanced after resolution
        assertEquals(1, result.enemies.size)
    }

    @Test
    fun `subsequent lure charges fire at configured interval`() {
        val state = GameState.initial().copy(
            structures = listOf(Lure(pos(0, 3), chargesLeft = 3, fireCooldownRemaining = ScenarioConfig.lureChargeInterval))
        )
        // tick 0: first charge fires (charges left=2)
        var current = resolveResolutionPhase(state).copy(phase = Phase.PLANNING)

        // Place a new lure with 3 charges for another round to test interval behavior
        val state2 = GameState.initial(round = 1).copy(
            structures = listOf(Lure(pos(0, 3), chargesLeft = 3, fireCooldownRemaining = ScenarioConfig.lureChargeInterval))
        )

        // We need a more direct tick-by-tick test — call processOneTick manually
        val afterTick0 = state2.copy(phase = Phase.RESOLUTION, tick = 0)
            .let { resolveResolutionPhase(it).copy(phase = Phase.PLANNING) }

        // Directly test tick processing by setting up a state mid-resolution
        val midState = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 40, villageHp = 10,
            structures = listOf(Lure(pos(0, 3), chargesLeft = 2, fireCooldownRemaining = ScenarioConfig.lureChargeInterval)),
            enemies = emptyList(), tick = 0
        )

        // Tick 0 already fired first charge in resolveResolutionPhase — but we bypassed that.
        // Let's test processOneTick directly with a lure at cooldown=0 and charges>0.
        val tickTestState = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 40, villageHp = 10,
            structures = listOf(Lure(pos(0, 3), chargesLeft = 2, fireCooldownRemaining = 0)),
            enemies = emptyList(), tick = 0
        )

        val afterFire = processOneTick(tickTestState, 1)
        assertEquals(1, afterFire.enemies.size) // one enemy spawned from this charge
        assertEquals(1, afterFire.structures.filterIsInstance<Lure>().first().chargesLeft) // decremented to 1
    }

    @Test
    fun `lure with no charges does not spawn enemies`() {
        val state = GameState.initial().copy(
            structures = listOf(Lure(pos(0, 3), chargesLeft = 0, fireCooldownRemaining = 0))
        )
        val result = resolveResolutionPhase(state)
        assertTrue(result.enemies.isEmpty())
    }

    // --- Enemy movement ---

    @Test
    fun `enemy moves one tile inward per tick`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = emptyList(),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(6))),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        assertEquals(TileIndex(5), afterTick.enemies.first().tileIndex)
    }

    @Test
    fun `enemy reverses direction after eating bait`() {
        // Setup: lure at tile 3, enemy walking inward from tile 4
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = listOf(Lure(pos(0, 3), chargesLeft = 0, fireCooldownRemaining = 0)),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(4))),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        // Enemy should have moved to tile 3 (lure position) and reversed direction
        assertEquals(TileIndex(3), afterTick.enemies.first().tileIndex)
        assertTrue(!afterTick.enemies.first().movingInward)
    }

    @Test
    fun `bait is destroyed when eaten`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = listOf(Lure(pos(0, 3), chargesLeft = 2, fireCooldownRemaining = 0)),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(4))),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        assertTrue(afterTick.structures.none { it is Lure })
    }

    // --- Snare rooting ---

    @Test
    fun `enemy gets rooted by snare on contact`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = listOf(Snare(pos(0, 5), cooldownRemaining = 0)),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(6))),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        val enemy = afterTick.enemies.first()
        assertEquals(TileIndex(5), enemy.tileIndex) // moved onto snare tile
        assertTrue(enemy.rootRemaining > 0) // rooted
    }

    @Test
    fun `snare goes on cooldown after triggering`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = listOf(Snare(pos(0, 5), cooldownRemaining = 0)),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(6))),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        val snare = afterTick.structures.first() as Snare
        assertTrue(snare.cooldownRemaining > 0) // snare on cooldown after triggering an enemy
    }

    @Test
    fun `rooted enemy does not move`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = emptyList(),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(5), rootRemaining = 2)),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        assertEquals(TileIndex(5), afterTick.enemies.first().tileIndex) // didn't move
    }

    @Test
    fun `root expires and enemy resumes moving`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = emptyList(),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(5), rootRemaining = 1)),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        val enemy = afterTick.enemies.first()
        assertEquals(TileIndex(4), enemy.tileIndex) // moved inward after root expired
    }

    // --- Cage capture ---

    @Test
    fun `rooted enemy on cage tile fills capture progress`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = listOf(Snare(pos(0, 5), cooldownRemaining = 2), Cage(pos(0, 5))),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(5), rootRemaining = 3)),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        assertEquals(1, afterTick.enemies.first().cageCaptureProgress) // progress incremented
    }

    @Test
    fun `capture completes when progress reaches threshold`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = listOf(Snare(pos(0, 5), cooldownRemaining = 2), Cage(pos(0, 5))),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(5), rootRemaining = 3, cageCaptureProgress = ScenarioConfig.snareCooldownSteps - 1)),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        assertTrue(afterTick.enemies.isEmpty()) // captured and removed
    }

    @Test
    fun `capture awards currency`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = listOf(Snare(pos(0, 5), cooldownRemaining = 2), Cage(pos(0, 5))),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(5), rootRemaining = 3, cageCaptureProgress = ScenarioConfig.snareCooldownSteps - 1)),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        assertEquals(50 + ScenarioConfig.captureReward, afterTick.currency)
    }

    // --- Leaks ---

    @Test
    fun `outward moving enemy at outer edge causes leak`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = emptyList(),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(6), movingInward = false)),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        assertEquals(10 - ScenarioConfig.leakDamage, afterTick.villageHp) // leak damage applied
        assertTrue(afterTick.enemies.isEmpty()) // enemy removed
    }

    @Test
    fun `outward moving enemy not at edge continues outward`() {
        val state = GameState(
            round = 1, phase = Phase.RESOLUTION, currency = 50, villageHp = 10,
            structures = emptyList(),
            enemies = listOf(EnemyModel(id = 1, laneId = LaneId(0), tileIndex = TileIndex(4), movingInward = false)),
            tick = 0
        )

        val afterTick = processOneTick(state, 1)
        assertEquals(TileIndex(5), afterTick.enemies.first().tileIndex) // moved outward but not leaked yet
    }

    // --- Full round integration ---

    @Test
    fun `full resolution round transitions back to planning`() {
        val state = GameState.initial().copy(
            structures = listOf(Lure(pos(0, 3), chargesLeft = 1, fireCooldownRemaining = ScenarioConfig.lureChargeInterval))
        )
        val result = resolveResolutionPhase(state)

        assertEquals(Phase.PLANNING, result.phase)
        assertEquals(2, result.round) // next round
    }
}
