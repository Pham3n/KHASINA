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
}
