package play.zulu.khasina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import play.zulu.khasina.ui.theme.KHASINATheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = UserStorage(this)
        enableEdgeToEdge()
        setContent {
            KHASINATheme {
                SettingsScreen(
                    storage = storage,
                    onBack = { finish() }
                )
            }
        }
    }
}

data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    storage: UserStorage,
    onBack: () -> Unit
) {
    var showNetworkDialog by remember { mutableStateOf(false) }
    
    val items = listOf(
        SettingsItem("ACCOUNT", Icons.Default.Person, "Account"),
        SettingsItem("GAMEPLAY", Icons.Default.SportsEsports, "Gameplay"),
        SettingsItem("MULTIPLAYER", Icons.Default.Public, "Multiplayer"),
        SettingsItem("CHAT", Icons.Default.Chat, "Chat"),
        SettingsItem("APPEARANCE", Icons.Default.Palette, "Appearance"),
        SettingsItem("LEAGUES", Icons.Default.EmojiEvents, "Leagues"),
        SettingsItem("ADVANCED", Icons.Default.Build, "Advanced"),
        SettingsItem("ABOUT", Icons.Default.Info, "About")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", color = Color(0xFFEBC98F), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFFEBC98F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B120B))
            )
        },
        containerColor = Color(0xFF1B120B)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            items(items) { item ->
                SettingsRow(item) {
                    if (item.title == "MULTIPLAYER" || item.title == "ADVANCED") {
                        showNetworkDialog = true
                    }
                }
                HorizontalDivider(color = Color(0xFF5A3822), thickness = 0.5.dp)
            }
        }

        if (showNetworkDialog) {
            NetworkSettingsDialog(
                storage = storage,
                onDismiss = { showNetworkDialog = false }
            )
        }
    }
}

@Composable
fun SettingsRow(item: SettingsItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, null, tint = Color(0xFFD6B37A), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF5A3822))
    }
}

@Composable
fun NetworkSettingsDialog(
    storage: UserStorage,
    onDismiss: () -> Unit
) {
    var discoveryEnabled by remember { mutableStateOf(storage.isDiscoveryEnabled()) }
    var pollingEnabled by remember { mutableStateOf(storage.isPollingEnabled()) }
    var manualIp by remember { mutableStateOf(storage.getManualIp()) }
    var port by remember { mutableStateOf(storage.getServerPort().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF24130C),
        title = { Text("Network & Server", color = Color(0xFFEBC98F)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-Discovery", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(
                        checked = discoveryEnabled,
                        onCheckedChange = { discoveryEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD6B37A))
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Session Polling", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(
                        checked = pollingEnabled,
                        onCheckedChange = { pollingEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD6B37A))
                    )
                }
                
                if (!discoveryEnabled) {
                    TextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("Manual Server IP") },
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
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Base Port (Auth)") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF3B2417),
                        unfocusedContainerColor = Color(0xFF3B2417),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    storage.setDiscoveryEnabled(discoveryEnabled)
                    storage.setPollingEnabled(pollingEnabled)
                    storage.setManualIp(manualIp)
                    storage.setServerPort(port.toIntOrNull() ?: 8000)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5E3C))
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFEBC98F))
            }
        }
    )
}
