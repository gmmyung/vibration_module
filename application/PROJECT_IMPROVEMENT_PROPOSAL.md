# 🚀 Voice-Text Chat Simulator 개선 작업 제안서

## 📊 현재 프로젝트 상태 분석

### ✅ 이미 구현된 기능
- **음성 인식 (STT)**: Android SpeechRecognizer 기반 실시간 음성 인식
- **텍스트 음성 변환 (TTS)**: 자동 음성 재생 기능
- **점자 변환**: liblouis-java를 사용한 텍스트 ↔ 점자 변환
- **점자 입력**: 6-dot 점자 그리드 기반 직접 점자 입력
- **BLE 통신**: JSON 기반 데이터 구조 설계 완료
- **채팅 시스템**: 실시간 메시지 교환 기능

### 🔍 개선이 필요한 영역
1. **JSON 구조 효율성**: 현재 구조의 최적화 필요
2. **점 단위 송신**: 개별 점자 점 전송 기능 부재
3. **사용자 설정**: 속도, 모드 설정 기능 부재
4. **다양한 입력 방식**: 음성 외 추가 입력 방식 부재

---

## 🎯 제안 작업 목록

### 0. 간단한 점자 점 통신 프로토콜 (최우선)

#### 0.1 현재 문제점
- 복잡한 JSON 구조로 인한 오버헤드
- 점자 점 정보만 전송하는데 100바이트 이상 사용
- 실시간 점자 점 전송에 부적합

#### 0.2 개선 방안
```kotlin
// 최소한의 점자 점 통신 (3바이트)
data class SimpleBrailleMessage(
    val messageType: Byte,    // 메시지 타입 (1바이트)
    val dots: Byte,           // 점자 점 상태 (1바이트) - 6개 점을 비트마스크로 표현
    val checksum: Byte        // 체크섬 (1바이트)
)

// 점자 점 변환 예시:
// 1점만:     00000001 (0x01) = "100000"
// 1,4,5점:   00011001 (0x19) = "100110"
// 1,2,3점:   00000111 (0x07) = "111000"
```

#### 0.3 구현 작업
- [ ] 3바이트 점자 점 통신 프로토콜 구현
- [ ] 점자 점 비트마스크 압축 (6개 점 → 1바이트)
- [ ] BLE/WiFi 공통 프로토콜 적용
- [ ] 실시간 점자 점 전송 테스트

### 1. 통합 통신 프로토콜 설계

#### 1.1 현재 문제점
- BLE와 WiFi 통신의 구조가 분리되어 있음
- JSON 구조가 너무 상세하고 중복 데이터 포함
- BLE MTU 크기 제한과 WiFi 대용량 데이터 처리 방식이 다름
- 점자 데이터 압축이 BLE에만 적용됨

#### 1.2 개선 방안
```kotlin
// 공통 메시지 인터페이스
interface BaseMessage {
    val messageId: String
    val timestamp: Long
    val deviceId: String
    val messageType: MessageType
}

// BLE용 압축된 메시지 (MTU 244바이트 제한)
data class CompressedBleMessage(
    override val messageId: String,
    override val timestamp: Long,
    override val deviceId: String,
    override val messageType: MessageType,
    val dots: Byte,                    // 6개 점을 1바이트로 압축
    val txt: String,                   // 압축된 텍스트
    val lang: String = "ko"
) : BaseMessage

// WiFi용 확장된 메시지 (대용량 데이터 지원)
data class ExtendedWifiMessage(
    override val messageId: String,
    override val timestamp: Long,
    override val deviceId: String,
    override val messageType: MessageType,
    val content: String,               // 원본 텍스트
    val braille: String,               // 점자 변환 결과
    val dots: List<Boolean>? = null,   // 점자 점 상태 (압축 없음)
    val metadata: Map<String, Any> = emptyMap()
) : BaseMessage

// 점 단위 전송을 위한 구조 (공통)
data class DotTransmissionMessage(
    override val messageId: String,
    override val timestamp: Long,
    override val deviceId: String,
    override val messageType: MessageType,
    val dotIndex: Int,                 // 0-5
    val isActive: Boolean,
    val pattern: String,               // 현재까지의 패턴
    val isComplete: Boolean
) : BaseMessage
```

#### 1.3 구현 작업
- [ ] 통합 통신 프로토콜 설계 (UNIFIED_COMMUNICATION_PROTOCOL.md 참조)
- [ ] BLE용 압축 메시지 구조 구현
- [ ] WiFi용 확장 메시지 구조 구현
- [ ] 메시지 변환기 구현 (BLE ↔ WiFi)
- [ ] 통합 메시지 매니저 구현

### 2. 점 단위 송신 기능

#### 2.1 현재 문제점
- 점자 입력이 완료된 후에만 전송
- 실시간 점자 점 상태 전송 불가
- 점자 입력 과정의 피드백 부족

