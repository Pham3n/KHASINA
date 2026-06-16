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
    val ownerIndex: Int, 
    val targetValue: Int,
    val cards: SnapshotStateList<Card> = mutableStateListOf()
) {
    val topCard: Card? get() = cards.lastOrNull()
}

class GameEngine(val playerCount: Int = 2) {
    val deck = mutableListOf<Card>()
    val hands = List(playerCount) { mutableStateListOf<Card>() }
    val floor = mutableStateListOf<Card>()
    // Individual stacks for each player
    val privateStacks = List(playerCount) { mutableStateListOf<Card>() }
    
    val constructions = mutableStateListOf<Construction>()
    
    var currentPlayerIndex by mutableStateOf(0)
    var gameOver by mutableStateOf(false)
    var useAI by mutableStateOf(true)

    // Helper properties for UI (Local Player is Index 0)
    val playerHand: SnapshotStateList<Card> get() = hands[0]
    val playerStack: SnapshotStateList<Card> get() = privateStacks[0]
    val isPlayerTurn: Boolean get() = currentPlayerIndex == 0

    init {
        setupGame()
    }

    fun setupGame() {
        val allCards = mutableListOf<Card>()
        for (suit in Suit.entries) {
            for (rank in 1..10) {
                allCards.add(Card(rank, suit))
            }
        }
        allCards.shuffle()
        deck.clear()
        deck.addAll(allCards)
        constructions.clear()
        floor.clear()
        privateStacks.forEach { it.clear() }
        hands.forEach { it.clear() }
        
        deal()
        currentPlayerIndex = 0
        gameOver = false
    }

    fun deal() {
        if (playerCount == 4) {
            // Deal all 40 cards initially
            repeat(10) {
                for (i in 0 until 4) {
                    if (deck.isNotEmpty()) hands[i].add(deck.removeAt(0))
                }
            }
        } else {
            // 2 player: 10 each
            repeat(10) {
                for (i in 0 until 2) {
                    if (deck.isNotEmpty()) hands[i].add(deck.removeAt(0))
                }
            }
        }
        // Deal 4 to the floor initially if empty
        if (floor.isEmpty() && deck.isNotEmpty()) {
            repeat(4) { if (deck.isNotEmpty()) floor.add(deck.removeAt(0)) }
        }
    }

    private fun checkDisassemble(playerIndex: Int, playedCard: Card) {
        val myConstruction = constructions.find { it.ownerIndex == playerIndex } ?: return
        if (playedCard.value == myConstruction.targetValue) {
            val remainingInHand = hands[playerIndex].count { it.value == myConstruction.targetValue }
            if (remainingInHand == 0) {
                floor.addAll(myConstruction.cards)
                constructions.remove(myConstruction)
            }
        }
    }

    fun executePlay(playedCard: Card, playerIndex: Int) {
        hands[playerIndex].remove(playedCard)
        checkDisassemble(playerIndex, playedCard)
        floor.add(playedCard)
        nextTurn()
    }

    fun executeBuild(
        playedCard: Card,
        selectedFloorCards: List<Card>,
        selectedConstructions: List<Construction>,
        recoveredOpponentCard: Card?,
        playerIndex: Int
    ): Boolean {
        val hand = hands[playerIndex]
        val partnerIndex = if (playerCount == 4) (playerIndex + 2) % 4 else -1
        
        val floorVal = selectedFloorCards.sumOf { it.value }
        val constructVal = selectedConstructions.sumOf { it.targetValue }
        val oppVal = recoveredOpponentCard?.value ?: 0
        val totalSelectedValue = floorVal + constructVal + oppVal
        val sum = totalSelectedValue + playedCard.value

        var targetConstruction = constructions.find { it.ownerIndex == playerIndex }
        if (targetConstruction == null && partnerIndex != -1) {
            targetConstruction = constructions.find { it.ownerIndex == partnerIndex }
        }

        val targetValue = if (targetConstruction != null) {
            targetConstruction.targetValue
        } else {
            val remainingHand = hand.toMutableList().apply { remove(playedCard) }
            remainingHand.map { it.value }.distinct()
                .filter { sum % it == 0 }
                .maxOrNull() ?: -1
        }

        val hasTargetInHand = (hand.toMutableList().apply { remove(playedCard) }).any { it.value == targetValue }

        if (targetValue > 0 && hasTargetInHand && sum % targetValue == 0) {
            if (targetConstruction == null) {
                targetConstruction = Construction(playerIndex, targetValue)
                constructions.add(targetConstruction)
            }

            targetConstruction.cards.addAll(selectedFloorCards)
            selectedConstructions.forEach { 
                if (it != targetConstruction) {
                    targetConstruction!!.cards.addAll(it.cards)
                    constructions.remove(it)
                }
            }
            if (recoveredOpponentCard != null) {
                targetConstruction.cards.add(recoveredOpponentCard)
                privateStacks.forEach { it.remove(recoveredOpponentCard) }
            }
            targetConstruction.cards.add(playedCard)
            floor.removeAll(selectedFloorCards)
            hand.remove(playedCard)
            return true
        }

        executePlay(playedCard, playerIndex)
        return false
    }

