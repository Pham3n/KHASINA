package play.zulu.khasina

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.*

class BluetoothService(
    private val onConnected: () -> Unit,
    private val onReceived: (String) -> Unit
) {
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val name = "KHASINA"
    private var adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverThread: ServerThread? = null
    private var clientThread: ClientThread? = null
    private var connectedThread: ConnectedThread? = null

    fun startHost() {
        stop()
        serverThread = ServerThread()
        serverThread?.start()
    }

    fun connect(address: String) {
        stop()
        val device = adapter?.getRemoteDevice(address)
        if (device != null) {
            clientThread = ClientThread(device)
            clientThread?.start()
        }
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

    private inner class ServerThread : Thread() {
        private var serverSocket: BluetoothServerSocket? = null

        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                serverSocket = adapter?.listenUsingInsecureRfcommWithServiceRecord(name, uuid)
                var shouldLoop = true
                while (shouldLoop) {
                    val socket: BluetoothSocket? = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        shouldLoop = false
                        null
                    }
                    socket?.also {
                        manageConnectedSocket(it)
                        serverSocket?.close()
                        shouldLoop = false
                    }
                }
            } catch (e: SecurityException) {
                // Log or handle permission error
            } catch (e: Exception) {
                // Handle other errors
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {}
        }
    }

    private inner class ClientThread(private val device: android.bluetooth.BluetoothDevice) : Thread() {
        private var socket: BluetoothSocket? = null

        @SuppressLint("MissingPermission")
        override fun run() {
            adapter?.cancelDiscovery()
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                socket?.connect()
                socket?.let { manageConnectedSocket(it) }
            } catch (e: SecurityException) {
                // Handle permission error
            } catch (e: IOException) {
                socket?.close()
            } catch (e: Exception) {
                socket?.close()
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {}
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        onConnected()
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream = socket.inputStream
        private val outputStream = socket.outputStream

        override fun run() {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        onReceived(message)
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {}
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {}
        }
    }
}
