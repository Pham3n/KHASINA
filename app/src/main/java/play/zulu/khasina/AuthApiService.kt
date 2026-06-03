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
}
