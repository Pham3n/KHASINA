package play.zulu.khasina

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/register")
    suspend fun register(@Body payload: UserCreate): Response<TokenPair>

    @POST("auth/login")
    suspend fun login(@Body payload: UserLogin): Response<TokenPair>

    @GET("users/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserRead>

    @GET("profiles/me")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileRead>

    @retrofit2.http.PATCH("profiles/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body payload: Map<String, String>
    ): Response<ProfileRead>
    @GET("users/search")
    suspend fun searchUsers(@retrofit2.http.Query("query") query: String): Response<List<UserRead>>

    @GET("users/{user_id}")
    suspend fun getUser(@retrofit2.http.Path("user_id") userId: java.util.UUID): Response<UserRead>

    @GET("friends")
    suspend fun getFriends(@Header("Authorization") token: String): Response<List<FriendRead>>

    @POST("friends")
    suspend fun addFriend(
        @Header("Authorization") token: String,
        @Body payload: FriendCreate
    ): Response<FriendRead>
}