#### 2.2 개선 방안
```kotlin
// 점 단위 전송 매니저
class DotTransmissionManager {
    fun sendDotUpdate(dotIndex: Int, isActive: Boolean)
    fun sendPatternUpdate(pattern: String)
    fun sendCompletionSignal()
    fun sendClearSignal()
}

// 실시간 점자 상태 동기화
data class RealtimeBrailleState(
    val dots: List<Boolean>,
    val currentPattern: String,
    val isComplete: Boolean,
    val lastUpdateTime: Long
)
```

#### 2.3 구현 작업
- [ ] 점자 점 터치 시 즉시 전송
- [ ] 실시간 점자 상태 동기화
- [ ] 점자 입력 과정 시각화
- [ ] 네트워크 지연 최적화

### 3. 사용자 설정 기능

#### 3.1 점자 표현 속도 설정
```kotlin
data class BrailleDisplaySettings(
    val displaySpeed: BrailleSpeed,    // SLOW, NORMAL, FAST
    val autoMode: Boolean,             // 자동 모드
    val manualMode: Boolean,           // 수동 모드
    val dotDelay: Long,                // 점 표시 지연 시간
    val patternDelay: Long             // 패턴 표시 지연 시간
)

enum class BrailleSpeed {
    SLOW(2000L),      // 2초
    NORMAL(1000L),    // 1초
    FAST(500L)        // 0.5초
}
```

#### 3.2 자동/수동 모드
```kotlin
enum class BrailleMode {
    AUTO,           // 자동으로 점자 표시
    MANUAL,         // 수동으로 점자 표시
    HYBRID          // 혼합 모드
}

class BrailleModeManager {
    fun setAutoMode(enabled: Boolean)
    fun setManualMode(enabled: Boolean)
    fun toggleMode()
    fun getCurrentMode(): BrailleMode
}
```

#### 3.3 구현 작업
- [ ] 설정 화면 UI 구현
- [ ] 점자 속도 조절 기능
- [ ] 자동/수동 모드 전환
- [ ] 설정 저장/로드 기능
- [ ] 실시간 설정 적용

### 4. 음성(영어, 한국어) 외 입력 방식

#### 4.1 호명 기능
```kotlin
class VoiceCallManager {
    fun startVoiceCall()
    fun endVoiceCall()
    fun sendCallSignal()
    fun handleIncomingCall()
}

data class VoiceCallMessage(
    val callerId: String,
    val callType: CallType,
    val timestamp: Long,
    val duration: Long? = null
)

enum class CallType {
    INCOMING,       // 수신
    OUTGOING,       // 발신
    MISSED,         // 부재중
    REJECTED        // 거절
}
```

#### 4.2 삐 소리 (시스템 알림)
```kotlin
class SystemSoundManager {
    fun playBeepSound(type: BeepType)
    fun playNotificationSound()
    fun playErrorSound()
    fun playSuccessSound()
}

enum class BeepType {
    SHORT,          // 짧은 삐
    LONG,           // 긴 삐
    DOUBLE,         // 두 번 삐
    TRIPLE,         // 세 번 삐
    PATTERN         // 패턴 삐
}
```

#### 4.3 응급 상황 알림
```kotlin
class EmergencyManager {
    fun sendEmergencyAlert(message: String)
    fun playEmergencySound()
    fun sendLocationData()
    fun contactEmergencyServices()
}

data class EmergencyMessage(
    val emergencyType: EmergencyType,
    val location: String?,
    val timestamp: Long,
    val message: String,
    val priority: EmergencyPriority
)

enum class EmergencyType {
    MEDICAL,        // 의료 응급상황
    SAFETY,         // 안전 사고
    TECHNICAL,      // 기술적 문제
    GENERAL         // 일반 응급상황
}
```

#### 4.4 구현 작업
- [ ] 호명 기능 구현
- [ ] 시스템 사운드 매니저
- [ ] 응급상황 알림 시스템
- [ ] 다양한 알림 타입 지원
- [ ] 접근성 고려한 사운드 디자인

---

## 📅 구현 우선순위 및 일정

### Phase 0: 간단한 점자 점 통신 (1주) - **최우선**
- [ ] 3바이트 점자 점 프로토콜 구현
- [ ] 점자 점 비트마스크 압축 (6개 점 → 1바이트)
- [ ] BLE/WiFi 공통 프로토콜 적용
- [ ] 실시간 점자 점 전송 테스트

### Phase 1: 통합 통신 프로토콜 (1주)
- [ ] BLE용 압축 메시지 구조 구현
- [ ] WiFi용 확장 메시지 구조 구현
- [ ] 메시지 변환기 구현 (BLE ↔ WiFi)
- [ ] 통합 메시지 매니저 구현

### Phase 2: 사용자 설정 (1주)
- [ ] 설정 화면 구현
- [ ] 점자 속도 조절
- [ ] 자동/수동 모드

