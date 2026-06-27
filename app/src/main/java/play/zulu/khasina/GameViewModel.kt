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
    var isConnectionEnabled by mutableStateOf(true)
    val onlinePlayers = mutableStateListOf<String>()
    val officialChats = mutableStateListOf<ChatItem>()
    val personalChats = mutableStateListOf<ChatItem>()
    var selectedChat by mutableStateOf<ChatItem?>(null)
    val chatMessages = mutableStateListOf<ChatMessage>()
    val roomMembers = mutableStateListOf<PresenceRead>()

    // Selection State
    var selectedCardHand by mutableStateOf<Card?>(null)
    val selectedCardsFloor = mutableStateListOf<Card>()
    val selectedConstructions = mutableStateListOf<Construction>()
    var selectedOpponentStackCard by mutableStateOf<Card?>(null)
    
    var isChatVisible by mutableStateOf(false)
    var isProfileVisible by mutableStateOf(false)
    var isFriendsVisible by mutableStateOf(false)
    var isUserDetailVisible by mutableStateOf(false)
    var floatingMessage by mutableStateOf<String?>(null)
    
    private var refreshFriendsJob: Job? = null
    
    // Invitation & AI Logic
    var incomingInvitation by mutableStateOf<GameSessionRead?>(null)
    var isLocalAiEnabled by mutableStateOf(true)
    var localPlayerCount by mutableStateOf(2)

    var currentRound by mutableStateOf(1)
    val maxRounds = 2
    val cumulativeScores = mutableStateListOf(0, 0, 0, 0) // Teams/Players 0-3

    var selectedUserForProfile by mutableStateOf<UserRead?>(null)
    val matchLobbyPlayers = mutableStateListOf<UserRead>()

    private var turnTimerJob: Job? = null
    var isMultiStagePlayActive by mutableStateOf(false)
    var captureRetriesRemaining by mutableStateOf(1)

    private var sessionPollingJob: Job? = null
    var activeSessionId: UUID? = null

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
    val friendsList = mutableStateListOf<FriendRead>()

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
            isConnectionEnabled = userStorage?.isConnectionEnabled() ?: true
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
            val cachedRooms = storage.getChatRooms()
            if (cachedRooms.isNotEmpty()) updateChatLists(cachedRooms)
            if (isConnectionEnabled) attemptServerConnection()
        }
    }

    private fun updateChatLists(rooms: List<ChatRoomRead>) {
        officialChats.clear(); personalChats.clear()
        rooms.forEach { room ->
            val roomNameLower = room.name.lowercase()
            val isGlobal = roomNameLower.contains("global") || roomNameLower.contains("lobby")
            val isLeague = roomNameLower.contains("league")
            val isAnnounce = roomNameLower.contains("announcements")
            val isOfficial = isGlobal || isLeague || isAnnounce
            val localName = when {
                isGlobal -> "Global"; isLeague -> "League"; isAnnounce -> "Announcements"; else -> room.name
            }
            val icon = when {
                isGlobal -> Icons.Default.Public; isLeague -> Icons.Default.EmojiEvents; isAnnounce -> Icons.Default.Campaign; else -> Icons.Default.Chat
            }
            val item = ChatItem(localName, if (isOfficial) "Official" else "Room", icon, if (isOfficial) Color(0xFFEBC98F) else Color(0xFFD6B37A), room.id)
            if (isOfficial) { if (!officialChats.any { it.title == localName }) officialChats.add(item) }
            else { if (!personalChats.any { it.title == localName }) personalChats.add(item) }
        }
        val sorted = officialChats.sortedBy { it.title }.toList()
        officialChats.clear(); officialChats.addAll(sorted)
    }

    private fun saveUserLocally() { userStorage?.saveUser(currentUserData, currentUserProfile, accessToken) }
    private fun clearUserLocally() { userStorage?.clear() }

    var cachedUsername by mutableStateOf("")
    var cachedDisplayName by mutableStateOf("")
    var cachedEmail by mutableStateOf("")
    var cachedPassword by mutableStateOf("")
    var cachedCountry by mutableStateOf("")

    private val gson = Gson()
    private val bluetoothService = BluetoothService(onConnected = { }, onReceived = { handleReceivedMessage(it) })
    private val lanService = LanService(onConnected = { }, onReceived = { handleReceivedMessage(it) })
    
    private val authService = OnlineService(onConnected = { }, onReceived = { }, onFailed = { })
    private val gameService = OnlineService(
        onConnected = { isConnectedToServer = true; connectionState = ConnectionState.ONLINE; onConnected() },
        onReceived = { handleReceivedMessage(it) },
        onFailed = { isConnectedToServer = false; connectionState = ConnectionState.OFFLINE }
    )
    private val chatService = OnlineService(onConnected = { }, onReceived = { message -> 
            if (message.startsWith("CHAT_MSG:")) {
                try {
                    val msg = gson.fromJson(message.substring(9), ChatMessage::class.java)
                    chatMessages.add(msg)
                } catch (e: Exception) {}
            }
        }, onFailed = { })

    private var authServerAddress: Pair<String, Int>? = null
    private var gameServerAddress: Pair<String, Int>? = null
    private var chatServerAddress: Pair<String, Int>? = null

    fun refreshChatRooms() {
        viewModelScope.launch {
            val api = chatApi ?: return@launch
            try {
                val response = api.listRooms()
                if (response.isSuccessful) {
                    val rooms = response.body() ?: emptyList()
                    userStorage?.saveChatRooms(rooms); updateChatLists(rooms)
                }
            } catch (e: Exception) {}
        }
    }

    fun showFloatingMessage(message: String) {
        viewModelScope.launch {
            floatingMessage = message; delay(3000)
            if (floatingMessage == message) floatingMessage = null
        }
    }

    suspend fun discoverServers(): Boolean {
        connectionState = ConnectionState.CONNECTING
        val storage = userStorage
        if (storage != null && !storage.isDiscoveryEnabled()) {
            val manualIp = storage.getManualIp(); val basePort = storage.getServerPort()
            authServerAddress = Pair(manualIp, basePort); chatServerAddress = Pair(manualIp, basePort + 1); gameServerAddress = Pair(manualIp, basePort + 2)
        } else {
            for (i in 100..105) {
                val ip = "192.168.8.$i"
                if (authService.queryServer(ip, 8000) == "PLAYAUTH") {
                    authServerAddress = Pair(ip, 8000); chatServerAddress = Pair(ip, 8001); gameServerAddress = Pair(ip, 8002)
                    break
                }
            }
        }
        val found = authServerAddress != null
        if (found) {
            createApis("http://${authServerAddress!!.first}:${authServerAddress!!.second}/")
            if (gameServerAddress != null) createGameApi("http://${gameServerAddress!!.first}:${gameServerAddress!!.second}/")
            if (chatServerAddress != null) createChatApi("http://${chatServerAddress!!.first}:${chatServerAddress!!.second}/")
        }
        return found
    }

    fun attemptServerConnection() {
        viewModelScope.launch {
            connectionState = ConnectionState.CONNECTING
            if (discoverServers()) {
                gameService.connect(gameServerAddress!!.first, gameServerAddress!!.second)
                chatService.connect(chatServerAddress!!.first, chatServerAddress!!.second)
            }
            else { connectionState = ConnectionState.OFFLINE; isConnectedToServer = false }
        }
    }

    fun toggleServerConnection(address: String, connect: Boolean) {
        isConnectionEnabled = connect; userStorage?.setConnectionEnabled(connect)
        if (connect) attemptServerConnection()
        else { gameService.stop(); chatService.stop(); isConnectedToServer = false; connectionState = ConnectionState.OFFLINE; if (connectionType == ConnectionType.ONLINE) disconnect() }
    }

    fun logout() {
        toggleServerConnection("", false)
        authState = AuthState.GUEST; currentUser = null; currentUserData = null; currentUserProfile = null; accessToken = null
        officialChats.clear(); personalChats.clear(); clearUserLocally()
    }

    fun disconnect() {
        gameService.stop(); chatService.stop(); isMultiplayer = false; isHost = false; connectionType = null
        activeSessionId = null
        resetLocalGame()
        engine.useAI = isLocalAiEnabled
        lastMessage = "Returned to Local Play."
    }

    fun getPairedDevices(): List<android.bluetooth.BluetoothDevice> {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        return try { adapter?.bondedDevices?.toList() ?: emptyList() } catch (e: SecurityException) { emptyList() }
    }

    private fun handleReceivedMessage(message: String) {
        try {
            if (message.startsWith("SYNC:")) {
                engine.importState(gson.fromJson(message.substring(5), GameState::class.java))
                lastMessage = if (engine.currentPlayerIndex == 0) "YOUR TURN" else "Waiting..."
            } else if (message == "RESET") { resetLocalGame(); lastMessage = "Game reset." }
        } catch (e: Exception) { }
    }

    fun onCardHandClicked(card: Card) {
        val canInteract = if (isMultiplayer) engine.currentPlayerIndex == 0 else true
        if (!canInteract || engine.gameOver || isMultiStagePlayActive) return
        selectedCardHand = if (selectedCardHand == card) null else card
    }
    fun onCardFloorClicked(card: Card) {
        val canInteract = if (isMultiplayer) engine.currentPlayerIndex == 0 else true
        if (!canInteract || engine.gameOver) return
        if (selectedCardsFloor.contains(card)) selectedCardsFloor.remove(card) else selectedCardsFloor.add(card)
    }
    fun onConstructionClicked(construction: Construction) {
        val canInteract = if (isMultiplayer) engine.currentPlayerIndex == 0 else true
        if (!canInteract || engine.gameOver) return
        if (selectedConstructions.contains(construction)) selectedConstructions.remove(construction) else selectedConstructions.add(construction)
    }
    fun onOpponentStackClicked(index: Int) {
        val canInteract = if (isMultiplayer) engine.currentPlayerIndex == 0 else true
        if (!canInteract || engine.gameOver) return
        val top = engine.privateStacks[index].lastOrNull() ?: return
        selectedOpponentStackCard = if (selectedOpponentStackCard == top) null else top
    }

    fun onPlayClicked() {
        if (engine.gameOver) return
        val card = selectedCardHand ?: return 
        val pIdx = if (isMultiplayer) 0 else engine.currentPlayerIndex
        engine.executePlay(card, pIdx)
        clearSelections()
        if (isMultiplayer && activeSessionId != null) { 
            sendGameAction("PLAY", mapOf("card" to card))
            sendGameAction("SYNC", mapOf("state" to engine.exportState())) 
        } else {
            finalizeTurn()
        }
    }

    fun onBuildClicked() {
        if (engine.gameOver) return
        val card = selectedCardHand ?: return 
        val pIdx = if (isMultiplayer) 0 else engine.currentPlayerIndex
        val success = engine.executeBuild(card, selectedCardsFloor.toList(), selectedConstructions.toList(), selectedOpponentStackCard, pIdx)
        clearSelections()
        if (success) {
            isMultiStagePlayActive = true
            if (isMultiplayer && activeSessionId != null) sendGameAction("BUILD", mapOf("card" to card))
            startMultiStageTimer()
        } else {
            if (isMultiplayer && activeSessionId != null) {
                sendGameAction("PLAY", mapOf("card" to card))
                sendGameAction("SYNC", mapOf("state" to engine.exportState()))
            } else {
                finalizeTurn()
            }
        }
    }

    fun onCaptureClicked() {
        if (engine.gameOver) return
        val card = selectedCardHand ?: return 
        val pIdx = if (isMultiplayer) 0 else engine.currentPlayerIndex
        val success = engine.executeCapture(card, selectedCardsFloor.toList(), selectedConstructions.toList(), selectedOpponentStackCard, pIdx)
        if (success) {
            clearSelections()
            isMultiStagePlayActive = true
            captureRetriesRemaining = 1
            if (isMultiplayer && activeSessionId != null) sendGameAction("CAPTURE", mapOf("card" to card))
            startMultiStageTimer()
        } else {
            if (captureRetriesRemaining > 0) {
                captureRetriesRemaining--
                showFloatingMessage("Invalid Capture! One chance to correct.")
            } else {
                engine.executePlay(card, pIdx)
                clearSelections()
                captureRetriesRemaining = 1
                if (isMultiplayer && activeSessionId != null) {
                    sendGameAction("PLAY", mapOf("card" to card))
                    sendGameAction("SYNC", mapOf("state" to engine.exportState()))
                } else {
                    finalizeTurn()
                }
            }
        }
    }

    private fun finalizeTurn() {
        if (engine.gameOver) {
            checkRoundEnd()
        } else if (isLocalAiEnabled && engine.currentPlayerIndex != 0) {
            triggerAiTurn()
        }
    }

    private fun triggerAiTurn() {
        viewModelScope.launch {
            val index = engine.currentPlayerIndex
            lastMessage = "AI $index Thinking..."
            delay(2000)
            val hand = engine.hands[index]
            if (hand.isNotEmpty()) {
                engine.playCard(hand.random(), index)
            }
            if (engine.gameOver) {
                checkRoundEnd()
            } else if (engine.currentPlayerIndex != 0) {
                finalizeTurn() // Recursive call for next AI
            } else {
                lastMessage = "YOUR TURN"
            }
        }
    }

    private fun checkRoundEnd() {
        if (!engine.gameOver) return
        
        val roundScores = engine.calculateScores()
        for (i in 0 until engine.playerCount) {
            val key = if (engine.playerCount == 4) "Team${i % 2}" else "Team$i"
            cumulativeScores[i] += roundScores[key] ?: 0
        }

        if (currentRound < maxRounds) {
            viewModelScope.launch {
                lastMessage = "ROUND $currentRound OVER"
                delay(3000)
                currentRound++
                val aiWasEnabled = isLocalAiEnabled
                val pCount = engine.playerCount
                engine = GameEngine(pCount)
                engine.useAI = aiWasEnabled
                lastMessage = "ROUND $currentRound START"
            }
        } else {
            lastMessage = "GAME OVER"
        }
    }

    private fun startMultiStageTimer() {
        turnTimerJob?.cancel()
        turnTimerJob = viewModelScope.launch {
            lastMessage = "YOUR TURN (5s)"
            delay(5000)
            endTurn()
        }
    }

    private fun endTurn() {
        turnTimerJob?.cancel()
        isMultiStagePlayActive = false
        engine.nextTurn()
        clearSelections()
        captureRetriesRemaining = 1
        if (isMultiplayer && activeSessionId != null) {
            sendGameAction("SYNC", mapOf("state" to engine.exportState()))
        } else {
            finalizeTurn()
        }
    }

    private fun sendGameAction(type: String, payload: Map<String, Any>) {
        viewModelScope.launch {
            val api = gameApi ?: return@launch
            val sessionId = activeSessionId ?: return@launch
            try { api.applyAction("Bearer $accessToken", sessionId, GameActionRequest(type, payload)) } catch (e: Exception) {}
        }
    }

    private fun sendData(data: String) {
        when (connectionType) {
            ConnectionType.BLUETOOTH -> bluetoothService.send(data)
            ConnectionType.LAN -> lanService.send(data)
            else -> {}
        }
    }

    fun addFriendByUsername(username: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val api = authApi ?: run { onComplete(false); return@launch }
            try {
                val searchResp = api.searchUsers(username)
                if (searchResp.isSuccessful) {
                    val users = searchResp.body() ?: emptyList()
                    val exactUser = users.find { it.username.equals(username, ignoreCase = true) }
                    if (exactUser != null) {
                        if (api.addFriend("Bearer $accessToken", FriendCreate(exactUser.id)).isSuccessful) { refreshFriends(); onComplete(true); return@launch }
                    }
                }
            } catch (e: Exception) {}
            onComplete(false)
        }
    }

    fun refreshFriends() {
        refreshFriendsJob?.cancel()
        refreshFriendsJob = viewModelScope.launch {
            val api = authApi ?: return@launch
            try {
                val response = api.getFriends("Bearer $accessToken")
                if (response.isSuccessful) {
                    val friends = response.body() ?: emptyList()
                    val processedList = mutableListOf<FriendRead>()
                    friends.forEach { friend ->
                        val otherId = if (friend.user_id == currentUserData?.id) friend.friend_id else friend.user_id
                        val userResp = api.getUser(otherId)
                        if (userResp.isSuccessful) friend.friendUsername = userResp.body()?.username
                        processedList.add(friend)
                    }
                    friendsList.clear()
                    friendsList.addAll(processedList.distinctBy { it.id })
                }
            } catch (e: Exception) {}
        }
    }

    fun acceptFriendRequest(requesterId: UUID) {
        viewModelScope.launch {
            val api = authApi ?: return@launch
            try { 
                val response = api.updateFriendStatus("Bearer $accessToken", requesterId, "ACCEPTED")
                if (response.isSuccessful) { 
                    refreshFriends()
                    showFloatingMessage("Accepted!") 
                } else {
                    showFloatingMessage("Failed to accept: ${response.code()}")
                }
            } catch (e: Exception) {
                showFloatingMessage("Error: ${e.localizedMessage}")
            }
        }
    }

    fun clearSelections() { selectedCardHand = null; selectedCardsFloor.clear(); selectedConstructions.clear(); selectedOpponentStackCard = null }

    fun resetGame() {
        resetLocalGame()
        if (isMultiplayer) { sendData("RESET"); if (isHost) sendData("SYNC:${gson.toJson(engine.exportState())}"); lastMessage = "Game Reset Sent." }
        else lastMessage = "YOUR TURN"
    }

    fun registerUser(username: String, displayName: String, email: String, pass: String, country: String) {
        viewModelScope.launch {
            authErrorMessage = null; authSuccessMessage = null; connectionState = ConnectionState.CONNECTING
            if (discoverServers()) {
                val api = authApi ?: return@launch
                try {
                    val response = api.register(UserCreate(username, email, pass, displayName))
                    if (response.isSuccessful) {
                        val tokens = response.body(); accessToken = tokens?.accessToken; val authHeader = "Bearer $accessToken"
                        api.updateProfile(authHeader, mapOf("country" to country))
                        currentUserData = api.getMe(authHeader).body(); currentUserProfile = api.getProfile(authHeader).body()
                        authState = AuthState.AUTHENTICATED; currentUser = username; authSuccessMessage = "Account created!"; saveUserLocally()
                        if (isConnectionEnabled) attemptServerConnection()
                        updatePresence("ONLINE", UUID.fromString("9ac54e7c-f2ee-49cb-ac01-7df6f8598846"))
                        refreshChatRooms(); startSessionPolling()
                    } else { authErrorMessage = "Failed: ${response.errorBody()?.string()}"; authState = AuthState.GUEST }
                } catch (e: Exception) { authErrorMessage = "Error: ${e.message}"; authState = AuthState.GUEST }
            } else { authErrorMessage = "Auth server not found"; authState = AuthState.GUEST }
        }
    }

    fun loginUser(username: String, pass: String) {
        viewModelScope.launch {
            authErrorMessage = null; authSuccessMessage = null; connectionState = ConnectionState.CONNECTING
            if (discoverServers()) {
                val api = authApi ?: return@launch
                try {
                    val response = api.login(UserLogin(username, pass))
                    if (response.isSuccessful) {
                        val tokens = response.body(); accessToken = tokens?.accessToken; val authHeader = "Bearer $accessToken"
                        currentUserData = api.getMe(authHeader).body(); currentUserProfile = api.getProfile(authHeader).body()
                        authState = AuthState.AUTHENTICATED; currentUser = username; authSuccessMessage = "Welcome back!"; saveUserLocally()
                        if (isConnectionEnabled) attemptServerConnection()
                        refreshChatRooms(); startSessionPolling()
                    } else { authErrorMessage = "Login failed"; authState = AuthState.GUEST }
                } catch (e: Exception) { authErrorMessage = "Error: ${e.message}"; authState = AuthState.GUEST }
            } else { authErrorMessage = "Auth server not found"; authState = AuthState.GUEST }
        }
    }

    fun refreshOnlinePlayers() {
        viewModelScope.launch {
            val api = chatApi ?: return@launch
            val auth = authApi ?: return@launch
            try {
                val response = api.listOnlinePlayers()
                if (response.isSuccessful) {
                    onlinePlayers.clear()
                    response.body()?.forEach { presence ->
                        if (presence.user_id != currentUserData?.id) {
                            val userResp = auth.getUser(presence.user_id)
                            val u = userResp.body()
                            val name = if (userResp.isSuccessful) u?.displayName ?: u?.username ?: presence.user_id.toString() else presence.user_id.toString()
                            onlinePlayers.add("$name|${presence.user_id}")
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun startSessionPolling() {
        sessionPollingJob?.cancel()
        if (userStorage?.isPollingEnabled() == false) return
        sessionPollingJob = viewModelScope.launch {
            val api = gameApi ?: return@launch
            while (true) {
                try {
                    if (!isMultiplayer && incomingInvitation == null) {
                        val response = api.listMySessions("Bearer $accessToken")
                        if (response.isSuccessful) {
                            val session = response.body()?.firstOrNull()
                            if (session != null) { incomingInvitation = session }
                        }
                    } else if (isMultiplayer && activeSessionId != null) { pollGameEvents(activeSessionId!!) }
                } catch (e: Exception) {}
                delay(3000)
            }
        }
    }

    fun acceptInvitation() {
        val session = incomingInvitation ?: return
        val pCount = session.players.size
        activeSessionId = session.id
        isMultiplayer = true
        connectionType = ConnectionType.ONLINE
        engine = GameEngine(pCount)
        engine.useAI = false
        engine.importState(gson.fromJson(gson.toJson(session.state), GameState::class.java))
        incomingInvitation = null
        lastMessage = "Online match joined!"
    }

    fun declineInvitation() {
        incomingInvitation = null
        // Optionally send a decline action to server
    }

    private suspend fun pollGameEvents(sessionId: UUID) {
        val api = gameApi ?: return
        try {
            val response = api.getEvents(sessionId)
            if (response.isSuccessful) {
                response.body()?.forEach { event -> if (event.player_id != currentUserData?.id) handleGameEvent(event) }
            }
        } catch (e: Exception) {}
    }

    private fun handleGameEvent(event: GameEventRead) {
        try {
            when (event.event_type) {
                "PLAY", "MOVE", "CAPTURE", "BUILD" -> {
                    val card = gson.fromJson(gson.toJson(event.payload["card"]), Card::class.java)
                    engine.playCard(card, 0)
                }
                "SYNC" -> engine.importState(gson.fromJson(gson.toJson(event.payload["state"]), GameState::class.java))
                "RESET" -> resetLocalGame()
            }
        } catch (e: Exception) {}
    }

    fun inviteToMatch(user: UserRead) {
        if (!matchLobbyPlayers.any { it.id == user.id }) { matchLobbyPlayers.add(user); showFloatingMessage("${user.displayName ?: user.username} added") }
    }

    fun removeFromLobby(userId: UUID) { matchLobbyPlayers.removeAll { it.id == userId } }

    fun startMultiplayerMatch() {
        if (matchLobbyPlayers.isEmpty()) { showFloatingMessage("Invite players first"); return }
        viewModelScope.launch {
            val api = gameApi ?: return@launch
            val myId = currentUserData?.id ?: return@launch
            val allPlayerIds = mutableListOf(myId)
            allPlayerIds.addAll(matchLobbyPlayers.map { it.id })
            try {
                showFloatingMessage("Starting ${allPlayerIds.size}P match...")
                val response = api.createSession("Bearer $accessToken", CreateSessionRequest(players = allPlayerIds))
                if (response.isSuccessful) {
                    val session = response.body()
                    if (session != null) {
                        activeSessionId = session.id; isMultiplayer = true; isHost = true; connectionType = ConnectionType.ONLINE
                        engine = GameEngine(allPlayerIds.size); engine.useAI = false
                        engine.importState(gson.fromJson(gson.toJson(session.state), GameState::class.java))
                        matchLobbyPlayers.clear(); lastMessage = "Match started!"; startSessionPolling()
                    }
                } else showFloatingMessage("Failed to start")
            } catch (e: Exception) { showFloatingMessage("Error: ${e.message}") }
        }
    }

    fun initiateOnlineMatch(opponentId: String) {
        if (opponentId.isBlank()) return
        showFloatingMessage("Challenging...")
        viewModelScope.launch {
            val api = gameApi ?: return@launch
            val myId = currentUserData?.id ?: return@launch
            try {
                val response = api.createSession("Bearer $accessToken", CreateSessionRequest(players = listOf(myId, UUID.fromString(opponentId))))
                if (response.isSuccessful) {
                    val session = response.body()
                    if (session != null) {
                        activeSessionId = session.id; isMultiplayer = true; isHost = true; connectionType = ConnectionType.ONLINE
                        engine.importState(gson.fromJson(gson.toJson(session.state), GameState::class.java)); startSessionPolling()
                    }
                }
            } catch (e: Exception) { }
        }
    }

    fun loadChatMessages(chat: ChatItem) {
        val rid = chat.roomId ?: return
        chatMessages.clear(); chatMessages.addAll(userStorage?.getMessages(rid) ?: emptyList())
        updatePresence("ONLINE", rid)
        viewModelScope.launch {
            val api = chatApi ?: return@launch
            try {
                val resp = api.getMessages(rid)
                if (resp.isSuccessful) {
                    val newMsgs = resp.body()?.map { ChatMessage(it.sender_id.toString(), it.content, it.created_at) } ?: emptyList()
                    if (newMsgs.isNotEmpty()) { chatMessages.clear(); chatMessages.addAll(newMsgs); userStorage?.saveMessages(rid, newMsgs) }
                }
            } catch (e: Exception) {}
        }
    }

    fun updatePresence(state: String, roomId: UUID? = null) {
        viewModelScope.launch {
            val api = chatApi ?: return@launch
            try { api.setPresence("Bearer $accessToken", PresenceUpdate(state, roomId)) } catch (e: Exception) {}
        }
    }

    fun sendChatMessage(content: String) {
        val chat = selectedChat ?: return
        val rid = chat.roomId ?: return
        viewModelScope.launch {
            val api = chatApi ?: return@launch
            try { if (api.sendMessage("Bearer $accessToken", rid, IncomingChatMessage(content)).isSuccessful) loadChatMessages(chat) } catch (e: Exception) {}
        }
    }

    fun refreshRoomMembers(roomId: UUID) {
        viewModelScope.launch {
            val api = chatApi ?: return@launch
            val auth = authApi ?: return@launch
            try {
                val response = api.getRoomMembers(roomId)
                if (response.isSuccessful) {
                    roomMembers.clear()
                    response.body()?.forEach { m ->
                        val userResp = auth.getUser(m.user_id)
                        if (userResp.isSuccessful) { val u = userResp.body(); m.username = u?.username; m.displayName = u?.displayName }
                        roomMembers.add(m)
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun showUserDetail(userId: UUID) {
        viewModelScope.launch {
            val api = authApi ?: return@launch
            try {
                refreshFriends()
                val response = api.getUser(userId)
                if (response.isSuccessful) { selectedUserForProfile = response.body(); isUserDetailVisible = true }
            } catch (e: Exception) {}
        }
    }

    fun startHosting(type: ConnectionType) {
        isMultiplayer = true; isHost = true; connectionType = type; engine.useAI = false
        if (type == ConnectionType.BLUETOOTH) bluetoothService.startHost()
        else if (type == ConnectionType.LAN) lanService.startHost()
        lastMessage = if (type == ConnectionType.LAN) "Hosting via WiFi at ${lanService.getLocalIpAddress()}" else "Hosting..."
    }

    fun connectToHost(address: String, type: ConnectionType) {
        if (type == ConnectionType.ONLINE) { initiateOnlineMatch(address) }
        else {
            isMultiplayer = true; isHost = false; connectionType = type; engine.useAI = false
            if (type == ConnectionType.BLUETOOTH) bluetoothService.connect(address)
            else if (type == ConnectionType.LAN) lanService.connect(address)
            lastMessage = "Connecting..."
        }
    }

    private fun resetLocalGame() {
        val currentUseAi = isLocalAiEnabled
        currentRound = 1
        for (i in cumulativeScores.indices) cumulativeScores[i] = 0
        engine = GameEngine(localPlayerCount)
        engine.useAI = currentUseAi; clearSelections(); isMultiStagePlayActive = false; turnTimerJob?.cancel()
    }

    private fun onConnected() {
        refreshOnlinePlayers(); refreshFriends()
        val globalRoomId = officialChats.find { it.title == "Global" }?.roomId
        updatePresence("ONLINE", globalRoomId)
    }

    fun addFriend(friendId: UUID) {
        viewModelScope.launch {
            val api = authApi ?: return@launch
            try { if (api.addFriend("Bearer $accessToken", FriendCreate(friendId)).isSuccessful) { refreshFriends(); showFloatingMessage("Friend request sent!") } } catch (e: Exception) {}
        }
    }

    fun searchUsers(query: String, onResult: (List<UserRead>) -> Unit) {
        viewModelScope.launch {
            val api = authApi ?: return@launch
            try {
                val response = api.searchUsers(query)
                if (response.isSuccessful) { onResult(response.body() ?: emptyList()) }
            } catch (e: Exception) {}
        }
    }
}
