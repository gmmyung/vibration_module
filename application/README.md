# Voice-Text Chat Simulator with Braille Support

A comprehensive Android chat application that simulates conversations between voice users and text users, featuring advanced speech recognition, text-to-speech, and braille input/output capabilities. Designed with accessibility in mind, this app supports multiple input methods including voice, text, and braille.

## 🌟 Key Features

### 🎤 Advanced Voice Processing
- **Real-time Speech Recognition**: Convert speech to text using Android's SpeechRecognizer
- **Advanced STT Processing**: Comprehensive status feedback and error handling
- **Status Management**: Visual feedback for all speech recognition states
- **Automatic Retry**: Smart retry logic for failed recognition attempts

### 📝 Multiple Input Methods
- **Text Input**: Traditional keyboard input with send functionality
- **Voice Input**: Tap-to-speak with real-time recognition
- **Braille Input**: 6-dot braille grid for tactile input
- **Input Mode Switching**: Seamless switching between input methods

### 🔊 Text-to-Speech Integration
- **Automatic TTS**: Text messages are automatically played back as speech
- **Real-time Feedback**: Immediate audio feedback for braille input
- **Language Support**: Configurable TTS language settings

### 🔤 Braille Support
- **liblouis Integration**: Professional-grade braille translation using liblouis-java
- **Bidirectional Conversion**: Text ↔ Braille conversion in both directions
- **Multiple Braille Tables**: Support for various braille table formats
- **Interactive Braille Input**: 6-dot braille grid for direct braille input
- **Real-time Translation**: Instant braille translation and audio feedback

### 📶 WiFi Prototype Communication (NEW!)
- **GATT-Compatible Protocol**: WiFi implementation using BLE GATT message structure
- **Real-time Braille Transmission**: 2-byte binary protocol for instant dot transmission
- **Text Message Support**: Compressed text and braille message transmission
- **User Settings**: Speed, mode, and configuration transmission
- **ESP32 Integration**: Complete ESP32 server implementation
- **Prototype Testing**: Dedicated WiFi test interface for rapid development

### 🎨 Modern UI/UX
- **Jetpack Compose**: Built with the latest Android UI toolkit
- **Material Design 3**: Modern, accessible design system
- **Responsive Layout**: Optimized for various screen sizes
- **Accessibility Features**: Enhanced accessibility for users with visual impairments
- **Haptic Feedback**: Tactile feedback for braille input

## 🚀 How It Works

### Voice User Mode
1. Toggle to "Voice User" mode
2. Tap the microphone button to start speech recognition
3. Speak your message clearly
4. Real-time status feedback shows recognition progress:
   - **Starting**: Initializing speech recognition
   - **Ready**: Ready to listen for speech
   - **Listening**: Actively listening to your voice
   - **Processing**: Converting speech to text
   - **Success**: Speech successfully recognized
   - **Error**: Recognition issues with helpful error messages
5. Recognized text appears as a blue message bubble
6. Braille translation is automatically generated and displayed

### Text User Mode
1. Toggle to "Text User" mode
2. Type your message in the text field
3. Tap the send button
4. Text appears as a gray message bubble
5. Message is automatically played back using TTS

### Braille Input Mode
1. Toggle to "Braille Input" mode
2. Use the 6-dot braille grid to input characters:
   - Touch dots 1-6 to create braille patterns
   - Visual feedback shows active dots
   - Complete pattern by tapping "Complete" button
3. Real-time character recognition and audio feedback
4. Seamless integration with chat system

### WiFi Prototype Testing (NEW!)
1. Tap "WiFi 프로토타입 테스트" button
2. Enter ESP32 IP address and port (default: 8080)
3. Tap "연결" to connect to ESP32 server
4. Test real-time braille dot transmission:
   - Tap numbered buttons (1-6) to send dot touch events
   - Tap "패턴 완성" to send complete braille pattern
   - Tap "초기화" to clear all dots
5. Test text message transmission:
   - Tap "텍스트 전송" to send sample message
   - Tap "설정 전송" to send user settings
6. Monitor real-time communication logs

## 🛠️ Technical Architecture

### Core Technologies
- **Platform**: Android API 30+ (Android 11+)
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with StateFlow and Coroutines
- **Build System**: Gradle 8.10.1

### Key Libraries
- **liblouis-java**: 5.1.0 - Professional braille translation
- **JNA**: 5.17.0 - Native library access for liblouis
- **AndroidX Compose BOM**: 2024.09.00 - Latest Compose components
- **Lifecycle**: 2.7.0 - ViewModel and state management

