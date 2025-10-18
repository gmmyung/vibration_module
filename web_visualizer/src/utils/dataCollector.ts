// 데이터 수집 및 저장 유틸리티
import { BraillePattern, BrailleData } from '../types/braille';

export class DataCollector {
  private data: BrailleData = {
    patterns: [],
    totalCount: 0,
    lastUpdated: Date.now()
  };
  
  private maxPatterns = 1000; // 최대 저장 패턴 수
  private onDataUpdate?: (data: BrailleData) => void;

  constructor(onDataUpdate?: (data: BrailleData) => void) {
    this.onDataUpdate = onDataUpdate;
  }

  // 새로운 점자 패턴 추가
  addPattern(pattern: BraillePattern): void {
    this.data.patterns.push(pattern);
    this.data.totalCount++;
    this.data.lastUpdated = Date.now();

    // 최대 패턴 수 초과 시 오래된 패턴 제거
    if (this.data.patterns.length > this.maxPatterns) {
      this.data.patterns = this.data.patterns.slice(-this.maxPatterns);
    }

    // 데이터 업데이트 콜백 호출
    this.onDataUpdate?.(this.data);
    
    console.log('새 패턴 추가:', {
      pattern: pattern.dots.map(d => d.isActive ? '1' : '0').join(''),
      character: pattern.character,
      totalCount: this.data.totalCount
    });
  }

  // 현재 데이터 가져오기
  getData(): BrailleData {
    return { ...this.data };
  }

  // 특정 시간 범위의 패턴 가져오기
  getPatternsInRange(startTime: number, endTime: number): BraillePattern[] {
    return this.data.patterns.filter(
      pattern => pattern.timestamp >= startTime && pattern.timestamp <= endTime
    );
  }

  // 최근 N개 패턴 가져오기
  getRecentPatterns(count: number): BraillePattern[] {
    return this.data.patterns.slice(-count);
  }

  // 패턴 통계 계산
  getStatistics(): {
    totalPatterns: number;
    uniquePatterns: number;
    mostCommonPattern: string | null;
    averagePatternsPerMinute: number;
    lastActivity: number;
  } {
    const patterns = this.data.patterns;
    const uniquePatterns = new Set(patterns.map(p => p.dots.map(d => d.isActive ? '1' : '0').join('')));
    
    // 가장 많이 사용된 패턴 찾기
    const patternCounts = new Map<string, number>();
    patterns.forEach(p => {
      const pattern = p.dots.map(d => d.isActive ? '1' : '0').join('');
      patternCounts.set(pattern, (patternCounts.get(pattern) || 0) + 1);
    });
    
    let mostCommonPattern: string | null = null;
    let maxCount = 0;
    patternCounts.forEach((count, pattern) => {
      if (count > maxCount) {
        maxCount = count;
        mostCommonPattern = pattern;
      }
    });

    // 분당 평균 패턴 수 계산
    const timeSpan = this.data.lastUpdated - (patterns[0]?.timestamp || this.data.lastUpdated);
    const averagePatternsPerMinute = timeSpan > 0 ? (patterns.length / (timeSpan / 60000)) : 0;

    return {
      totalPatterns: patterns.length,
      uniquePatterns: uniquePatterns.size,
      mostCommonPattern,
      averagePatternsPerMinute: Math.round(averagePatternsPerMinute * 100) / 100,
      lastActivity: this.data.lastUpdated
    };
  }

  // 데이터 초기화
  clearData(): void {
    this.data = {
      patterns: [],
      totalCount: 0,
      lastUpdated: Date.now()
    };
    this.onDataUpdate?.(this.data);
  }

  // 데이터 내보내기 (JSON)
  exportData(): string {
    return JSON.stringify(this.data, null, 2);
  }

  // 데이터 가져오기 (JSON)
  importData(jsonData: string): boolean {
    try {
      const importedData = JSON.parse(jsonData) as BrailleData;
      this.data = importedData;
      this.onDataUpdate?.(this.data);
      return true;
    } catch (error) {
      console.error('데이터 가져오기 실패:', error);
      return false;
    }
  }
}
