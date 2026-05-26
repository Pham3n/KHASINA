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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import play.zulu.khasina.ui.theme.KHASINATheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            drawerContainerColor = Color(0xFF2A1B12),
                            drawerContentColor = Color(0xFFEBC98F)
                        ) {
                            MultiplayerDrawerContent(
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
fun MultiplayerDrawerContent(
    viewModel: GameViewModel,
    checkAndRun: (Array<String>, () -> Unit) -> Unit,
    onActionStarted: () -> Unit
) {
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showIpDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Multiplayer", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFEBC98F))
        Spacer(modifier = Modifier.height(24.dp))

        // Bluetooth Section
        MultiplayerCategory(
            title = "Bluetooth",
            onHost = {
                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
                } else {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                checkAndRun(perms) {
                    viewModel.startHosting(GameViewModel.ConnectionType.BLUETOOTH)
                    onActionStarted()
                }
            },
            onJoin = {
                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                checkAndRun(perms) { showDeviceDialog = true }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // WiFi Section
        MultiplayerCategory(
            title = "WiFi (LAN)",
            onHost = {
                viewModel.startHosting(GameViewModel.ConnectionType.LAN)
                onActionStarted()
            },
            onJoin = { showIpDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Online Section
        MultiplayerCategory(
            title = "Online",
            onHost = null,
            onJoin = { showServerDialog = true }
        )

        if (viewModel.isMultiplayer) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { viewModel.disconnect(); onActionStarted() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f))
            ) {
                Text("Disconnect")
            }
        }
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

    if (showServerDialog) {
        IpInputDialog(
            title = "Enter Server IP Address",
            onIpEntered = { ip ->
                viewModel.connectToHost(ip, GameViewModel.ConnectionType.ONLINE)
                showServerDialog = false
                onActionStarted()
            },
            onDismiss = { showServerDialog = false }
        )
    }
}

@Composable
fun MultiplayerCategory(
    title: String,
    onHost: (() -> Unit)?,
    onJoin: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            color = Color.Transparent
        ) {
            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color(0xFFEBC98F))
                Spacer(modifier = Modifier.weight(1f))
                Text(if (expanded) "▲" else "▼", color = Color(0xFFEBC98F))
            }
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                if (onHost != null) {
                    TextButton(onClick = onHost) { Text("Host Game", color = Color.White) }
                }
                TextButton(onClick = onJoin) { Text("Join Game", color = Color.White) }
            }
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
