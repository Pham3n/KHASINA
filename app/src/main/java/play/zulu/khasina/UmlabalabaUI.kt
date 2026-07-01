package play.zulu.khasina

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UmlabalabaScreen(viewModel: GameViewModel, onMenuClick: () -> Unit) {

    val boardState = remember {
        mutableStateListOf(
            // Example board positions
            Piece.BLACK, null, Piece.BLACK,
            null, Piece.WHITE, null,
            Piece.BLACK, null, Piece.WHITE,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B120B))
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
                    tint = Color(0xFFD6B37A)
                )
            }

            Text(
                text = "PlayUMLABALABA",
                color = Color(0xFFD6B37A),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = { 
                    viewModel.isChatVisible = true 
                    viewModel.refreshChatRooms()
                }) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFFD6B37A)
                    )
                }

                IconButton(onClick = { viewModel.isProfileVisible = true }) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFD6B37A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== PLAYER INFO =====

        PlayerPanel(
            name = "Opponent",
            pieces = 9,
            mills = 1,
            isTop = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== MAIN CONTENT =====

        Row(
            modifier = Modifier.weight(1f)
        ) {

            // ===== BOARD =====

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Color(0xFF8B5E3C),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {

                UmlabalabaBoard(
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ===== SIDE PANEL =====

            Column(
                modifier = Modifier.width(120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                SideCard(
                    title = "TURN",
                    content = "YOUR TURN"
                )

                SideCard(
                    title = "PHASE",
                    content = "MOVEMENT"
                )

                SideCard(
                    title = "MILLS",
                    content = "YOU: 1\nOPP: 0"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== PLAYER INFO =====

        PlayerPanel(
            name = "You",
            pieces = 9,
            mills = 0,
            isTop = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== ACTION BUTTONS =====

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            GameButton("NEW") { }

            GameButton("UNDO") { }

            GameButton("HINT") { }

            GameButton("PASS") { }

            GameButton("RESIGN") { }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== BOTTOM INFO =====

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            BottomInfoCard(
                modifier = Modifier.weight(1f),
                title = "SCORE",
                content = "You: 1\nOpponent: 0"
            )

            BottomInfoCard(
                modifier = Modifier.weight(1f),
                title = "HOW TO WIN",
                content = "Reduce opponent to 2 pieces"
            )

            BottomInfoCard(
                modifier = Modifier.weight(1f),
                title = "GAME INFO",
                content = "2 Player\nOnline Match"
            )
        }
    }
}

@Composable
fun UmlabalabaBoard(
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val boardColor = Color(0xFF2E1D10)
        val stroke = 6f

        val padding = 80f

        val outer = Rect(
            padding,
            padding,
            size.width - padding,
            size.height - padding
        )

        val middle = Rect(
            padding * 2,
            padding * 2,
            size.width - padding * 2,
            size.height - padding * 2
        )

        val inner = Rect(
            padding * 3,
            padding * 3,
            size.width - padding * 3,
            size.height - padding * 3
        )

        drawRect(
            color = boardColor,
            topLeft = outer.topLeft,
            size = outer.size,
            style = Stroke(stroke)
        )

        drawRect(
            color = boardColor,
            topLeft = middle.topLeft,
            size = middle.size,
            style = Stroke(stroke)
        )

        drawRect(
            color = boardColor,
            topLeft = inner.topLeft,
            size = inner.size,
            style = Stroke(stroke)
        )

        // Connecting lines

        drawLine(
            color = boardColor,
            start = Offset(size.width / 2, outer.top),
            end = Offset(size.width / 2, inner.top),
            strokeWidth = stroke
        )

        drawLine(
            color = boardColor,
            start = Offset(size.width / 2, inner.bottom),
            end = Offset(size.width / 2, outer.bottom),
            strokeWidth = stroke
        )

        drawLine(
            color = boardColor,
            start = Offset(outer.left, size.height / 2),
            end = Offset(inner.left, size.height / 2),
            strokeWidth = stroke
        )

        drawLine(
            color = boardColor,
            start = Offset(inner.right, size.height / 2),
            end = Offset(outer.right, size.height / 2),
            strokeWidth = stroke
        )
    }
}

@Composable
fun PlayerPanel(
    name: String,
    pieces: Int,
    mills: Int,
    isTop: Boolean
) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1B12)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color(0xFF6A4528),
                        CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {

                Text(
                    text = name,
                    color = Color(0xFFE7C58A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = "Pieces: $pieces",
                    color = Color.White
                )

                Text(
                    text = "Mills: $mills",
                    color = Color(0xFFFFC857)
                )
            }
        }
    }
}

@Composable
fun SideCard(
    title: String,
    content: String
) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1B12)
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = title,
                color = Color(0xFFE7C58A),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                color = Color.White
            )
        }
    }
}

@Composable
fun BottomInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    content: String
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1B12)
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = title,
                color = Color(0xFFE7C58A),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                color = Color.White
            )
        }
    }
}

@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF5A3822)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {

        Text(
            text = text,
            color = Color(0xFFE7C58A)
        )
    }
}

enum class Piece {
    BLACK,
    WHITE
}
