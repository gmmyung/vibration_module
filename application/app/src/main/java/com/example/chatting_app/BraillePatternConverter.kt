package com.example.chatting_app

/**
 * 점자 패턴을 문자로 변환하는 클래스
 * 6점 점자 시스템을 사용하여 점의 조합을 문자로 매핑
 */
class BraillePatternConverter {
    
    /**
     * 6개 점의 위치: 1 4
     *                2 5  
     *                3 6
     */
    
    // 영문 점자 패턴 매핑 (기본적인 문자들)
    private val braillePatterns = mapOf(
        // 영문 알파벳 A-Z
        "1" to 'a',      // 1번 점
        "12" to 'b',     // 1, 2번 점
        "14" to 'c',     // 1, 4번 점
        "145" to 'd',    // 1, 4, 5번 점
        "15" to 'e',     // 1, 5번 점
        "124" to 'f',    // 1, 2, 4번 점
        "1245" to 'g',   // 1, 2, 4, 5번 점
        "125" to 'h',    // 1, 2, 5번 점
        "24" to 'i',     // 2, 4번 점
        "245" to 'j',    // 2, 4, 5번 점
        "13" to 'k',     // 1, 3번 점
        "123" to 'l',    // 1, 2, 3번 점
        "134" to 'm',    // 1, 3, 4번 점
        "1345" to 'n',   // 1, 3, 4, 5번 점
        "135" to 'o',    // 1, 3, 5번 점
        "1234" to 'p',   // 1, 2, 3, 4번 점
        "12345" to 'q',  // 1, 2, 3, 4, 5번 점
        "1235" to 'r',   // 1, 2, 3, 5번 점
        "234" to 's',    // 2, 3, 4번 점
        "2345" to 't',   // 2, 3, 4, 5번 점
        "136" to 'u',    // 1, 3, 6번 점
        "1236" to 'v',   // 1, 2, 3, 6번 점
        "2456" to 'w',   // 2, 4, 5, 6번 점
        "1346" to 'x',   // 1, 3, 4, 6번 점
        "13456" to 'y',  // 1, 3, 4, 5, 6번 점
        "1356" to 'z',   // 1, 3, 5, 6번 점
        
        // 숫자 0-9
        "245" to '0',    // 2, 4, 5번 점 (J와 동일)
        "1" to '1',      // 1번 점 (A와 동일)
        "12" to '2',     // 1, 2번 점 (B와 동일)
        "14" to '3',     // 1, 4번 점 (C와 동일)
        "145" to '4',    // 1, 4, 5번 점 (D와 동일)
        "15" to '5',     // 1, 5번 점 (E와 동일)
        "124" to '6',    // 1, 2, 4번 점 (F와 동일)
        "1245" to '7',   // 1, 2, 4, 5번 점 (G와 동일)
        "125" to '8',    // 1, 2, 5번 점 (H와 동일)
        "24" to '9',     // 2, 4번 점 (I와 동일)
        
        // 기본 기호
        "2" to '.',      // 2번 점 (마침표)
        "6" to ',',      // 6번 점 (쉼표)
        "235" to '!',    // 2, 3, 5번 점 (느낌표)
        "236" to '?',    // 2, 3, 6번 점 (물음표)
        "3" to ';',      // 3번 점 (세미콜론)
        "23" to ':',     // 2, 3번 점 (콜론)
        "256" to '-',    // 2, 5, 6번 점 (하이픈)
        "36" to '"',     // 3, 6번 점 (따옴표)
        "2356" to '\'',  // 2, 3, 5, 6번 점 (작은따옴표)
        
        // 공백
        "" to ' ',       // 점이 없는 경우 공백
    )
    
    /**
     * 6개 점의 상태를 패턴 문자열로 변환
     * @param dots 6개 점의 활성화 상태 (true = 활성화, false = 비활성화)
     * @return 점의 번호들을 문자열로 반환 (예: "125")
     */
    fun dotsToPattern(dots: List<Boolean>): String {
        if (dots.size != 6) {
            throw IllegalArgumentException("점자 점은 정확히 6개여야 합니다")
        }
        
        val activeDots = mutableListOf<Int>()
        for (i in dots.indices) {
            if (dots[i]) {
                activeDots.add(i + 1) // 1부터 시작하는 번호로 변환
            }
        }
        
        return activeDots.sorted().joinToString("")
    }
    
    /**
     * 점자 패턴을 문자로 변환
     * @param pattern 점자 패턴 문자열 (예: "125")
     * @return 해당하는 문자 또는 null (패턴이 없을 경우)
     */
    fun patternToCharacter(pattern: String): Char? {
        return braillePatterns[pattern]
    }
    
    /**
     * 점자 패턴이 완성된 패턴인지 확인
     * @param pattern 점자 패턴 문자열
     * @return 완성된 패턴이면 true
     */
    fun isCompletePattern(pattern: String): Boolean {
        return braillePatterns.containsKey(pattern)
    }
    
    /**
     * 점자 패턴이 유효한지 검증
     * @param pattern 점자 패턴 문자열
     * @return 유효한 패턴이면 true
     */
    fun validatePattern(pattern: String): Boolean {
        // 빈 패턴은 공백으로 유효
        if (pattern.isEmpty()) return true
        
        // 1-6 범위의 숫자들로만 구성되어야 함
        val validChars = pattern.all { it.isDigit() && it in '1'..'6' }
        if (!validChars) return false
        
        // 중복된 숫자가 없어야 함
        val uniqueChars = pattern.toSet().size == pattern.length
        if (!uniqueChars) return false
        
        return true
    }
    
    /**
     * 사용 가능한 모든 점자 패턴 반환
     * @return 패턴과 문자의 맵
     */
    fun getAllPatterns(): Map<String, Char> {
        return braillePatterns.toMap()
    }
    
    /**
     * 특정 문자에 해당하는 점자 패턴 찾기
     * @param character 찾을 문자
     * @return 해당하는 점자 패턴 또는 null
     */
    fun characterToPattern(character: Char): String? {
        return braillePatterns.entries.find { it.value == character }?.key
    }
    
    /**
     * 점자 패턴을 시각적으로 표시하는 문자열 생성
     * @param pattern 점자 패턴 문자열
     * @return 시각적 표현 (● = 활성화, ○ = 비활성화)
     */
    fun patternToVisual(pattern: String): String {
        val dots = BooleanArray(6) { false }
        
        for (char in pattern) {
            if (char.isDigit() && char in '1'..'6') {
                dots[char.digitToInt() - 1] = true
            }
        }
        
        return buildString {
            // 1 4
            append(if (dots[0]) "●" else "○")
            append(" ")
            append(if (dots[3]) "●" else "○")
            append("\n")
            
            // 2 5
            append(if (dots[1]) "●" else "○")
            append(" ")
            append(if (dots[4]) "●" else "○")
            append("\n")
            
            // 3 6
            append(if (dots[2]) "●" else "○")
            append(" ")
            append(if (dots[5]) "●" else "○")
        }
    }
}
