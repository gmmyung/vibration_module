# 🌐 OFBGlove 웹 비주얼라이저 개발 가이드

## 📋 프로젝트 개요

OFBGlove의 실제 장갑에서 표현될 점자를 실시간으로 비주얼라이즈하는 웹 애플리케이션입니다. 데모 및 시연 목적으로 개발되며, WiFi와 BLE 통신을 지원합니다.

## 🎯 핵심 요구사항

### 1. 시각적 표현
- **OFBGlove_image.png** 이미지를 배경으로 표시
- 이미지 위에 **6개 점자 점**을 실시간으로 표시
- 점자 점은 들어오는 신호에 따라 **켜졌다/꺼졌다** 함
- **디자인이 잘 되어야 함** (현대적이고 직관적인 UI)

### 2. 통신 지원
- **WiFi 통신**: WebSocket을 통한 실시간 통신
- **BLE 통신**: Bluetooth Low Energy 지원 (향후)
- **실시간 업데이트**: 장갑에서 오는 신호를 즉시 반영

### 3. 기술 스택
- **React** + **TypeScript**
- **매우 간단한 페이지** 구조
- **반응형 디자인** 지원

## 🏗️ 프로젝트 구조

```
web_visualizer/
├── public/
│   ├── index.html
│   └── OFBGlove_image.png          # 장갑 이미지
├── src/
│   ├── components/
│   │   ├── BrailleDot.tsx          # 개별 점자 점 컴포넌트
│   │   ├── BrailleGrid.tsx         # 6개 점자 점 그리드
│   │   ├── BrailleCell.tsx         # 실제 점자 셀 (2×3 그리드)
│   │   ├── ConnectionStatus.tsx    # 연결 상태 표시
│   │   ├── LayoutController.tsx    # 레이아웃 제어 컴포넌트
│   │   ├── InfoPanel.tsx           # 정보 패널 (컴팩트)
│   │   └── OFBGloveVisualizer.tsx  # 메인 비주얼라이저
│   ├── services/
│   │   ├── WebSocketService.ts     # WebSocket 통신 서비스
│   │   └── BrailleProtocol.ts      # 점자 프로토콜 처리
│   ├── types/
│   │   ├── braille.ts              # 점자 관련 타입 정의
│   │   └── layout.ts               # 레이아웃 설정 타입
│   ├── hooks/
│   │   ├── useWebSocket.ts         # WebSocket 커스텀 훅
│   │   └── useLayout.ts            # 레이아웃 관리 훅
│   ├── utils/
│   │   ├── brailleUtils.ts         # 점자 유틸리티 함수
│   │   └── layoutUtils.ts          # 레이아웃 유틸리티 함수
│   ├── config/
│   │   └── layoutConfig.ts         # 레이아웃 설정 파일
│   ├── styles/
│   │   ├── globals.css
│   │   ├── components.css
│   │   └── responsive.css
│   ├── App.tsx
│   └── index.tsx
├── package.json
├── tsconfig.json
└── README.md
```

## 🔧 기술 구현 세부사항

### 1. 점자 점 표현 방식

```typescript
// types/braille.ts
export interface BrailleDot {
  id: number;        // 1-6번 점
  isActive: boolean; // 활성화 상태
  position: {        // 화면상 위치 (이미지 기준 상대 좌표)
    x: number;       // 이미지 내부 상대 위치 (0-1)
    y: number;       // 이미지 내부 상대 위치 (0-1)
  };
}

export interface BraillePattern {
  dots: BrailleDot[];
  timestamp: number;
  source: 'esp32' | 'web' | 'demo';
}

// types/layout.ts
export interface LayoutConfig {
  orientation: 'horizontal' | 'vertical' | 'auto';
  imageSize: 'small' | 'medium' | 'large' | 'full';
  infoPanelPosition: 'right' | 'bottom' | 'overlay' | 'hidden';
  dotSize: 'small' | 'medium' | 'large';
  dotSpacing: number; // 점 간격 배율
  showInfo: boolean;
  showConnectionStatus: boolean;
  showBrailleCell: boolean; // 실제 점자 셀 표시 여부
  brailleCellPosition: 'left' | 'right' | 'top' | 'bottom'; // 점자 셀 위치
}

export interface DotPosition {
  x: number;  // 이미지 기준 상대 위치 (0-1)
  y: number;  // 이미지 기준 상대 위치 (0-1)
}
```

### 2. WebSocket 통신 프로토콜

```typescript
// services/WebSocketService.ts
export class WebSocketService {
  private ws: WebSocket | null = null;
  private url: string;
  
  constructor(url: string = 'ws://localhost:8000/ws') {
    this.url = url;
  }
  
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.url);
      
      this.ws.onopen = () => {
        console.log('WebSocket 연결됨');
        resolve();
      };
      
      this.ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        this.handleMessage(data);
      };
      
      this.ws.onerror = (error) => {
        console.error('WebSocket 오류:', error);
        reject(error);
      };
    });
  }
  
  private handleMessage(data: any) {
    switch (data.type) {
      case 'braille_pattern':
        this.onBraillePattern?.(data);
        break;
      case 'connected':
        this.onConnected?.(data);
        break;
      case 'status':
        this.onStatus?.(data);
        break;
    }
  }
  
  // 이벤트 핸들러들
  onBraillePattern?: (data: any) => void;
  onConnected?: (data: any) => void;
  onStatus?: (data: any) => void;
}
```

