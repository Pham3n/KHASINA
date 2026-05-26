package play.zulu.khasina

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KHASINAScreen(viewModel: GameViewModel, onMenuClick: () -> Unit) {
    val engine = viewModel.engine
    val scores = engine.calculateScores()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A120D))
            .padding(12.dp)
    ) {

        // ===== TOP BAR =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    tint = Color(0xFFE0BC7A)
                )
            }

            Text(
                text = "KHASINA",
                color = Color(0xFFE0BC7A),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            Row {
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFFE0BC7A)
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFE0BC7A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== OPPONENT =====
        PlayerHandPanel(
            name = if (viewModel.isMultiplayer) "Opponent" else "AI",
            cardsRemaining = engine.aiHand.size,
            score = scores["AI"] ?: 0,
            isTop = true,
            topStackCard = engine.aiStack.lastOrNull()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== MAIN GAME AREA =====
        Row(
            modifier = Modifier.weight(1f)
        ) {
            // ===== FLOOR =====
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Color(0xFF6A4528),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "FLOOR",
                        color = Color(0xFFEBC98F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FloorSection(cards = engine.floor)
                }

                // Construction Section - Anchored to the bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    ConstructionSection()
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ===== SIDE PANEL =====
            Column(
                modifier = Modifier.width(130.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SideInfoCard(
                    title = "TURN",
                    content = if (engine.gameOver) "GAME OVER" 
                              else if (engine.isPlayerTurn) "YOUR TURN" 
                              else if (viewModel.isMultiplayer) "OPPONENT" 
                              else "AI TURN"
                )

                SideInfoCard(
                    title = "DECK",
                    content = "${engine.deck.size} Cards"
                )

                SideInfoCard(
                    title = "MODE",
                    content = if (viewModel.isMultiplayer) viewModel.connectionType?.name ?: "ONLINE" else "LOCAL"
                )

                SideInfoCard(
                    title = "YOUR STACK",
                    content = "${engine.playerStack.size} Cards"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== PLAYER HAND =====
        PlayerCardsSection(
            cards = engine.playerHand,
            selectedCard = viewModel.selectedCard,
            onCardSelect = { viewModel.selectedCard = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== ACTION BUTTONS =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GameActionButton(
                text = "CAPTURE",
                enabled = viewModel.selectedCard != null && engine.isPlayerTurn && !engine.gameOver
            ) {
                viewModel.selectedCard?.let { 
                    viewModel.onCardClicked(it)
                    viewModel.selectedCard = null
                }
            }

            GameActionButton("RESET") { viewModel.resetGame() }

            if (viewModel.isMultiplayer) {
                GameActionButton("EXIT") { viewModel.disconnect() }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== PLAYER INFO =====
        PlayerHandPanel(
            name = "You",
            cardsRemaining = engine.playerHand.size,
            score = scores["Player"] ?: 0,
            isTop = false,
            topStackCard = engine.playerStack.lastOrNull()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloorSection(cards: List<Card>) {
    // Wrapped row for floor cards
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cards.forEach { card ->
            PlayingCard(card)
        }
    }
}

@Composable
fun ConstructionSection() {
    // Fixed row of 4 slots at the bottom
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) {
            ConstructionPlaceholder()
        }
    }
}

@Composable
fun ConstructionPlaceholder() {
    Box(
        modifier = Modifier
            .size(width = 49.dp, height = 70.dp)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
    }
}

@Composable
fun PlayerCardsSection(
    cards: List<Card>,
    selectedCard: Card?,
    onCardSelect: (Card) -> Unit
) {
    Column {
        Text(
            text = "YOUR HAND",
            color = Color(0xFFEBC98F),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(cards) { card ->
                PlayingCard(
                    card = card,
                    isSelected = card == selectedCard,
                    onClick = { onCardSelect(card) }
                )
            }
        }
    }
}

@Composable
fun PlayingCard(
    card: Card,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val rankText = when(card.rank) {
        1 -> "A"
        else -> card.rank.toString()
    }
    val suitColor = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Color.Red else Color.Black
    val suitSymbol = when(card.suit) {
        Suit.SPADES -> "♠"
        Suit.DIAMONDS -> "♦"
        Suit.HEARTS -> "♥"
        Suit.CLUBS -> "♣"
    }

    Card(
        modifier = Modifier
            .size(width = 49.dp, height = 70.dp)
            .then(if (isSelected) Modifier.border(2.dp, Color(0xFFEBC98F), RoundedCornerShape(8.dp)) else Modifier)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E3C3)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = rankText,
                modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                color = suitColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = suitSymbol,
                    color = suitColor,
                    fontSize = 21.sp
                )
                Text(
                    text = rankText,
                    color = suitColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Text(
                text = rankText,
                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                color = suitColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PlayerHandPanel(
    name: String,
    cardsRemaining: Int,
    score: Int,
    isTop: Boolean,
    topStackCard: Card? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1B12)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF6A4528), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (topStackCard != null) {
                    Text(topStackCard.toString(), color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color(0xFFEBC98F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Row {
                    Text(text = "Cards: $cardsRemaining", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Score: $score", color = Color(0xFFFFD166), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SideInfoCard(
    title: String,
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1B12)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                color = Color(0xFFEBC98F),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GameActionButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF5A3822),
            disabledContainerColor = Color(0xFF5A3822).copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) Color(0xFFEBC98F) else Color(0xFFEBC98F).copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}
