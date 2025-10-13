// 점자 관련 타입 정의
export interface BrailleDot {
  id: number;        // 1-6번 점
  isActive: boolean; // 활성화 상태
}

export interface BraillePattern {
  dots: BrailleDot[];
  timestamp: number;
  source: 'esp32' | 'web' | 'demo';
  character?: string; // 변환된 문자
  binary?: string;    // 이진 패턴 (예: "100110")
}

export interface BrailleData {
  patterns: BraillePattern[];
  totalCount: number;
  lastUpdated: number;
}
