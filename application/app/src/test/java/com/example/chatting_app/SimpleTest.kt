package com.example.chatting_app

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Simple test to verify basic functionality without MockK
 * This helps isolate the MockK compilation issues
 */
@RunWith(JUnit4::class)
class SimpleTest {

    @Test
    fun testChatMessageCreation() {
        // Test basic ChatMessage creation
        val message = ChatMessage(
            content = "Test message",
            senderType = SenderType.VOICE_USER
        )
        
        assertEquals("Test message", message.content)
        assertEquals(SenderType.VOICE_USER, message.senderType)
        assertTrue(message.id.isNotEmpty())
    }

    @Test
    fun testSttStatusCreation() {
        // Test SttStatus creation
        val idleStatus = SttStatus.Idle
        val errorStatus = SttStatus.Error("Test error")
        
        assertTrue(idleStatus is SttStatus.Idle)
        assertTrue(errorStatus is SttStatus.Error)
        assertEquals("Test error", errorStatus.message)
    }

    @Test
    fun testChatMessageEquality() {
        // Test ChatMessage equality
        val message1 = ChatMessage(
            content = "Same content",
            senderType = SenderType.VOICE_USER
        )
        val message2 = ChatMessage(
            content = "Same content", 
            senderType = SenderType.VOICE_USER
        )
        
        // Note: These won't be equal because they have different IDs and timestamps
        // This is expected behavior for the data class
        assertTrue(message1.content == message2.content)
        assertTrue(message1.senderType == message2.senderType)
    }
}
