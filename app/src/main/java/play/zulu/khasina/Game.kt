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
    val ownerId: String, 
    val targetValue: Int,
    val cards: SnapshotStateList<Card> = mutableStateListOf()
) {
    val topCard: Card? get() = cards.lastOrNull()
}

class GameEngine(val playerCount: Int = 2) {
    val deck = mutableListOf<Card>()
    val hands = List(playerCount) { mutableStateListOf<Card>() }
    val floor = mutableStateListOf<Card>()
    // Team 0: Players 0 and 2. Team 1: Players 1 and 3.
    val teamStacks = List(2) { mutableStateListOf<Card>() }
    
    val constructions = mutableStateListOf<Construction>()
    
    var currentPlayerIndex by mutableStateOf(0)
    var gameOver by mutableStateOf(false)
    var useAI by mutableStateOf(true)

    // Helper properties for UI (Local Player is Index 0)
    val playerHand: SnapshotStateList<Card> get() = hands[0]
    val aiHand: SnapshotStateList<Card> get() = if (playerCount > 1) hands[1] else mutableStateListOf()
    val playerStack: SnapshotStateList<Card> get() = teamStacks[0]
    val aiStack: SnapshotStateList<Card> get() = teamStacks[1]
    val isPlayerTurn: Boolean get() = currentPlayerIndex == 0

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
        floor.clear()
        teamStacks.forEach { it.clear() }
        hands.forEach { it.clear() }
        
