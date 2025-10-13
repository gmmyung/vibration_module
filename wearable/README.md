# OFBGlove Wearable Device

ESP32 기반 점자 장갑 제어 코드와 서버 프로그램입니다.

## 🚀 빠른 시작

### ESP32 장갑 코드
```bash
# Arduino IDE에서 열기
esp32_ble_server.ino    # BLE 서버 (Android 앱용)
esp32_web_client.ino    # WiFi 클라이언트 (웹 비주얼라이저용)
```

### Python 서버
```bash
# WiFi 서버 실행
python python_wifi_server.py

# WebSocket 서버 실행  
python websocket_server.py
```

## 🔧 주요 구성

### ESP32 코드
- **BLE 서버**: Android 앱과 Bluetooth 통신
- **WiFi 클라이언트**: 웹 비주얼라이저와 TCP 통신
- **점자 제어**: 6개 점자 핀 제어


## 📁 주요 파일

- `esp32_ble_server.ino` - BLE 서버 (Android 앱용)
- `esp32_web_client.ino` - WiFi 클라이언트 (웹용)
- `python_wifi_server.py` - Python WiFi 서버
- `websocket_server.py` - WebSocket 서버
- `glove/` - 장갑 하드웨어 관련 파일
- `visualizer/` - 시각화 관련 파일

## 🔗 통신 프로토콜

- **BLE**: GATT 프로토콜로 점자 데이터 수신
- **WiFi**: TCP 소켓으로 점자 데이터 전송
- **WebSocket**: 웹 클라이언트와 실시간 통신