### 3. 점자 점 컴포넌트

```typescript
// components/BrailleDot.tsx
import React from 'react';
import './BrailleDot.css';

interface BrailleDotProps {
  id: number;
  isActive: boolean;
  position: { x: number; y: number };
  onClick?: (id: number) => void;
}

export const BrailleDot: React.FC<BrailleDotProps> = ({
  id,
  isActive,
  position,
  onClick
}) => {
  return (
    <div
      className={`braille-dot ${isActive ? 'active' : 'inactive'}`}
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
      }}
      onClick={() => onClick?.(id)}
      data-dot-id={id}
    >
      <div className="dot-inner" />
    </div>
  );
};
```

### 4. 레이아웃 설정 파일

```typescript
// config/layoutConfig.ts
import { LayoutConfig } from '../types/layout';

export const defaultLayoutConfig: LayoutConfig = {
  orientation: 'auto',
  imageSize: 'large',
  infoPanelPosition: 'overlay',
  dotSize: 'medium',
  dotSpacing: 1.0,
  showInfo: true,
  showConnectionStatus: true,
  showBrailleCell: true,
  brailleCellPosition: 'left',
};

export const layoutPresets = {
  compact: {
    orientation: 'horizontal' as const,
    imageSize: 'medium' as const,
    infoPanelPosition: 'right' as const,
    dotSize: 'small' as const,
    dotSpacing: 0.8,
    showInfo: true,
    showConnectionStatus: true,
    showBrailleCell: true,
    brailleCellPosition: 'left' as const,
  },
  fullscreen: {
    orientation: 'vertical' as const,
    imageSize: 'full' as const,
    infoPanelPosition: 'bottom' as const,
    dotSize: 'large' as const,
    dotSpacing: 1.2,
    showInfo: true,
    showConnectionStatus: false,
    showBrailleCell: true,
    brailleCellPosition: 'top' as const,
  },
  demo: {
    orientation: 'horizontal' as const,
    imageSize: 'large' as const,
    infoPanelPosition: 'overlay' as const,
    dotSize: 'medium' as const,
    dotSpacing: 1.0,
    showInfo: true,
    showConnectionStatus: true,
    showBrailleCell: true,
    brailleCellPosition: 'left' as const,
  }
};

// 점자 점 위치 설정 (이미지 기준 상대 좌표)
export const dotPositions = {
  // 표준 6점 점자 배치 (이미지 내부 상대 위치)
  standard: [
    { x: 0.3, y: 0.2 },  // 1번 점 (왼쪽 위)
    { x: 0.3, y: 0.4 },  // 2번 점 (왼쪽 중간)
    { x: 0.3, y: 0.6 },  // 3번 점 (왼쪽 아래)
    { x: 0.7, y: 0.2 },  // 4번 점 (오른쪽 위)
    { x: 0.7, y: 0.4 },  // 5번 점 (오른쪽 중간)
    { x: 0.7, y: 0.6 },  // 6번 점 (오른쪽 아래)
  ],
  // 장갑 이미지에 맞춘 커스텀 배치
  glove: [
    { x: 0.25, y: 0.15 }, // 1번 점
    { x: 0.25, y: 0.35 }, // 2번 점
    { x: 0.25, y: 0.55 }, // 3번 점
    { x: 0.75, y: 0.15 }, // 4번 점
    { x: 0.75, y: 0.35 }, // 5번 점
    { x: 0.75, y: 0.55 }, // 6번 점
  ]
};
```

### 5. 레이아웃 관리 훅

```typescript
// hooks/useLayout.ts
import { useState, useEffect } from 'react';
import { LayoutConfig } from '../types/layout';
import { defaultLayoutConfig, layoutPresets } from '../config/layoutConfig';

export const useLayout = () => {
  const [layoutConfig, setLayoutConfig] = useState<LayoutConfig>(defaultLayoutConfig);
  const [windowSize, setWindowSize] = useState({ width: 0, height: 0 });

  useEffect(() => {
    const updateWindowSize = () => {
      setWindowSize({ width: window.innerWidth, height: window.innerHeight });
    };

    updateWindowSize();
    window.addEventListener('resize', updateWindowSize);
    return () => window.removeEventListener('resize', updateWindowSize);
  }, []);

  // 자동 방향 감지
  useEffect(() => {
    if (layoutConfig.orientation === 'auto') {
      const isLandscape = windowSize.width > windowSize.height;
      setLayoutConfig(prev => ({
        ...prev,
        orientation: isLandscape ? 'horizontal' : 'vertical'
      }));
    }
  }, [windowSize, layoutConfig.orientation]);

  const applyPreset = (presetName: keyof typeof layoutPresets) => {
    setLayoutConfig(layoutPresets[presetName]);
  };

  const updateConfig = (updates: Partial<LayoutConfig>) => {
    setLayoutConfig(prev => ({ ...prev, ...updates }));
  };

  return {
    layoutConfig,
    windowSize,
    applyPreset,
    updateConfig,
    isLandscape: windowSize.width > windowSize.height,
    isMobile: windowSize.width < 768,
  };
};
```

