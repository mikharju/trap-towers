package trap.towers.command

import trap.towers.model.Position

sealed class Command {
    object EndTurn : Command()
    data class PlaceLure(val position: Position) : Command()
    data class PlaceSnare(val position: Position) : Command()
    data class PlaceCage(val position: Position) : Command()
}
