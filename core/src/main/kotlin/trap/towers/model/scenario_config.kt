package trap.towers.model

object ScenarioConfig {
    const val laneCount: Int = 3
    const val tilesPerLane: Int = 8
    const val initialCurrency: Int = 50
    const val villageMaxHp: Int = 10
    const val leakDamage: Int = 2
    const val captureReward: Int = 15
    const val totalRounds: Int = 5

    const val lureChargeInterval: Int = 2
    const val snareCooldownSteps: Int = 2

    object Costs {
        const val Lure: Int = 10
        const val Snare: Int = 8
        const val Cage: Int = 12
    }
}
