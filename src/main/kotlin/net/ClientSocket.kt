package net

import net.handlers.PacketHandler
import java.io.*
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

class ClientSocket {
    companion object {// This class is responsible for managing the client socket connection
        private lateinit var socket: Socket
        private var keyPair: KeyPair? = null
        private var inputStream: InputStream? = null
        private var outputStream: OutputStream? = null
        private var dataOutputStream: DataOutputStream? = null
        private var dataInputStream: DataInputStream? = null
        private lateinit var serverPublicKey: PublicKey
        private var cipher: Cipher? = null
        public var username: String = "test"
        public var password: String = "test"
        private var CHUNK_SIZE = 245

        var onlineUsers = mutableListOf<String>()

        // This function initializes the socket connection and starts the key exchange and authentication process and returns a boolean
        fun init(username: String, password: String) : Boolean {
            println("ClientSocket initializing...")
            // Host and port should be passed as arguments
            val host = "localhost"
            val port = 2005
            this.username = username
            this.password = password
            try {
                socket = Socket(host, port)
                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()
                dataOutputStream = DataOutputStream(outputStream)
                dataInputStream = inputStream?.let { DataInputStream(it) }
                keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
                cipher = Cipher.getInstance("RSA")
            } catch (e: Exception) {
                println("Error initializing socket: ${e.message}")
                return false
            }
            println("ClientSocket initialized with host: $host and port: $port")
            keyExchange()
            val b = auth()
            Thread{ startReceivingPacket() }.start()
            return b
        }

        private fun keyExchange() {
            try {
                // Send client's public key to server
                val publicKey = keyPair?.public?.encoded
                if (publicKey != null) {
                    println("Size of public key: ${publicKey.size}")
                    dataOutputStream?.writeInt(publicKey.size) // Use writeInt instead of write
                    dataOutputStream?.write(publicKey)
                    dataOutputStream?.flush()
                }

                // Receive server's public key
                val length = dataInputStream?.readInt()
                if (length != null && length > 0) {
                    val packet = ByteArray(length)
                    dataInputStream?.readFully(packet)
                    serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(packet))
                    println("Server public key received")
                } else {
                    throw Exception("Invalid public key length received: $length")
                }
            } catch (e: Exception) {
                println("Error during key exchange: ${e.message}")
            }
        }

        private fun auth() : Boolean {
            try {
                // Initialize cipher instance first
                cipher = Cipher.getInstance("RSA")
                cipher?.init(Cipher.ENCRYPT_MODE, serverPublicKey)

                val encryptedUsername = cipher?.doFinal(username.toByteArray())
                val encryptedPassword = cipher?.doFinal(password.toByteArray())

                encryptedUsername?.let {
                    dataOutputStream?.writeInt(it.size)
                    dataOutputStream?.write(it)
                }

                encryptedPassword?.let {
                    dataOutputStream?.writeInt(it.size)
                    dataOutputStream?.write(it)
                }

                dataOutputStream?.flush()

                val response = dataInputStream?.readInt()
                if (response == 1) {
                    println("Authentication successful")
                    return true
                } else {
                    println("Authentication failed")
                    return false
                }
            } catch (e: Exception) {
                println("Error during authentication: ${e.message}")
                e.printStackTrace()
            }
            return false
        }

        fun sendPacket(bytes: ByteArray) {
            try {
                if (socket.isClosed || !socket.isConnected) {
                    throw IOException("Socket is closed or not connected")
                }

                val totalChunks = Math.ceil(bytes.size.toDouble() / CHUNK_SIZE).toInt()
                sendHeader(bytes.size, totalChunks)

                for (i in 0 until totalChunks) {
                    val start = i * CHUNK_SIZE
                    val end = minOf(bytes.size, start + CHUNK_SIZE)
                    val chunk = ByteArray(end - start)
                    System.arraycopy(bytes, start, chunk, 0, end - start)
                    sendChunk(chunk)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun sendHeader(totalSize: Int, totalChunks: Int) {
            try {
                cipher?.init(Cipher.ENCRYPT_MODE, serverPublicKey)
                val header = "SIZE:$totalSize;CHUNKS:$totalChunks".toByteArray()
                val encryptedHeader = cipher?.doFinal(header)

                encryptedHeader?.let {
                    dataOutputStream?.writeInt(it.size)
                    dataOutputStream?.write(it)
                    dataOutputStream?.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun sendChunk(chunk: ByteArray) {
            try {
                cipher?.init(Cipher.ENCRYPT_MODE, serverPublicKey)
                val encryptedChunk = cipher?.doFinal(chunk)

                encryptedChunk?.let {
                    dataOutputStream?.writeInt(it.size)
                    dataOutputStream?.write(it)
                    dataOutputStream?.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun startReceivingPacket() {
            try {
                println("Listening for packets")
                while (true) {
                    val headerLength = dataInputStream?.readInt() ?: break
                    val headerBytes = ByteArray(headerLength)
                    dataInputStream?.readFully(headerBytes)

                    cipher?.init(Cipher.DECRYPT_MODE, keyPair?.private)
                    val decryptedHeader = cipher?.doFinal(headerBytes)

                    decryptedHeader?.let {
                        val header = String(it)
                        header.split(";")[0].split(":")[1].toInt()
                        val totalChunks = header.split(";")[1].split(":")[1].toInt()

                        val completePacket = ByteArrayOutputStream()
                        for (i in 0 until totalChunks) {
                            val chunkLength = dataInputStream?.readInt() ?: break
                            val chunk = ByteArray(chunkLength)
                            dataInputStream?.readFully(chunk)

                            cipher?.init(Cipher.DECRYPT_MODE, keyPair?.private)
                            val decryptedChunk = cipher?.doFinal(chunk)

                            decryptedChunk?.let { chunk ->
                                completePacket.write(chunk)
                            }
                        }

                        val decryptedBytes = completePacket.toByteArray()
                        println("Received packet (size: ${decryptedBytes.size} bytes)")
                        // Handle the decrypted bytes with the virtual thread
                        Thread {
                            PacketHandler.handlePacket(decryptedBytes)
                        }.start()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun disconnect() {
            try {
                socket.close()
                inputStream?.close()
                outputStream?.close()
                dataInputStream?.close()
                dataOutputStream?.close()
                println("Disconnected from server")
            } catch (e: Exception) {
                println("Error disconnecting: ${e.message}")
            }
        }
    }
}