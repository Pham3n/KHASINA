package play.zulu.khasina

data class GameState(
    val deck: List<Card>,
    val hands: List<List<Card>>,
    val floor: List<Card>,
    val teamStacks: List<List<Card>>,
    val constructions: List<ConstructionState>,
    val currentPlayerIndex: Int,
    val gameOver: Boolean,
    val playerCount: Int = 2
)

data class ConstructionState(
    val ownerId: String,
    val targetValue: Int,
    val cards: List<Card>
)
