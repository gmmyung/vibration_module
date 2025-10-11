# 📶 WiFi 프로토타입 프로토콜 (GATT 호환)

## 📋 개요

BLE GATT 구조를 기반으로 한 WiFi 프로토타입 프로토콜입니다. GATT와 동일한 메시지 구조를 사용하여 빠른 프로토타입 개발을 지원합니다.

---

## 🎯 설계 원칙

1. **GATT 호환성**: BLE GATT와 동일한 메시지 구조 사용
2. **빠른 프로토타입**: WiFi의 높은 대역폭 활용
3. **간편한 디버깅**: TCP 소켓으로 쉬운 테스트
4. **확장성**: GATT로 쉽게 마이그레이션 가능

---

## 🔧 GATT 호환 메시지 구조

### 1. 기본 메시지 헤더 (4바이트)

```kotlin
// GATT와 동일한 헤더 구조
data class MessageHeader(
    val messageType: Byte,    // 메시지 타입 (1바이트)
    val dataLength: Short,    // 데이터 길이 (2바이트) - Little Endian
    val checksum: Byte        // 체크섬 (1바이트)
)
```

### 2. 메시지 타입 정의 (GATT와 동일)

```kotlin
enum class MessageType(val value: Byte) {
    // 송신 메시지 (앱 → ESP32)
    DOT_TOUCH(0x01),          // 점 터치
    DOT_RELEASE(0x02),        // 점 릴리즈
    PATTERN_COMPLETE(0x03),   // 패턴 완성
    CLEAR(0x04),              // 초기화
    TEXT_MESSAGE(0x10),       // 텍스트 메시지
    SETTINGS(0x20),           // 설정
    
    // 수신 메시지 (ESP32 → 앱)
    STATUS_UPDATE(0x81),      // 상태 업데이트 (0x80 + 0x01)
    MESSAGE_RECEIVED(0x82),   // 메시지 수신 확인 (0x80 + 0x02)
    SETTINGS_UPDATED(0x83)    // 설정 업데이트 확인 (0x80 + 0x03)
}
```

---

## 📦 WiFi 전송 프로토콜

### 1. 전체 패킷 구조

```
[4바이트: 헤더][N바이트: 데이터]
```

### 2. 메시지 직렬화

```kotlin
object WifiMessageProtocol {
    fun serializeMessage(messageType: MessageType, data: ByteArray): ByteArray {
        val header = MessageHeader(
            messageType = messageType.value,
            dataLength = data.size.toShort(),
            checksum = calculateChecksum(data)
        )
        
        return buildByteArray {
            // 헤더 추가 (Little Endian)
            add(header.messageType)
            addAll(header.dataLength.toByteArray())  // Little Endian
            add(header.checksum)
            // 데이터 추가
            addAll(data)
        }
    }
    
    fun deserializeMessage(packet: ByteArray): WifiMessage? {
        if (packet.size < 4) return null
        
        val header = MessageHeader(
            messageType = packet[0],
            dataLength = ((packet[2].toInt() and 0xFF) shl 8) or (packet[1].toInt() and 0xFF),
            checksum = packet[3]
        )
        
        if (packet.size < 4 + header.dataLength) return null
        
        val data = packet.sliceArray(4 until 4 + header.dataLength)
        
        // 체크섬 검증
        if (calculateChecksum(data) != header.checksum) return null
        
        return WifiMessage(
            messageType = MessageType.values().find { it.value == header.messageType },
            data = data
        )
    }
    
    private fun calculateChecksum(data: ByteArray): Byte {
        return data.fold(0) { acc, byte -> acc xor byte.toInt() }.toByte()
    }
    
    private fun Short.toByteArray(): ByteArray {
        return byteArrayOf(
            (this.toInt() and 0xFF).toByte(),        // LSB
            ((this.toInt() shr 8) and 0xFF).toByte()  // MSB
        )
    }
}

data class WifiMessage(
    val messageType: MessageType?,
    val data: ByteArray
)
```

---

## 🔄 GATT 호환 메시지 구현

### 1. 점자 점 전송

