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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: String
)

data class ChatItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val roomId: java.util.UUID? = null
)

@Composable
fun ChatsDropdownMenu(
    viewModel: GameViewModel,
    onClose: () -> Unit = {},
    onChatClick: (ChatItem) -> Unit = {},
    onAddChatClick: () -> Unit = {}
) {
    val officialChats = viewModel.officialChats
    val personalChats = viewModel.personalChats
    var showMembersDialog by remember { mutableStateOf<ChatItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.65f)
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF24130C)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFE7C58A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF5A3822), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ===== CHATS LIST =====
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text(
                            text = "OFFICIAL",
                            color = Color(0xFFE7C58A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(officialChats) { chat ->
                        ChatRow(chat = chat, 
                            onClick = { 
                                viewModel.selectedChat = chat
                                viewModel.loadChatMessages(chat)
                                onClose()
                            },
                            onMembersClick = { showMembersDialog = chat }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "YOUR CHATS",
                            color = Color(0xFFE7C58A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(personalChats) { chat ->
                        ChatRow(chat = chat, 
                            onClick = { 
                                viewModel.selectedChat = chat
                                viewModel.loadChatMessages(chat)
                                onClose()
                            },
                            onMembersClick = { showMembersDialog = chat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ===== ADD CHAT BUTTON =====
                Button(
                    onClick = onAddChatClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5E3C))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFFEFD7A5),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEW CHAT",
                            color = Color(0xFFEFD7A5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        if (showMembersDialog != null) {
            MembersDialog(
                viewModel = viewModel,
                chat = showMembersDialog!!,
                onDismiss = { showMembersDialog = null }
            )
        }
    }
}

@Composable
fun MembersDialog(
    viewModel: GameViewModel,
    chat: ChatItem,
    onDismiss: () -> Unit
) {
    val rid = chat.roomId ?: return
    LaunchedEffect(rid) {
        viewModel.refreshRoomMembers(rid)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF24130C),
        title = { Text("${chat.title} Members", color = Color(0xFFE7C58A)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(viewModel.roomMembers) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                viewModel.showUserDetail(member.user_id)
                                onDismiss()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(if(member.state == "ONLINE") Color.Green else Color.Gray, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(member.displayName ?: member.username ?: member.user_id.toString().take(8), color = Color.White)
                            if (member.displayName != null) {
                                Text("@${member.username}", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFFE7C58A)) }
        }
    )
}

@Composable
fun ChatRow(chat: ChatItem, onClick: () -> Unit, onMembersClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = chat.iconColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = chat.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = chat.subtitle,
                    color = Color(0xFFD8B073),
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = onMembersClick) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = Color(0xFFE7C58A)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFE7C58A),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
