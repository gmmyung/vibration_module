# 🔤 간단한 점자 점 통신 프로토콜

## 📋 개요

가장 기본적인 점자 점 정보만을 전송하는 최소한의 통신 프로토콜입니다. 복잡한 JSON 구조 없이 점자 점의 상태만 효율적으로 전송합니다.

## 🎯 설계 목표

1. **최대 단순화**: 점자 점 정보만 전송
2. **최소 데이터**: 1바이트로 6개 점 상태 표현
3. **실시간 전송**: 점 터치 즉시 전송
4. **범용성**: BLE와 WiFi 모두 동일한 구조 사용

---

## 🔢 점자 점 표현 방식

### 1. 6개 점 위치
```
점자 점 배치:
1 4
2 5  
3 6
```

### 2. 비트마스크 표현
```kotlin
// 1바이트로 6개 점 상태 표현
// 비트 위치: 76543210
// 점 위치:   --654321

// 예시:
// 1점만:     00000001 (0x01) = 1
// 1,4,5점:   00011001 (0x19) = 25
// 1,2,3점:   00000111 (0x07) = 7
// 1,4,5,6점: 00110001 (0x31) = 49
```

### 3. 점자 점 변환 유틸리티
```kotlin
object BrailleDotConverter {
    /**
     * 6개 점 상태를 1바이트로 압축
     * @param dots 점 상태 배열 [1점, 2점, 3점, 4점, 5점, 6점]
     * @return 압축된 바이트
     */
    fun dotsToByte(dots: List<Boolean>): Byte {
        require(dots.size == 6) { "점자는 정확히 6개 점이어야 합니다" }
        
        var result: Byte = 0
        dots.forEachIndexed { index, isActive ->
            if (isActive) {
                result = (result or (1 shl index).toByte()).toByte()
            }
        }
        return result
    }
    
    /**
     * 1바이트를 6개 점 상태로 복원
     * @param compressed 압축된 바이트
     * @return 점 상태 배열 [1점, 2점, 3점, 4점, 5점, 6점]
     */
    fun byteToDots(compressed: Byte): List<Boolean> {
        return (0..5).map { index ->
            (compressed.toInt() and (1 shl index)) != 0
        }
    }
    
    /**
     * 점 상태를 문자열로 표현 (디버깅용)
     * @param dots 점 상태 배열
     * @return "100110" 형태의 문자열
     */
    fun dotsToString(dots: List<Boolean>): String {
        return dots.map { if (it) "1" else "0" }.joinToString("")
    }
    
    /**
     * 압축된 바이트를 문자열로 표현 (디버깅용)
     * @param compressed 압축된 바이트
     * @return "100110" 형태의 문자열
     */
    fun byteToString(compressed: Byte): String {
        return dotsToString(byteToDots(compressed))
    }
}
```

---

## 📦 최소 통신 프로토콜

### 1. 메시지 구조 (3바이트)

```kotlin
data class SimpleBrailleMessage(
    val messageType: Byte,    // 메시지 타입 (1바이트)
    val dots: Byte,           // 점자 점 상태 (1바이트)
    val checksum: Byte        // 체크섬 (1바이트)
) {
    companion object {
        const val MESSAGE_TYPE_DOT_UPDATE = 0x01.toByte()
        const val MESSAGE_TYPE_CLEAR = 0x02.toByte()
        const val MESSAGE_TYPE_COMPLETE = 0x03.toByte()
    }
}
```

### 2. 메시지 타입 정의

```kotlin
enum class SimpleMessageType(val value: Byte) {
    DOT_UPDATE(0x01),     // 점 상태 업데이트
    CLEAR(0x02),          // 점 상태 초기화
    COMPLETE(0x03)        // 점자 입력 완료
}
```

### 3. 체크섬 계산

```kotlin
object SimpleChecksum {
    fun calculate(messageType: Byte, dots: Byte): Byte {
        return (messageType.toInt() xor dots.toInt()).toByte()
    }
    
    fun verify(message: SimpleBrailleMessage): Boolean {
        val expectedChecksum = calculate(message.messageType, message.dots)
        return expectedChecksum == message.checksum
    }
}
```