```kotlin
class WifiBrailleDotTransmitter(private val wifiClient: WifiClient) {
    
    // 점 터치 전송 (GATT와 동일)
    fun sendDotTouch(dotIndex: Int) {
        val data = byteArrayOf(dotIndex.toByte())
        val packet = WifiMessageProtocol.serializeMessage(MessageType.DOT_TOUCH, data)
        wifiClient.send(packet)
    }
    
    // 점 릴리즈 전송 (GATT와 동일)
    fun sendDotRelease(dotIndex: Int) {
        val data = byteArrayOf(dotIndex.toByte())
        val packet = WifiMessageProtocol.serializeMessage(MessageType.DOT_RELEASE, data)
        wifiClient.send(packet)
    }
    
    // 패턴 완성 전송 (GATT와 동일)
    fun sendPatternComplete(dots: List<Boolean>) {
        val dotsByte = packBrailleDots(dots)
        val data = byteArrayOf(dotsByte)
        val packet = WifiMessageProtocol.serializeMessage(MessageType.PATTERN_COMPLETE, data)
        wifiClient.send(packet)
    }
    
    // 초기화 전송 (GATT와 동일)
    fun sendClear() {
        val data = byteArrayOf(0x00)
        val packet = WifiMessageProtocol.serializeMessage(MessageType.CLEAR, data)
        wifiClient.send(packet)
    }
    
    private fun packBrailleDots(dots: List<Boolean>): Byte {
        var result: Byte = 0
        dots.forEachIndexed { index, isActive ->
            if (isActive) {
                result = (result or (1 shl index).toByte()).toByte()
            }
        }
        return result
    }
}
```

### 2. 텍스트 메시지 전송

```kotlin
class WifiTextMessageTransmitter(private val wifiClient: WifiClient) {
    
    // 텍스트 메시지 전송 (GATT와 동일)
    fun sendTextMessage(text: String, braille: String) {
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val brailleBytes = braille.toByteArray(Charsets.UTF_8)
        
        val data = ByteArray(1 + textBytes.size + 1 + brailleBytes.size)
        var offset = 0
        
        data[offset++] = textBytes.size.toByte()
        System.arraycopy(textBytes, 0, data, offset, textBytes.size)
        offset += textBytes.size
        data[offset++] = brailleBytes.size.toByte()
        System.arraycopy(brailleBytes, 0, data, offset, brailleBytes.size)
        
        val packet = WifiMessageProtocol.serializeMessage(MessageType.TEXT_MESSAGE, data)
        wifiClient.send(packet)
    }
}
```

### 3. 설정 전송

```kotlin
class WifiSettingsTransmitter(private val wifiClient: WifiClient) {
    
    // 설정 전송 (GATT와 동일)
    fun sendSettings(settings: BrailleSettings) {
        val data = ByteArray(3)
        data[0] = settings.speed.value
        data[1] = settings.mode.value
        data[2] = settings.flags
        
        val packet = WifiMessageProtocol.serializeMessage(MessageType.SETTINGS, data)
        wifiClient.send(packet)
    }
}

data class BrailleSettings(
    val speed: BrailleSpeed,
    val mode: BrailleMode,
    val flags: Byte = 0x00
)

enum class BrailleSpeed(val value: Byte) {
    SLOW(0), NORMAL(1), FAST(2)
}

enum class BrailleMode(val value: Byte) {
    AUTO(0), MANUAL(1), REPEAT(2)
}
```

---

## 📱 Android 앱 구현

### 1. WiFi 클라이언트

```kotlin
class WifiClient(private val context: Context) {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    fun connect(ipAddress: String, port: Int) {
        try {
            socket = Socket(ipAddress, port)
            outputStream = socket?.getOutputStream()
            inputStream = socket?.getInputStream()
            _connectionState.value = ConnectionState.CONNECTED
            
            // 수신 스레드 시작
            startReceivingThread()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
        }
    }
    
    fun disconnect() {
        try {
            socket?.close()
            outputStream?.close()
            inputStream?.close()
            _connectionState.value = ConnectionState.DISCONNECTED
        } catch (e: Exception) {
            // 에러 처리
        }
    }
    
    fun send(data: ByteArray) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: Exception) {
            // 에러 처리
        }
    }
    
    private fun startReceivingThread() {
        Thread {
            val buffer = ByteArray(1024)
            while (socket?.isConnected == true) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: 0
                    if (bytesRead > 0) {
                        val packet = buffer.sliceArray(0 until bytesRead)
                        handleReceivedMessage(packet)
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }.start()
    }
    
    private fun handleReceivedMessage(packet: ByteArray) {
        val message = WifiMessageProtocol.deserializeMessage(packet) ?: return
        
        when (message.messageType) {
            MessageType.STATUS_UPDATE -> handleStatusUpdate(message.data)
            MessageType.MESSAGE_RECEIVED -> handleMessageReceived()
            MessageType.SETTINGS_UPDATED -> handleSettingsUpdated()
            else -> { /* 알 수 없는 메시지 타입 */ }
        }
    }
}
```

### 2. 통합 메시지 매니저

