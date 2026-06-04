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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class GameViewModel : ViewModel() {
    var engine by mutableStateOf(GameEngine())
        private set
    
    var lastMessage by mutableStateOf("Your turn!")
        private set

    var isMultiplayer by mutableStateOf(false)
    var isHost by mutableStateOf(false)
    var connectionType by mutableStateOf<ConnectionType?>(null)
    var isConnectedToServer by mutableStateOf(false)
    val onlinePlayers = mutableStateListOf<String>()
    val userChats = mutableStateListOf<ChatItem>()
    var selectedChat by mutableStateOf<ChatItem?>(null)
    val chatMessages = mutableStateListOf<ChatMessage>()

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
    var accessToken by mutableStateOf<String?>(null)
    var currentUserProfile by mutableStateOf<ProfileRead?>(null)
    var currentUserData by mutableStateOf<UserRead?>(null)
    var authErrorMessage by mutableStateOf<String?>(null)
    var authSuccessMessage by mutableStateOf<String?>(null)

    private var userStorage: UserStorage? = null
    private var authApi: AuthApiService? = null
    private var gameApi: GameApiService? = null
    private var chatApi: ChatApiService? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private fun createApis(baseUrl: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        authApi = retrofit.create(AuthApiService::class.java)
    }

    private fun createGameApi(baseUrl: String) {
        gameApi = Retrofit.Builder().baseUrl(baseUrl).client(httpClient).addConverterFactory(GsonConverterFactory.create()).build().create(GameApiService::class.java)
    }

    private fun createChatApi(baseUrl: String) {
        chatApi = Retrofit.Builder().baseUrl(baseUrl).client(httpClient).addConverterFactory(GsonConverterFactory.create()).build().create(ChatApiService::class.java)
    }

    fun initStorage(context: android.content.Context) {
        if (userStorage == null) {
            userStorage = UserStorage(context)
            loadUserLocally()
        }
    }

    private fun loadUserLocally() {
        val storage = userStorage ?: return
        val token = storage.getAccessToken()
        if (token != null) {
            accessToken = token
            currentUserData = storage.getUserData()
            currentUserProfile = storage.getProfileData()
            currentUser = currentUserData?.username
            authState = AuthState.AUTHENTICATED
            // Optionally auto-connect if we have a token
            attemptServerConnection()
        }
    }

    private fun saveUserLocally() {
        userStorage?.saveUser(currentUserData, currentUserProfile, accessToken)
    }

    private fun clearUserLocally() {
        userStorage?.clear()
    }

    // Form Cache
    var cachedUsername by mutableStateOf("")
    var cachedEmail by mutableStateOf("")
    var cachedPassword by mutableStateOf("")
    var cachedCountry by mutableStateOf("")

    private val gson = Gson()
    private val bluetoothService = BluetoothService(
        onConnected = { onConnected() },
        onReceived = { message -> handleReceivedMessage(message) }
    )
    private val lanService = LanService(
        onConnected = { onConnected() },
        onReceived = { message -> handleReceivedMessage(message) }
    )
    
    private val authService = OnlineService(
        onConnected = { },
        onReceived = { }
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
        onConnected = { },
        onReceived = { message -> 
            if (message.startsWith("CHAT_MSG:")) {
                try {
                    val msg = gson.fromJson(message.substring(9), ChatMessage::class.java)
                    chatMessages.add(msg)
                } catch (e: Exception) {}
            }
        }
    )

    private var authServerAddress: Pair<String, Int>? = null
    private var gameServerAddress: Pair<String, Int>? = null
    private var chatServerAddress: Pair<String, Int>? = null

    init {
        userChats.addAll(listOf(
            ChatItem("League Chat", "Default", Icons.Default.EmojiEvents, Color(0xFF8B3A1A)),
            ChatItem("Global Chat", "Public", Icons.Default.Shield, Color(0xFF9C6B28)),
            ChatItem("Announcements", "Updates", Icons.Default.Campaign, Color(0xFF5B3C6F))
        ))
    }

    private fun onConnected() {
        if (isHost || connectionType == ConnectionType.ONLINE) {
            sendData("SYNC:${gson.toJson(engine.exportState())}")
            lastMessage = "Opponent connected! Your turn."
        } else lastMessage = "Connected! Waiting for host..."
        
        if (isConnectedToServer) {
            refreshOnlinePlayers()
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

    suspend fun discoverServers(): Boolean {
        connectionState = ConnectionState.CONNECTING
        authErrorMessage = "Searching for servers..."
        
        var foundAuth = false
        var foundGame = false
        var foundChat = false

        // Scan range 192.168.8.100 - 105
        for (i in 100..105) {
            val ip = "192.168.8.$i"
            for (port in 8000..8010) {
                val result = gameService.queryServer(ip, port)
                when (result) {
                    "PLAYAUTH" -> { authServerAddress = Pair(ip, port); foundAuth = true }
                    "PLAYGAME" -> { gameServerAddress = Pair(ip, port); foundGame = true }
                    "PLAYCHAT" -> { chatServerAddress = Pair(ip, port); foundChat = true }
                }
                if (foundAuth && foundGame && foundChat) break
            }
            if (foundAuth && foundGame && foundChat) break
        }

        if (foundAuth) {
            authErrorMessage = "Server found! Authenticating..."
            createApis("http://${authServerAddress!!.first}:${authServerAddress!!.second}/")
            if (gameServerAddress != null) createGameApi("http://${gameServerAddress!!.first}:${gameServerAddress!!.second}/")
            if (chatServerAddress != null) createChatApi("http://${chatServerAddress!!.first}:${chatServerAddress!!.second}/")
        } else {
            // Hardcoded fallback if discovery fails or to keep it preserved
            // val testIp = "10.54.16.238"
            // authServerAddress = Pair(testIp, 8000)
            // chatServerAddress = Pair(testIp, 8001)
            // gameServerAddress = Pair(testIp, 8002)
            // foundAuth = true
            authErrorMessage = "Server not found on network."
        }
        return foundAuth
    }

    fun attemptServerConnection() {
        viewModelScope.launch {
            val found = discoverServers()
            if (found) {
                connectionState = ConnectionState.ONLINE
                authService.connect(authServerAddress!!.first, authServerAddress!!.second)
                gameService.connect(gameServerAddress!!.first, gameServerAddress!!.second)
                chatService.connect(chatServerAddress!!.first, chatServerAddress!!.second)
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
            initiateOnlineMatch(address)
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
            authService.stop(); gameService.stop(); chatService.stop()
            isConnectedToServer = false; connectionState = ConnectionState.OFFLINE
            authServerAddress = null; gameServerAddress = null; chatServerAddress = null
            authState = AuthState.GUEST; currentUser = null; userChats.clear()
            clearUserLocally()
            if (connectionType == ConnectionType.ONLINE) disconnect()
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

    fun onCaptureClicked() { executePlay(isCapture = true) }
    fun onBuildClicked() { executePlay(isCapture = false) }

    private fun executePlay(isCapture: Boolean) {
        if (engine.gameOver) return
        val card = selectedCardHand ?: return 
        val success = engine.executeManualPlay(card, selectedCardsFloor.toList(), selectedConstructions.toList(), selectedOpponentStackCard, true, isCapture)
        clearSelections()
        if (success) {
            isMultiStagePlayActive = true
            if (isMultiplayer && activeSessionId != null) {
                sendGameAction("MOVE", mapOf("card" to card))
            }
            startMultiStageTimer()
        } else endTurn()
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
        if (isMultiplayer && activeSessionId != null) {
            sendGameAction("SYNC", mapOf("state" to engine.exportState()))
        } else if (engine.useAI) {
            viewModelScope.launch {
                lastMessage = "AI TURN"
                delay(3000)
                engine.aiTurn()
                lastMessage = if (engine.gameOver) "Game Over!" else "YOUR TURN"
            }
        }
    }

    private fun sendGameAction(type: String, payload: Map<String, Any>) {
        viewModelScope.launch {
            val api = gameApi ?: return@launch
            val sessionId = activeSessionId ?: return@launch
            try {
                api.applyAction("Bearer $accessToken", sessionId, GameActionRequest(type, payload))
            } catch (e: Exception) {}
        }
    }

    fun clearSelections() {
        selectedCardHand = null; selectedCardsFloor.clear(); selectedConstructions.clear(); selectedOpponentStackCard = null
    }

    fun resetGame() {
        resetLocalGame()
        if (isMultiplayer) {
            sendData("RESET")
            if (isHost) sendData("SYNC:${gson.toJson(engine.exportState())}")
            lastMessage = "Game Reset Sent."
        } else lastMessage = "YOUR TURN"
    }

    fun registerUser(username: String, email: String, pass: String, country: String) {
        viewModelScope.launch {
            authErrorMessage = null
            authSuccessMessage = null
            connectionState = ConnectionState.CONNECTING
            val foundAuth = discoverServers()
            
            if (foundAuth) {
                val api = authApi
                if (api != null) {
                    try {
                        val response = api.register(UserCreate(username, email, pass))
                        if (response.isSuccessful) {
                            val tokens = response.body()
                            accessToken = tokens?.accessToken
                            val authHeader = "Bearer ${tokens?.accessToken}"

                            // 1. Update Profile (Country)
                            api.updateProfile(authHeader, mapOf("country" to country))
                            
                            // 2. Fetch User & Profile details for the UI
                            currentUserData = api.getMe(authHeader).body()
                            currentUserProfile = api.getProfile(authHeader).body()

                            authState = AuthState.AUTHENTICATED
                            connectionState = ConnectionState.ONLINE
                            currentUser = username
                            lastMessage = "Welcome, $username! Registered."
                            authErrorMessage = null
                            authSuccessMessage = "Account created and logged in!"
                            saveUserLocally()
                            
                            authService.connect(authServerAddress!!.first, authServerAddress!!.second)
                            if (gameServerAddress != null) gameService.connect(gameServerAddress!!.first, gameServerAddress!!.second)
                            if (chatServerAddress != null) chatService.connect(chatServerAddress!!.first, chatServerAddress!!.second)

                            if (!userChats.any { it.title == "Main Lobby" }) {
                                userChats.add(0, ChatItem("Main Lobby", "Public • Everyone", Icons.Default.Public, Color(0xFFEBC98F)))
                            }
                            startSessionPolling()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            authErrorMessage = "Failed: $errorBody"
                            lastMessage = "Registration failed."
                            authState = AuthState.GUEST
                            connectionState = ConnectionState.OFFLINE
                        }
                    } catch (e: Exception) {
                        authErrorMessage = "Error: ${e.message}"
                        lastMessage = "Error: ${e.message}"
                        authState = AuthState.GUEST
                        connectionState = ConnectionState.OFFLINE
                    }
                }
            } else {
                lastMessage = "Auth server not found."
                authErrorMessage = "Auth server not found on network."
                authState = AuthState.GUEST
                connectionState = ConnectionState.OFFLINE
            }
        }
    }

    fun loginUser(username: String, pass: String) {
        viewModelScope.launch {
            authErrorMessage = null
            authSuccessMessage = null
            connectionState = ConnectionState.CONNECTING
            val foundAuth = discoverServers()
            
            if (foundAuth) {
                val api = authApi
                if (api != null) {
                    try {
                        val response = api.login(UserLogin(username, pass))
                        if (response.isSuccessful) {
                            val tokens = response.body()
                            accessToken = tokens?.accessToken
                            val authHeader = "Bearer ${tokens?.accessToken}"

                            // Fetch User & Profile details for the UI
                            currentUserData = api.getMe(authHeader).body()
                            currentUserProfile = api.getProfile(authHeader).body()

                            authState = AuthState.AUTHENTICATED
                            connectionState = ConnectionState.ONLINE
                            currentUser = username
                            lastMessage = "Welcome back, $username!"
                            authErrorMessage = null
                            authSuccessMessage = "Successfully logged in!"
                            saveUserLocally()
                            
                            authService.connect(authServerAddress!!.first, authServerAddress!!.second)
                            if (gameServerAddress != null) gameService.connect(gameServerAddress!!.first, gameServerAddress!!.second)
                            if (chatServerAddress != null) chatService.connect(chatServerAddress!!.first, chatServerAddress!!.second)

                            if (!userChats.any { it.title == "Main Lobby" }) {
                                userChats.add(0, ChatItem("Main Lobby", "Public • Everyone", Icons.Default.Public, Color(0xFFEBC98F)))
                            }
                            startSessionPolling()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            authErrorMessage = "Invalid credentials or Server error."
                            lastMessage = "Login failed."
                            authState = AuthState.GUEST
                            connectionState = ConnectionState.OFFLINE
                        }
                    } catch (e: Exception) {
                        authErrorMessage = "Error: ${e.message}"
                        lastMessage = "Error: ${e.message}"
                        authState = AuthState.GUEST
                        connectionState = ConnectionState.OFFLINE
                    }
                }
            } else {
                lastMessage = "Auth server not found."
                authErrorMessage = "Auth server not found on network."
                authState = AuthState.GUEST
                connectionState = ConnectionState.OFFLINE
            }
        }
    }

    fun refreshOnlinePlayers() {
        viewModelScope.launch {
            val api = chatApi ?: return@launch
            val auth = authApi ?: return@launch
            try {
                val response = api.listOnlinePlayers()
                if (response.isSuccessful) {
                    val presences = response.body() ?: emptyList()
                    onlinePlayers.clear()
                    presences.forEach { presence ->
                        if (presence.user_id != currentUserData?.id) {
                            val userResp = auth.getUser(presence.user_id)
                            val name = if (userResp.isSuccessful) userResp.body()?.username ?: presence.user_id.toString() 
                                       else presence.user_id.toString()
                            onlinePlayers.add("$name|${presence.user_id}")
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private var sessionPollingJob: Job? = null
    var activeSessionId: UUID? = null

    fun startSessionPolling() {
        sessionPollingJob?.cancel()
        sessionPollingJob = viewModelScope.launch {
            val api = gameApi ?: return@launch
            while (true) {
                try {
                    if (!isMultiplayer) {
                        val response = api.listMySessions("Bearer $accessToken")
                        if (response.isSuccessful) {
                            val activeSession = response.body()?.firstOrNull()
                            if (activeSession != null) {
                                activeSessionId = activeSession.id
                                isMultiplayer = true
                                isHost = activeSession.players[0] == currentUserData?.id.toString()
                                connectionType = ConnectionType.ONLINE
                                engine.useAI = false
                                lastMessage = if (engine.isPlayerTurn) "Match joined! Your turn." else "Match joined! Waiting..."
                                engine.importState(gson.fromJson(gson.toJson(activeSession.state), GameState::class.java))
                            }
                        }
                    } else if (activeSessionId != null) {
                        pollGameEvents(activeSessionId!!)
                    }
                } catch (e: Exception) {}
                delay(3000)
            }
        }
    }

    private suspend fun pollGameEvents(sessionId: UUID) {
        val api = gameApi ?: return
        try {
            val response = api.getEvents(sessionId)
            if (response.isSuccessful) {
                response.body()?.forEach { event ->
                    if (event.player_id != currentUserData?.id) {
                        handleGameEvent(event)
                    }
                }
            }
        } catch (e: Exception) {}
    }

    private fun handleGameEvent(event: GameEventRead) {
        try {
            when (event.event_type) {
                "MOVE" -> {
                    val card = gson.fromJson(gson.toJson(event.payload["card"]), Card::class.java)
                    engine.playCard(card, false)
                    lastMessage = "Your turn!"
                }
                "SYNC" -> {
                    val state = gson.fromJson(gson.toJson(event.payload["state"]), GameState::class.java)
                    engine.importState(state)
                    lastMessage = if (engine.isPlayerTurn) "YOUR TURN" else "Waiting for opponent..."
                }
                "RESET" -> {
                    resetLocalGame()
                    lastMessage = "Game reset by opponent."
                }
            }
        } catch (e: Exception) {}
    }

    fun initiateOnlineMatch(opponentId: String) {
        viewModelScope.launch {
            val api = gameApi ?: return@launch
            val myId = currentUserData?.id ?: return@launch
            try {
                val response = api.createSession(
                    "Bearer $accessToken", 
                    CreateSessionRequest(players = listOf(myId, UUID.fromString(opponentId)))
                )
                if (response.isSuccessful) {
                    val session = response.body()
                    if (session != null) {
                        activeSessionId = session.id
                        isMultiplayer = true
                        isHost = true
                        connectionType = ConnectionType.ONLINE
                        engine.useAI = false
                        lastMessage = "Match started! Your turn."
                        startSessionPolling()
                    }
                } else {
                    lastMessage = "Failed: ${response.message()}"
                }
            } catch (e: Exception) { lastMessage = "Error: ${e.message}" }
        }
    }

    fun loadChatMessages(chat: ChatItem) {
        viewModelScope.launch {
            val api = chatApi ?: return@launch
            val roomsResponse = api.listRooms()
            if (roomsResponse.isSuccessful) {
                val room = roomsResponse.body()?.find { it.name == chat.title }
                if (room != null) {
                    val msgResponse = api.getMessages(room.id)
                    if (msgResponse.isSuccessful) {
                        chatMessages.clear()
                        msgResponse.body()?.forEach { m -> chatMessages.add(ChatMessage(m.sender_id.toString(), m.content, m.created_at)) }
                    }
                }
            }
        }
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