---

## 🔄 통신 플로우

### 1. 점 터치 시 전송
```kotlin
// 사용자가 1번 점 터치
val dots = listOf(true, false, false, false, false, false)  // 100000
val message = SimpleBrailleMessage(
    messageType = SimpleMessageType.DOT_UPDATE.value,
    dots = BrailleDotConverter.dotsToByte(dots),
    checksum = SimpleChecksum.calculate(SimpleMessageType.DOT_UPDATE.value, BrailleDotConverter.dotsToByte(dots))
)
// 전송: [0x01, 0x01, 0x00]
```

### 2. 추가 점 터치 시 전송
```kotlin
// 사용자가 4번, 5번 점 추가 터치
val dots = listOf(true, false, false, true, true, false)  // 100110
val message = SimpleBrailleMessage(
    messageType = SimpleMessageType.DOT_UPDATE.value,
    dots = BrailleDotConverter.dotsToByte(dots),
    checksum = SimpleChecksum.calculate(SimpleMessageType.DOT_UPDATE.value, BrailleDotConverter.dotsToByte(dots))
)
// 전송: [0x01, 0x19, 0x18]
```

### 3. 점자 입력 완료 시 전송
```kotlin
val message = SimpleBrailleMessage(
    messageType = SimpleMessageType.COMPLETE.value,
    dots = 0x00,  // 완료 시점에서는 점 상태 무관
    checksum = SimpleChecksum.calculate(SimpleMessageType.COMPLETE.value, 0x00)
)
// 전송: [0x03, 0x00, 0x03]
```

### 4. 점 상태 초기화 시 전송
```kotlin
val message = SimpleBrailleMessage(
    messageType = SimpleMessageType.CLEAR.value,
    dots = 0x00,
    checksum = SimpleChecksum.calculate(SimpleMessageType.CLEAR.value, 0x00)
)
// 전송: [0x02, 0x00, 0x02]
```

---

## 🛠️ 구현 예시

### 1. 메시지 직렬화/역직렬화

```kotlin
object SimpleBrailleProtocol {
    fun serialize(message: SimpleBrailleMessage): ByteArray {
        return byteArrayOf(
            message.messageType,
            message.dots,
            message.checksum
        )
    }
    
    fun deserialize(data: ByteArray): SimpleBrailleMessage? {
        if (data.size != 3) return null
        
        val message = SimpleBrailleMessage(
            messageType = data[0],
            dots = data[1],
            checksum = data[2]
        )
        
        return if (SimpleChecksum.verify(message)) message else null
    }
}
```

### 2. BLE 전송

```kotlin
class SimpleBleTransmitter {
    fun sendDotUpdate(dots: List<Boolean>) {
        val message = SimpleBrailleMessage(
            messageType = SimpleMessageType.DOT_UPDATE.value,
            dots = BrailleDotConverter.dotsToByte(dots),
            checksum = 0x00 // 계산됨
        )
        message.checksum = SimpleChecksum.calculate(message.messageType, message.dots)
        
        val data = SimpleBrailleProtocol.serialize(message)
        // BLE 전송 로직
        bleService.writeCharacteristic(data)
    }
    
    fun sendComplete() {
        val message = SimpleBrailleMessage(
            messageType = SimpleMessageType.COMPLETE.value,
            dots = 0x00,
            checksum = SimpleChecksum.calculate(SimpleMessageType.COMPLETE.value, 0x00)
        )
        
        val data = SimpleBrailleProtocol.serialize(message)
        bleService.writeCharacteristic(data)
    }
}
```

### 3. WiFi 전송

```kotlin
class SimpleWifiTransmitter {
    fun sendDotUpdate(dots: List<Boolean>) {
        val message = SimpleBrailleMessage(
            messageType = SimpleMessageType.DOT_UPDATE.value,
            dots = BrailleDotConverter.dotsToByte(dots),
            checksum = 0x00 // 계산됨
        )
        message.checksum = SimpleChecksum.calculate(message.messageType, message.dots)
        
        val data = SimpleBrailleProtocol.serialize(message)
        // WiFi 전송 로직
        wifiSocket.write(data)
    }
}
```

