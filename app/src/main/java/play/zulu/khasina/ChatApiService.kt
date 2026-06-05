package play.zulu.khasina

import retrofit2.Response
import retrofit2.http.*
import java.util.UUID

interface ChatApiService {
    @GET("rooms")
    suspend fun listRooms(): Response<List<ChatRoomRead>>

    @POST("rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: RoomCreate
    ): Response<ChatRoomRead>

    @GET("rooms/{room_id}/messages")
    suspend fun getMessages(
        @Path("room_id") roomId: UUID,
        @Query("limit") limit: Int = 50
    ): Response<List<ChatMessageRead>>

    @POST("rooms/{room_id}/send")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Path("room_id") roomId: UUID,
        @Body request: IncomingChatMessage
    ): Response<ChatMessageRead>

    @PUT("presence/me")
    suspend fun setPresence(
        @Header("Authorization") token: String,
        @Body request: PresenceUpdate
    ): Response<PresenceRead>

    @GET("presence/online-players") // I'll assume this is added to server or use list + status
    suspend fun listOnlinePlayers(): Response<List<PresenceRead>>
}

data class ChatRoomRead(
    val id: UUID,
    val name: String,
    val room_type: String,
    val metadata: Map<String, Any>
)

data class RoomCreate(
    val name: String,
    val room_type: String,
    val metadata: Map<String, Any> = emptyMap()
)

data class ChatMessageRead(
    val id: UUID,
    val room_id: UUID,
    val sender_id: UUID,
    val content: String,
    val created_at: String
)

data class IncomingChatMessage(
    val content: String
)

data class PresenceUpdate(
    val state: String,
    val room_id: UUID? = null
)

data class PresenceRead(
    val user_id: UUID,
    val state: String,
    val room_id: UUID?,
    val last_seen_at: String,
    val updated_at: String,
    var username: String? = null // For UI
)
