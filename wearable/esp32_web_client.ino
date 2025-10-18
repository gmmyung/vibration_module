/*
 * ESP32 Web Braille Client
 * 웹 기반 점자 시각화 시스템을 위한 ESP32 클라이언트
 * 
 * 기능:
 * - WiFi 연결
 * - WebSocket 서버 연결 (ws://localhost:8000/ws)
 * - 점자 패턴 전송
 * - 실시간 상태 동기화
 * - HTTP REST API 지원
 */

#include <WiFi.h>
#include <WebSocketsClient.h>
#include <ArduinoJson.h>
#include <HTTPClient.h>

// WiFi 설정
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// 서버 설정
const char* websocket_host = "192.168.1.100";  // 웹 서버 IP 주소
const int websocket_port = 8000;
const char* websocket_path = "/ws";

// HTTP 서버 설정
const char* http_host = "192.168.1.100";
const int http_port = 8000;

// WebSocket 클라이언트
WebSocketsClient webSocket;

// 점자 상태
bool brailleDots[6] = {false, false, false, false, false, false};
unsigned long lastUpdateTime = 0;
const unsigned long updateInterval = 100; // 100ms마다 상태 전송

// 연결 상태
bool wifiConnected = false;
bool websocketConnected = false;
unsigned long lastReconnectAttempt = 0;
const unsigned long reconnectInterval = 5000; // 5초마다 재연결 시도

// 점자 핀 정의 (실제 하드웨어에 맞게 수정)
const int BRAILLE_PINS[6] = {2, 3, 4, 5, 6, 7};

// 점자 입력 핀 (터치 센서 또는 버튼)
const int INPUT_PINS[6] = {14, 15, 16, 17, 18, 19};

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("🌐 ESP32 Web Braille Client 시작");
  Serial.println("=" * 50);
  
  // 점자 출력 핀 초기화
  for (int i = 0; i < 6; i++) {
    pinMode(BRAILLE_PINS[i], OUTPUT);
    digitalWrite(BRAILLE_PINS[i], LOW);
  }
  
  // 점자 입력 핀 초기화 (내부 풀업 저항 사용)
  for (int i = 0; i < 6; i++) {
    pinMode(INPUT_PINS[i], INPUT_PULLUP);
  }
  
  // WiFi 연결
  connectToWiFi();
  
  // WebSocket 초기화
  setupWebSocket();
  
  Serial.println("✅ 초기화 완료");
  Serial.println("📡 웹 서버 연결 대기 중...");
}

void loop() {
  // WiFi 연결 상태 확인
  if (WiFi.status() != WL_CONNECTED) {
    if (wifiConnected) {
      Serial.println("❌ WiFi 연결 끊어짐");
      wifiConnected = false;
      websocketConnected = false;
    }
    connectToWiFi();
  } else if (!wifiConnected) {
    Serial.println("✅ WiFi 연결됨");
    wifiConnected = true;
    setupWebSocket();
  }
  
  // WebSocket 연결 상태 확인
  if (wifiConnected && !websocketConnected) {
    if (millis() - lastReconnectAttempt > reconnectInterval) {
      Serial.println("🔄 WebSocket 재연결 시도...");
      webSocket.begin(websocket_host, websocket_port, websocket_path);
      lastReconnectAttempt = millis();
    }
  }
  
  // WebSocket 이벤트 처리
  webSocket.loop();
  
  // 점자 입력 처리
  handleBrailleInput();
  
  // 주기적 상태 전송
  if (millis() - lastUpdateTime > updateInterval) {
    sendBrailleStatus();
    lastUpdateTime = millis();
  }
  
  delay(10);
}

void connectToWiFi() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }
  
  Serial.print("📶 WiFi 연결 중");
  WiFi.begin(ssid, password);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.println("✅ WiFi 연결 성공!");
    Serial.print("📡 IP 주소: ");
    Serial.println(WiFi.localIP());
    wifiConnected = true;
  } else {
    Serial.println();
    Serial.println("❌ WiFi 연결 실패");
    wifiConnected = false;
  }
}

void setupWebSocket() {
  webSocket.begin(websocket_host, websocket_port, websocket_path);
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(5000);
  webSocket.enableHeartbeat(15000, 3000, 2);
  
  Serial.println("🔌 WebSocket 클라이언트 초기화 완료");
}

