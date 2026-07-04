package trap.towers.model

sealed class Structure {
    abstract val position: Position
}

data class Lure(
    override val position: Position,
    var chargesLeft: Int,
    var fireCooldownRemaining: Int
) : Structure()

data class Snare(
    override val position: Position,
    var cooldownRemaining: Int
) : Structure()

data class Cage(override val position: Position) : Structure()
