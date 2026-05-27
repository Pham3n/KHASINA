package play.zulu.khasina

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

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

data class Construction(
    val ownerId: String, // "Player" or "AI" (or "Opponent" in multiplayer)
    val targetValue: Int,
    val cards: SnapshotStateList<Card> = mutableStateListOf()
) {
    val topCard: Card? get() = cards.lastOrNull()
}

class GameEngine {
    val deck = mutableListOf<Card>()
    val playerHand = mutableStateListOf<Card>()
    val aiHand = mutableStateListOf<Card>()
    val floor = mutableStateListOf<Card>()
    val playerStack = mutableStateListOf<Card>()
    val aiStack = mutableStateListOf<Card>()
    
    val constructions = mutableStateListOf<Construction>()
    
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
        constructions.clear()
        
        deal()
    }

    fun deal() {
        repeat(10) {
            if (deck.isNotEmpty()) playerHand.add(deck.removeAt(0))
            if (deck.isNotEmpty()) aiHand.add(deck.removeAt(0))
        }
    }

    // Manual play logic as requested
    fun executeManualCapture(
        playedCard: Card,
        selectedFloorCards: List<Card>,
        selectedConstructions: List<Construction>,
        recoveredOpponentCard: Card?,
        isPlayer: Boolean
    ): Boolean {
        val hand = if (isPlayer) playerHand else aiHand
        val stack = if (isPlayer) playerStack else aiStack
        val opponentStack = if (isPlayer) aiStack else playerStack
        val ownerId = if (isPlayer) "Player" else "AI"

        if (!hand.contains(playedCard)) return false

        // 1. Validate target value
        // The total value of selected items must equal the playedCard.value OR 
        // we are adding to a construction (total + card = targetValue)
        
        val totalFloorValue = selectedFloorCards.sumOf { it.value }
        val totalConstructionValue = selectedConstructions.sumOf { it.targetValue }
        val opponentValue = recoveredOpponentCard?.value ?: 0
        
        val totalSelectedValue = totalFloorValue + totalConstructionValue + opponentValue
        
        // Option A: Full Capture (matching rank or sum)
        if (totalSelectedValue == playedCard.value) {
            stack.addAll((selectedFloorCards + selectedConstructions.flatMap { it.cards }).sortedByDescending { it.value })
            if (recoveredOpponentCard != null) {
                stack.add(recoveredOpponentCard)
                opponentStack.remove(recoveredOpponentCard)
            }
            stack.add(playedCard)
            
            floor.removeAll(selectedFloorCards)
            constructions.removeAll(selectedConstructions)
            hand.remove(playedCard)
            checkEndOfRound()
            return true
        }
        
        // Option B: Construction Creation/Addition
        // Find existing construction if it exists
        val myConstruction = constructions.find { it.ownerId == ownerId }
        
        if (myConstruction != null) {
            // Already have a construction, can we add to it?
            if (totalSelectedValue + playedCard.value == myConstruction.targetValue) {
                myConstruction.cards.addAll(selectedFloorCards.sortedByDescending { it.value })
                if (recoveredOpponentCard != null) {
                    myConstruction.cards.add(recoveredOpponentCard)
                    opponentStack.remove(recoveredOpponentCard)
                }
                myConstruction.cards.add(playedCard)
                
                floor.removeAll(selectedFloorCards)
                hand.remove(playedCard)
                checkEndOfRound()
                return true
            }
        } else {
            // No construction, try to create one if selected value + played card > 0
            // In Casino, usually you define a target value. For basic version, 
            // if you didn't match the card value exactly, we assume you are building 
            // to a higher value in your hand. This requires multi-stage play.
            // But rules say: "Construction may only start with floor cards and one from the player hand"
            // Let's assume for now the target value is what you say it is or what matches a card in hand.
        }

        // Invalid play: "selected card from the player hand will go to floor"
        hand.remove(playedCard)
        floor.add(playedCard)
        isPlayerTurn = !isPlayer
        checkEndOfRound()
        return false
    }

    private fun checkEndOfRound() {
        if (playerHand.isEmpty() && aiHand.isEmpty()) {
            if (deck.isNotEmpty()) {
                deal()
            } else {
                gameOver = true
            }
        }
    }

    // Temporary basic capture for AI/Legacy compatibility
    fun playCard(card: Card, isPlayer: Boolean): Boolean {
        // Find automatic capture if possible
        val hand = if (isPlayer) playerHand else aiHand
        val stack = if (isPlayer) playerStack else aiStack
        
        if (!hand.contains(card)) return false
        
        val captured = findCapture(card, floor)
        if (captured.isNotEmpty()) {
            stack.addAll(captured.sortedByDescending { it.value })
            stack.add(card)
            floor.removeAll(captured)
        } else {
            floor.add(card)
        }
        
        hand.remove(card)
        checkEndOfRound()
        
        if (!gameOver) {
            isPlayerTurn = !isPlayerTurn
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
            } else break
        }
        return result
    }

    private fun findSubsetSum(cards: List<Card>, target: Int): List<Card> {
        for (i in cards.indices) {
            if (cards[i].value == target) return listOf(cards[i])
            for (j in i + 1 until cards.size) {
                if (cards[i].value + cards[j].value == target) return listOf(cards[i], cards[j])
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
        deck.clear(); deck.addAll(state.deck)
        playerHand.clear(); playerHand.addAll(state.playerHand)
        aiHand.clear(); aiHand.addAll(state.aiHand)
        floor.clear(); floor.addAll(state.floor)
        playerStack.clear(); playerStack.addAll(state.playerStack)
        aiStack.clear(); aiStack.addAll(state.aiStack)
        constructions.clear()
        state.constructions.forEach { cs ->
            val c = Construction(cs.ownerId, cs.targetValue)
            c.cards.addAll(cs.cards)
            constructions.add(c)
        }
        isPlayerTurn = state.isPlayerTurn
        gameOver = state.gameOver
    }

    fun exportState(): GameState {
        return GameState(
            deck.toList(), playerHand.toList(), aiHand.toList(), floor.toList(),
            playerStack.toList(), aiStack.toList(),
            constructions.map { ConstructionState(it.ownerId, it.targetValue, it.cards.toList()) },
            isPlayerTurn, gameOver
        )
    }
}
