package play.zulu.khasina

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.gson.Gson

class GameViewModel : ViewModel() {
    var engine by mutableStateOf(GameEngine())
        private set
    
    var lastMessage by mutableStateOf("Your turn!")
        private set

    var isMultiplayer by mutableStateOf(false)
    var isHost by mutableStateOf(false)
    var connectionType by mutableStateOf<ConnectionType?>(null)

    var selectedCard by mutableStateOf<Card?>(null)
    
    enum class ConnectionType { BLUETOOTH, LAN, ONLINE }

    private val gson = Gson()
    private val bluetoothService = BluetoothService(
        onConnected = { onConnected() },
        onReceived = { message -> handleReceivedMessage(message) }
    )
    
    private val lanService = LanService(
        onConnected = { onConnected() },
        onReceived = { message -> handleReceivedMessage(message) }
    )

    private val onlineService = OnlineService(
        onConnected = { onConnected() },
        onReceived = { message -> handleReceivedMessage(message) }
    )

    private fun onConnected() {
        if (isHost || connectionType == ConnectionType.ONLINE) {
            // For ONLINE, the server might handle initial sync or we do it here if it's the 2nd player
            val state = engine.exportState()
            sendData("SYNC:${gson.toJson(state)}")
            lastMessage = "Opponent connected! Your turn."
        } else {
            lastMessage = "Connected! Waiting for host..."
        }
    }

    private fun sendData(data: String) {
        when (connectionType) {
            ConnectionType.BLUETOOTH -> bluetoothService.send(data)
            ConnectionType.LAN -> lanService.send(data)
            ConnectionType.ONLINE -> onlineService.send(data)
            null -> {}
        }
    }

    fun startHosting(type: ConnectionType) {
        isMultiplayer = true
        isHost = true
        connectionType = type
        engine.useAI = false
        when (type) {
            ConnectionType.BLUETOOTH -> {
                bluetoothService.startHost()
                lastMessage = "Hosting via Bluetooth..."
            }
            ConnectionType.LAN -> {
                lanService.startHost()
                val ip = lanService.getLocalIpAddress()
                lastMessage = "Hosting via WiFi at $ip"
            }
            ConnectionType.ONLINE -> {
                // For online, "hosting" usually means connecting to a lobby
                // This depends on the python server logic. For now, same as connect.
                lastMessage = "Online hosting not implemented specifically, use Connect."
            }
        }
    }

    fun connectToHost(address: String, type: ConnectionType) {
        isMultiplayer = true
        isHost = false
        connectionType = type
        engine.useAI = false
        when (type) {
            ConnectionType.BLUETOOTH -> bluetoothService.connect(address)
            ConnectionType.LAN -> lanService.connect(address)
            ConnectionType.ONLINE -> onlineService.connect(address)
        }
        lastMessage = "Connecting..."
    }

    fun disconnect() {
        bluetoothService.stop()
        lanService.stop()
        onlineService.stop()
        isMultiplayer = false
        isHost = false
        connectionType = null
        resetLocalGame()
        engine.useAI = true
        lastMessage = "Disconnected. AI mode."
    }

    fun getPairedDevices(): List<android.bluetooth.BluetoothDevice> {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        return try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    private fun handleReceivedMessage(message: String) {
        try {
            if (message.startsWith("MOVE:")) {
                val cardJson = message.substring(5)
                val card = gson.fromJson(cardJson, Card::class.java)
                engine.playCard(card, false) 
                lastMessage = "Your turn!"
            } else if (message.startsWith("SYNC:")) {
                val stateJson = message.substring(5)
                val state = gson.fromJson(stateJson, GameState::class.java)
                engine.importState(state)
                lastMessage = if (engine.isPlayerTurn) "Your turn!" else "Waiting for opponent..."
            } else if (message == "RESET") {
                resetLocalGame()
                lastMessage = "Game reset by opponent."
            }
        } catch (e: Exception) {
            lastMessage = "Error: ${e.message}"
        }
    }

    fun onCardClicked(card: Card) {
        if (!engine.isPlayerTurn || engine.gameOver) return
        
        val captured = engine.playCard(card, true) 
        if (captured) {
            if (isMultiplayer) {
                lastMessage = "Waiting for opponent..."
                sendData("MOVE:${gson.toJson(card)}")
            } else {
                lastMessage = "AI is thinking..."
                if (engine.gameOver) {
                    val scores = engine.calculateScores()
                    lastMessage = "Game Over! Player: ${scores["Player"]}, AI: ${scores["AI"]}"
                } else {
                    lastMessage = "Your turn!"
                }
            }
        }
    }

    fun resetGame() {
        resetLocalGame()
        if (isMultiplayer) {
            sendData("RESET")
            if (isHost) {
                val state = engine.exportState()
                sendData("SYNC:${gson.toJson(state)}")
            }
            lastMessage = "Game Reset Sent."
        } else {
            lastMessage = "Your turn!"
        }
    }

    private fun resetLocalGame() {
        val currentUseAi = engine.useAI
        engine = GameEngine()
        engine.useAI = currentUseAi
    }
}
