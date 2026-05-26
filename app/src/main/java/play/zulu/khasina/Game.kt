package play.zulu.khasina

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Suit { SPADES, DIAMONDS, HEARTS, CLUBS }

data class Card(val rank: Int, val suit: Suit) {
    val value: Int = rank
    val points: Int
        get() = when {
            rank == 1 -> 1
            rank == 10 && suit == Suit.DIAMONDS -> 2
            rank == 2 && suit == Suit.SPADES -> 1
            else -> 0
        }

    override fun toString(): String {
        val r = when (rank) {
            1 -> "A"
            else -> rank.toString()
        }
        val s = when (suit) {
            Suit.SPADES -> "♠"
            Suit.DIAMONDS -> "♦"
            Suit.HEARTS -> "♥"
            Suit.CLUBS -> "♣"
        }
        return "$r$s"
    }
}

class GameEngine {
    val deck = mutableListOf<Card>()
    val playerHand = mutableStateListOf<Card>()
    val aiHand = mutableStateListOf<Card>()
    val floor = mutableStateListOf<Card>()
    val playerStack = mutableStateListOf<Card>()
    val aiStack = mutableStateListOf<Card>()
    
    var isPlayerTurn by mutableStateOf(true)
    var gameOver by mutableStateOf(false)
    var useAI by mutableStateOf(true)

    init {
        setupGame()
    }

    fun setupGame() {
        val allCards = mutableListOf<Card>()
        for (suit in Suit.values()) {
            for (rank in 1..10) {
                allCards.add(Card(rank, suit))
            }
        }
        allCards.shuffle()
        deck.clear()
        deck.addAll(allCards)
        
        deal()
    }

    fun deal() {
        repeat(10) {
            if (deck.isNotEmpty()) playerHand.add(deck.removeAt(0))
            if (deck.isNotEmpty()) aiHand.add(deck.removeAt(0))
        }
    }

    fun playCard(card: Card, isPlayer: Boolean): Boolean {
        val hand = if (isPlayer) playerHand else aiHand
        val stack = if (isPlayer) playerStack else aiStack
        
        if (!hand.contains(card)) return false
        
        val captured = findCapture(card, floor)
        if (captured.isNotEmpty()) {
            // Rules Confirm: "the top-most card must be shown"
            // The played card is placed ON TOP of the captured combination.
            stack.addAll(captured.sortedBy { it.value }) // Put smaller cards at the bottom of the new capture group
            stack.add(card) // The played card is the absolute top-most
            floor.removeAll(captured)
        } else {
            floor.add(card)
        }
        
        hand.remove(card)
        
        if (playerHand.isEmpty() && aiHand.isEmpty()) {
            if (deck.isNotEmpty()) {
                deal()
            } else {
                gameOver = true
            }
        }
        
        if (!gameOver) {
            if (isPlayer) {
                isPlayerTurn = false
            } else {
                isPlayerTurn = true
            }
        }
        return true
    }

    private fun findCapture(playCard: Card, floorCards: List<Card>): List<Card> {
        val result = mutableListOf<Card>()
        val target = playCard.value
        
        val matches = floorCards.filter { it.value == target }
        result.addAll(matches)
        
        val remainingFloor = floorCards.filter { it.value != target }.toMutableList()
        
        while (remainingFloor.isNotEmpty()) {
            val subset = findSubsetSum(remainingFloor, target)
            if (subset.isNotEmpty()) {
                result.addAll(subset)
                remainingFloor.removeAll(subset)
            } else {
                break
            }
        }
        return result
    }

    private fun findSubsetSum(cards: List<Card>, target: Int): List<Card> {
        for (i in cards.indices) {
            if (cards[i].value == target) return listOf(cards[i])
            for (j in i + 1 until cards.size) {
                if (cards[i].value + cards[j].value == target) {
                    return listOf(cards[i], cards[j])
                }
            }
        }
        return emptyList()
    }

    fun aiTurn() {
        if (aiHand.isEmpty()) return
        
        var bestCard: Card? = null
        var bestCaptureCount = -1
        
        for (card in aiHand) {
            val capture = findCapture(card, floor)
            if (capture.size > bestCaptureCount) {
                bestCaptureCount = capture.size
                bestCard = card
            }
        }
        
        val cardToPlay = bestCard ?: aiHand.minByOrNull { it.rank } ?: aiHand[0]
        playCard(cardToPlay, false)
    }

    fun calculateScores(): Map<String, Int> {
        var pScore = playerStack.sumOf { it.points }
        var aScore = aiStack.sumOf { it.points }
        
        if (playerStack.size > aiStack.size) pScore += 1
        else if (aiStack.size > playerStack.size) aScore += 1
        
        val pSpades = playerStack.count { it.suit == Suit.SPADES }
        val aSpades = aiStack.count { it.suit == Suit.SPADES }
        if (pSpades >= 6) pScore += 1
        if (aSpades >= 6) aScore += 1
        
        return mapOf("Player" to pScore, "AI" to aScore)
    }

    fun importState(state: GameState) {
        deck.clear()
        deck.addAll(state.deck)
        playerHand.clear()
        playerHand.addAll(state.playerHand)
        aiHand.clear()
        aiHand.addAll(state.aiHand)
        floor.clear()
        floor.addAll(state.floor)
        playerStack.clear()
        playerStack.addAll(state.playerStack)
        aiStack.clear()
        aiStack.addAll(state.aiStack)
        isPlayerTurn = state.isPlayerTurn
        gameOver = state.gameOver
    }

    fun exportState(): GameState {
        return GameState(
            deck.toList(),
            playerHand.toList(),
            aiHand.toList(),
            floor.toList(),
            playerStack.toList(),
            aiStack.toList(),
            isPlayerTurn,
            gameOver
        )
    }
}
