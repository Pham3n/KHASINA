package play.zulu.khasina

data class GameState(
    val deck: List<Card>,
    val playerHand: List<Card>,
    val aiHand: List<Card>,
    val floor: List<Card>,
    val playerStack: List<Card>,
    val aiStack: List<Card>,
    val isPlayerTurn: Boolean,
    val gameOver: Boolean
)
