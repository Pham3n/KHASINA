package play.zulu.khasina

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.NetworkInterface

class LanService(
    private val onConnected: () -> Unit,
    private val onReceived: (String) -> Unit
) {
    private val port = 8888
    private var serverThread: ServerThread? = null
    private var clientThread: ClientThread? = null
    private var connectedThread: ConnectedThread? = null

    fun startHost() {
        stop()
        serverThread = ServerThread()
        serverThread?.start()
    }

    fun connect(ipAddress: String) {
        stop()
        clientThread = ClientThread(ipAddress)
        clientThread?.start()
    }

    fun stop() {
        serverThread?.cancel()
        serverThread = null
        clientThread?.cancel()
        clientThread = null
        connectedThread?.cancel()
        connectedThread = null
    }

    fun send(data: String) {
        connectedThread?.write(data.toByteArray())
    }

    fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is java.net.Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    private inner class ServerThread : Thread() {
        private var serverSocket: ServerSocket? = null

        override fun run() {
            try {
                serverSocket = ServerSocket(port)
                val socket = serverSocket?.accept()
                socket?.also {
                    manageConnectedSocket(it)
                    serverSocket?.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {}
        }
    }

    private inner class ClientThread(private val ipAddress: String) : Thread() {
        private var socket: Socket? = null

        override fun run() {
            try {
                socket = Socket(ipAddress, port)
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
