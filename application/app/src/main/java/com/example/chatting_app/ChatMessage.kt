package com.example.chatting_app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a chat message in the conversation
 * @param id Unique identifier for the message
 * @param content Text content of the message
 * @param senderType Type of sender (Voice User or Text User)
 * @param timestamp When the message was sent
 * @param brailleContent Braille representation of the message content
 */
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val senderType: SenderType,
    val timestamp: Date = Date(),
    val brailleContent: String? = null
) {
    /**
     * Formatted timestamp for display
     */
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)
    
    /**
     * Check if braille content is available
     */
    val hasBraille: Boolean
        get() = !brailleContent.isNullOrBlank()
}

/**
 * Enum representing the type of message sender
 */
enum class SenderType {
    VISUAL_IMPAIRED_USER,    // 왼쪽 사용자 - 시청각장애인 (텍스트/점자 입력 → 텍스트/음성 출력)
    SIGHTED_USER             // 오른쪽 사용자 - 음성 발화자 (텍스트/음성 입력 → 텍스트/점자 출력)
}
