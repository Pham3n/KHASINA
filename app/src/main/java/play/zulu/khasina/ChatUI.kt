package play.zulu.khasina

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChatItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color
)

@Composable
fun ChatsDropdownMenu(
    onClose: () -> Unit = {},
    onChatClick: (ChatItem) -> Unit = {},
    onAddChatClick: () -> Unit = {}
) {

    val defaultChats = listOf(
        ChatItem(
            title = "League Chat",
            subtitle = "Default",
            icon = Icons.Default.EmojiEvents,
            iconColor = Color(0xFF8B3A1A)
        ),
        ChatItem(
            title = "Global Chat",
            subtitle = "Default",
            icon = Icons.Default.Shield,
            iconColor = Color(0xFF9C6B28)
        ),
        ChatItem(
            title = "Room Chat",
            subtitle = "Default",
            icon = Icons.Default.Groups,
            iconColor = Color(0xFF006D6F)
        ),
        ChatItem(
            title = "Announcements",
            subtitle = "Default",
            icon = Icons.Default.Campaign,
            iconColor = Color(0xFF5B3C6F)
        )
    )

    val userChats = listOf(
        ChatItem(
            title = "Friends",
            subtitle = "Private • 3 members",
            icon = Icons.Default.Groups,
            iconColor = Color(0xFF1C6E6A)
        ),
        ChatItem(
            title = "Khasina Players",
            subtitle = "Private • 8 members",
            icon = Icons.Default.Person,
            iconColor = Color(0xFF7A431F)
        ),
        ChatItem(
            title = "Umlabalaba Club",
            subtitle = "Private • 12 members",
            icon = Icons.Default.SportsEsports,
            iconColor = Color(0xFF244E7A)
        ),
        ChatItem(
            title = "Strategy Masters",
            subtitle = "Private • 5 members",
            icon = Icons.Default.Psychology,
            iconColor = Color(0xFF476B2D)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f)
                .align(Alignment.TopCenter)
                .padding(top = 70.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF24130C)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 14.dp
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {

                // ===== HEADER =====

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "CHATS",
                        color = Color(0xFFE7C58A),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onClose
                    ) {

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFE7C58A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = Color(0xFF5A3822),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ===== DEFAULT CHATS =====

                Text(
                    text = "DEFAULT CHATS",
                    color = Color(0xFFE7C58A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                defaultChats.forEach { chat ->

                    ChatRow(
                        chat = chat,
                        onClick = {
                            onChatClick(chat)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = Color(0xFF5A3822),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ===== USER CHATS =====

                Text(
                    text = "YOUR CHATS",
                    color = Color(0xFFE7C58A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {

                    items(userChats) { chat ->

                        ChatRow(
                            chat = chat,
                            onClick = {
                                onChatClick(chat)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ===== ADD CHAT BUTTON =====

                Button(
                    onClick = onAddChatClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5E3C)
                    )
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFFEFD7A5),
                            modifier = Modifier.size(34.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {

                            Text(
                                text = "ADD CHAT",
                                color = Color(0xFFEFD7A5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )

                            Text(
                                text = "Create a chat by exact name",
                                color = Color(0xFFF5E4C5),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRow(
    chat: ChatItem,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 10.dp,
                    horizontal = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ===== ICON =====

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        color = chat.iconColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = chat.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // ===== CHAT INFO =====

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = chat.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = chat.subtitle,
                    color = Color(0xFFD8B073),
                    fontSize = 16.sp
                )
            }

            // ===== OPTIONS BUTTON =====

            IconButton(
                onClick = {
                    // future chat options menu
                }
            ) {

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFE7C58A),
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}
