# OFBGlove - 점자 장갑 시스템

음성 입력을 점자로 변환하여 시각화하고 실제 장갑에 전송하는 통합 시스템입니다.

## 📁 프로젝트 구조

### `application/` - 스마트폰 앱
- **사용자 음성 입력**: 음성을 텍스트로 변환
- **점자 변환**: 텍스트를 6점 점자 패턴으로 변환
- **통신**: BLE/WiFi로 장갑과 웹 비주얼라이저와 연결

### `web_visualizer/` - 웹 비주얼라이저
- **시각화**: 실시간 점자 패턴 시각화
- **장갑을 대체하는 시각화**: 물리적 장갑 없이도 점자 체험 가능
- **장갑 없이도 시연 가능**: 독립적인 시연 환경 제공

### `wearable/` - 장갑 제어
- **Arduino 코드**: ESP32 기반 점자 장갑 제어
- **실제 장갑 제어 코드**: 6개 점자 핀 제어 로직
- **ESP32 제어**: BLE/WiFi 통신 및 하드웨어 제어

## 🚀 빠른 시작

1. **웹 비주얼라이저 실행** (시연용)
   ```bash
   cd web_visualizer
   npm install
   npm run dev
   ```

2. **Android 앱 빌드** (실제 사용)
   ```bash
   cd application
   # Android Studio에서 프로젝트 열기
   ```

3. **ESP32 장갑 설정** (하드웨어)
   ```bash
   cd wearable
   # Arduino IDE에서 esp32_ble_server.ino 열기
   ```

## 🔗 통신 흐름

```
음성 입력 → Android 앱 → 점자 변환 → ESP32 장갑
                    ↓
              웹 비주얼라이저 (시각화)
```