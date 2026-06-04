package play.zulu.khasina

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.UUID

interface GameApiService {
    @GET("sessions")
    suspend fun listMySessions(
        @Header("Authorization") token: String
    ): Response<List<GameSessionRead>>

    @POST("sessions")
    suspend fun createSession(
        @Header("Authorization") token: String,
        @Body request: CreateSessionRequest
    ): Response<GameSessionRead>

    @GET("sessions/{session_id}")
    suspend fun getSession(
        @Path("session_id") sessionId: UUID
    ): Response<GameSessionRead>

    @POST("sessions/{session_id}/actions")
    suspend fun applyAction(
        @Header("Authorization") token: String,
        @Path("session_id") sessionId: UUID,
        @Body request: GameActionRequest
    ): Response<GameSessionRead>

    @GET("sessions/{session_id}/events")
    suspend fun getEvents(
        @Path("session_id") sessionId: UUID
    ): Response<List<GameEventRead>>
}

data class CreateSessionRequest(
    val game_type: String = "KHASINA",
    val players: List<UUID>
)

data class GameActionRequest(
    val event_type: String,
    val payload: Map<String, Any>
)

data class GameSessionRead(
    val id: UUID,
    val game_type: String,
    val players: List<String>,
    val current_player_id: UUID,
    val status: String,
    val state: Map<String, Any>,
    val version: Int
)

data class GameEventRead(
    val event_id: UUID,
    val session_id: UUID,
    val player_id: UUID,
    val event_type: String,
    val payload: Map<String, Any>,
    val timestamp: String
)
