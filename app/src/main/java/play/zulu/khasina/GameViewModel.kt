package play.zulu.khasina

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    var engine by mutableStateOf(GameEngine())
        private set
    
    var lastMessage by mutableStateOf("Your turn!")
        private set

    var isMultiplayer by mutableStateOf(false)
    var isHost by mutableStateOf(false)
    var connectionType by mutableStateOf<ConnectionType?>(null)
    var isConnectedToServer by mutableStateOf(false)

    // Selection State
    var selectedCardHand by mutableStateOf<Card?>(null)
    val selectedCardsFloor = mutableStateListOf<Card>()
    val selectedConstructions = mutableStateListOf<Construction>()
    var selectedOpponentStackCard by mutableStateOf<Card?>(null)
    
    var isChatVisible by mutableStateOf(false)
    
    private var turnTimerJob: Job? = null
    var isMultiStagePlayActive by mutableStateOf(false)

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
        onConnected = { 
            isConnectedToServer = true
            onConnected() 
        },
        onReceived = { message -> handleReceivedMessage(message) }
    )

    private fun onConnected() {
        if (isHost || connectionType == ConnectionType.ONLINE) {
            sendData("SYNC:${gson.toJson(engine.exportState())}")
            lastMessage = "Opponent connected! Your turn."
        } else lastMessage = "Connected! Waiting for host..."
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
        isMultiplayer = true; isHost = true; connectionType = type; engine.useAI = false
        if (type == ConnectionType.BLUETOOTH) bluetoothService.startHost()
        else if (type == ConnectionType.LAN) lanService.startHost()
        lastMessage = if (type == ConnectionType.LAN) "Hosting via WiFi at ${lanService.getLocalIpAddress()}" else "Hosting..."
    }

    fun connectToHost(address: String, type: ConnectionType) {
        if (type == ConnectionType.ONLINE) {
            // Check if we are already connected via the global switch
            if (!isConnectedToServer) {
                onlineService.connect(address)
            }
            // Logic to transition mode to ONLINE after connection is confirmed
            viewModelScope.launch {
                // Wait for connection (simple polling for this implementation)
                var retry = 10
                while (!isConnectedToServer && retry > 0) {
                    delay(500)
                    retry--
                }
                if (isConnectedToServer) {
                    isMultiplayer = true
                    isHost = false
                    connectionType = type
                    engine.useAI = false
                    lastMessage = "Connected to Server. Joined lobby."
                } else {
                    lastMessage = "Connection failed."
                }
            }
        } else {
            isMultiplayer = true; isHost = false; connectionType = type; engine.useAI = false
            if (type == ConnectionType.BLUETOOTH) bluetoothService.connect(address)
            else if (type == ConnectionType.LAN) lanService.connect(address)
            lastMessage = "Connecting..."
        }
    }

    fun toggleServerConnection(address: String, connect: Boolean) {
        if (connect) {
            onlineService.connect(address)
        } else {
            onlineService.stop()
            isConnectedToServer = false
            if (connectionType == ConnectionType.ONLINE) {
                disconnect()
            }
        }
    }

    fun disconnect() {
        bluetoothService.stop(); lanService.stop(); onlineService.stop()
        isMultiplayer = false; isHost = false; connectionType = null
        resetLocalGame(); engine.useAI = true
        lastMessage = "Disconnected. AI mode."
    }

    fun getPairedDevices(): List<android.bluetooth.BluetoothDevice> {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        return try { adapter?.bondedDevices?.toList() ?: emptyList() } catch (e: SecurityException) { emptyList() }
    }

    private fun handleReceivedMessage(message: String) {
        try {
            if (message.startsWith("SYNC:")) {
                engine.importState(gson.fromJson(message.substring(5), GameState::class.java))
                lastMessage = if (engine.isPlayerTurn) "YOUR TURN" else "Waiting for opponent..."
            } else if (message == "RESET") {
                resetLocalGame(); lastMessage = "Game reset by opponent."
            }
            // For simplicity in this architecture, full state sync is safer than delta moves
        } catch (e: Exception) { lastMessage = "Error: ${e.message}" }
    }

    // Toggle Selection Handlers
    fun onCardHandClicked(card: Card) {
        if (!engine.isPlayerTurn || engine.gameOver || isMultiStagePlayActive) return
        selectedCardHand = if (selectedCardHand == card) null else card
    }
    fun onCardFloorClicked(card: Card) {
        if (!engine.isPlayerTurn || engine.gameOver) return
        if (selectedCardsFloor.contains(card)) selectedCardsFloor.remove(card) else selectedCardsFloor.add(card)
    }
    fun onConstructionClicked(construction: Construction) {
        if (!engine.isPlayerTurn || engine.gameOver) return
        if (selectedConstructions.contains(construction)) selectedConstructions.remove(construction) else selectedConstructions.add(construction)
    }
    fun onOpponentStackClicked() {
        if (!engine.isPlayerTurn || engine.gameOver) return
        val top = engine.aiStack.lastOrNull() ?: return
        selectedOpponentStackCard = if (selectedOpponentStackCard == top) null else top
    }

    fun onCaptureClicked() {
        executePlay(isCapture = true)
    }

    fun onBuildClicked() {
        executePlay(isCapture = false)
    }

    private fun executePlay(isCapture: Boolean) {
        if (engine.gameOver) return
        val card = selectedCardHand ?: return 
        
        val success = engine.executeManualPlay(
            playedCard = card,
            selectedFloorCards = selectedCardsFloor.toList(),
            selectedConstructions = selectedConstructions.toList(),
            recoveredOpponentCard = selectedOpponentStackCard,
            isPlayer = true,
            isCapture = isCapture
        )

        clearSelections()

        if (success) {
            isMultiStagePlayActive = true
            startMultiStageTimer()
            if (isMultiplayer) sendData("SYNC:${gson.toJson(engine.exportState())}")
        } else {
            endTurn()
        }
    }

    private fun startMultiStageTimer() {
        turnTimerJob?.cancel()
        turnTimerJob = viewModelScope.launch {
            lastMessage = "YOUR TURN (5s to add more)"
            delay(5000)
            endTurn()
        }
    }

    private fun endTurn() {
        turnTimerJob?.cancel()
        isMultiStagePlayActive = false
        engine.isPlayerTurn = false
        clearSelections()
        
        if (isMultiplayer) sendData("SYNC:${gson.toJson(engine.exportState())}")
        else if (engine.useAI) {
            viewModelScope.launch {
                lastMessage = "AI TURN"
                delay(3000)
                engine.aiTurn()
                lastMessage = if (engine.gameOver) "Game Over!" else "YOUR TURN"
            }
        }
    }

    fun clearSelections() {
        selectedCardHand = null
        selectedCardsFloor.clear()
        selectedConstructions.clear()
        selectedOpponentStackCard = null
    }

    fun resetGame() {
        resetLocalGame()
        if (isMultiplayer) {
            sendData("RESET")
            if (isHost) sendData("SYNC:${gson.toJson(engine.exportState())}")
            lastMessage = "Game Reset Sent."
        } else lastMessage = "YOUR TURN"
    }

    private fun resetLocalGame() {
        val currentUseAi = engine.useAI
        engine = GameEngine()
        engine.useAI = currentUseAi
        clearSelections()
        isMultiStagePlayActive = false
        turnTimerJob?.cancel()
    }
}
