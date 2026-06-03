package play.zulu.khasina

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class UserCreate(
    val username: String,
    val email: String,
    val password: String,
    val avatar: String? = null
)

data class UserLogin(
    @SerializedName("username_or_email")
    val usernameOrEmail: String,
    val password: String
)

data class TokenPair(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String = "bearer"
)

data class UserRead(
    val id: UUID,
    val username: String,
    val email: String,
    val avatar: String?,
    val rating: Int,
    val roles: List<String>,
    @SerializedName("created_at")
    val createdAt: String
)

data class ProfileRead(
    val id: UUID,
    @SerializedName("user_id")
    val userId: UUID,
    @SerializedName("display_name")
    val displayName: String?,
    val bio: String?,
    val country: String?
)
