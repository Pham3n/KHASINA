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

    // Selection State
    var selectedCardHand by mutableStateOf<Card?>(null)
    val selectedCardsFloor = mutableStateListOf<Card>()
    val selectedConstructions = mutableStateListOf<Construction>()
    var selectedOpponentStackCard by mutableStateOf<Card?>(null)
    
    var turnTimerJob: Job? = null
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
        onConnected = { onConnected() },
        onReceived = { message -> handleReceivedMessage(message) }
    )

    private fun onConnected() {
        if (isHost || connectionType == ConnectionType.ONLINE) {
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
            ConnectionType.BLUETOOTH -> { bluetoothService.startHost(); lastMessage = "Hosting via Bluetooth..." }
            ConnectionType.LAN -> { lanService.startHost(); lastMessage = "Hosting via WiFi at ${lanService.getLocalIpAddress()}" }
            ConnectionType.ONLINE -> { lastMessage = "Online lobby enabled." }
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
        bluetoothService.stop(); lanService.stop(); onlineService.stop()
        isMultiplayer = false; isHost = false; connectionType = null
        resetLocalGame()
        engine.useAI = true
        lastMessage = "Disconnected. AI mode."
    }

    fun getPairedDevices(): List<android.bluetooth.BluetoothDevice> {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        return try { adapter?.bondedDevices?.toList() ?: emptyList() } catch (e: SecurityException) { emptyList() }
    }

    private fun handleReceivedMessage(message: String) {
        try {
            if (message.startsWith("MOVE:")) {
                // For manual play, MOVE might need more data (selected floor cards etc)
                // For basic version, we stick to the card played
                val cardJson = message.substring(5)
                val card = gson.fromJson(cardJson, Card::class.java)
                engine.playCard(card, false) 
                lastMessage = "Your turn!"
            } else if (message.startsWith("SYNC:")) {
                val state = gson.fromJson(message.substring(5), GameState::class.java)
                engine.importState(state)
                lastMessage = if (engine.isPlayerTurn) "Your turn!" else "Waiting for opponent..."
            } else if (message == "RESET") {
                resetLocalGame()
                lastMessage = "Game reset by opponent."
            }
        } catch (e: Exception) { lastMessage = "Error: ${e.message}" }
    }

    // Toggle Selection Handlers
    fun onCardHandClicked(card: Card) {
        if (!engine.isPlayerTurn || engine.gameOver) return
        selectedCardHand = if (selectedCardHand == card) null else card
    }

    fun onCardFloorClicked(card: Card) {
        if (!engine.isPlayerTurn || engine.gameOver) return
        if (selectedCardsFloor.contains(card)) selectedCardsFloor.remove(card)
        else selectedCardsFloor.add(card)
    }

    fun onConstructionClicked(construction: Construction) {
        if (!engine.isPlayerTurn || engine.gameOver) return
        if (selectedConstructions.contains(construction)) selectedConstructions.remove(construction)
        else selectedConstructions.add(construction)
    }

    fun onOpponentStackClicked() {
        if (!engine.isPlayerTurn || engine.gameOver) return
        val top = engine.aiStack.lastOrNull() ?: return
        selectedOpponentStackCard = if (selectedOpponentStackCard == top) null else top
    }

    fun onCaptureClicked() {
        if (engine.gameOver) return
        val card = selectedCardHand ?: return // Must play a card from hand to start/capture
        
        val success = engine.executeManualCapture(
            card,
            selectedCardsFloor.toList(),
            selectedConstructions.toList(),
            selectedOpponentStackCard,
            true
        )

        clearSelections()

        if (success) {
            // Valid play: start/reset 3-second timer for multi-stage
            isMultiStagePlayActive = true
            startMultiStageTimer()
        } else {
            // Invalid play: card went to floor, turn ended.
            endTurn()
        }
    }

    private fun startMultiStageTimer() {
        turnTimerJob?.cancel()
        turnTimerJob = viewModelScope.launch {
            lastMessage = "3s to add more..."
            delay(3000)
            endTurn()
        }
    }

    private fun endTurn() {
        isMultiStagePlayActive = false
        engine.isPlayerTurn = false
        if (isMultiplayer) {
            // Send move to other player
            // bluetoothService.send(...)
        } else if (engine.useAI) {
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
        } else lastMessage = "Your turn!"
    }

    private fun resetLocalGame() {
        val currentUseAi = engine.useAI
        engine = GameEngine()
        engine.useAI = currentUseAi
        clearSelections()
    }
}