    fun executeCapture(
        playedCard: Card,
        selectedFloorCards: List<Card>,
        selectedConstructions: List<Construction>,
        recoveredOpponentCard: Card?,
        playerIndex: Int
    ): Boolean {
        val floorVal = selectedFloorCards.sumOf { it.value }
        val constructVal = selectedConstructions.sumOf { it.targetValue }
        val oppVal = recoveredOpponentCard?.value ?: 0
        val totalSelectedValue = floorVal + constructVal + oppVal

        if (totalSelectedValue > 0 && totalSelectedValue % playedCard.value == 0) {
            if (selectedConstructions.any { it.targetValue != playedCard.value }) return false

            val stack = privateStacks[playerIndex]
            val allCardsToCapture = mutableListOf<Card>()
            allCardsToCapture.addAll(selectedFloorCards)
            selectedConstructions.forEach { allCardsToCapture.addAll(it.cards) }
            if (recoveredOpponentCard != null) {
                allCardsToCapture.add(recoveredOpponentCard)
                privateStacks.forEach { it.remove(recoveredOpponentCard) }
            }
            
            stack.addAll(allCardsToCapture)
            stack.add(playedCard)
            
            floor.removeAll(selectedFloorCards)
            constructions.removeAll(selectedConstructions)
            hands[playerIndex].remove(playedCard)
            return true
        }

        return false
    }

    fun nextTurn() {
        if (!gameOver) {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerCount
            if (hands.all { it.isEmpty() } && deck.isEmpty()) {
                gameOver = true
            }
        }
    }

    fun playCard(card: Card, playerIndex: Int): Boolean {
        val hand = hands[playerIndex]
        if (!hand.contains(card)) return false
        val captured = findCapture(card, floor)
        if (captured.isNotEmpty()) {
            privateStacks[playerIndex].addAll(captured)
            privateStacks[playerIndex].add(card)
            floor.removeAll(captured)
        } else floor.add(card)
        hand.remove(card)
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
        if (playerCount == 4) {
            val team0Stack = privateStacks[0] + privateStacks[2]
            val team1Stack = privateStacks[1] + privateStacks[3]
            var s0 = team0Stack.sumOf { it.points }
            var s1 = team1Stack.sumOf { it.points }
            if (team0Stack.size > team1Stack.size) s0 += 1 else if (team1Stack.size > team0Stack.size) s1 += 1
            if (team0Stack.count { it.suit == Suit.SPADES } >= 6) s0 += 1
            if (team1Stack.count { it.suit == Suit.SPADES } >= 6) s1 += 1
            scores["Team0"] = s0
            scores["Team1"] = s1
        } else {
            var s0 = privateStacks[0].sumOf { it.points }
            var s1 = privateStacks[1].sumOf { it.points }
            if (privateStacks[0].size > privateStacks[1].size) s0 += 1 else if (privateStacks[1].size > privateStacks[0].size) s1 += 1
            if (privateStacks[0].count { it.suit == Suit.SPADES } >= 6) s0 += 1
            if (privateStacks[1].count { it.suit == Suit.SPADES } >= 6) s1 += 1
            scores["Player"] = s0
            scores["AI"] = s1
        }
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
            if (i < state.teamStacks.size) {
                privateStacks[i].clear()
                privateStacks[i].addAll(state.teamStacks[i])
            }
        }
        constructions.clear()
        state.constructions.forEach { cs ->
            val ownerIdParts = cs.ownerId.removePrefix("Player")
            val idx = ownerIdParts.toIntOrNull() ?: 0
            val c = Construction(idx, cs.targetValue)
            c.cards.addAll(cs.cards)
            constructions.add(c)
        }
    }

    fun exportState(): GameState {
        return GameState(
            deck = deck.toList(),
            hands = hands.map { it.toList() },
            floor = floor.toList(),
            teamStacks = privateStacks.map { it.toList() },
            constructions = constructions.map { ConstructionState("Player${it.ownerIndex}", it.targetValue, it.cards.toList()) },
            currentPlayerIndex = currentPlayerIndex,
            gameOver = gameOver,
            playerCount = playerCount
        )
    }
}
