package play.zulu.khasina

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
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

import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.delay

data class ProfileMenuItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun ProfileDropdownMenu(
    viewModel: GameViewModel,
    onClose: () -> Unit = {}
) {
    var showAuthDialog by remember { mutableStateOf(false) }

    // Close dialog automatically on success
    if (viewModel.authSuccessMessage != null) {
        LaunchedEffect(Unit) {
            delay(1000)
            showAuthDialog = false
            viewModel.authSuccessMessage = null
        }
    }

    val menuItems = listOf(
        ProfileMenuItem("Profile", Icons.Default.Person),
        ProfileMenuItem("Statistics", Icons.Default.BarChart),
        ProfileMenuItem("Achievements", Icons.Default.EmojiEvents),
        ProfileMenuItem("Match History", Icons.Default.History),
        ProfileMenuItem("Leaderboards", Icons.Default.Leaderboard),
        ProfileMenuItem("Friends", Icons.Default.Groups),
        ProfileMenuItem("Clubs", Icons.Default.AccountBalance),
        ProfileMenuItem("Themes", Icons.Default.Palette)
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
                .padding(top = 20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF24130C)
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {

                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROFILE",
                        color = Color(0xFFE7C58A),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFFE7C58A))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF5A3822))
                Spacer(modifier = Modifier.height(24.dp))

                // PLAYER CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3B2417)
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(Color(0xFF8B5E3C), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (viewModel.authState == GameViewModel.AuthState.GUEST) "Guest" else (viewModel.currentUserProfile?.displayName ?: viewModel.currentUser ?: "User"),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        val statusLabel = when (viewModel.connectionState) {
                            GameViewModel.ConnectionState.ONLINE -> "Online"
                            GameViewModel.ConnectionState.CONNECTING -> "Connecting..."
                            else -> "Offline"
                        }

                        val rating = viewModel.currentUserData?.rating ?: 1000
                        val country = viewModel.currentUserProfile?.country ?: "Unknown"

                        Text(
                            text = if (viewModel.authState == GameViewModel.AuthState.GUEST) "No profile" else "Rating: $rating • $country • $statusLabel",
                            color = Color(0xFFD8B073),
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(menuItems) { item ->
                        ProfileMenuRow(item) {
                            if (item.title == "Friends") {
                                viewModel.refreshFriends()
                                viewModel.isFriendsVisible = true
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    onClick = { 
                        if (viewModel.authState == GameViewModel.AuthState.AUTHENTICATED) {
                            viewModel.logout()
                        } else {
                            showAuthDialog = true
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.authState == GameViewModel.AuthState.AUTHENTICATED) Color(0xFF7A2F24) else Color(0xFF476B2D)
                    )
                ) {
                    Text(
                        text = if (viewModel.authState == GameViewModel.AuthState.AUTHENTICATED) "LOG OUT" else "LOG IN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }

        if (showAuthDialog) {
            AuthDialog(
                viewModel = viewModel,
                onLogin = { user, pass -> 
                    viewModel.loginUser(user, pass)
                },
                onRegister = { user, disp, email, pass, country ->
                    viewModel.registerUser(user, disp, email, pass, country)
                },
                onDismiss = { 
                    showAuthDialog = false
                    viewModel.authErrorMessage = null
                    viewModel.authSuccessMessage = null
                }
            )
        }
    }
}

@Composable
fun AuthDialog(
    viewModel: GameViewModel,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var isRegistering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(viewModel.cachedUsername) }
    var displayName by remember { mutableStateOf(viewModel.cachedDisplayName) }
    var email by remember { mutableStateOf(viewModel.cachedEmail) }
    var password by remember { mutableStateOf(viewModel.cachedPassword) }
    var country by remember { mutableStateOf(viewModel.cachedCountry) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF24130C),
        title = {
            Text(
                if (isRegistering) "Register New User" else "Login",
                color = Color(0xFFE7C58A),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (Unique ID)") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF3B2417),
                        unfocusedContainerColor = Color(0xFF3B2417),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                if (isRegistering) {
                    TextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name (Public)") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF3B2417),
                            unfocusedContainerColor = Color(0xFF3B2417),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF3B2417),
                            unfocusedContainerColor = Color(0xFF3B2417),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF3B2417),
                        unfocusedContainerColor = Color(0xFF3B2417),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                if (isRegistering) {
                    TextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF3B2417),
                            unfocusedContainerColor = Color(0xFF3B2417),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                if (viewModel.authErrorMessage != null) {
                    Text(
                        text = viewModel.authErrorMessage!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (viewModel.authSuccessMessage != null) {
                    Text(
                        text = viewModel.authSuccessMessage!!,
                        color = Color.Green,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                TextButton(onClick = { isRegistering = !isRegistering }) {
                    Text(
                        if (isRegistering) "Already have an account? Login" else "Don't have an account? Register",
                        color = Color(0xFFE7C58A)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.cachedUsername = username
                    viewModel.cachedDisplayName = displayName
                    viewModel.cachedEmail = email
                    viewModel.cachedPassword = password
                    viewModel.cachedCountry = country

                    if (isRegistering) {
                        onRegister(username, displayName, email, password, country)
                    } else {
                        onLogin(username, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5E3C))
            ) {
                Text(if (isRegistering) "Register" else "Login", color = Color(0xFFEFD7A5))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFE7C58A))
            }
        }
    )
}

@Composable
fun ProfileMenuRow(item: ProfileMenuItem, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color(0xFFE7C58A),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF5A3822),
            modifier = Modifier.size(20.dp)
        )
    }
}