### Project Structure
```
app/src/main/java/com/example/chatting_app/
├── MainActivity.kt              # App entry point with permission handling
├── ChatScreen.kt               # Main chat UI composable
├── ChatViewModel.kt            # Business logic and state management
├── ChatMessage.kt              # Message data class
├── BrailleConverter.kt         # Braille conversion wrapper
├── BrailleConverterJava.kt     # liblouis-java integration
├── BrailleInputGrid.kt         # 6-dot braille input UI
├── BrailleInputManager.kt      # Braille input logic
├── BraillePatternConverter.kt  # Braille pattern recognition
└── ui/theme/                   # Material Design 3 theming
```

## 📱 Setup and Installation

### Prerequisites
- Android Studio (latest version)
- Android SDK 35
- Android device with API 30+ (Android 11+)
- Microphone access
- Internet connection for TTS
- ESP32 development board (for WiFi prototype testing)

### Installation Steps
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run

### ESP32 Setup (WiFi Prototype)
1. Install Arduino IDE with ESP32 support
2. Open `esp32_wifi_server.ino` in Arduino IDE
3. Update WiFi credentials in the code:
   ```cpp
   const char* ssid = "YOUR_WIFI_SSID";
   const char* password = "YOUR_WIFI_PASSWORD";
   ```
4. Upload the code to your ESP32
5. Note the IP address displayed in Serial Monitor
6. Use this IP address in the Android app's WiFi test interface on an Android device
5. Grant necessary permissions when prompted

### Required Permissions
- `RECORD_AUDIO`: For speech recognition functionality
- `INTERNET`: For TTS engine access
- `WRITE_EXTERNAL_STORAGE`: For braille table files
- `READ_EXTERNAL_STORAGE`: For braille table files

## 🎯 Current Implementation Status

### ✅ Completed Features
- [x] Advanced speech recognition with status management
- [x] Text-to-speech integration
- [x] liblouis-java braille translation
- [x] 6-dot braille input grid
- [x] Real-time braille pattern recognition
- [x] Interactive braille input with audio feedback
- [x] Modern Material Design 3 UI
- [x] MVVM architecture with StateFlow
- [x] Comprehensive error handling
- [x] Runtime permission management

### 🔄 In Progress
- [ ] Braille table optimization
- [ ] Enhanced accessibility features
- [ ] Performance optimizations

### 📋 Planned Features
- [ ] Message persistence with Room Database
- [ ] Multiple language support
- [ ] Voice message playback
- [ ] Advanced braille table management
- [ ] Dark mode support
- [ ] Export/import chat history

## 🧪 Testing

### Unit Tests
- ViewModel business logic testing
- Braille conversion testing
- Pattern recognition testing

### UI Tests
- Compose component testing
- Braille input interaction testing
- Accessibility testing

### Integration Tests
- End-to-end chat flow testing
- Voice recognition testing
- TTS functionality testing

## 🐍 Python Server Testing (NEW!)

### Quick Start
```bash
# Windows (PowerShell)
.\run_python_server.ps1

# Windows (Command Prompt)
run_python_server.bat

# Linux/Mac
python3 python_wifi_server.py
```

### Python Server Features
- **Arduino 호환**: ESP32 서버와 완전히 동일한 프로토콜
- **실시간 로깅**: 모든 메시지와 상태 변화를 터미널에 출력
- **점자 패턴 표시**: 현재 점자 상태를 실시간으로 확인
- **체크섬 검증**: 데이터 무결성 보장
- **멀티 클라이언트**: 여러 앱 연결 지원

### 테스트 시나리오
1. **Python 서버 실행**: `python python_wifi_server.py`
2. **앱에서 WiFi 테스트**: MainActivity → "WiFi 테스트" 버튼
3. **터미널에서 확인**: 점자 터치, 텍스트, 설정 변경 로그 확인

## 🔧 Development

### Building the Project
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

### Code Style
- Follow Kotlin coding conventions
- Apply Compose guidelines
- Use Material Design 3 components
- Maintain MVVM architecture principles

## 📚 Documentation

- [Development Instructions](DEVELOPMENT_INSTRUCTIONS.md) - Comprehensive development guide
- [Braille Input Implementation](BRAILLE_INPUT_IMPLEMENTATION.md) - Detailed braille feature documentation

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Follow the coding standards
4. Add tests for new features
5. Submit a pull request

## 📄 License

This project uses the following open-source libraries:
- **liblouis**: LGPL 2.1
- **JNA**: Apache 2.0
- **AndroidX**: Apache 2.0

## 🆘 Support

For issues and questions:
1. Check the documentation files
2. Review the development instructions
3. Open an issue on GitHub

---

**Note**: This app is designed with accessibility in mind and supports multiple input methods to accommodate users with different needs and preferences.