        deal()
        currentPlayerIndex = 0
        gameOver = false
    }

    fun deal() {
        val cardsToDeal = if (playerCount == 2) 10 else 4 // In 4 players, deal 4 initially and 4 each round
        repeat(cardsToDeal) {
            for (i in 0 until playerCount) {
                if (deck.isNotEmpty()) hands[i].add(deck.removeAt(0))
            }
        }
        // Initially deal 4 to the floor if it's empty
        if (floor.isEmpty() && deck.isNotEmpty()) {
            repeat(4) { if (deck.isNotEmpty()) floor.add(deck.removeAt(0)) }
        }
    }

    fun executeManualPlay(
        playedCard: Card,
        selectedFloorCards: List<Card>,
        selectedConstructions: List<Construction>,
        recoveredOpponentCard: Card?,
        playerIndex: Int,
        isCapture: Boolean
    ): Boolean {
        val hand = hands[playerIndex]
        val teamIndex = playerIndex % 2
        val stack = teamStacks[teamIndex]
        val opponentStack = teamStacks[1 - teamIndex]
        val ownerId = "Player$playerIndex"

        if (!hand.contains(playedCard)) return false

        val floorVal = selectedFloorCards.sumOf { it.value }
        val constructVal = selectedConstructions.sumOf { it.targetValue }
        val oppVal = recoveredOpponentCard?.value ?: 0
        val totalSelectedValue = floorVal + constructVal + oppVal

        if (isCapture) {
            if (totalSelectedValue > 0 && totalSelectedValue % playedCard.value == 0) {
                if (selectedConstructions.any { it.targetValue != playedCard.value }) return false

                val allCardsToCapture = mutableListOf<Card>()
                allCardsToCapture.addAll(selectedFloorCards)
                selectedConstructions.forEach { allCardsToCapture.addAll(it.cards) }
                if (recoveredOpponentCard != null) {
                    allCardsToCapture.add(recoveredOpponentCard)
                    opponentStack.remove(recoveredOpponentCard)
                }
                
                stack.addAll(allCardsToCapture.sortedByDescending { it.value })
                stack.add(playedCard)
                
                floor.removeAll(selectedFloorCards)
                constructions.removeAll(selectedConstructions)
                hand.remove(playedCard)
                checkEndOfRound()
                return true
            }
        } else {
            var myExisting = constructions.find { it.ownerId == ownerId }
            val sum = totalSelectedValue + playedCard.value
            
            val targetValue = if (myExisting != null) {
                myExisting.targetValue
            } else {
                val remainingHandCards = hand.toMutableList()
                remainingHandCards.remove(playedCard)
                remainingHandCards.map { it.value }.distinct()
                    .filter { sum % it == 0 }
                    .maxOrNull() ?: -1 
            }

            val remainingHand = hand.toMutableList()
            remainingHand.remove(playedCard)
            val hasTargetInHand = remainingHand.any { it.value == targetValue }

            if (targetValue > 0 && hasTargetInHand && sum % targetValue == 0) {
                if (myExisting == null) {
                    myExisting = Construction(ownerId, targetValue)
                    constructions.add(myExisting)
                }
                
                myExisting.cards.addAll(selectedFloorCards)
                selectedConstructions.forEach { 
                    if (it != myExisting) {
                        myExisting!!.cards.addAll(it.cards)
                        constructions.remove(it)
                    }
                }
                if (recoveredOpponentCard != null) {
                    myExisting.cards.add(recoveredOpponentCard)
                    opponentStack.remove(recoveredOpponentCard)
                }
                myExisting.cards.add(playedCard)
                
                floor.removeAll(selectedFloorCards)
                hand.remove(playedCard)
                checkEndOfRound()
                return true
            }
        }

        // Invalid or simple throw
        hand.remove(playedCard)
        floor.add(playedCard)
        nextTurn()
        checkEndOfRound()
        return false
    }

    fun nextTurn() {
        if (!gameOver) {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerCount
        }
    }

    private fun checkEndOfRound() {
        if (hands.all { it.isEmpty() }) {
            if (deck.isNotEmpty()) {
                deal()
            } else {
                gameOver = true
            }
        }
    }

    fun playCard(card: Card, playerIndex: Int): Boolean {
        val hand = hands[playerIndex]
        val teamIndex = playerIndex % 2
        val stack = teamStacks[teamIndex]
        if (!hand.contains(card)) return false
        val captured = findCapture(card, floor)
        if (captured.isNotEmpty()) {
            stack.addAll(captured.sortedByDescending { it.value })
            stack.add(card)
            floor.removeAll(captured)
        } else floor.add(card)
        hand.remove(card)
        checkEndOfRound()
        nextTurn()
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

    fun calculateScores(): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        for (i in 0..1) {
            val stack = teamStacks[i]
            var score = stack.sumOf { it.points }
            val spades = stack.count { it.suit == Suit.SPADES }
            if (spades >= 6) score += 1
            scores["Team$i"] = score
        }
        // Most cards bonus
        if (teamStacks[0].size > teamStacks[1].size) scores["Team0"] = scores["Team0"]!! + 1
        else if (teamStacks[1].size > teamStacks[0].size) scores["Team1"] = scores["Team1"]!! + 1
        
        return scores
    }

    fun importState(state: GameState) {
        deck.clear(); deck.addAll(state.deck)
        floor.clear(); floor.addAll(state.floor)
        currentPlayerIndex = state.currentPlayerIndex
        gameOver = state.gameOver
        
        for (i in 0 until playerCount) {
            if (i < state.hands.size) {
                hands[i].clear()
                hands[i].addAll(state.hands[i])
            }
        }
        for (i in 0 until 2) {
            if (i < state.teamStacks.size) {
                teamStacks[i].clear()
                teamStacks[i].addAll(state.teamStacks[i])
            }
        }

        constructions.clear()
        state.constructions.forEach { cs ->
            val c = Construction(cs.ownerId, cs.targetValue)
            c.cards.addAll(cs.cards)
            constructions.add(c)
        }
    }

    fun exportState(): GameState {
        return GameState(
            deck = deck.toList(),
            hands = hands.map { it.toList() },
            floor = floor.toList(),
            teamStacks = teamStacks.map { it.toList() },
            constructions = constructions.map { ConstructionState(it.ownerId, it.targetValue, it.cards.toList()) },
            currentPlayerIndex = currentPlayerIndex,
            gameOver = gameOver,
            playerCount = playerCount
        )
    }
}
