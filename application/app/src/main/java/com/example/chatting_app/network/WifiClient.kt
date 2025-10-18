package com.example.chatting_app.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * WiFi 클라이언트 - GATT 호환 프로토콜 사용
 */
class WifiClient(private val context: Context) {
    
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isReceiving = false
    
    // 연결 상태
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // 수신된 메시지
    private val _receivedMessage = MutableStateFlow<WifiMessage?>(null)
    val receivedMessage: StateFlow<WifiMessage?> = _receivedMessage.asStateFlow()
    
    companion object {
        private const val TAG = "WifiClient"
        private const val BUFFER_SIZE = 1024
    }
    
    /**
     * WiFi 서버에 연결
     */
    fun connect(ipAddress: String, port: Int = 8888) {
        Log.d(TAG, "Connecting to $ipAddress:$port")
        _connectionState.value = ConnectionState.CONNECTING
        
        // 백그라운드 스레드에서 네트워크 작업 수행
        Thread {
            try {
                Log.d(TAG, "Creating socket...")
                socket = Socket(ipAddress, port)
                
                Log.d(TAG, "Getting streams...")
                outputStream = socket?.getOutputStream()
                inputStream = socket?.getInputStream()
                
                Log.d(TAG, "Socket connected: ${socket?.isConnected}")
                Log.d(TAG, "Socket bound: ${socket?.isBound}")
                Log.d(TAG, "Socket closed: ${socket?.isClosed}")
                
                _connectionState.value = ConnectionState.CONNECTED
                Log.d(TAG, "Connected successfully to $ipAddress:$port")
                
                // 수신 스레드 시작
                startReceivingThread()
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed to $ipAddress:$port: ${e.message}", e)
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                _connectionState.value = ConnectionState.ERROR
            }
        }.start()
    }
    
    /**
     * 연결 해제
     */
    fun disconnect() {
        // 백그라운드 스레드에서 연결 해제 작업 수행
        Thread {
            try {
                isReceiving = false
                socket?.close()
                outputStream?.close()
                inputStream?.close()
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.d(TAG, "Disconnected")
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error: ${e.message}")
            }
        }.start()
    }
    
    /**
     * 데이터 전송
     */
    fun send(data: ByteArray) {
        // 백그라운드 스레드에서 데이터 전송
        Thread {
            try {
                outputStream?.write(data)
                outputStream?.flush()
                Log.d(TAG, "Sent ${data.size} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "Send error: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
            }
        }.start()
    }
    
    /**
     * 수신 스레드 시작
     */
    private fun startReceivingThread() {
        isReceiving = true
        Thread {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isReceiving && socket?.isConnected == true) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: 0
                    if (bytesRead > 0) {
                        val packet = buffer.sliceArray(0 until bytesRead)
                        handleReceivedMessage(packet)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Receive error: ${e.message}")
                    break
                }
            }
            Log.d(TAG, "Receiving thread stopped")
        }.start()
    }
    
    /**
     * 수신된 메시지 처리
     */
    private fun handleReceivedMessage(packet: ByteArray) {
        val message = WifiMessageProtocol.deserializeMessage(packet)
        if (message != null) {
            Log.d(TAG, "Received message: ${message.messageType}")
            _receivedMessage.value = message
        } else {
            Log.w(TAG, "Failed to deserialize message")
        }
    }
    
    /**
     * 연결 상태 확인
     */
    fun isConnected(): Boolean {
        return socket?.isConnected == true && _connectionState.value == ConnectionState.CONNECTED
    }
    
    /**
     * 연결 상태 디버그 정보
     */
    fun getDebugInfo(): String {
        return "Socket: ${socket?.let { "connected=${it.isConnected}, bound=${it.isBound}, closed=${it.isClosed}" } ?: "null"}, " +
                "State: ${_connectionState.value}, " +
                "OutputStream: ${outputStream != null}, " +
                "InputStream: ${inputStream != null}, " +
                "Receiving: $isReceiving"
    }
}

/**
 * 연결 상태
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * WiFi 메시지
 */
data class WifiMessage(
    val messageType: MessageType?,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WifiMessage

        if (messageType != other.messageType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = messageType?.hashCode() ?: 0
        result = 31 * result + data.contentHashCode()
        return result
    }
}
