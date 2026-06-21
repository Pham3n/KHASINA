package play.zulu.khasina

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KHASINAScreen(viewModel: GameViewModel, onMenuClick: () -> Unit) {
    val engine = viewModel.engine
    val scores = engine.calculateScores()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bgbr),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            // ===== TOP BAR =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color(0xFFE0BC7A))
                }
                Text(text = "KHASINA", color = Color(0xFFE0BC7A), fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Row {
                    IconButton(onClick = { 
                        viewModel.isChatVisible = true 
                        viewModel.refreshChatRooms()
                    }) { 
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFFE0BC7A)) 
                    }
                    IconButton(onClick = { viewModel.isProfileVisible = true }) { 
                        Icon(Icons.Default.Person, null, tint = Color(0xFFE0BC7A)) 
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            ChatBox(viewModel)
            Spacer(modifier = Modifier.height(12.dp))

            // ===== MAIN GAME AREA =====
            Row(modifier = Modifier.weight(1f)) {
                // ===== FLOOR =====
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF6A4528).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(text = "FLOOR", color = Color(0xFFEBC98F), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        FloorSection(
                            cards = engine.floor,
                            selectedCards = viewModel.selectedCardsFloor,
                            onCardClick = { viewModel.onCardFloorClicked(it) }
                        )
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
                        ConstructionSection(
                            constructions = engine.constructions,
                            selectedConstructions = viewModel.selectedConstructions,
                            onConstructionClick = { viewModel.onConstructionClicked(it) },
                            playerCount = engine.playerCount
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // ===== SIDE PANEL =====
                Column(modifier = Modifier.width(130.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SideInfoCard(
                        title = "ROUND",
                        content = "${viewModel.currentRound} / ${viewModel.maxRounds}"
                    )
                    
                    SideInfoCard(
                        title = "TURN",
                        content = if (viewModel.engine.gameOver && viewModel.currentRound >= viewModel.maxRounds) "GAME OVER" 
                                  else if (viewModel.engine.gameOver) "ROUND OVER"
                                  else if (viewModel.isMultiStagePlayActive) "STAGE..."
                                  else if (engine.currentPlayerIndex == 0) "YOU" 
                                  else if (engine.playerCount == 2 && viewModel.isLocalAiEnabled && !viewModel.isMultiplayer) "AI"
                                  else if (engine.playerCount == 2 && !viewModel.isLocalAiEnabled && !viewModel.isMultiplayer) "FRIEND"
                                  else if (engine.playerCount == 2) "OPPONENT"
                                  else "PLAYER ${engine.currentPlayerIndex}"
                    )

                    // DECK
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "DECK", color = Color(0xFFEBC98F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (engine.deck.isNotEmpty()) {
                                Box(modifier = Modifier.size(width = 40.dp, height = 56.dp)) {
                                    Image(painterResource(R.drawable.crd), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                    Text(engine.deck.size.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.align(Alignment.Center))
                                }
                            } else { ConstructionPlaceholder() }
                        }
                    }

                    SideInfoCard(title = "MODE", content = if (viewModel.isMultiplayer) "ONLINE" else "LOCAL")
                    SideInfoCard(title = "PRIVATE STACK", content = "${engine.privateStacks[0].size} Cards")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== PLAYER HAND =====
            PlayerCardsSection(
                cards = engine.hands[0],
                selectedCard = viewModel.selectedCardHand,
                onCardSelect = { viewModel.onCardHandClicked(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ===== ACTION BUTTONS =====
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GameActionButton(
                    text = "PLAY",
                    enabled = (viewModel.selectedCardHand != null) && engine.currentPlayerIndex == 0 && !engine.gameOver
                ) { viewModel.onPlayClicked() }

                GameActionButton(
                    text = "CAPTURE",
                    enabled = (viewModel.selectedCardHand != null) && engine.currentPlayerIndex == 0 && !engine.gameOver
                ) { viewModel.onCaptureClicked() }

                GameActionButton(
                    text = "BUILD",
                    enabled = (viewModel.selectedCardHand != null) && engine.currentPlayerIndex == 0 && !engine.gameOver
                ) { viewModel.onBuildClicked() }

                GameActionButton("RESET") { viewModel.resetGame() }

                if (viewModel.isMultiplayer) {
                    GameActionButton("EXIT") { viewModel.disconnect() }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== PLAYER INFO PANELS =====
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(engine.playerCount) { index ->
                    val scoreKey = if (engine.playerCount == 4) "Team${index % 2}" else "Team$index"
                    val currentRoundScore = scores[scoreKey] ?: 0
                    val totalScore = viewModel.cumulativeScores[index] + currentRoundScore
                    
                    PlayerHandPanel(
                        modifier = Modifier.width(160.dp),
                        name = if (index == 0) "You" 
                               else if (viewModel.isLocalAiEnabled && !viewModel.isMultiplayer) (if (engine.playerCount == 2) "AI" else "AI $index")
                               else if (!viewModel.isLocalAiEnabled && !viewModel.isMultiplayer && engine.playerCount == 2) "Friend"
                               else "Player $index",
                        cardsRemaining = engine.hands[index].size,
                        score = totalScore,
                        isTop = index != 0,
                        topStackCard = engine.privateStacks[index].lastOrNull(),
                        isSelected = index != 0 && viewModel.selectedOpponentStackCard != null && viewModel.selectedOpponentStackCard == engine.privateStacks[index].lastOrNull(),
                        onClick = { if (index != 0) viewModel.onOpponentStackClicked(index) }
                    )
                }
            }
        }

        // ===== CHATS DROPDOWN OVERLAY =====
        if (viewModel.isChatVisible) {
            ChatsDropdownMenu(
                viewModel = viewModel,
                onClose = { viewModel.isChatVisible = false }
            )
        }

        // ===== PROFILE DROPDOWN OVERLAY =====
        if (viewModel.isProfileVisible) {
            ProfileDropdownMenu(
                viewModel = viewModel,
                onClose = { viewModel.isProfileVisible = false }
            )
        }

        // ===== FRIENDS DROPDOWN OVERLAY =====
        if (viewModel.isFriendsVisible) {
            FriendsDropdownMenu(
                viewModel = viewModel,
                onClose = { viewModel.isFriendsVisible = false }
            )
        }

        // ===== USER DETAIL OVERLAY =====
        if (viewModel.isUserDetailVisible) {
            UserDetailDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.isUserDetailVisible = false }
            )
        }
    }
}

@Composable
fun UserDetailDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val user = viewModel.selectedUserForProfile ?: return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF24130C),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(Color(0xFF8B5E3C), CircleShape))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(user.displayName ?: user.username, color = Color(0xFFE7C58A), fontWeight = FontWeight.Bold)
                    if (user.displayName != null) {
                        Text("@${user.username}", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rating: ${user.rating}", color = Color.White)
                Text("Member Since: ${user.createdAt.take(10)}", color = Color.Gray, fontSize = 12.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val isAlreadyFriend = viewModel.friendsList.any { it.friend_id == user.id }
                if (!isAlreadyFriend && user.id != viewModel.currentUserData?.id) {
                    Button(
                        onClick = { 
                            viewModel.addFriend(user.id)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF476B2D))
                    ) {
                        Icon(Icons.Default.PersonAdd, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADD FRIEND")
                    }
                } else if (isAlreadyFriend) {
                    Text("You are friends", color = Color.Green, fontWeight = FontWeight.Bold)
                }

                if (user.id != viewModel.currentUserData?.id) {
                    Button(
                        onClick = {
                            viewModel.inviteToMatch(user)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5E3C))
                    ) {
                        Icon(Icons.Default.GroupAdd, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INVITE TO MATCH")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFFE7C58A)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloorSection(cards: List<Card>, selectedCards: List<Card>, onCardClick: (Card) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cards.forEach { card ->
            PlayingCard(card, isSelected = selectedCards.contains(card), onClick = { onCardClick(card) })
        }
    }
}

@Composable
fun ConstructionSection(
    constructions: List<Construction>,
    selectedConstructions: List<Construction>,
    onConstructionClick: (Construction) -> Unit,
    playerCount: Int
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(playerCount) { index ->
            val construction = constructions.find { it.ownerIndex == index }
            if (construction != null) {
                construction.topCard?.let { card ->
                    PlayingCard(
                        card = card,
                        isSelected = selectedConstructions.contains(construction),
                        onClick = { onConstructionClick(construction) }
                    )
                }
            } else {
                ConstructionPlaceholder()
            }
        }
    }
}

@Composable
fun ConstructionPlaceholder() {
    Box(
        modifier = Modifier
            .size(width = 49.dp, height = 70.dp)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
    )
}

@Composable
fun PlayerCardsSection(cards: List<Card>, selectedCard: Card?, onCardSelect: (Card) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "YOUR HAND", color = Color(0xFFEBC98F), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 16.dp)) {
            if (cards.isEmpty()) { items(3) { ConstructionPlaceholder() } }
            else {
                items(cards) { card ->
                    PlayingCard(card = card, isSelected = card == selectedCard, onClick = { onCardSelect(card) })
                }
            }
        }
    }
}

