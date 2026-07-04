package trap.towers.state

import trap.towers.model.Enemy
import trap.towers.model.Lure
import trap.towers.model.ScenarioConfig
import trap.towers.model.Structure

data class GameState(
    val round: Int,
    val phase: Phase,
    val currency: Int,
    val villageHp: Int,
    val structures: List<Structure>,
    val enemies: List<Enemy>,
    val tick: Int = 0
) {
    fun isLost(): Boolean = villageHp <= 0

    fun shouldEndGame(): Boolean =
        isLost() || (phase == Phase.RESOLUTION && noActiveLures() && noEnemies())

    private fun noActiveLures(): Boolean = structures.none { it is Lure }
    private fun noEnemies(): Boolean = enemies.isEmpty()

    companion object {
        fun initial(round: Int = 1): GameState = GameState(
            round = round,
            phase = Phase.PLANNING,
            currency = ScenarioConfig.initialCurrency,
            villageHp = ScenarioConfig.villageMaxHp,
            structures = emptyList(),
            enemies = emptyList()
        )
    }
}