```kotlin
class UnifiedMessageManager(private val context: Context) {
    private val wifiClient = WifiClient(context)
    private val bleManager = BrailleBleManager(context) // 나중에 구현
    
    private val brailleDotTransmitter = WifiBrailleDotTransmitter(wifiClient)
    private val textMessageTransmitter = WifiTextMessageTransmitter(wifiClient)
    private val settingsTransmitter = WifiSettingsTransmitter(wifiClient)
    
    // 연결 타입에 따른 전송
    fun sendDotTouch(dotIndex: Int, connectionType: ConnectionType) {
        when (connectionType) {
            ConnectionType.WIFI -> brailleDotTransmitter.sendDotTouch(dotIndex)
            ConnectionType.BLE -> {
                // BLE 구현 후 추가
                // bleManager.sendDotTouch(dotIndex)
            }
        }
    }
    
    fun sendTextMessage(text: String, braille: String, connectionType: ConnectionType) {
        when (connectionType) {
            ConnectionType.WIFI -> textMessageTransmitter.sendTextMessage(text, braille)
            ConnectionType.BLE -> {
                // BLE 구현 후 추가
                // bleManager.sendTextMessage(text, braille)
            }
        }
    }
    
    fun sendSettings(settings: BrailleSettings, connectionType: ConnectionType) {
        when (connectionType) {
            ConnectionType.WIFI -> settingsTransmitter.sendSettings(settings)
            ConnectionType.BLE -> {
                // BLE 구현 후 추가
                // bleManager.sendSettings(settings)
            }
        }
    }
}

enum class ConnectionType {
    WIFI, BLE
}
```

---

## 🔧 ESP32 서버 구현

### 1. WiFi 서버

```cpp
#include <WiFi.h>
#include <WiFiServer.h>

class WifiBrailleServer {
private:
    WiFiServer server;
    WiFiClient client;
    bool clientConnected = false;
    
public:
    void init() {
        // WiFi 설정
        WiFi.begin("SSID", "PASSWORD");
        while (WiFi.status() != WL_CONNECTED) {
            delay(1000);
        }
        
        server.begin(8080);
        Serial.println("WiFi Server started on port 8080");
    }
    
    void handleClient() {
        if (!clientConnected) {
            client = server.available();
            if (client) {
                clientConnected = true;
                Serial.println("Client connected");
            }
        } else {
            if (client.connected()) {
                if (client.available()) {
                    handleReceivedData();
                }
            } else {
                clientConnected = false;
                Serial.println("Client disconnected");
            }
        }
    }
    
private:
    void handleReceivedData() {
        uint8_t buffer[256];
        int bytesRead = client.readBytes(buffer, sizeof(buffer));
        
        if (bytesRead >= 4) {
            // 헤더 파싱
            uint8_t messageType = buffer[0];
            uint16_t dataLength = (buffer[2] << 8) | buffer[1]; // Little Endian
            uint8_t checksum = buffer[3];
            
            if (bytesRead >= 4 + dataLength) {
                // 데이터 추출
                uint8_t* data = buffer + 4;
                
                // 체크섬 검증
                if (calculateChecksum(data, dataLength) == checksum) {
                    processMessage(messageType, data, dataLength);
                }
            }
        }
    }
    
    void processMessage(uint8_t messageType, uint8_t* data, uint16_t dataLength) {
        switch (messageType) {
            case 0x01: // DOT_TOUCH
                handleDotTouch(data[0]);
                break;
            case 0x02: // DOT_RELEASE
                handleDotRelease(data[0]);
                break;
            case 0x03: // PATTERN_COMPLETE
                handlePatternComplete(data[0]);
                break;
            case 0x04: // CLEAR
                handleClear();
                break;
            case 0x10: // TEXT_MESSAGE
                handleTextMessage(data, dataLength);
                break;
            case 0x20: // SETTINGS
                handleSettings(data, dataLength);
                break;
        }
    }
    
    void handleDotTouch(uint8_t dotIndex) {
        // 점자 디스플레이 업데이트
        setBrailleDot(dotIndex, true);
        
        // 상태 업데이트 전송
        sendStatusUpdate();
    }
    
    void handleDotRelease(uint8_t dotIndex) {
        // 점자 디스플레이 업데이트
        setBrailleDot(dotIndex, false);
        
        // 상태 업데이트 전송
        sendStatusUpdate();
    }
    
    void handlePatternComplete(uint8_t dotsByte) {
        // 6개 점 상태 업데이트
        for (int i = 0; i < 6; i++) {
            setBrailleDot(i, (dotsByte & (1 << i)) != 0);
        }
        
        // 상태 업데이트 전송
        sendStatusUpdate();
    }
    
    void handleClear() {
        // 모든 점 초기화
        for (int i = 0; i < 6; i++) {
            setBrailleDot(i, false);
        }
        
        // 상태 업데이트 전송
        sendStatusUpdate();
    }
    
    void handleTextMessage(uint8_t* data, uint16_t dataLength) {
        // 텍스트 메시지 처리
        uint8_t textLength = data[0];
        String text = String((char*)(data + 1), textLength);
        
        uint8_t brailleLength = data[1 + textLength];
        String braille = String((char*)(data + 2 + textLength), brailleLength);
        
        // 점자 디스플레이에 표시
        displayBrailleText(braille);
        
        // 메시지 수신 확인 전송
        sendMessageReceived();
    }
    
    void handleSettings(uint8_t* data, uint16_t dataLength) {
        if (dataLength >= 3) {
            uint8_t speed = data[0];
            uint8_t mode = data[1];
            uint8_t flags = data[2];
            
            // 설정 적용
            applySettings(speed, mode, flags);
            
            // 설정 확인 전송
            sendSettingsUpdated();
        }
    }
    
    void sendStatusUpdate() {
        uint8_t data[1] = {0x01}; // STATUS_UPDATE
        sendMessage(0x01, data, 1);
    }
    
    void sendMessageReceived() {
        uint8_t data[1] = {0x02}; // MESSAGE_RECEIVED
        sendMessage(0x02, data, 1);
    }
    
    void sendSettingsUpdated() {
        uint8_t data[1] = {0x03}; // SETTINGS_UPDATED
        sendMessage(0x03, data, 1);
    }
    
    void sendMessage(uint8_t messageType, uint8_t* data, uint16_t dataLength) {
        if (client.connected()) {
            uint8_t packet[4 + dataLength];
            packet[0] = messageType;
            packet[1] = dataLength & 0xFF;        // LSB
            packet[2] = (dataLength >> 8) & 0xFF; // MSB
            packet[3] = calculateChecksum(data, dataLength);
            
            memcpy(packet + 4, data, dataLength);
            
            client.write(packet, 4 + dataLength);
        }
    }
    
    uint8_t calculateChecksum(uint8_t* data, uint16_t length) {
        uint8_t checksum = 0;
        for (uint16_t i = 0; i < length; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }
};
```

