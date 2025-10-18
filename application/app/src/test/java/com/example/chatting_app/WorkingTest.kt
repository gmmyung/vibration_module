package com.example.chatting_app

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Working unit tests that compile successfully
 * This demonstrates proper unit testing without compilation errors
 */
@RunWith(JUnit4::class)
class WorkingTest {

    /**
     * Test ChatMessage data class functionality
     */
    @Test
    fun testChatMessageCreation() {
        // Test basic ChatMessage creation
        val message = ChatMessage(
            content = "Hello World",
            senderType = SenderType.VOICE_USER
        )
        
        // Verify properties
        assertEquals("Hello World", message.content, "Content should match")
        assertEquals(SenderType.VOICE_USER, message.senderType, "Sender type should match")
        assertTrue(message.id.isNotEmpty(), "ID should be generated")
        assertTrue(message.formattedTime.matches(Regex("\\d{2}:\\d{2}")), "Time should be formatted correctly")
        assertFalse(message.hasBraille, "Should not have braille by default")
    }
    
    @Test
    fun testChatMessageWithBraille() {
        // Test ChatMessage with braille content
        val message = ChatMessage(
            content = "Hello World",
            senderType = SenderType.VOICE_USER,
            brailleContent = "⠓⠑⠇⠇⠕ ⠺⠕⠗⠇⠙"
        )
        
        // Verify properties
        assertEquals("Hello World", message.content, "Content should match")
        assertEquals("⠓⠑⠇⠇⠕ ⠺⠕⠗⠇⠙", message.brailleContent, "Braille content should match")
        assertTrue(message.hasBraille, "Should have braille content")
    }

    /**
     * Test SttStatus sealed class functionality
     */
    @Test
    fun testSttStatusBehavior() {
        // Test object instances
        val idle = SttStatus.Idle
        val starting = SttStatus.Starting
        val ready = SttStatus.Ready
        
        // Test data class instance
        val error = SttStatus.Error("Test error message")
        
        // Verify types
        assertTrue(idle is SttStatus.Idle, "Should be Idle type")
        assertTrue(starting is SttStatus.Starting, "Should be Starting type")
        assertTrue(ready is SttStatus.Ready, "Should be Ready type")
        assertTrue(error is SttStatus.Error, "Should be Error type")
        
        // Verify error message
        assertEquals("Test error message", error.message, "Error message should match")
    }

    /**
     * Test SenderType enum functionality
     */
    @Test
    fun testSenderTypeEnum() {
        // Test enum values
        assertEquals("VOICE_USER", SenderType.VOICE_USER.name, "VOICE_USER name should match")
        assertEquals("TEXT_USER", SenderType.TEXT_USER.name, "TEXT_USER name should match")
        
        // Test enum values array
        val values = SenderType.values()
        assertEquals(2, values.size, "Should have 2 enum values")
        assertTrue(values.contains(SenderType.VOICE_USER), "Should contain VOICE_USER")
        assertTrue(values.contains(SenderType.TEXT_USER), "Should contain TEXT_USER")
    }

    /**
     * Test ChatMessage equality behavior
     */
    @Test
    fun testChatMessageEquality() {
        // Create messages with same content but different timestamps
        val message1 = ChatMessage(
            content = "Same content",
            senderType = SenderType.VOICE_USER
        )
        // Add small delay to ensure different timestamps
        Thread.sleep(1)
        val message2 = ChatMessage(
            content = "Same content",
            senderType = SenderType.VOICE_USER
        )
        
        // Messages should have same content and sender type
        assertEquals(message1.content, message2.content, "Content should be equal")
        assertEquals(message1.senderType, message2.senderType, "Sender type should be equal")
        
        // But different IDs and timestamps (so not equal as objects)
        assertTrue(message1.id != message2.id, "IDs should be different")
        assertTrue(message1.timestamp != message2.timestamp, "Timestamps should be different")
    }

    /**
     * Test edge cases for ChatMessage
     */
    @Test
    fun testChatMessageEdgeCases() {
        // Test empty content
        val emptyMessage = ChatMessage(
            content = "",
            senderType = SenderType.TEXT_USER
        )
        assertEquals("", emptyMessage.content, "Empty content should be preserved")
        
        // Test long content
        val longContent = "A".repeat(1000)
        val longMessage = ChatMessage(
            content = longContent,
            senderType = SenderType.VOICE_USER
        )
        assertEquals(longContent, longMessage.content, "Long content should be preserved")
        assertEquals(1000, longMessage.content.length, "Content length should be correct")
        
        // Test special characters
        val specialContent = "Hello! @#$%^&*()_+{}|:<>?[]\\;'\",./"
        val specialMessage = ChatMessage(
            content = specialContent,
            senderType = SenderType.TEXT_USER
        )
        assertEquals(specialContent, specialMessage.content, "Special characters should be preserved")
    }

    /**
     * Test SttStatus pattern matching
     */
    @Test
    fun testSttStatusPatternMatching() {
        val statuses = listOf(
            SttStatus.Idle,
            SttStatus.Starting,
            SttStatus.Ready,
            SttStatus.Listening,
            SttStatus.Processing,
            SttStatus.Success,
            SttStatus.Error("Test error")
        )
        
        // Test when expression with all status types
        statuses.forEach { status ->
            val result = when (status) {
                is SttStatus.Idle -> "idle"
                is SttStatus.Starting -> "starting"
                is SttStatus.Ready -> "ready"
                is SttStatus.Listening -> "listening"
                is SttStatus.Processing -> "processing"
                is SttStatus.Success -> "success"
                is SttStatus.Error -> "error: ${status.message}"
            }
            
            assertTrue(result.isNotEmpty(), "When expression should return non-empty result for $status")
        }
    }
    
    /**
     * Test BrailleResult data class
     */
    @Test
    fun testBrailleResult() {
        val result = BrailleResult(
            originalText = "Hello",
            brailleText = "⠓⠑⠇⠇⠕",
            isSuccess = true
        )
        
        assertEquals("Hello", result.originalText, "Original text should match")
        assertEquals("⠓⠑⠇⠇⠕", result.brailleText, "Braille text should match")
        assertTrue(result.isSuccess, "Should be successful")
        assertTrue(result.errorMessage == null, "Error message should be null for success")
    }
    
    /**
     * Test BrailleResult with error
     */
    @Test
    fun testBrailleResultWithError() {
        val result = BrailleResult(
            originalText = "Test",
            brailleText = "",
            isSuccess = false,
            errorMessage = "Conversion failed"
        )
        
        assertEquals("Test", result.originalText, "Original text should match")
        assertEquals("", result.brailleText, "Braille text should be empty")
        assertFalse(result.isSuccess, "Should not be successful")
        assertEquals("Conversion failed", result.errorMessage, "Error message should match")
    }
}
