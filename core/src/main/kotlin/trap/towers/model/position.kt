package trap.towers.model

data class LaneId(val value: Int) {
    init { require(value in 0 until ScenarioConfig.laneCount) }
}

data class TileIndex(val value: Int) {
    init { require(value in 0 until ScenarioConfig.tilesPerLane) }
}

data class Position(val laneId: LaneId, val tileIndex: TileIndex) {
    companion object {
        fun base(laneId: LaneId) = Position(laneId, TileIndex(0))
        fun outermost(laneId: LaneId) = Position(laneId, TileIndex(ScenarioConfig.tilesPerLane - 1))
    }

    val laneNumber: Int get() = laneId.value + 1
    val index: Int get() = tileIndex.value
}
