/*
 * ESP32 WiFi 점자 서버 - GATT 호환 프로토콜
 * 
 * 기능:
 * - WiFi TCP 서버 (포트 8080)
 * - 점자 점 전송/수신
 * - 텍스트 메시지 처리
 * - 사용자 설정 처리
 * - 점자 디스플레이 제어
 */

#include <WiFi.h>
#include <WiFiClient.h>
#include <WiFiServer.h>

// WiFi 설정
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// 서버 설정
WiFiServer server(8888);
WiFiClient client;
bool clientConnected = false;

// 점자 상태
bool brailleDots[6] = {false, false, false, false, false, false};
BrailleSettings currentSettings;

// 메시지 타입 정의 (Android와 동일)
enum MessageType {
  // 송신 메시지 (앱 → ESP32)
  DOT_TOUCH = 0x01,
  DOT_RELEASE = 0x02,
  PATTERN_COMPLETE = 0x03,
  CLEAR = 0x04,
  WORD_SEPARATOR = 0x05,     // 단어 구분자 (공백)
  SENTENCE_SEPARATOR = 0x06, // 문장 구분자
  BRAILLE_SEPARATOR = 0x07,  // 점자 구분자 (개별 점자 모드)
  TEXT_MESSAGE = 0x10,
  SETTINGS = 0x20,
  
  // 수신 메시지 (ESP32 → 앱)
  STATUS_UPDATE = 0x81,      // 0x80 + 0x01
  MESSAGE_RECEIVED = 0x82,   // 0x80 + 0x02
  SETTINGS_UPDATED = 0x83    // 0x80 + 0x03
};

// 점자 설정 구조체
struct BrailleSettings {
  uint8_t speed;  // 0=SLOW, 1=NORMAL, 2=FAST
  uint8_t mode;   // 0=AUTO, 1=MANUAL, 2=REPEAT
  uint8_t flags;  // 기타 플래그
};

// 점자 핀 정의 (실제 하드웨어에 맞게 수정)
const int BRAILLE_PINS[6] = {2, 3, 4, 5, 6, 7};

void setup() {
  Serial.begin(115200);
  
  // 점자 핀 초기화
  for (int i = 0; i < 6; i++) {
    pinMode(BRAILLE_PINS[i], OUTPUT);
    digitalWrite(BRAILLE_PINS[i], LOW);
  }
  
  // 기본 설정 초기화
  currentSettings.speed = 1; // NORMAL
  currentSettings.mode = 0;  // AUTO
  currentSettings.flags = 0x00;
  
  // WiFi 연결
  connectToWiFi();
  
  // 서버 시작
  server.begin();
  Serial.println("WiFi Braille Server started on port 8888");
  Serial.println("IP address: " + WiFi.localIP().toString());
}

void loop() {
  handleClient();
  delay(10);
}

void connectToWiFi() {
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }
  
  Serial.println();
  Serial.println("WiFi connected!");
  Serial.println("IP address: " + WiFi.localIP().toString());
}

