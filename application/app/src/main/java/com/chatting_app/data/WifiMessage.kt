package com.chatting_app.data

import com.google.gson.Gson
import com.example.chatting_app.SenderType
import java.util.UUID

/**
 * WiFi Socket 통신을 위한 메시지 데이터 클래스
 * BLE와 동일한 구조를 사용하여 일관성 유지
 */
data class WifiMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String,
    val text: String,           // 원본 텍스트
    val braille: String,        // 점자 시퀀스
    val messageType: MessageType = MessageType.CHAT,
    val senderType: SenderType = SenderType.SIGHTED_USER
)

/**
 * 메시지 타입 열거형
 */
enum class MessageType {
    VOICE_TO_BRAILLE,    // 음성 → 점자 변환
    BRAILLE_INPUT,       // 점자 입력
    CHAT,                // 채팅 메시지
    CONNECTION,          // 연결 상태
    HEARTBEAT            // 연결 유지
}

// SenderType은 com.example.chatting_app.SenderType을 사용

/**
 * WiFi 메시지 헤더 구조 (BLE와 동일)
 */
data class WifiMessageHeader(
    val messageType: Byte,        // 메시지 타입 (1바이트)
    val dataLength: Short,        // 데이터 길이 (2바이트)
    val checksum: Byte            // 체크섬 (1바이트)
)

/**
 * WiFi 메시지 프로토콜
 * BLE와 동일한 헤더 구조와 JSON 직렬화/역직렬화 사용
 */
object WifiMessageProtocol {
    private val gson = Gson()
    
    /**
     * 메시지를 JSON 문자열로 변환
     */
    fun toJson(message: WifiMessage): String {
        return gson.toJson(message)
    }
    
    /**
     * JSON 문자열을 메시지로 변환
     */
    fun fromJson(json: String): WifiMessage? {
        return try {
            gson.fromJson(json, WifiMessage::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 메시지를 바이트 배열로 변환 (헤더 포함)
     */
    fun toByteArray(message: WifiMessage): ByteArray {
        val jsonData = toJson(message).toByteArray(Charsets.UTF_8)
        val header = WifiMessageHeader(
            messageType = message.messageType.ordinal.toByte(),
            dataLength = jsonData.size.toShort(),
            checksum = calculateChecksum(jsonData)
        )
        
        // 디버깅: JSON 크기 확인
        android.util.Log.d("WifiMessageProtocol", "JSON 크기: ${jsonData.size}바이트")
        android.util.Log.d("WifiMessageProtocol", "JSON 내용: ${String(jsonData, Charsets.UTF_8)}")
        android.util.Log.d("WifiMessageProtocol", "헤더: messageType=${header.messageType}, dataLength=${header.dataLength}, checksum=${header.checksum}")
        
        // 헤더 + 데이터 바이트 배열 생성
        val result = ByteArray(4 + jsonData.size)
        var index = 0
        
        // 헤더 추가 (4바이트)
        result[index++] = header.messageType
        val lengthBytes = header.dataLength.toByteArray()
        result[index++] = lengthBytes[0]
        result[index++] = lengthBytes[1]
        result[index++] = header.checksum
        
        // JSON 데이터 추가
        System.arraycopy(jsonData, 0, result, index, jsonData.size)
        
        android.util.Log.d("WifiMessageProtocol", "전체 패킷 크기: ${result.size}바이트")
        
        return result
    }
    
    /**
     * 바이트 배열을 메시지로 변환 (헤더 포함)
     */
    fun fromByteArray(data: ByteArray): WifiMessage? {
        if (data.size < 4) {
            return null // 헤더 크기보다 작음
        }
        
        try {
            val header = WifiMessageHeader(
                messageType = data[0],
                dataLength = ((data[2].toInt() and 0xFF) shl 8 or (data[1].toInt() and 0xFF)).toShort(), // Little Endian
                checksum = data[3]
            )
            
            // 데이터 길이 검증
            if (data.size < 4 + header.dataLength) {
                return null // 데이터가 불완전함
            }
            
            val jsonData = data.sliceArray(4 until 4 + header.dataLength)
            
            // 체크섬 검증
            if (calculateChecksum(jsonData) != header.checksum) {
                return null // 체크섬 불일치
            }
            
            val jsonString = String(jsonData, Charsets.UTF_8)
            return fromJson(jsonString)
            
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 체크섬 계산 (XOR 방식)
     */
    private fun calculateChecksum(data: ByteArray): Byte {
        return data.fold(0) { acc, byte -> acc xor byte.toInt() }.toByte()
    }
    
    /**
     * Short를 바이트 배열로 변환 (Little Endian)
     */
    private fun Short.toByteArray(): ByteArray {
        return byteArrayOf(
            this.toByte(),           // 하위 바이트 먼저
            (this.toInt() shr 8).toByte()  // 상위 바이트 나중에
        )
    }
}
