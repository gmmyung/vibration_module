# OFBGlove Web Visualizer

WiFi를 통해 실시간 점자 패턴을 시각화하는 웹 애플리케이션입니다.

## 🚀 빠른 시작

```bash
# 의존성 설치
npm install

# 웹 서버와 TCP/WebSocket 서버 동시 실행
npm run dev
```

## 📱 주요 기능

- **실시간 점자 시각화**: 2×3 점자 셀 실시간 표시
- **문장 시퀀스**: 실시간 입력을 문장 단위로 그룹화
- **재생 제어**: 다양한 재생 모드와 속도 조절
- **데이터 수집**: 점자 패턴 통계 및 내보내기
- **연결 모니터링**: WebSocket 연결 상태 실시간 확인

## 🔧 포트 설정

- **웹 서버**: 3000번 포트 (React)
- **TCP 서버**: 8888번 포트 (ESP32/Android 앱용)
- **WebSocket 서버**: 8889번 포트 (웹 클라이언트용)

## 📁 주요 파일

- `src/App.tsx` - 메인 애플리케이션
- `src/components/BrailleCell.tsx` - 점자 셀 컴포넌트
- `src/server/brailleServer.js` - 통합 서버 (TCP + WebSocket)
- `src/services/WebSocketService.ts` - WebSocket 통신