---

## 🧪 테스트 및 디버깅

### 1. Android 앱 테스트

```kotlin
class WifiProtocolTest {
    fun testDotTouch() {
        val transmitter = WifiBrailleDotTransmitter(wifiClient)
        transmitter.sendDotTouch(0) // 1번 점 터치
        // ESP32에서 점자 디스플레이 확인
    }
    
    fun testTextMessage() {
        val transmitter = WifiTextMessageTransmitter(wifiClient)
        transmitter.sendTextMessage("안녕하세요", "⠁⠝⠓⠑⠽⠁⠎⠑⠽⠕")
        // ESP32에서 점자 텍스트 표시 확인
    }
    
    fun testSettings() {
        val settings = BrailleSettings(
            speed = BrailleSpeed.SLOW,
            mode = BrailleMode.AUTO
        )
        val transmitter = WifiSettingsTransmitter(wifiClient)
        transmitter.sendSettings(settings)
        // ESP32에서 설정 적용 확인
    }
}
```

### 2. ESP32 디버깅

```cpp
void setup() {
    Serial.begin(115200);
    
    WifiBrailleServer server;
    server.init();
    
    while (true) {
        server.handleClient();
        delay(10);
    }
}
```

---

## 📊 성능 비교

| 특성 | WiFi 프로토타입 | BLE GATT |
|------|----------------|----------|
| **설정 복잡도** | 낮음 (TCP 소켓) | 높음 (GATT 서비스) |
| **디버깅 용이성** | 높음 | 중간 |
| **전송 속도** | 빠름 (WiFi) | 중간 (BLE) |
| **전력 소비** | 높음 | 낮음 |
| **연결 안정성** | 높음 | 중간 |
| **실제 사용** | 프로토타입 | 최종 제품 |

---

## 🎯 구현 순서

1. **WiFi 프로토타입 구현** (현재)
   - Android WiFi 클라이언트
   - ESP32 WiFi 서버
   - 기본 메시지 전송/수신

2. **BLE GATT 구현** (다음)
   - Android BLE 클라이언트
   - ESP32 BLE 서버
   - GATT 서비스 및 특성

3. **통합 매니저 구현**
   - WiFi ↔ BLE 자동 전환
   - 동일한 API 사용

---

*이 WiFi 프로토타입 프로토콜을 통해 GATT 구조를 먼저 검증하고, 이후 BLE로 마이그레이션할 수 있습니다.*