### 6. 메인 비주얼라이저 컴포넌트 (개선된 버전)

```typescript
// components/OFBGloveVisualizer.tsx
import React, { useState, useEffect } from 'react';
import { BrailleGrid } from './BrailleGrid';
import { BrailleCell } from './BrailleCell';
import { ConnectionStatus } from './ConnectionStatus';
import { LayoutController } from './LayoutController';
import { InfoPanel } from './InfoPanel';
import { WebSocketService } from '../services/WebSocketService';
import { BraillePattern } from '../types/braille';
import { useLayout } from '../hooks/useLayout';
import { dotPositions } from '../config/layoutConfig';
import './OFBGloveVisualizer.css';

export const OFBGloveVisualizer: React.FC = () => {
  const [braillePattern, setBraillePattern] = useState<BraillePattern | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [wsService, setWsService] = useState<WebSocketService | null>(null);
  const { layoutConfig, windowSize, applyPreset, updateConfig } = useLayout();

  useEffect(() => {
    const service = new WebSocketService();
    
    service.onBraillePattern = (data) => {
      setBraillePattern({
        dots: data.pattern.map((isActive: boolean, index: number) => ({
          id: index + 1,
          isActive,
          position: getDotPosition(index + 1)
        })),
        timestamp: data.timestamp,
        source: data.source || 'esp32'
      });
    };
    
    service.onConnected = () => {
      setIsConnected(true);
    };
    
    service.connect().catch(console.error);
    setWsService(service);
    
    return () => {
      service.ws?.close();
    };
  }, []);

  const getDotPosition = (dotId: number) => {
    // 설정에 따라 점 위치 결정
    const positions = dotPositions.glove; // 또는 dotPositions.standard
    return positions[dotId - 1];
  };

  const getLayoutClass = () => {
    const classes = ['ofbglove-visualizer'];
    classes.push(`orientation-${layoutConfig.orientation}`);
    classes.push(`image-${layoutConfig.imageSize}`);
    classes.push(`info-${layoutConfig.infoPanelPosition}`);
    classes.push(`dots-${layoutConfig.dotSize}`);
    if (layoutConfig.showBrailleCell) {
      classes.push(`braille-cell-${layoutConfig.brailleCellPosition}`);
    }
    return classes.join(' ');
  };

  return (
    <div className={getLayoutClass()}>
      {/* 헤더 - 컴팩트하게 */}
      <div className="header">
        <h1>OFBGlove 점자 비주얼라이저</h1>
        {layoutConfig.showConnectionStatus && (
          <ConnectionStatus isConnected={isConnected} />
        )}
        <LayoutController 
          layoutConfig={layoutConfig}
          onPresetChange={applyPreset}
          onConfigChange={updateConfig}
        />
      </div>
      
      {/* 메인 컨테이너 - 한 화면에 모든 요소 */}
      <div className="main-container">
        {/* 점자 셀 - 왼쪽에 표시 */}
        {layoutConfig.showBrailleCell && layoutConfig.brailleCellPosition === 'left' && (
          <div className="braille-cell-container left">
            <BrailleCell 
              dots={braillePattern?.dots || []}
              size={layoutConfig.dotSize}
              showLabels={true}
              onDotClick={(id) => console.log(`점자 셀 점 ${id} 클릭됨`)}
            />
          </div>
        )}
        
        {/* 장갑 이미지 컨테이너 */}
        <div className="glove-image-container">
          <img 
            src="/OFBGlove_image.png" 
            alt="OFBGlove 장갑" 
            className="glove-image"
          />
          {braillePattern && (
            <BrailleGrid 
              dots={braillePattern.dots}
              dotSize={layoutConfig.dotSize}
              dotSpacing={layoutConfig.dotSpacing}
              onDotClick={(id) => console.log(`장갑 점 ${id} 클릭됨`)}
            />
          )}
        </div>
        
        {/* 점자 셀 - 오른쪽에 표시 */}
        {layoutConfig.showBrailleCell && layoutConfig.brailleCellPosition === 'right' && (
          <div className="braille-cell-container right">
            <BrailleCell 
              dots={braillePattern?.dots || []}
              size={layoutConfig.dotSize}
              showLabels={true}
              onDotClick={(id) => console.log(`점자 셀 점 ${id} 클릭됨`)}
            />
          </div>
        )}
        
        {/* 점자 셀 - 위에 표시 */}
        {layoutConfig.showBrailleCell && layoutConfig.brailleCellPosition === 'top' && (
          <div className="braille-cell-container top">
            <BrailleCell 
              dots={braillePattern?.dots || []}
              size={layoutConfig.dotSize}
              showLabels={true}
              onDotClick={(id) => console.log(`점자 셀 점 ${id} 클릭됨`)}
            />
          </div>
        )}
        
        {/* 점자 셀 - 아래에 표시 */}
        {layoutConfig.showBrailleCell && layoutConfig.brailleCellPosition === 'bottom' && (
          <div className="braille-cell-container bottom">
            <BrailleCell 
              dots={braillePattern?.dots || []}
              size={layoutConfig.dotSize}
              showLabels={true}
              onDotClick={(id) => console.log(`점자 셀 점 ${id} 클릭됨`)}
            />
          </div>
        )}
        
        {/* 정보 패널 */}
        {layoutConfig.showInfo && (
          <InfoPanel 
            braillePattern={braillePattern}
            position={layoutConfig.infoPanelPosition}
            isCompact={layoutConfig.infoPanelPosition === 'overlay'}
          />
        )}
      </div>
    </div>
  );
};

const getBrailleCharacter = (dots: any[]): string => {
  const pattern = dots.map(d => d.isActive ? '1' : '0').join('');
  const brailleMap: { [key: string]: string } = {
    "100000": "A", "110000": "B", "100100": "C", "100110": "D",
    "100010": "E", "110100": "F", "110110": "G", "110010": "H",
    "010100": "I", "010110": "J", "101000": "K", "111000": "L",
    "101100": "M", "101110": "N", "101010": "O", "111100": "P",
    "111110": "Q", "111010": "R", "011100": "S", "011110": "T",
    "101001": "U", "111001": "V", "010111": "W", "101101": "X",
    "101111": "Y", "101011": "Z", "000000": " "
  };
  return brailleMap[pattern] || "?";
};
```

