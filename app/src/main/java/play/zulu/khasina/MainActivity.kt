package play.zulu.khasina

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import play.zulu.khasina.ui.theme.KHASINATheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initStorage(this)
        enableEdgeToEdge()
        setContent {
            KHASINATheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results: Map<String, Boolean> ->
                    if (results.values.all { it }) {
                        pendingAction?.invoke()
                    }
                    pendingAction = null
                }

                fun checkAndRun(permissions: Array<String>, action: () -> Unit) {
                    val allGranted = permissions.all { 
                        checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
                    }
                    if (allGranted) {
                        action()
                    } else {
                        pendingAction = action
                        permissionLauncher.launch(permissions)
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF1B120B),
                            drawerShape = RoundedCornerShape(0.dp)
                        ) {
                            SidebarContent(
                                viewModel = viewModel,
                                checkAndRun = ::checkAndRun,
                                onActionStarted = { scope.launch { drawerState.close() } }
                            )
                        }
                    }
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            KHASINAScreen(viewModel, onMenuClick = {
                                scope.launch { drawerState.open() }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserStatusHeader(viewModel: GameViewModel) {
    val statusColor = when {
        viewModel.authState == GameViewModel.AuthState.GUEST -> Color.Gray
        viewModel.connectionState == GameViewModel.ConnectionState.ONLINE -> Color.Green
        viewModel.connectionState == GameViewModel.ConnectionState.CONNECTING -> Color.Yellow
        else -> Color(0xFFFFA500) // Orange for Offline
    }
    
    val statusText = when {
        viewModel.authState == GameViewModel.AuthState.GUEST -> "Guest"
        viewModel.connectionState == GameViewModel.ConnectionState.ONLINE -> "Online"
        viewModel.connectionState == GameViewModel.ConnectionState.CONNECTING -> "Connecting..."
        else -> "Offline"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(statusColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = if (viewModel.authState == GameViewModel.AuthState.GUEST) "Guest" else (viewModel.currentUser ?: "User"),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = statusText,
                color = Color(0xFFD6B37A),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StatusSection(viewModel: GameViewModel) {
    val engine = viewModel.engine
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TURN", color = Color(0xFFEBC98F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                if (engine.gameOver) "OVER" else if (engine.isPlayerTurn) "YOU" else "OPP",
                color = Color.White,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DECK", color = Color(0xFFEBC98F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${engine.deck.size} Cards", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun SidebarContent(
    viewModel: GameViewModel,
    checkAndRun: (Array<String>, () -> Unit) -> Unit,
    onActionStarted: () -> Unit
) {
    var expandedMode by remember { mutableStateOf<GameViewModel.ConnectionType?>(null) }
    var showError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .padding(24.dp)
    ) {
        // User Status Section
        UserStatusHeader(viewModel)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "GAME MODES",
            color = Color(0xFFD6B37A),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Status Section: TURN and DECK
        StatusSection(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF5A3822), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // LOCAL PLAY
        ModeItem(
            title = "LOCAL PLAY",
            icon = Icons.Default.Person,
            isSelected = !viewModel.isMultiplayer,
            onClick = {
                viewModel.disconnect()
                expandedMode = null
                onActionStarted()
            }
        )

        // BLUETOOTH
        ModeItem(
            title = "BLUETOOTH",
            icon = Icons.Default.Bluetooth,
            isSelected = viewModel.isMultiplayer && viewModel.connectionType == GameViewModel.ConnectionType.BLUETOOTH,
            onClick = {
                expandedMode = if (expandedMode == GameViewModel.ConnectionType.BLUETOOTH) null else GameViewModel.ConnectionType.BLUETOOTH
            }
        )
        AnimatedVisibility(visible = expandedMode == GameViewModel.ConnectionType.BLUETOOTH) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                SubModeItem("Host Game") {
                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    checkAndRun(perms) {
                        viewModel.startHosting(GameViewModel.ConnectionType.BLUETOOTH)
                        onActionStarted()
                    }
                }
                var showDeviceDialog by remember { mutableStateOf(false) }
                SubModeItem("Join Game") {
                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    checkAndRun(perms) { showDeviceDialog = true }
                }
                if (showDeviceDialog) {
                    DeviceSelectionDialog(
                        devices = viewModel.getPairedDevices(),
                        onDeviceSelected = { device ->
                            viewModel.connectToHost(device.address, GameViewModel.ConnectionType.BLUETOOTH)
                            showDeviceDialog = false
                            onActionStarted()
                        },
                        onDismiss = { showDeviceDialog = false }
                    )
                }
            }
        }

        // WIFI (LAN)
        ModeItem(
            title = "WIFI (LAN)",
            icon = Icons.Default.Wifi,
            isSelected = viewModel.isMultiplayer && viewModel.connectionType == GameViewModel.ConnectionType.LAN,
            onClick = {
                expandedMode = if (expandedMode == GameViewModel.ConnectionType.LAN) null else GameViewModel.ConnectionType.LAN
            }
        )
        AnimatedVisibility(visible = expandedMode == GameViewModel.ConnectionType.LAN) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                SubModeItem("Host Game") {
                    viewModel.startHosting(GameViewModel.ConnectionType.LAN)
                    onActionStarted()
                }
                var showIpDialog by remember { mutableStateOf(false) }
                SubModeItem("Join Game") {
                    showIpDialog = true
                }
                if (showIpDialog) {
                    IpInputDialog(
                        title = "Enter Host IP Address",
                        onIpEntered = { ip ->
                            viewModel.connectToHost(ip, GameViewModel.ConnectionType.LAN)
                            showIpDialog = false
                            onActionStarted()
                        },
                        onDismiss = { showIpDialog = false }
                    )
                }
            }
        }

        // ONLINE PLAY
        ModeItem(
            title = "ONLINE PLAY",
            icon = Icons.Default.Public,
            isSelected = viewModel.isMultiplayer && viewModel.connectionType == GameViewModel.ConnectionType.ONLINE,
            onClick = {
                expandedMode = if (expandedMode == GameViewModel.ConnectionType.ONLINE) null else GameViewModel.ConnectionType.ONLINE
                if (expandedMode == GameViewModel.ConnectionType.ONLINE) {
                    viewModel.refreshOnlinePlayers()
                }
            }
        )
        AnimatedVisibility(visible = expandedMode == GameViewModel.ConnectionType.ONLINE) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                if (!viewModel.isConnectedToServer) {
                    Text("Log in from Profile to see players", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(8.dp))
                } else if (viewModel.onlinePlayers.isEmpty()) {
                    Text("No players online", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(8.dp))
                } else {
                    viewModel.onlinePlayers.forEach { entry ->
                        val parts = entry.split("|")
                        val name = parts.getOrNull(0) ?: "Unknown"
                        val id = parts.getOrNull(1) ?: ""
                        SubModeItem(name) {
                            viewModel.connectToHost(id, GameViewModel.ConnectionType.ONLINE)
                            onActionStarted()
                            
                            scope.launch {
                                delay(5000)
                                if (viewModel.connectionType != GameViewModel.ConnectionType.ONLINE) {
                                    showError = true
                                    delay(3000)
                                    showError = false
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ModeItem(
            title = "SETTINGS",
            icon = Icons.Default.Settings,
            isSelected = false,
            onClick = {
                onActionStarted()
                context.startActivity(android.content.Intent(context, SettingsActivity::class.java))
            }
        )
        ModeItem(
            title = "HELP",
            icon = Icons.AutoMirrored.Filled.Help,
            isSelected = false,
            onClick = { /* Handle help */ }
        )

        if (viewModel.isMultiplayer) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.disconnect(); onActionStarted() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A2F24)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("DISCONNECT", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF5A3822), thickness = 1.dp)
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "CONNECT TO SERVER", 
                color = if (viewModel.authState == GameViewModel.AuthState.GUEST) Color.Gray else Color.White, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold
            )
                Switch(
                    enabled = viewModel.authState != GameViewModel.AuthState.GUEST,
                    checked = viewModel.isConnectedToServer,
                    onCheckedChange = { 
                        viewModel.toggleServerConnection("", it)
                    },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFD6B37A),
                    checkedTrackColor = Color(0xFF5A3822),
                    disabledCheckedThumbColor = Color.Gray,
                    disabledUncheckedThumbColor = Color.DarkGray
                )
            )
        }
    }

    if (showError) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("connection error", color = Color.White, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun ModeItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF5A3822) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color(0xFFD6B37A))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SubModeItem(title: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowRight, contentDescription = null, tint = Color(0xFFD6B37A))
            Text(title, color = Color.LightGray)
        }
    }
}

@Composable
fun DeviceSelectionDialog(
    devices: List<android.bluetooth.BluetoothDevice>,
    onDeviceSelected: (android.bluetooth.BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Host") },
        text = {
            Column {
                if (devices.isEmpty()) {
                    Text("No paired devices found.")
                }
                devices.forEach { device ->
                    @SuppressLint("MissingPermission")
                    val name = device.name ?: "Unknown Device"
                    Text(
                        text = "$name\n${device.address}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device) }
                            .padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun IpInputDialog(
    title: String,
    onIpEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var ip by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("IP Address") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (ip.isNotBlank()) onIpEntered(ip) }) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
