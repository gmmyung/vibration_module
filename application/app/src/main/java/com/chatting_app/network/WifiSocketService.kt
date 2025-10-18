package com.chatting_app.network

import android.content.Context
import android.util.Log
import com.chatting_app.data.WifiMessage
import com.chatting_app.data.WifiMessageProtocol
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import kotlin.random.Random

/**
 * WiFi Socket 통신 서비스
 * BLE와 동일한 데이터를 WiFi로 전송
 */
class WifiSocketService(private val context: Context) {
    
    companion object {
        private const val TAG = "WifiSocketService"
        private const val DEFAULT_PORT = 8888
        private const val CONNECTION_TIMEOUT = 5000L
        private const val HEARTBEAT_INTERVAL = 30000L // 30초
    }
    
    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var isConnected = false
    private var isServer = false
    private var serverSocket: ServerSocket? = null
    private var heartbeatJob: Job? = null
    
    // 콜백 함수들
    private var onMessageReceived: ((WifiMessage) -> Unit)? = null
    private var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 서버 모드로 시작 (다른 디바이스의 연결을 기다림)
     */
    suspend fun startServer(port: Int = DEFAULT_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "WiFi 서버 시작됨 - 포트: $port")
                
                // 연결 대기
                val clientSocket = serverSocket?.accept()
                if (clientSocket != null) {
                    setupConnection(clientSocket)
                    isServer = true
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "서버 시작 실패", e)
                onError?.invoke("서버 시작 실패: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 클라이언트 모드로 연결 (다른 디바이스에 연결)
     */
    suspend fun connectToServer(ipAddress: String, port: Int = DEFAULT_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val clientSocket = Socket()
                clientSocket.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT.toInt())
                setupConnection(clientSocket)
                isServer = false
                true
            } catch (e: Exception) {
                Log.e(TAG, "서버 연결 실패", e)
                onError?.invoke("서버 연결 실패: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 연결 설정
     */
    private fun setupConnection(socket: Socket) {
        this.socket = socket
        outputStream = DataOutputStream(socket.getOutputStream())
        inputStream = DataInputStream(socket.getInputStream())
        isConnected = true
        
        Log.d(TAG, "WiFi 연결 성공")
        onConnectionStateChanged?.invoke(true)
        
        // 수신 스레드 시작
        startReceiving()
        
        // 하트비트 시작
        startHeartbeat()
    }
    
    /**
     * 메시지 전송 (헤더 포함)
     */
    fun sendMessage(message: WifiMessage) {
        if (!isConnected) {
            onError?.invoke("연결되지 않음")
            return
        }
        
        serviceScope.launch {
            try {
                val data = WifiMessageProtocol.toByteArray(message)
                outputStream?.write(data) // 헤더가 포함된 전체 데이터 전송
                outputStream?.flush()
                
                // 메시지 전송 성공 (로그 제거로 성능 최적화)
            } catch (e: Exception) {
                Log.e(TAG, "메시지 전송 실패", e)
                onError?.invoke("메시지 전송 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 메시지 수신 시작 (헤더 포함)
     */
    private fun startReceiving() {
        serviceScope.launch {
            try {
                while (isConnected && !socket?.isClosed!!) {
                    // 헤더 읽기 (4바이트)
                    val headerBytes = ByteArray(4)
                    val headerRead = inputStream?.read(headerBytes) ?: break
                    if (headerRead != 4) {
                        Log.w(TAG, "헤더 읽기 불완전: $headerRead 바이트")
                        break
                    }
                    
                    // 헤더 파싱 (Little Endian)
                    val messageType = headerBytes[0]
                    val dataLength = ((headerBytes[2].toInt() and 0xFF) shl 8) or (headerBytes[1].toInt() and 0xFF)
                    val checksum = headerBytes[3]
                    
                    // 디버깅: 헤더 바이트 확인
                    android.util.Log.d("WifiSocketService", "헤더 바이트: [${headerBytes[0]}, ${headerBytes[1]}, ${headerBytes[2]}, ${headerBytes[3]}]")
                    android.util.Log.d("WifiSocketService", "파싱된 데이터 길이: $dataLength")
                    
                    // 데이터 읽기
                    val data = ByteArray(dataLength)
                    val dataRead = inputStream?.read(data) ?: break
                    if (dataRead != dataLength) {
                        Log.w(TAG, "데이터 읽기 불완전: $dataRead/$dataLength 바이트")
                        break
                    }
                    
                    // 전체 패킷 구성 (헤더 + 데이터)
                    val fullPacket = headerBytes + data
                    
                    val message = WifiMessageProtocol.fromByteArray(fullPacket)
                    message?.let {
                        Log.d(TAG, "메시지 수신됨: ${it.text} (타입: ${it.messageType}, 크기: ${fullPacket.size}바이트)")
                        onMessageReceived?.invoke(it)
                    } ?: run {
                        Log.w(TAG, "메시지 파싱 실패 (타입: $messageType, 크기: $dataLength)")
                    }
                }
            } catch (e: Exception) {
                if (isConnected) {
                    Log.e(TAG, "메시지 수신 실패", e)
                    onError?.invoke("메시지 수신 실패: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 하트비트 시작
     */
    private fun startHeartbeat() {
        heartbeatJob = serviceScope.launch {
            while (isConnected) {
                delay(HEARTBEAT_INTERVAL)
                if (isConnected) {
                    val heartbeat = WifiMessage(
                        deviceId = getDeviceId(),
                        text = "heartbeat",
                        braille = "",
                        messageType = com.chatting_app.data.MessageType.HEARTBEAT
                    )
                    sendMessage(heartbeat)
                }
            }
        }
    }
    
    /**
     * 연결 해제
     */
    fun disconnect() {
        isConnected = false
        heartbeatJob?.cancel()
        
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "연결 해제 중 오류", e)
        }
        
        socket = null
        outputStream = null
        inputStream = null
        serverSocket = null
        
        Log.d(TAG, "WiFi 연결 해제됨")
        onConnectionStateChanged?.invoke(false)
    }
    
    /**
     * 디바이스 ID 생성
     */
    private fun getDeviceId(): String {
        return "device_${Random.nextInt(1000, 9999)}"
    }
    
    /**
     * 로컬 IP 주소 가져오기
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "IP 주소 가져오기 실패", e)
        }
        return null
    }
    
    /**
     * 연결 상태 확인
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * 서버 모드인지 확인
     */
    fun isServerMode(): Boolean = isServer
    
    // 콜백 설정 메서드들
    fun setOnMessageReceived(callback: (WifiMessage) -> Unit) {
        onMessageReceived = callback
    }
    
    fun setOnConnectionStateChanged(callback: (Boolean) -> Unit) {
        onConnectionStateChanged = callback
    }
    
    fun setOnError(callback: (String) -> Unit) {
        onError = callback
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        disconnect()
        serviceScope.cancel()
    }
}
