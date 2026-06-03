package play.zulu.khasina

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnlineService(
    private val onConnected: () -> Unit,
    private val onReceived: (String) -> Unit
) {
    private var clientThread: ClientThread? = null
    private var connectedThread: ConnectedThread? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS) // Increased from 300ms
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun connect(ipAddress: String, port: Int) {
        stop()
        clientThread = ClientThread(ipAddress, port)
        clientThread?.start()
    }

    suspend fun queryServer(ip: String, port: Int): String? {
        return withContext(Dispatchers.IO) {
            val url = "http://$ip:$port/identity"
            val request = Request.Builder().url(url).build()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            return@withContext json.optString("service", null)
                        }
                    }
                }
            } catch (e: Exception) {
                // Not an HTTP server or unreachable
            }
            null
        }
    }

    fun stop() {
        clientThread?.cancel()
        clientThread = null
        connectedThread?.cancel()
        connectedThread = null
    }

    fun send(data: String) {
        connectedThread?.write(data.toByteArray())
    }

    private inner class ClientThread(private val ipAddress: String, private val port: Int) : Thread() {
        private var socket: Socket? = null

        override fun run() {
            try {
                socket = Socket()
                socket?.connect(InetSocketAddress(ipAddress, port), 2000)
                socket?.let { manageConnectedSocket(it) }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {}
        }
    }

    private fun manageConnectedSocket(socket: Socket) {
        onConnected()
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
    }

    private inner class ConnectedThread(private val socket: Socket) : Thread() {
        private val inputStream = socket.getInputStream()
        private val outputStream = socket.getOutputStream()

        override fun run() {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        onReceived(message)
                    } else if (bytes == -1) {
                        break
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            Thread {
                try {
                    outputStream.write(bytes)
                    outputStream.flush()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }.start()
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {}
        }
    }
}
