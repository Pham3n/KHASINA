package play.zulu.khasina

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FriendsDropdownMenu(
    viewModel: GameViewModel,
    onClose: () -> Unit = {}
) {
    val friends = viewModel.friendsList
    var showAddFriendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshFriends()
    }

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
                        text = "FRIENDS",
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

                // ===== FRIENDS LIST =====
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (friends.isEmpty()) {
                        item {
                            Text(
                                text = "No friends yet.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(friends) { friend ->
                            FriendRow(viewModel = viewModel, friend = friend, onClose = onClose)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ===== ADD FRIEND BUTTON =====
                Button(
                    onClick = { showAddFriendDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF476B2D))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADD FRIEND",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        if (showAddFriendDialog) {
            AddFriendDialog(
                viewModel = viewModel,
                onDismiss = { showAddFriendDialog = false }
            )
        }
    }
}

@Composable
fun AddFriendDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserRead>>(emptyList()) }
    var isChecking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF24130C),
        title = { Text("Add Friend", color = Color(0xFFE7C58A)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the exact username or select from results.", color = Color.LightGray, fontSize = 14.sp)
                TextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        if (it.length >= 3) {
                            viewModel.searchUsers(it) { list -> results = list }
                        } else {
                            results = emptyList()
                        }
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = !isChecking,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF3B2417),
                        unfocusedContainerColor = Color(0xFF3B2417),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (results.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(results) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { username = user.username }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null, tint = Color(0xFFD6B37A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(user.username, color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }

                if (isChecking) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE7C58A),
                        trackColor = Color(0xFF3B2417)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = username.isNotBlank() && !isChecking,
                onClick = {
                    isChecking = true
                    viewModel.addFriendByUsername(username) { success ->
                        isChecking = false
                        if (!success) {
                            viewModel.showFloatingMessage("User '$username' not found")
                        }
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5E3C))
            ) {
                Text("ADD", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isChecking) {
                Text("Cancel", color = Color(0xFFE7C58A))
            }
        }
    )
}

@Composable
fun FriendRow(viewModel: GameViewModel, friend: FriendRead, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = Color(0xFF8B5E3C), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.friendUsername ?: friend.friend_id.toString().take(8),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = friend.status,
                color = if (friend.status == "ACCEPTED") Color.Green else Color.Yellow,
                fontSize = 13.sp
            )
        }

        IconButton(onClick = { 
            // Try to find a personal chat with this friend
            val chat = viewModel.personalChats.find { it.title == friend.friendUsername }
            if (chat != null) {
                viewModel.selectedChat = chat
                viewModel.loadChatMessages(chat)
                viewModel.isFriendsVisible = false
                viewModel.isProfileVisible = false
                viewModel.isChatVisible = true
            }
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = Color(0xFFE7C58A)
            )
        }
    }
}
