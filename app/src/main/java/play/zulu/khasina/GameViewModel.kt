package play.zulu.khasina

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    val onlinePlayers = mutableStateListOf<String>() // Mock online players list
    val userChats = mutableStateListOf<ChatItem>()

    // Selection State
    var selectedCardHand by mutableStateOf<Card?>(null)
    val selectedCardsFloor = mutableStateListOf<Card>()
    val selectedConstructions = mutableStateListOf<Construction>()
    var selectedOpponentStackCard by mutableStateOf<Card?>(null)
    
    var isChatVisible by mutableStateOf(false)
    var isProfileVisible by mutableStateOf(false)
    
    private var turnTimerJob: Job? = null
    var isMultiStagePlayActive by mutableStateOf(false)

    enum class ConnectionType { BLUETOOTH, LAN, ONLINE }
    enum class AuthState { GUEST, AUTHENTICATED }
    enum class ConnectionState { OFFLINE, CONNECTING, ONLINE }

    var authState by mutableStateOf(AuthState.GUEST)
    var connectionState by mutableStateOf(ConnectionState.OFFLINE)
    var currentUser by mutableStateOf<String?>(null)

    private val gson = Gson()
    private val bluetoothService = BluetoothService(
        onConnected = { onConnected() },
        onReceived = { message -> handleReceivedMessage(message) }
    )
    private val lanService = LanService(
        onConnected = { onConnected() },
        onReceived = { message -> handleReceivedMessage(message) }
    )
    
    // Services for the three servers
    private val authService = OnlineService(
        onConnected = { /* Auth usually handles session tokens */ },
        onReceived = { message -> /* Handle auth-specific logic (profiles, friends) */ }
    )
    private val gameService = OnlineService(
        onConnected = { 
            isConnectedToServer = true
            connectionState = ConnectionState.ONLINE
            onConnected() 
        },
        onReceived = { message -> handleReceivedMessage(message) }
    )
    private val chatService = OnlineService(
        onConnected = { /* Connected to Chat */ },
        onReceived = { message -> /* Handle chat messages/presence */ }
    )

    private var authServerAddress: Pair<String, Int>? = null
    private var gameServerAddress: Pair<String, Int>? = null
    private var chatServerAddress: Pair<String, Int>? = null

    init {
        // Initialize default user chats
        userChats.addAll(listOf(
            ChatItem("Friends", "Private • 3 members", Icons.Default.Groups, Color(0xFF1C6E6A)),
            ChatItem("Khasina Players", "Private • 8 members", Icons.Default.Person, Color(0xFF7A431F)),
            ChatItem("Umlabalaba Club", "Private • 12 members", Icons.Default.SportsEsports, Color(0xFF244E7A)),
            ChatItem("Strategy Masters", "Private • 5 members", Icons.Default.Psychology, Color(0xFF476B2D))
        ))
    }

    private fun onConnected() {
        if (isHost || connectionType == ConnectionType.ONLINE) {
            sendData("SYNC:${gson.toJson(engine.exportState())}")
            lastMessage = "Opponent connected! Your turn."
        } else lastMessage = "Connected! Waiting for host..."
        
        if (isConnectedToServer) {
            onlinePlayers.clear()
            onlinePlayers.addAll(listOf("ZuluWarrior", "KhasinaKing", "StrategyQueen", "CardMaster"))
        }
    }

    private fun sendData(data: String) {
        when (connectionType) {
            ConnectionType.BLUETOOTH -> bluetoothService.send(data)
            ConnectionType.LAN -> lanService.send(data)
            ConnectionType.ONLINE -> gameService.send(data)
            null -> {}
        }
    }

    fun attemptServerConnection() {
        viewModelScope.launch {
            connectionState = ConnectionState.CONNECTING
            
            var foundAuth = false
            var foundGame = false
            var foundChat = false

            // Query IP range 192.168.8.100 - 192.168.8.105
            // Query Port range 8000 - 8010
            for (i in 100..105) {
                val ip = "192.168.8.$i"
                for (port in 8000..8010) {
                    // Use authService as a scanner (it's agnostic until connected)
                    val result = gameService.queryServer(ip, port)
                    when (result) {
                        "PLAYAUTH" -> {
                            authServerAddress = Pair(ip, port)
                            foundAuth = true
                        }
                        "PLAYGAME" -> {
                            gameServerAddress = Pair(ip, port)
                            foundGame = true
                        }
                        "PLAYCHAT" -> {
                            chatServerAddress = Pair(ip, port)
                            foundChat = true
                        }
                    }
                    if (foundAuth && foundGame && foundChat) break
                }
                if (foundAuth && foundGame && foundChat) break
            }

            if (foundAuth && foundGame && foundChat) {
                authService.connect(authServerAddress!!.first, authServerAddress!!.second)
                gameService.connect(gameServerAddress!!.first, gameServerAddress!!.second)
                chatService.connect(chatServerAddress!!.first, chatServerAddress!!.second)
            } else if (foundGame) {
                // Fallback: at least connect to game if found
                gameService.connect(gameServerAddress!!.first, gameServerAddress!!.second)
            } else {
                connectionState = ConnectionState.OFFLINE
                isConnectedToServer = false
            }
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
            if (!isConnectedToServer) {
                attemptServerConnection()
            }
            viewModelScope.launch {
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
            attemptServerConnection()
        } else {
            authService.stop()
            gameService.stop()
            chatService.stop()
            isConnectedToServer = false
            connectionState = ConnectionState.OFFLINE
            authServerAddress = null
            gameServerAddress = null
            chatServerAddress = null
            authState = AuthState.GUEST
            currentUser = null
            userChats.clear()
            if (connectionType == ConnectionType.ONLINE) {
                disconnect()
            }
        }
    }

    fun disconnect() {
        bluetoothService.stop(); lanService.stop(); gameService.stop(); chatService.stop(); authService.stop()
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

    fun registerUser(username: String, country: String, gender: String) {
        toggleServerConnection("10.0.2.2", true)
        if (!userChats.any { it.title == "Main Lobby" }) {
            userChats.add(0, ChatItem("Main Lobby", "Public • Everyone", Icons.Default.Public, Color(0xFFEBC98F)))
        }
        authState = AuthState.AUTHENTICATED
        currentUser = username
        lastMessage = "Welcome, $username! Registered and connected."
    }

    fun loginUser(username: String) {
        toggleServerConnection("10.0.2.2", true)
        if (!userChats.any { it.title == "Main Lobby" }) {
            userChats.add(0, ChatItem("Main Lobby", "Public • Everyone", Icons.Default.Public, Color(0xFFEBC98F)))
        }
        authState = AuthState.AUTHENTICATED
        currentUser = username
        lastMessage = "Welcome back, $username!"
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