## 🎨 스타일링 가이드

### 1. 점자 점 스타일 (이미지 위에 오버레이)

```css
/* components/BrailleDot.css */
.braille-dot {
  position: absolute;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10;
  transform: translate(-50%, -50%); /* 중앙 정렬 */
}

/* 점 크기 변형 */
.braille-dot.dots-small {
  width: 16px;
  height: 16px;
}

.braille-dot.dots-medium {
  width: 24px;
  height: 24px;
}

.braille-dot.dots-large {
  width: 32px;
  height: 32px;
}

.braille-dot.active {
  background: linear-gradient(45deg, #ff6b6b, #ff8e8e);
  box-shadow: 
    0 0 20px rgba(255, 107, 107, 0.8),
    inset 0 2px 4px rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%) scale(1.2);
}

.braille-dot.inactive {
  background: linear-gradient(45deg, #e0e0e0, #f5f5f5);
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.2),
    inset 0 1px 2px rgba(255, 255, 255, 0.8);
  transform: translate(-50%, -50%) scale(1);
}

.braille-dot:hover {
  transform: translate(-50%, -50%) scale(1.1);
}

.dot-inner {
  width: 40%;
  height: 40%;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
```

### 2. 메인 컨테이너 스타일 (한 화면 최적화)

```css
/* components/OFBGloveVisualizer.css */
.ofbglove-visualizer {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 헤더 - 컴팩트 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e0e0e0;
  z-index: 100;
  flex-shrink: 0;
}

.header h1 {
  color: #333;
  font-size: 1.5rem;
  margin: 0;
  font-weight: 600;
}

/* 메인 컨테이너 - 남은 공간 모두 사용 */
.main-container {
  flex: 1;
  display: flex;
  position: relative;
  overflow: hidden;
}

/* 장갑 이미지 컨테이너 - 이미지 위에 점자 점 오버레이 */
.glove-image-container {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
}

.glove-image {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* 점자 그리드 - 이미지 위에 절대 위치 */
.braille-grid {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none; /* 이미지 클릭 통과 */
}

.braille-grid .braille-dot {
  pointer-events: auto; /* 점자 점만 클릭 가능 */
}

/* 레이아웃별 스타일 */
.orientation-horizontal .main-container {
  flex-direction: row;
}

.orientation-vertical .main-container {
  flex-direction: column;
}

/* 이미지 크기별 스타일 */
.image-small .glove-image {
  max-width: 300px;
  max-height: 300px;
}

.image-medium .glove-image {
  max-width: 500px;
  max-height: 500px;
}

.image-large .glove-image {
  max-width: 700px;
  max-height: 700px;
}

.image-full .glove-image {
  max-width: 90%;
  max-height: 90%;
}

/* 점자 셀 위치별 스타일 */
.braille-cell-left .main-container {
  display: flex;
  align-items: center;
}

.braille-cell-right .main-container {
  display: flex;
  align-items: center;
}

.braille-cell-top .main-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.braille-cell-bottom .main-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 정보 패널 위치별 스타일 */
.info-right {
  width: 300px;
  background: white;
  border-left: 1px solid #e0e0e0;
  overflow-y: auto;
}

.info-bottom {
  height: 200px;
  background: white;
  border-top: 1px solid #e0e0e0;
  overflow-y: auto;
}

.info-overlay {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 300px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 50;
}

.info-hidden {
  display: none;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .header h1 {
    font-size: 1.2rem;
  }
  
  .orientation-horizontal .main-container {
    flex-direction: column;
  }
  
  .info-overlay {
    width: calc(100% - 40px);
    top: 10px;
    right: 10px;
    left: 10px;
  }
  
  .image-large .glove-image,
  .image-full .glove-image {
    max-width: 100%;
    max-height: 60vh;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 8px 15px;
  }
  
  .header h1 {
    font-size: 1rem;
  }
  
  .info-overlay {
    position: fixed;
    bottom: 0;
    top: auto;
    left: 0;
    right: 0;
    width: 100%;
    border-radius: 15px 15px 0 0;
    max-height: 50vh;
  }
}
```

