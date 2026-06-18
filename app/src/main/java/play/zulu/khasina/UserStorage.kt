package play.zulu.khasina

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class UserStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("playzulu_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUser(user: UserRead?, profile: ProfileRead?, token: String?) {
        prefs.edit().apply {
            putString("access_token", token)
            putString("user_data", if (user != null) gson.toJson(user) else null)
            putString("profile_data", if (profile != null) gson.toJson(profile) else null)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun getUserData(): UserRead? {
        val json = prefs.getString("user_data", null) ?: return null
        return try { gson.fromJson(json, UserRead::class.java) } catch (e: Exception) { null }
    }

    fun getProfileData(): ProfileRead? {
        val json = prefs.getString("profile_data", null) ?: return null
        return try { gson.fromJson(json, ProfileRead::class.java) } catch (e: Exception) { null }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // Settings
    fun isDiscoveryEnabled(): Boolean = prefs.getBoolean("discovery_enabled", true)
    fun setDiscoveryEnabled(enabled: Boolean) = prefs.edit().putBoolean("discovery_enabled", enabled).apply()

    fun isPollingEnabled(): Boolean = prefs.getBoolean("polling_enabled", true)
    fun setPollingEnabled(enabled: Boolean) = prefs.edit().putBoolean("polling_enabled", enabled).apply()

    fun getManualIp(): String = prefs.getString("manual_ip", "192.168.8.102") ?: "192.168.8.102"
    fun setManualIp(ip: String) = prefs.edit().putString("manual_ip", ip).apply()

    fun getServerPort(): Int = prefs.getInt("server_port", 8000)
    fun setServerPort(port: Int) = prefs.edit().putInt("server_port", port).apply()

    fun isConnectionEnabled(): Boolean = prefs.getBoolean("connection_enabled", true)
    fun setConnectionEnabled(enabled: Boolean) = prefs.edit().putBoolean("connection_enabled", enabled).apply()

    // Chat Persistence
    fun saveChatRooms(rooms: List<ChatRoomRead>) {
        prefs.edit().putString("cached_rooms", gson.toJson(rooms)).apply()
    }

    fun getChatRooms(): List<ChatRoomRead> {
        val json = prefs.getString("cached_rooms", null) ?: return emptyList()
        val type = object : com.google.gson.reflect.TypeToken<List<ChatRoomRead>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun saveMessages(roomId: java.util.UUID, messages: List<ChatMessage>) {
        prefs.edit().putString("msgs_$roomId", gson.toJson(messages)).apply()
    }

    fun getMessages(roomId: java.util.UUID): List<ChatMessage> {
        val json = prefs.getString("msgs_$roomId", null) ?: return emptyList()
        val type = object : com.google.gson.reflect.TypeToken<List<ChatMessage>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }
}