@Composable
fun PlayingCard(card: Card, isSelected: Boolean = false, onClick: (() -> Unit)? = null) {
    val rankText = when(card.rank) { 1 -> "A"; else -> card.rank.toString() }
    val suitColor = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Color.Red else Color.Black
    val suitSymbol = when(card.suit) {
        Suit.SPADES -> "♠"; Suit.DIAMONDS -> "♦"; Suit.HEARTS -> "♥"; Suit.CLUBS -> "♣"
    }

    Card(
        modifier = Modifier
            .size(width = 49.dp, height = 70.dp)
            .then(if (isSelected) Modifier.border(2.dp, Color(0xFFEBC98F), RoundedCornerShape(8.dp)) else Modifier)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E3C3)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = rankText, modifier = Modifier.align(Alignment.TopStart).padding(2.dp), color = suitColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = suitSymbol, color = suitColor, fontSize = 21.sp)
                Text(text = rankText, color = suitColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(text = rankText, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp), color = suitColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun PlayerHandPanel(
    modifier: Modifier = Modifier,
    name: String,
    cardsRemaining: Int,
    score: Int,
    isTop: Boolean,
    topStackCard: Card? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (isSelected) Modifier.border(2.dp, Color(0xFFEBC98F), RoundedCornerShape(16.dp)) else Modifier)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(Color(0xFF6A4528), CircleShape), contentAlignment = Alignment.Center) {
                if (topStackCard != null) { Text(text = topStackCard.toString(), color = Color.White, fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color(0xFFEBC98F), fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
fun ChatBox(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val selectedChat = viewModel.selectedChat
    val messages = viewModel.chatMessages
    var text by remember { mutableStateOf("") }
    
    Card(
        modifier = modifier.fillMaxWidth().height(140.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedChat?.title ?: "GAME CHAT", 
                    color = Color(0xFFEBC98F), 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                if (selectedChat != null) {
                    IconButton(
                        onClick = { viewModel.refreshChatRooms(); viewModel.isChatVisible = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFFEBC98F), modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true) {
                    items(messages.asReversed()) { msg ->
                        val timeOnly = msg.timestamp.split("T").getOrNull(1)?.take(5) ?: msg.timestamp.take(5)
                        Text(
                            text = "[$timeOnly] ${msg.sender.take(8)}: ${msg.text}",
                            color = Color.White, 
                            fontSize = 11.sp
                        )
                    }
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                text = if (selectedChat == null) "Select a chat to start messaging" else "No messages in ${selectedChat.title}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (selectedChat != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f).height(40.dp),
                        placeholder = { Text("Type a message...", fontSize = 10.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF3B2417),
                            unfocusedContainerColor = Color(0xFF3B2417),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                viewModel.sendChatMessage(text)
                                text = ""
                            }
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color(0xFFEBC98F))
                    }
                }
            }
        }
    }
}

@Composable
fun SideInfoCard(modifier: Modifier = Modifier, title: String, content: String) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
            Text(text = title, color = Color(0xFFEBC98F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = content, color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun GameActionButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
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
        Text(text = text, color = if (enabled) Color(0xFFEBC98F) else Color(0xFFEBC98F).copy(alpha = 0.5f), fontSize = 12.sp)
    }
}