### 3. 레이아웃 컨트롤러 스타일

```css
/* components/LayoutController.css */
.layout-controller {
  display: flex;
  gap: 10px;
  align-items: center;
}

.layout-preset-buttons {
  display: flex;
  gap: 5px;
}

.preset-btn {
  padding: 4px 8px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.preset-btn:hover {
  background: #f0f0f0;
}

.preset-btn.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.layout-settings {
  display: flex;
  gap: 10px;
  align-items: center;
}

.layout-settings select,
.layout-settings input {
  padding: 2px 6px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.8rem;
}

@media (max-width: 768px) {
  .layout-controller {
    flex-direction: column;
    gap: 5px;
  }
  
  .layout-settings {
    flex-wrap: wrap;
    gap: 5px;
  }
}
```

### 4. 정보 패널 스타일 (컴팩트)

```css
/* components/InfoPanel.css */
.info-panel {
  padding: 15px;
}

.info-panel h3 {
  color: #333;
  margin: 0 0 15px 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.pattern-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pattern-info p {
  margin: 0;
  padding: 6px 10px;
  background: #f8f9fa;
  border-radius: 6px;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  border-left: 3px solid #007bff;
}

.pattern-info .pattern-binary {
  font-size: 1.2rem;
  font-weight: bold;
  color: #007bff;
}

.pattern-info .pattern-character {
  font-size: 1.5rem;
  font-weight: bold;
  color: #28a745;
  text-align: center;
}

.pattern-info .pattern-source {
  font-size: 0.8rem;
  color: #6c757d;
}

.pattern-info .pattern-time {
  font-size: 0.8rem;
  color: #6c757d;
}

/* 컴팩트 모드 */
.info-panel.compact {
  padding: 10px;
}

.info-panel.compact h3 {
  font-size: 1rem;
  margin-bottom: 10px;
}

.info-panel.compact .pattern-info p {
  padding: 4px 8px;
  font-size: 0.8rem;
}

.info-panel.compact .pattern-info .pattern-character {
  font-size: 1.2rem;
}
```

### 5. 점자 셀 스타일

```css
/* components/BrailleCell.css */
.braille-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 15px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border: 2px solid #e0e0e0;
  min-width: 200px;
}

.braille-cell-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
  text-align: center;
}

.braille-cell-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr;
  gap: 8px;
  margin-bottom: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
  border: 1px solid #dee2e6;
}

.braille-cell-dot {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #dee2e6;
}

.braille-cell-dot.active {
  background: linear-gradient(45deg, #28a745, #20c997);
  box-shadow: 
    0 0 15px rgba(40, 167, 69, 0.6),
    inset 0 2px 4px rgba(255, 255, 255, 0.3);
  border-color: #28a745;
  transform: scale(1.1);
}

.braille-cell-dot.inactive {
  background: linear-gradient(45deg, #f8f9fa, #e9ecef);
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.1),
    inset 0 1px 2px rgba(255, 255, 255, 0.8);
  border-color: #dee2e6;
}

.braille-cell-dot:hover {
  transform: scale(1.05);
  border-color: #007bff;
}

.braille-cell-dot-inner {
  width: 60%;
  height: 60%;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.braille-cell-dot-label {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 18px;
  height: 18px;
  background: #007bff;
  color: white;
  border-radius: 50%;
  font-size: 0.7rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
}

.braille-cell-pattern {
  font-family: 'Courier New', monospace;
  font-size: 1.2rem;
  font-weight: bold;
  color: #007bff;
  background: #f8f9fa;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #dee2e6;
  text-align: center;
  min-width: 80px;
}

/* 점자 셀 크기별 스타일 */
.braille-cell-small {
  min-width: 150px;
  padding: 15px;
}

.braille-cell-small .braille-cell-dot {
  width: 30px;
  height: 30px;
}

.braille-cell-small .braille-cell-dot-label {
  width: 14px;
  height: 14px;
  font-size: 0.6rem;
  top: -6px;
  right: -6px;
}

.braille-cell-large {
  min-width: 250px;
  padding: 25px;
}

.braille-cell-large .braille-cell-dot {
  width: 50px;
  height: 50px;
}

.braille-cell-large .braille-cell-dot-label {
  width: 22px;
  height: 22px;
  font-size: 0.8rem;
  top: -10px;
  right: -10px;
}

/* 점자 셀 컨테이너 위치별 스타일 */
.braille-cell-container {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.braille-cell-container.left {
  order: -1;
  margin-right: 20px;
}

.braille-cell-container.right {
  order: 1;
  margin-left: 20px;
}

.braille-cell-container.top {
  order: -1;
  margin-bottom: 20px;
  width: 100%;
  justify-content: center;
}

.braille-cell-container.bottom {
  order: 1;
  margin-top: 20px;
  width: 100%;
  justify-content: center;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .braille-cell-container.left,
  .braille-cell-container.right {
    order: 0;
    margin: 10px 0;
    width: 100%;
  }
  
  .braille-cell {
    min-width: auto;
    width: 100%;
    max-width: 300px;
  }
  
  .braille-cell-grid {
    gap: 6px;
    padding: 10px;
  }
  
  .braille-cell-dot {
    width: 35px;
    height: 35px;
  }
}

@media (max-width: 480px) {
  .braille-cell {
    padding: 15px;
  }
  
  .braille-cell-dot {
    width: 30px;
    height: 30px;
  }
  
  .braille-cell-dot-label {
    width: 16px;
    height: 16px;
    font-size: 0.6rem;
  }
}
```

