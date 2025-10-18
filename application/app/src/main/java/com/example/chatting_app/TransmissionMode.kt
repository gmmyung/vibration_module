package com.example.chatting_app

/**
 * 전송 모드 정의
 */
enum class TransmissionMode {
    PATTERN,    // 패턴 모드 (기존): 100100 전체를 한 번에 전송
    DOT_BY_DOT  // 개별 점 모드 (신규): 100100을 000100, 100000으로 분할 전송
}