void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_DISCONNECTED:
      Serial.println("🔌 WebSocket 연결 해제");
      websocketConnected = false;
      break;
      
    case WStype_CONNECTED:
      Serial.println("✅ WebSocket 연결됨");
      Serial.printf("📡 서버: %s\n", payload);
      websocketConnected = true;
      
      // 연결 확인 메시지 전송
      sendConnectionMessage();
      break;
      
    case WStype_TEXT:
      handleWebSocketMessage((char*)payload);
      break;
      
    case WStype_BIN:
      Serial.printf("📦 바이너리 메시지 수신: %d bytes\n", length);
      break;
      
    case WStype_ERROR:
      Serial.printf("❌ WebSocket 오류: %s\n", payload);
      break;
      
    case WStype_FRAGMENT_TEXT_START:
    case WStype_FRAGMENT_BIN_START:
    case WStype_FRAGMENT:
    case WStype_FRAGMENT_FIN:
      Serial.println("📦 프래그먼트 메시지 수신");
      break;
      
    default:
      Serial.printf("❓ 알 수 없는 WebSocket 이벤트: %d\n", type);
      break;
  }
}

void handleWebSocketMessage(char* message) {
  Serial.printf("📥 WebSocket 메시지 수신: %s\n", message);
  
  // JSON 파싱
  DynamicJsonDocument doc(1024);
  DeserializationError error = deserializeJson(doc, message);
  
  if (error) {
    Serial.printf("❌ JSON 파싱 오류: %s\n", error.c_str());
    return;
  }
  
  String type = doc["type"];
  
  if (type == "pong") {
    // 핑-퐁 응답
    unsigned long t0 = doc["t0"];
    unsigned long latency = millis() - t0;
    Serial.printf("🏓 Pong 수신, 지연시간: %lu ms\n", latency);
    
  } else if (type == "sequence") {
    // 시퀀스 메시지 처리
    JsonArray steps = doc["steps"];
    bool loop = doc["loop"] | false;
    
    Serial.printf("🎬 시퀀스 수신: %d개 스텝, 루프: %s\n", 
                  steps.size(), loop ? "ON" : "OFF");
    
    // 시퀀스 실행 (간단한 예시)
    executeSequence(steps, loop);
    
  } else if (type == "braille_pattern") {
    // 점자 패턴 업데이트
    JsonArray pattern = doc["pattern"];
    String character = doc["character"] | "?";
    
    Serial.printf("📝 점자 패턴 수신: %s (%s)\n", 
                  pattern.as<String>().c_str(), character.c_str());
    
    // 점자 패턴 적용
    for (int i = 0; i < 6 && i < pattern.size(); i++) {
      brailleDots[i] = pattern[i];
    }
    updateBrailleDisplay();
    
  } else if (type == "settings") {
    // 설정 업데이트
    int speed = doc["speed"] | 1;
    int mode = doc["mode"] | 0;
    
    Serial.printf("⚙️ 설정 업데이트: speed=%d, mode=%d\n", speed, mode);
    
  } else {
    Serial.printf("❓ 알 수 없는 메시지 타입: %s\n", type.c_str());
  }
}

void sendConnectionMessage() {
  DynamicJsonDocument doc(512);
  doc["type"] = "esp32_connected";
  doc["device_id"] = WiFi.macAddress();
  doc["ip_address"] = WiFi.localIP().toString();
  doc["timestamp"] = millis();
  
  String message;
  serializeJson(doc, message);
  webSocket.sendTXT(message);
  
  Serial.println("📤 연결 메시지 전송");
}

void sendBrailleStatus() {
  if (!websocketConnected) {
    return;
  }
  
  DynamicJsonDocument doc(512);
  doc["type"] = "braille_status";
  doc["pattern"] = brailleDots;
  doc["binary"] = getBraillePattern();
  doc["character"] = getBrailleCharacter();
  doc["timestamp"] = millis();
  
  String message;
  serializeJson(doc, message);
  webSocket.sendTXT(message);
}