void handleClient() {
  if (!clientConnected) {
    client = server.available();
    if (client) {
      clientConnected = true;
      Serial.println("Client connected: " + client.remoteIP().toString());
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

void handleReceivedData() {
  uint8_t buffer[256];
  int bytesRead = client.readBytes(buffer, sizeof(buffer));
  
  if (bytesRead >= 4) {
    // 헤더 파싱
    uint8_t messageType = buffer[0];
    uint16_t dataLength = (buffer[2] << 8) | buffer[1]; // Little Endian
    uint8_t checksum = buffer[3];
    
    Serial.printf("Received: type=0x%02X, length=%d\n", messageType, dataLength);
    
    if (bytesRead >= 4 + dataLength) {
      // 데이터 추출
      uint8_t* data = buffer + 4;
      
      // 체크섬 검증
      if (calculateChecksum(data, dataLength) == checksum) {
        processMessage(messageType, data, dataLength);
      } else {
        Serial.println("Checksum mismatch");
      }
    } else {
      Serial.println("Incomplete packet");
    }
  }
}

void processMessage(uint8_t messageType, uint8_t* data, uint16_t dataLength) {
  switch (messageType) {
    case DOT_TOUCH:
      handleDotTouch(data[0]);
      break;
    case DOT_RELEASE:
      handleDotRelease(data[0]);
      break;
    case PATTERN_COMPLETE:
      handlePatternComplete(data[0]);
      break;
    case CLEAR:
      handleClear();
      break;
    case WORD_SEPARATOR:
      handleWordSeparator();
      break;
    case SENTENCE_SEPARATOR:
      handleSentenceSeparator();
      break;
    case BRAILLE_SEPARATOR:
      handleBrailleSeparator();
      break;
    case TEXT_MESSAGE:
      handleTextMessage(data, dataLength);
      break;
    case SETTINGS:
      handleSettings(data, dataLength);
      break;
    default:
      Serial.printf("Unknown message type: 0x%02X\n", messageType);
  }
}

void handleDotTouch(uint8_t dotIndex) {
  if (dotIndex < 6) {
    brailleDots[dotIndex] = true;
    updateBrailleDisplay();
    sendStatusUpdate();
    Serial.printf("Dot %d touched\n", dotIndex + 1);
  }
}

void handleDotRelease(uint8_t dotIndex) {
  if (dotIndex < 6) {
    brailleDots[dotIndex] = false;
    updateBrailleDisplay();
    sendStatusUpdate();
    Serial.printf("Dot %d released\n", dotIndex + 1);
  }
}

void handlePatternComplete(uint8_t dotsByte) {
  // 6개 점 상태 업데이트
  for (int i = 0; i < 6; i++) {
    brailleDots[i] = (dotsByte & (1 << i)) != 0;
  }
  updateBrailleDisplay();
  sendMessageReceived();
  
  // 패턴을 6자리 이진수로 출력 (Python 서버와 동일)
  String pattern = "";
  for (int i = 0; i < 6; i++) {
    pattern += brailleDots[i] ? "1" : "0";
  }
  Serial.println("점자 패턴: " + pattern);
  Serial.printf("바이트 값: 0x%02X\n", dotsByte);
}

void handleClear() {
  // 모든 점 초기화
  for (int i = 0; i < 6; i++) {
    brailleDots[i] = false;
  }
  updateBrailleDisplay();
  sendStatusUpdate();
  Serial.println("Cleared all dots");
}

void handleWordSeparator() {
  // 단어 구분자 처리 (공백)
  Serial.println("Word separator received");
  sendStatusUpdate();
}

void handleSentenceSeparator() {
  // 문장 구분자 처리
  Serial.println("Sentence separator received");
  sendStatusUpdate();
}

void handleBrailleSeparator() {
  // 점자 구분자 처리 (개별 점자 모드)
  Serial.println("Braille separator received");
  sendStatusUpdate();
}

void handleTextMessage(uint8_t* data, uint16_t dataLength) {
  if (dataLength < 1) return;
  
  // 텍스트 메시지 처리 (간단한 형태)
  String text = String((char*)data, dataLength);
  
  Serial.println("텍스트 메시지: '" + text + "'");
  Serial.printf("길이: %d 문자\n", dataLength);
  
  // 메시지 수신 확인 전송
  sendMessageReceived();
}

void handleSettings(uint8_t* data, uint16_t dataLength) {
  if (dataLength >= 3) {
    uint8_t speed = data[0];
    uint8_t mode = data[1];
    uint8_t flags = data[2];
    
    currentSettings.speed = speed;
    currentSettings.mode = mode;
    currentSettings.flags = flags;
    
    // 설정 적용
    applySettings();
    
    Serial.printf("Settings updated: speed=%d, mode=%d, flags=0x%02X\n", 
                  speed, mode, flags);
    
    // 설정 확인 전송
    sendSettingsUpdated();
  }
}

void updateBrailleDisplay() {
  for (int i = 0; i < 6; i++) {
    digitalWrite(BRAILLE_PINS[i], brailleDots[i] ? HIGH : LOW);
  }
}

void displayBrailleText(String braille) {
  // 점자 텍스트를 순차적으로 표시
  for (int i = 0; i < braille.length(); i++) {
    char brailleChar = braille.charAt(i);
    
    // 점자 문자를 6개 점으로 변환 (간단한 예시)
    // 실제로는 점자 테이블을 사용해야 함
    if (brailleChar == '⠁') { // 'a' 점자
      setBraillePattern(0b000001); // 1번 점만
    } else if (brailleChar == '⠃') { // 'b' 점자
      setBraillePattern(0b000011); // 1,2번 점
    } else if (brailleChar == '⠉') { // 'c' 점자
      setBraillePattern(0b000101); // 1,3번 점
    } else {
      setBraillePattern(0b000000); // 공백
    }
    
    // 설정된 속도로 지연
    delay(getDisplayDelay());
  }
}

void setBraillePattern(uint8_t pattern) {
  for (int i = 0; i < 6; i++) {
    brailleDots[i] = (pattern & (1 << i)) != 0;
  }
  updateBrailleDisplay();
}

void applySettings() {
  // 설정에 따른 동작 변경
  Serial.printf("Applying settings: speed=%d, mode=%d\n", 
                currentSettings.speed, currentSettings.mode);
}

uint32_t getDisplayDelay() {
  switch (currentSettings.speed) {
    case 0: return 2000; // SLOW
    case 1: return 1000; // NORMAL
    case 2: return 500;  // FAST
    default: return 1000;
  }
}

void sendStatusUpdate() {
  uint8_t data[1] = {0x01}; // STATUS_UPDATE
  sendMessage(STATUS_UPDATE, data, 1);
}

void sendMessageReceived() {
  uint8_t data[1] = {0x02}; // MESSAGE_RECEIVED
  sendMessage(MESSAGE_RECEIVED, data, 1);
}

void sendSettingsUpdated() {
  uint8_t data[1] = {0x03}; // SETTINGS_UPDATED
  sendMessage(SETTINGS_UPDATED, data, 1);
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
    Serial.printf("Sent message: type=0x%02X, length=%d\n", messageType, dataLength);
  }
}

uint8_t calculateChecksum(uint8_t* data, uint16_t length) {
  uint8_t checksum = 0;
  for (uint16_t i = 0; i < length; i++) {
    checksum ^= data[i];
  }
  return checksum;
}