### 4. 수신 처리

```kotlin
class SimpleBrailleReceiver {
    fun onDataReceived(data: ByteArray) {
        val message = SimpleBrailleProtocol.deserialize(data) ?: return
        
        when (message.messageType) {
            SimpleMessageType.DOT_UPDATE.value -> {
                val dots = BrailleDotConverter.byteToDots(message.dots)
                updateBrailleDisplay(dots)
                Log.d("Braille", "점 상태 업데이트: ${BrailleDotConverter.dotsToString(dots)}")
            }
            SimpleMessageType.CLEAR.value -> {
                clearBrailleDisplay()
                Log.d("Braille", "점 상태 초기화")
            }
            SimpleMessageType.COMPLETE.value -> {
                completeBrailleInput()
                Log.d("Braille", "점자 입력 완료")
            }
        }
    }
}
```

---

## 📊 데이터 크기 비교

| 방식 | 크기 | 설명 |
|------|------|------|
| **기존 JSON** | ~100바이트 | `{"messageId":"...", "timestamp":..., "dots":[true,false,...]}` |
| **간단한 프로토콜** | **3바이트** | `[messageType, dots, checksum]` |
| **절약률** | **97%** | 100바이트 → 3바이트 |

---

## 🧪 테스트 예시

```kotlin
class SimpleBrailleProtocolTest {
    @Test
    fun testDotConversion() {
        // 1점만 (100000)
        val dots1 = listOf(true, false, false, false, false, false)
        val byte1 = BrailleDotConverter.dotsToByte(dots1)
        assertEquals(0x01, byte1.toInt())
        assertEquals("100000", BrailleDotConverter.dotsToString(dots1))
        
        // 1,4,5점 (100110)
        val dots2 = listOf(true, false, false, true, true, false)
        val byte2 = BrailleDotConverter.dotsToByte(dots2)
        assertEquals(0x19, byte2.toInt())
        assertEquals("100110", BrailleDotConverter.dotsToString(dots2))
        
        // 복원 테스트
        val restoredDots = BrailleDotConverter.byteToDots(byte2)
        assertEquals(dots2, restoredDots)
    }
    
    @Test
    fun testMessageSerialization() {
        val dots = listOf(true, false, false, true, true, false)
        val message = SimpleBrailleMessage(
            messageType = SimpleMessageType.DOT_UPDATE.value,
            dots = BrailleDotConverter.dotsToByte(dots),
            checksum = 0x00
        )
        message.checksum = SimpleChecksum.calculate(message.messageType, message.dots)
        
        val data = SimpleBrailleProtocol.serialize(message)
        assertEquals(3, data.size)
        
        val deserialized = SimpleBrailleProtocol.deserialize(data)
        assertNotNull(deserialized)
        assertEquals(message.messageType, deserialized!!.messageType)
        assertEquals(message.dots, deserialized.dots)
        assertEquals(message.checksum, deserialized.checksum)
    }
}
```

---

## 📋 구현 체크리스트

### 기본 구조
- [ ] `BrailleDotConverter` 구현
- [ ] `SimpleBrailleMessage` 데이터 클래스
- [ ] `SimpleChecksum` 유틸리티
- [ ] `SimpleBrailleProtocol` 직렬화/역직렬화

### 통신 구현
- [ ] BLE 전송기 구현
- [ ] WiFi 전송기 구현
- [ ] 수신 처리기 구현
- [ ] 에러 처리 및 재전송

### 테스트
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] 성능 테스트 작성

---

## 🎯 다음 단계

1. **간단한 점자 점 통신 구현** (현재 단계)
2. **점자 패턴 → 문자 변환 추가**
3. **텍스트 메시지 통신 추가**
4. **고급 기능 추가** (압축, 청크 분할 등)

---

*이 프로토콜은 가장 기본적인 점자 점 통신을 위한 최소한의 구조입니다. 복잡한 기능은 이 기본 구조가 안정화된 후 단계적으로 추가할 예정입니다.*
