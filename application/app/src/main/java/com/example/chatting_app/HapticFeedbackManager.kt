package com.example.chatting_app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * 햅틱 피드백을 관리하는 클래스
 * 점자 점의 위치에 따라 다른 진동 패턴을 제공
 */
class HapticFeedbackManager(private val context: Context) {
    
    private val vibrator: Vibrator? = getVibrator()
    private var isEnabled = true
    
    /**
     * 진동 패턴 정의
     * 모든 점자 점에 동일한 진동 패턴 적용
     */
    companion object {
        // 통일된 점자 점 진동 패턴 (밀리초 단위)
        // 패턴: [진동 시간, 대기 시간, 진동 시간, ...]
        private val UNIFIED_DOT_PATTERN = longArrayOf(50, 20, 50)  // 짧은 진동 2번
        
        // 진동 강도 (0.0 ~ 1.0)
        private const val VIBRATION_AMPLITUDE = 0.7f
        
        // 기본 진동 시간 (밀리초)
        private const val DEFAULT_VIBRATION_DURATION = 50L
    }
    
    /**
     * Vibrator 인스턴스 가져오기 (Android 버전별 호환성)
     */
    private fun getVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (e: Exception) {
            Log.e("HapticFeedbackManager", "Vibrator 초기화 실패", e)
            null
        }
    }
    
    /**
     * 점자 점 터치 시 햅틱 피드백 제공
     * @param dotIndex 터치한 점의 인덱스 (0-5)
     * @param isActivated 점이 활성화되었는지 여부
     */
    fun provideDotFeedback(dotIndex: Int, isActivated: Boolean) {
        if (!isEnabled || vibrator == null) {
            Log.d("HapticFeedbackManager", "햅틱 피드백 비활성화됨")
            return
        }
        
        if (dotIndex !in 0..5) {
            Log.w("HapticFeedbackManager", "잘못된 점 인덱스: $dotIndex")
            return
        }
        
        try {
            // 모든 점에 동일한 진동 패턴 적용
            val vibrationPattern = if (isActivated) {
                // 활성화: 기본 패턴
                UNIFIED_DOT_PATTERN
            } else {
                // 비활성화: 더 짧고 약한 패턴
                UNIFIED_DOT_PATTERN.map { it / 2 }.toLongArray()
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0 이상: VibrationEffect 사용
                val effect = VibrationEffect.createWaveform(
                    vibrationPattern,
                    -1 // 반복하지 않음
                )
                vibrator.vibrate(effect)
            } else {
                // Android 8.0 미만: 기존 방식 사용
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationPattern, -1)
            }
            
            Log.d("HapticFeedbackManager", "점자 점 ${dotIndex + 1}번 햅틱 피드백: ${if (isActivated) "활성화" else "비활성화"}")
            
        } catch (e: Exception) {
            Log.e("HapticFeedbackManager", "햅틱 피드백 제공 실패", e)
        }
    }
    
    /**
     * 점자 패턴 완성 시 햅틱 피드백 제공
     */
    fun providePatternCompleteFeedback() {
        if (!isEnabled || vibrator == null) return
        
        try {
            // 패턴 완성 시 특별한 진동 패턴 (긴 진동 + 짧은 진동 + 긴 진동)
            val pattern = longArrayOf(200, 50, 100, 50, 200)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            
            Log.d("HapticFeedbackManager", "점자 패턴 완성 햅틱 피드백 제공")
            
        } catch (e: Exception) {
            Log.e("HapticFeedbackManager", "패턴 완성 햅틱 피드백 실패", e)
        }
    }
    
    /**
     * 점자 입력 오류 시 햅틱 피드백 제공
     */
    fun provideErrorFeedback() {
        if (!isEnabled || vibrator == null) return
        
        try {
            // 오류 시 진동 패턴 (빠른 진동 3번)
            val pattern = longArrayOf(100, 50, 100, 50, 100)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            
            Log.d("HapticFeedbackManager", "오류 햅틱 피드백 제공")
            
        } catch (e: Exception) {
            Log.e("HapticFeedbackManager", "오류 햅틱 피드백 실패", e)
        }
    }
    
    /**
     * 점자 문자 변환 성공 시 햅틱 피드백 제공
     */
    fun provideSuccessFeedback() {
        if (!isEnabled || vibrator == null) return
        
        try {
            // 성공 시 진동 패턴 (점진적으로 강해지는 진동)
            val pattern = longArrayOf(50, 30, 100, 30, 150)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            
            Log.d("HapticFeedbackManager", "성공 햅틱 피드백 제공")
            
        } catch (e: Exception) {
            Log.e("HapticFeedbackManager", "성공 햅틱 피드백 실패", e)
        }
    }
    
    /**
     * 햅틱 피드백 활성화/비활성화
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d("HapticFeedbackManager", "햅틱 피드백 ${if (enabled) "활성화" else "비활성화"}")
    }
    
    /**
     * 햅틱 피드백 활성화 상태 확인
     */
    fun isHapticEnabled(): Boolean {
        return isEnabled && vibrator != null
    }
    
    /**
     * 현재 진동 패턴 테스트
     */
    fun testVibrationPattern(dotIndex: Int) {
        if (dotIndex in 0..5) {
            provideDotFeedback(dotIndex, true)
        }
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("HapticFeedbackManager", "진동 취소 실패", e)
        }
    }
}
