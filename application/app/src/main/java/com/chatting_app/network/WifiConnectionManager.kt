package com.chatting_app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import com.chatting_app.data.WifiMessage
import com.chatting_app.data.MessageType
import com.example.chatting_app.SenderType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WiFi 연결 관리자
 * BLE와 동일한 인터페이스를 제공하여 일관성 유지
 */
class WifiConnectionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "WifiConnectionManager"
        private const val DEFAULT_PORT = 8888
    }
    
    private val wifiSocketService = WifiSocketService(context)
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // 상태 관리
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isServerMode = MutableStateFlow(false)
    val isServerMode: StateFlow<Boolean> = _isServerMode.asStateFlow()
    
    private val _localIpAddress = MutableStateFlow<String?>(null)
    val localIpAddress: StateFlow<String?> = _localIpAddress.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow("연결 안됨")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    // 콜백 함수들
    private var onMessageReceived: ((WifiMessage) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        setupCallbacks()
        updateLocalIpAddress()
    }
    
    /**
     * 콜백 설정
     */
    private fun setupCallbacks() {
        wifiSocketService.setOnMessageReceived { message ->
            onMessageReceived?.invoke(message)
        }
        
        wifiSocketService.setOnConnectionStateChanged { connected ->
            _isConnected.value = connected
            _connectionStatus.value = if (connected) "연결됨" else "연결 안됨"
        }
        
        wifiSocketService.setOnError { error ->
            _connectionStatus.value = "오류: $error"
            onError?.invoke(error)
        }
    }
    
    /**
     * 서버 모드로 시작
     */
    suspend fun startServer(port: Int = DEFAULT_PORT): Boolean {
        if (!isWifiConnected()) {
            onError?.invoke("WiFi가 연결되지 않음")
            return false
        }
        
        _connectionStatus.value = "서버 시작 중..."
        _isServerMode.value = true
        
        val success = wifiSocketService.startServer(port)
        if (success) {
            _connectionStatus.value = "서버 실행 중 - 포트: $port"
            updateLocalIpAddress()
        } else {
            _connectionStatus.value = "서버 시작 실패"
            _isServerMode.value = false
        }
        
        return success
    }
    
    /**
     * 클라이언트 모드로 연결
     */
    suspend fun connectToServer(ipAddress: String, port: Int = DEFAULT_PORT): Boolean {
        if (!isWifiConnected()) {
            onError?.invoke("WiFi가 연결되지 않음")
            return false
        }
        
        _connectionStatus.value = "연결 중..."
        _isServerMode.value = false
        
        val success = wifiSocketService.connectToServer(ipAddress, port)
        if (success) {
            _connectionStatus.value = "연결됨 - $ipAddress:$port"
        } else {
            _connectionStatus.value = "연결 실패"
        }
        
        return success
    }
    
    /**
     * 연결 해제
     */
    fun disconnect() {
        wifiSocketService.disconnect()
        _isConnected.value = false
        _isServerMode.value = false
        _connectionStatus.value = "연결 안됨"
    }
    
    /**
     * 메시지 전송
     */
    fun sendMessage(message: WifiMessage) {
        if (!_isConnected.value) {
            onError?.invoke("연결되지 않음")
            return
        }
        
        wifiSocketService.sendMessage(message)
    }
    
    /**
     * 텍스트 메시지 전송 (편의 메서드)
     */
    fun sendTextMessage(text: String, braille: String = "", senderType: com.example.chatting_app.SenderType = com.example.chatting_app.SenderType.SIGHTED_USER) {
        val message = WifiMessage(
            deviceId = getDeviceId(),
            text = text,
            braille = braille,
            messageType = MessageType.CHAT,
            senderType = senderType
        )
        sendMessage(message)
    }
    
    /**
     * 점자 시퀀스 메시지 전송 (편의 메서드)
     */
    fun sendBrailleSequenceMessage(text: String, braille: String) {
        val message = WifiMessage(
            deviceId = getDeviceId(),
            text = text,
            braille = braille,
            messageType = MessageType.VOICE_TO_BRAILLE,
            senderType = com.example.chatting_app.SenderType.SIGHTED_USER
        )
        sendMessage(message)
    }
    
    /**
     * 점자 입력 메시지 전송 (편의 메서드)
     */
    fun sendBrailleInputMessage(text: String, braille: String) {
        val message = WifiMessage(
            deviceId = getDeviceId(),
            text = text,
            braille = braille,
            messageType = MessageType.BRAILLE_INPUT,
            senderType = com.example.chatting_app.SenderType.VISUAL_IMPAIRED_USER
        )
        sendMessage(message)
    }
    
    /**
     * WiFi 연결 상태 확인
     */
    private fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * 로컬 IP 주소 업데이트
     */
    private fun updateLocalIpAddress() {
        val ipAddress = wifiSocketService.getLocalIpAddress()
        _localIpAddress.value = ipAddress
        Log.d(TAG, "로컬 IP 주소: $ipAddress")
    }
    
    /**
     * 디바이스 ID 생성
     */
    private fun getDeviceId(): String {
        return "wifi_device_${System.currentTimeMillis() % 10000}"
    }
    
    /**
     * 연결 상태 확인
     */
    fun isConnected(): Boolean = _isConnected.value
    
    /**
     * 서버 모드인지 확인
     */
    fun isServerMode(): Boolean = _isServerMode.value
    
    /**
     * 현재 연결 상태 문자열 반환
     */
    fun getConnectionStatus(): String = _connectionStatus.value
    
    /**
     * 로컬 IP 주소 반환
     */
    fun getLocalIpAddress(): String? = _localIpAddress.value
    
    // 콜백 설정 메서드들
    fun setOnMessageReceived(callback: (WifiMessage) -> Unit) {
        onMessageReceived = callback
    }
    
    fun setOnError(callback: (String) -> Unit) {
        onError = callback
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        disconnect()
        wifiSocketService.cleanup()
        managerScope.cancel()
    }
    
    /**
     * 네트워크 상태 모니터링 시작
     */
    fun startNetworkMonitoring() {
        managerScope.launch {
            while (isActive) {
                delay(5000) // 5초마다 체크
                updateLocalIpAddress()
                
                if (!isWifiConnected() && _isConnected.value) {
                    Log.w(TAG, "WiFi 연결 끊어짐")
                    disconnect()
                }
            }
        }
    }
}
