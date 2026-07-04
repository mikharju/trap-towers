package trap.towers.model

data class Enemy(
    val id: Int,
    val laneId: LaneId,
    var tileIndex: TileIndex,
    var movingInward: Boolean = true,
    var rootRemaining: Int = 0,
    var cageCaptureProgress: Int = 0
) {
    fun toPosition(): Position = Position(laneId, tileIndex)

    fun isAtOuterEdge(): Boolean = tileIndex.value >= ScenarioConfig.tilesPerLane - 1
    fun isAtBase(): Boolean = tileIndex.value == 0
}