### 5. 추가 컴포넌트들

```typescript
// components/LayoutController.tsx
import React from 'react';
import { LayoutConfig } from '../types/layout';
import './LayoutController.css';

interface LayoutControllerProps {
  layoutConfig: LayoutConfig;
  onPresetChange: (preset: string) => void;
  onConfigChange: (updates: Partial<LayoutConfig>) => void;
}

export const LayoutController: React.FC<LayoutControllerProps> = ({
  layoutConfig,
  onPresetChange,
  onConfigChange
}) => {
  return (
    <div className="layout-controller">
      <div className="layout-preset-buttons">
        <button 
          className={`preset-btn ${layoutConfig.imageSize === 'small' ? 'active' : ''}`}
          onClick={() => onPresetChange('compact')}
        >
          컴팩트
        </button>
        <button 
          className={`preset-btn ${layoutConfig.imageSize === 'large' ? 'active' : ''}`}
          onClick={() => onPresetChange('demo')}
        >
          데모
        </button>
        <button 
          className={`preset-btn ${layoutConfig.imageSize === 'full' ? 'active' : ''}`}
          onClick={() => onPresetChange('fullscreen')}
        >
          전체화면
        </button>
      </div>
      
      <div className="layout-settings">
        <select 
          value={layoutConfig.orientation}
          onChange={(e) => onConfigChange({ orientation: e.target.value as any })}
        >
          <option value="auto">자동</option>
          <option value="horizontal">가로</option>
          <option value="vertical">세로</option>
        </select>
        
        <select 
          value={layoutConfig.imageSize}
          onChange={(e) => onConfigChange({ imageSize: e.target.value as any })}
        >
          <option value="small">작게</option>
          <option value="medium">중간</option>
          <option value="large">크게</option>
          <option value="full">전체</option>
        </select>
        
        <select 
          value={layoutConfig.infoPanelPosition}
          onChange={(e) => onConfigChange({ infoPanelPosition: e.target.value as any })}
        >
          <option value="right">오른쪽</option>
          <option value="bottom">아래</option>
          <option value="overlay">오버레이</option>
          <option value="hidden">숨김</option>
        </select>
        
        <label>
          <input 
            type="checkbox" 
            checked={layoutConfig.showBrailleCell}
            onChange={(e) => onConfigChange({ showBrailleCell: e.target.checked })}
          />
          점자 셀
        </label>
        
        {layoutConfig.showBrailleCell && (
          <select 
            value={layoutConfig.brailleCellPosition}
            onChange={(e) => onConfigChange({ brailleCellPosition: e.target.value as any })}
          >
            <option value="left">왼쪽</option>
            <option value="right">오른쪽</option>
            <option value="top">위</option>
            <option value="bottom">아래</option>
          </select>
        )}
      </div>
    </div>
  );
};
```