void handleBrailleInput() {
  static bool lastInputState[6] = {false, false, false, false, false, false};
  bool currentInputState[6];
  
  // 현재 입력 상태 읽기
  for (int i = 0; i < 6; i++) {
    currentInputState[i] = !digitalRead(INPUT_PINS[i]); // 풀업이므로 반전
  }
  
  // 상태 변화 감지
  for (int i = 0; i < 6; i++) {
    if (currentInputState[i] != lastInputState[i]) {
      if (currentInputState[i]) {
        // 점 터치
        brailleDots[i] = true;
        sendDotTouch(i);
        Serial.printf("👆 점 터치: %d번 점\n", i + 1);
      } else {
        // 점 릴리즈
        brailleDots[i] = false;
        sendDotRelease(i);
        Serial.printf("👆 점 릴리즈: %d번 점\n", i + 1);
      }
      
      updateBrailleDisplay();
      lastInputState[i] = currentInputState[i];
    }
  }
}

void sendDotTouch(int dotIndex) {
  if (!websocketConnected) return;
  
  DynamicJsonDocument doc(256);
  doc["type"] = "dot_touch";
  doc["dot_index"] = dotIndex;
  doc["timestamp"] = millis();
  
  String message;
  serializeJson(doc, message);
  webSocket.sendTXT(message);
}

void sendDotRelease(int dotIndex) {
  if (!websocketConnected) return;
  
  DynamicJsonDocument doc(256);
  doc["type"] = "dot_release";
  doc["dot_index"] = dotIndex;
  doc["timestamp"] = millis();
  
  String message;
  serializeJson(doc, message);
  webSocket.sendTXT(message);
}

void executeSequence(JsonArray steps, bool loop) {
  // 시퀀스 실행 (간단한 예시)
  // 실제로는 더 정교한 타이밍 제어가 필요
  Serial.println("🎬 시퀀스 실행 시작");
  
  do {
    for (JsonObject step : steps) {
      JsonArray cell = step["cell"];
      int duration = step["ms"];
      
      // 점자 패턴 적용
      for (int i = 0; i < 6 && i < cell.size(); i++) {
        brailleDots[i] = cell[i];
      }
      updateBrailleDisplay();
      
      // 지연
      delay(duration);
    }
  } while (loop);
  
  // 시퀀스 완료 후 초기화
  for (int i = 0; i < 6; i++) {
    brailleDots[i] = false;
  }
  updateBrailleDisplay();
  
  Serial.println("🎬 시퀀스 실행 완료");
}

void updateBrailleDisplay() {
  for (int i = 0; i < 6; i++) {
    digitalWrite(BRAILLE_PINS[i], brailleDots[i] ? HIGH : LOW);
  }
}

String getBraillePattern() {
  String pattern = "";
  for (int i = 0; i < 6; i++) {
    pattern += brailleDots[i] ? "1" : "0";
  }
  return pattern;
}

String getBrailleCharacter() {
  String pattern = getBraillePattern();
  
  // 간단한 점자-문자 매핑
  if (pattern == "100000") return "A";
  if (pattern == "110000") return "B";
  if (pattern == "100100") return "C";
  if (pattern == "100110") return "D";
  if (pattern == "100010") return "E";
  if (pattern == "110100") return "F";
  if (pattern == "110110") return "G";
  if (pattern == "110010") return "H";
  if (pattern == "010100") return "I";
  if (pattern == "010110") return "J";
  if (pattern == "101000") return "K";
  if (pattern == "111000") return "L";
  if (pattern == "101100") return "M";
  if (pattern == "101110") return "N";
  if (pattern == "101010") return "O";
  if (pattern == "111100") return "P";
  if (pattern == "111110") return "Q";
  if (pattern == "111010") return "R";
  if (pattern == "011100") return "S";
  if (pattern == "011110") return "T";
  if (pattern == "101001") return "U";
  if (pattern == "111001") return "V";
  if (pattern == "010111") return "W";
  if (pattern == "101101") return "X";
  if (pattern == "101111") return "Y";
  if (pattern == "101011") return "Z";
  if (pattern == "000000") return " ";
  
  return "?";
}

void sendHTTPRequest(String endpoint, String data) {
  if (!wifiConnected) return;
  
  HTTPClient http;
  String url = "http://" + String(http_host) + ":" + String(http_port) + endpoint;
  
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  
  int httpResponseCode = http.POST(data);
  
  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.printf("📡 HTTP 응답 (%d): %s\n", httpResponseCode, response.c_str());
  } else {
    Serial.printf("❌ HTTP 오류: %d\n", httpResponseCode);
  }
  
  http.end();
}