### Phase 3: 추가 입력 방식 (1주)
- [ ] 호명 기능
- [ ] 시스템 사운드
- [ ] 응급상황 알림

---

## 🛠️ 기술적 구현 세부사항

### 1. JSON 압축 전략
```kotlin
// 압축 전 (현재)
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1703123456789,
  "deviceId": "device_001",
  "dots": [true, true, false, false, true, false],
  "pattern": "125",
  "character": "H",
  "inputText": "HELLO",
  "isComplete": true,
  "messageType": "BRAILLE_INPUT"
}

// 압축 후 (제안)
{
  "id": "a7164466",
  "ts": 1703123456789,
  "dev": "001",
  "dots": 0x23,  // 00100011 (125 패턴)
  "txt": "HELLO",
  "type": 1
}
```

### 2. 점 단위 전송 프로토콜
```kotlin
// 점 업데이트 메시지
data class DotUpdateMessage(
    val id: String,
    val ts: Long,
    val dot: Int,        // 0-5
    val active: Boolean,
    val pattern: String  // 현재 패턴
)

// 패턴 완성 메시지
data class PatternCompleteMessage(
    val id: String,
    val ts: Long,
    val pattern: String,
    val character: Char?,
    val text: String
)
```

### 3. 설정 관리 시스템
```kotlin
class SettingsManager {
    private val sharedPrefs: SharedPreferences
    
    fun saveBrailleSettings(settings: BrailleDisplaySettings)
    fun loadBrailleSettings(): BrailleDisplaySettings
    fun resetToDefaults()
    fun exportSettings(): String
    fun importSettings(settingsJson: String)
}
```

---

## 📋 체크리스트

### JSON 구조 최적화
- [ ] 기존 JSON 구조 분석
- [ ] 압축 알고리즘 설계
- [ ] 비트마스크 압축 구현
- [ ] BLE MTU 크기 최적화
- [ ] 성능 테스트

### 점 단위 송신
- [ ] 실시간 전송 프로토콜 설계
- [ ] 점자 상태 동기화 구현
- [ ] 네트워크 지연 최적화
- [ ] 오류 처리 및 재전송

### 사용자 설정
- [ ] 설정 UI 설계
- [ ] 점자 속도 조절 구현
- [ ] 자동/수동 모드 구현
- [ ] 설정 저장/로드 기능

### 추가 입력 방식
- [ ] 호명 기능 구현
- [ ] 시스템 사운드 매니저
- [ ] 응급상황 알림 시스템
- [ ] 접근성 테스트

---

## 🎯 예상 효과

### 성능 개선
- **JSON 크기 50% 이상 감소**
- **BLE 전송 속도 2배 향상**
- **실시간 점자 동기화**

### 사용자 경험 개선
- **직관적인 설정 인터페이스**
- **다양한 입력 방식 지원**
- **응급상황 대응 능력**

### 접근성 향상
- **시청각장애인을 위한 최적화**
- **다양한 피드백 방식**
- **안전한 통신 환경**

---

## 📚 참고 자료

### 기술 문서
- [SIMPLE_BRAILLE_DOT_PROTOCOL.md](./SIMPLE_BRAILLE_DOT_PROTOCOL.md) - **간단한 점자 점 통신 프로토콜 (최우선)**
- [UNIFIED_COMMUNICATION_PROTOCOL.md](./UNIFIED_COMMUNICATION_PROTOCOL.md) - 통합 통신 프로토콜 설계서
- [BLE_DATA_STRUCTURE.md](./BLE_DATA_STRUCTURE.md) - BLE 통신 데이터 구조
- [WIFI_PROTOCOL_HEADER.md](./WIFI_PROTOCOL_HEADER.md) - WiFi 통신 프로토콜
- [BRAILLE_INPUT_IMPLEMENTATION.md](./BRAILLE_INPUT_IMPLEMENTATION.md) - 점자 입력 구현
- [USER_INTERFACE_REDESIGN_PLAN.md](./USER_INTERFACE_REDESIGN_PLAN.md) - UI 재설계 계획

### 관련 라이브러리
- **liblouis-java**: 5.1.0 - 전문급 점자 번역
- **Android BLE**: 2.7.5 - BLE 통신
- **Gson**: 2.10.1 - JSON 직렬화/역직렬화

### 핵심 설계 원칙
- **통일성**: BLE와 WiFi가 동일한 메시지 구조 사용
- **효율성**: 각 통신 방식의 특성에 맞는 최적화
- **확장성**: 새로운 통신 방식 추가 시 기존 구조 재사용
- **호환성**: 기존 시스템과의 완벽한 통합

---

*이 문서는 Voice-Text Chat Simulator 프로젝트의 개선 작업을 위한 제안서입니다. 구현 과정에서 필요에 따라 내용을 업데이트하겠습니다.*