```typescript
// components/InfoPanel.tsx
import React from 'react';
import { BraillePattern } from '../types/braille';
import './InfoPanel.css';

interface InfoPanelProps {
  braillePattern: BraillePattern | null;
  position: 'right' | 'bottom' | 'overlay' | 'hidden';
  isCompact?: boolean;
}

export const InfoPanel: React.FC<InfoPanelProps> = ({
  braillePattern,
  position,
  isCompact = false
}) => {
  if (position === 'hidden') return null;

  const getBrailleCharacter = (dots: any[]): string => {
    const pattern = dots.map(d => d.isActive ? '1' : '0').join('');
    const brailleMap: { [key: string]: string } = {
      "100000": "A", "110000": "B", "100100": "C", "100110": "D",
      "100010": "E", "110100": "F", "110110": "G", "110010": "H",
      "010100": "I", "010110": "J", "101000": "K", "111000": "L",
      "101100": "M", "101110": "N", "101010": "O", "111100": "P",
      "111110": "Q", "111010": "R", "011100": "S", "011110": "T",
      "101001": "U", "111001": "V", "010111": "W", "101101": "X",
      "101111": "Y", "101011": "Z", "000000": " "
    };
    return brailleMap[pattern] || "?";
  };

  return (
    <div className={`info-panel info-${position} ${isCompact ? 'compact' : ''}`}>
      <h3>점자 패턴</h3>
      {braillePattern ? (
        <div className="pattern-info">
          <p className="pattern-binary">
            {braillePattern.dots.map(d => d.isActive ? '1' : '0').join('')}
          </p>
          <p className="pattern-character">
            {getBrailleCharacter(braillePattern.dots)}
          </p>
          <p className="pattern-source">
            소스: {braillePattern.source}
          </p>
          <p className="pattern-time">
            {new Date(braillePattern.timestamp).toLocaleTimeString()}
          </p>
        </div>
      ) : (
        <p>점자 패턴을 기다리는 중...</p>
      )}
    </div>
  );
};
```

```typescript
// components/BrailleGrid.tsx
import React from 'react';
import { BrailleDot } from './BrailleDot';
import { BrailleDot as BrailleDotType } from '../types/braille';
import './BrailleGrid.css';

interface BrailleGridProps {
  dots: BrailleDotType[];
  dotSize?: 'small' | 'medium' | 'large';
  dotSpacing?: number;
  onDotClick?: (id: number) => void;
}

export const BrailleGrid: React.FC<BrailleGridProps> = ({
  dots,
  dotSize = 'medium',
  dotSpacing = 1.0,
  onDotClick
}) => {
  return (
    <div className="braille-grid">
      {dots.map((dot) => (
        <BrailleDot
          key={dot.id}
          id={dot.id}
          isActive={dot.isActive}
          position={dot.position}
          size={dotSize}
          spacing={dotSpacing}
          onClick={onDotClick}
        />
      ))}
    </div>
  );
};
```

```typescript
// components/BrailleDot.tsx (개선된 버전)
import React from 'react';
import { DotPosition } from '../types/layout';
import './BrailleDot.css';

interface BrailleDotProps {
  id: number;
  isActive: boolean;
  position: DotPosition;
  size?: 'small' | 'medium' | 'large';
  spacing?: number;
  onClick?: (id: number) => void;
}

export const BrailleDot: React.FC<BrailleDotProps> = ({
  id,
  isActive,
  position,
  size = 'medium',
  spacing = 1.0,
  onClick
}) => {
  const handleClick = () => {
    onClick?.(id);
  };

  return (
    <div
      className={`braille-dot ${isActive ? 'active' : 'inactive'} dots-${size}`}
      style={{
        left: `${position.x * 100}%`,
        top: `${position.y * 100}%`,
        transform: `translate(-50%, -50%) scale(${spacing})`,
      }}
      onClick={handleClick}
      data-dot-id={id}
    >
      <div className="dot-inner" />
    </div>
  );
};
```

```typescript
// components/BrailleCell.tsx (새로운 컴포넌트)
import React from 'react';
import { BrailleDot as BrailleDotType } from '../types/braille';
import './BrailleCell.css';

interface BrailleCellProps {
  dots: BrailleDotType[];
  size?: 'small' | 'medium' | 'large';
  showLabels?: boolean;
  onDotClick?: (id: number) => void;
}

export const BrailleCell: React.FC<BrailleCellProps> = ({
  dots,
  size = 'medium',
  showLabels = true,
  onDotClick
}) => {
  // 표준 점자 셀 배치 (2×3 그리드)
  const cellPositions = [
    { row: 1, col: 1, id: 1 }, // 1번 점 (왼쪽 위)
    { row: 2, col: 1, id: 2 }, // 2번 점 (왼쪽 중간)
    { row: 3, col: 1, id: 3 }, // 3번 점 (왼쪽 아래)
    { row: 1, col: 2, id: 4 }, // 4번 점 (오른쪽 위)
    { row: 2, col: 2, id: 5 }, // 5번 점 (오른쪽 중간)
    { row: 3, col: 2, id: 6 }, // 6번 점 (오른쪽 아래)
  ];

  return (
    <div className={`braille-cell braille-cell-${size}`}>
      <div className="braille-cell-title">점자 셀</div>
      <div className="braille-cell-grid">
        {cellPositions.map((pos) => {
          const dot = dots.find(d => d.id === pos.id);
          const isActive = dot?.isActive || false;
          
          return (
            <div
              key={pos.id}
              className={`braille-cell-dot ${isActive ? 'active' : 'inactive'}`}
              style={{
                gridRow: pos.row,
                gridColumn: pos.col,
              }}
              onClick={() => onDotClick?.(pos.id)}
              data-dot-id={pos.id}
            >
              <div className="braille-cell-dot-inner" />
              {showLabels && (
                <div className="braille-cell-dot-label">{pos.id}</div>
              )}
            </div>
          );
        })}
      </div>
      <div className="braille-cell-pattern">
        {dots.map(d => d.isActive ? '1' : '0').join('')}
      </div>
    </div>
  );
};
```

