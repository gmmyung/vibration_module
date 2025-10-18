// WebSocket 통신 관련 타입 정의
export interface WebSocketMessage {
  type: string;
  data?: any;
  timestamp?: number;
}

export interface BraillePatternMessage extends WebSocketMessage {
  type: 'braille_pattern';
  pattern: boolean[];
  binary: string;
  character: string;
  source: string;
}

export interface ConnectedMessage extends WebSocketMessage {
  type: 'connected';
  status: 'connected';
  braille_dots: boolean[];
  pattern: string;
  character: string;
}

export interface StatusMessage extends WebSocketMessage {
  type: 'status';
  braille_dots: boolean[];
  pattern: string;
  character: string;
  connected_clients: number;
}

export type WebSocketMessageType = 
  | BraillePatternMessage 
  | ConnectedMessage 
  | StatusMessage;