## 🔌 WebSocket 서버 연동

### 1. 서버 설정

기존 `wearable/websocket_server.py`를 사용하여 WebSocket 서버를 실행합니다:

```bash
# PowerShell에서 실행
cd C:\Users\jaeyu\OFBGlove\wearable
python websocket_server.py
```

### 2. 메시지 프로토콜

서버에서 받는 메시지 형식:

```typescript
interface BraillePatternMessage {
  type: 'braille_pattern';
  pattern: boolean[];        // 6개 점 상태
  binary: string;           // "100110" 형태
  character: string;        // 변환된 문자
  visual: string;           // 시각적 표현
  timestamp: number;
}

interface ConnectedMessage {
  type: 'connected';
  status: 'connected';
  braille_dots: boolean[];
  pattern: string;
  character: string;
  timestamp: number;
}
```

## 🚀 개발 시작하기

### 1. 프로젝트 초기화

```bash
# React + TypeScript 프로젝트 생성
npx create-react-app web_visualizer --template typescript
cd web_visualizer

# 필요한 패키지 설치
npm install
```

### 2. 파일 구조 생성

```bash
# 디렉토리 생성
mkdir -p src/components src/services src/types src/hooks src/utils src/styles

# OFBGlove 이미지 복사
copy "C:\Users\jaeyu\OFBGlove\web_visualizer\OFBGlove_image.png" public\
```

### 3. 개발 서버 실행

```bash
# 개발 서버 시작
npm start

# 다른 터미널에서 WebSocket 서버 실행
cd C:\Users\jaeyu\OFBGlove\wearable
python websocket_server.py
```

## 🧪 테스트 시나리오

### 1. 기본 연결 테스트
1. WebSocket 서버 실행
2. 웹 애플리케이션 접속
3. 연결 상태 확인
4. 점자 패턴 수신 테스트

### 2. 점자 패턴 테스트
1. ESP32에서 점자 패턴 전송
2. 웹에서 실시간 업데이트 확인
3. 점자 → 문자 변환 확인
4. 시각적 표현 확인

### 3. 반응형 테스트
1. 다양한 화면 크기에서 테스트
2. 모바일 디바이스에서 테스트
3. 터치 인터페이스 테스트

## 📱 배포 가이드

### 1. 빌드

```bash
npm run build
```

### 2. 정적 호스팅

```bash
# build 폴더를 웹 서버에 업로드
# 예: nginx, Apache, 또는 GitHub Pages
```

### 3. 환경 설정

```typescript
// 환경별 WebSocket URL 설정
const WS_URL = process.env.NODE_ENV === 'production' 
  ? 'wss://your-domain.com/ws'
  : 'ws://localhost:8000/ws';
```

## 🔧 고급 기능 (선택사항)

### 1. 점자 패턴 히스토리

```typescript
const [patternHistory, setPatternHistory] = useState<BraillePattern[]>([]);

// 최근 50개 패턴만 유지
const addToHistory = (pattern: BraillePattern) => {
  setPatternHistory(prev => [...prev.slice(-49), pattern]);
};
```

### 2. 실시간 통계

```typescript
const [stats, setStats] = useState({
  totalPatterns: 0,
  charactersPerMinute: 0,
  lastActivity: null as Date | null
});
```

### 3. 다크 모드

```typescript
const [isDarkMode, setIsDarkMode] = useState(false);

// CSS 변수로 테마 전환
const theme = isDarkMode ? 'dark' : 'light';
```

## 🐛 디버깅 가이드

### 1. WebSocket 연결 문제

```typescript
// 연결 상태 확인
wsService.ws?.readyState === WebSocket.OPEN

// 재연결 로직
const reconnect = () => {
  setTimeout(() => {
    wsService.connect().catch(console.error);
  }, 3000);
};
```

### 2. 점자 패턴 표시 문제

```typescript
// 점 위치 계산 디버깅
console.log('Dot positions:', dots.map(d => ({ id: d.id, pos: d.position })));

// 패턴 상태 확인
console.log('Pattern:', pattern.map(d => d.isActive ? '1' : '0').join(''));
```

## 📚 참고 자료

- [React 공식 문서](https://reactjs.org/)
- [TypeScript 핸드북](https://www.typescriptlang.org/docs/)
- [WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [CSS Grid 레이아웃](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Grid_Layout)

---

이 가이드를 따라하면 OFBGlove의 점자를 실시간으로 비주얼라이즈하는 웹 애플리케이션을 개발할 수 있습니다. 데모 및 시연 목적에 최적화되어 있으며, 현대적이고 직관적인 UI를 제공합니다.
