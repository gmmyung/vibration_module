#!/usr/bin/env python3
"""
Web Braille Visualizer - WebSocket Server
웹 기반 점자 시각화를 위한 WebSocket 서버
ESP32와 웹 클라이언트 간의 실시간 통신을 담당
"""

import asyncio
import websockets
import json
import time
import logging
from typing import Set, Dict, Any
from dataclasses import dataclass
from enum import Enum

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class MessageType(Enum):
    """메시지 타입 정의"""
    # ESP32 → 서버 → 웹 클라이언트
    DOT_TOUCH = 0x01
    DOT_RELEASE = 0x02
    PATTERN_COMPLETE = 0x03
    CLEAR = 0x04
    WORD_SEPARATOR = 0x05
    SENTENCE_SEPARATOR = 0x06
    BRAILLE_SEPARATOR = 0x07
    TEXT_MESSAGE = 0x10
    SETTINGS = 0x20
    
    # 서버 → 웹 클라이언트
    STATUS_UPDATE = 0x81
    MESSAGE_RECEIVED = 0x82
    SETTINGS_UPDATED = 0x83
    
    # WebSocket 전용
    PING = "ping"
    PONG = "pong"
    SEQUENCE = "sequence"
    BRAILLE_PATTERN = "braille_pattern"

@dataclass
class BrailleSettings:
    """점자 설정"""
    speed: int = 1  # 0=SLOW, 1=NORMAL, 2=FAST
    mode: int = 0   # 0=AUTO, 1=MANUAL, 2=REPEAT
    flags: int = 0x00

@dataclass
class BraillePattern:
    """점자 패턴"""
    dots: list[bool]  # 6개 점의 상태
    timestamp: float
    source: str  # "esp32", "web", "demo"

class WebBrailleServer:
    """웹 점자 시각화 서버"""
    
    def __init__(self, host='localhost', port=8000):
        self.host = host
        self.port = port
        self.connected_clients: Set[websockets.WebSocketServerProtocol] = set()
        self.braille_dots = [False] * 6
        self.current_settings = BrailleSettings()
        self.pattern_history: list[BraillePattern] = []
        self.running = False
        
        logger.info(f"🌐 Web Braille Server 초기화")
        logger.info(f"📡 서버 주소: {host}:{port}")
        logger.info(f"🎯 지원 메시지 타입: {len(MessageType)}개")
        logger.info("-" * 50)

    def calculate_checksum(self, data: bytes) -> int:
        """체크섬 계산"""
        checksum = 0
        for byte in data:
            checksum ^= byte
        return checksum & 0xFF

    def get_braille_pattern(self) -> str:
        """점자 패턴 문자열 반환"""
        return ''.join('1' if dot else '0' for dot in self.braille_dots)

    def get_visual_pattern(self) -> str:
        """점자 패턴을 시각적으로 표현"""
        visual = ""
        visual += "●" if self.braille_dots[0] else "○"  # 1번 점
        visual += " " if self.braille_dots[0] or self.braille_dots[3] else " "
        visual += "●" if self.braille_dots[3] else "○"  # 4번 점
        visual += "\n"
        visual += "●" if self.braille_dots[1] else "○"  # 2번 점
        visual += " " if self.braille_dots[1] or self.braille_dots[4] else " "
        visual += "●" if self.braille_dots[4] else "○"  # 5번 점
        visual += "\n"
        visual += "●" if self.braille_dots[2] else "○"  # 3번 점
        visual += " " if self.braille_dots[2] or self.braille_dots[5] else " "
        visual += "●" if self.braille_dots[5] else "○"  # 6번 점
        return visual

    def get_braille_character(self) -> str:
        """점자 문자 반환 (간단한 매핑)"""
        pattern = self.get_braille_pattern()
        braille_map = {
            "100000": "A", "110000": "B", "100100": "C", "100110": "D",
            "100010": "E", "110100": "F", "110110": "G", "110010": "H",
            "010100": "I", "010110": "J", "101000": "K", "111000": "L",
            "101100": "M", "101110": "N", "101010": "O", "111100": "P",
            "111110": "Q", "111010": "R", "011100": "S", "011110": "T",
            "101001": "U", "111001": "V", "010111": "W", "101101": "X",
            "101111": "Y", "101011": "Z", "000000": " "
        }
        return braille_map.get(pattern, "?")

    async def broadcast_to_clients(self, message: Dict[str, Any]):
        """모든 연결된 클라이언트에 메시지 브로드캐스트"""
        if not self.connected_clients:
            return
            
        message_str = json.dumps(message)
        disconnected = set()
        
        for client in self.connected_clients:
            try:
                await client.send(message_str)
            except websockets.exceptions.ConnectionClosed:
                disconnected.add(client)
            except Exception as e:
                logger.error(f"클라이언트 전송 오류: {e}")
                disconnected.add(client)
        
        # 연결이 끊어진 클라이언트 제거
        self.connected_clients -= disconnected

    async def handle_esp32_message(self, message_type: int, data: bytes):
        """ESP32 메시지 처리"""
        try:
            if message_type == MessageType.DOT_TOUCH.value:
                if len(data) >= 1:
                    dot_index = data[0]
                    if 0 <= dot_index < 6:
                        self.braille_dots[dot_index] = True
                        logger.info(f"👆 점 터치: {dot_index + 1}번 점 활성화")
                        await self.broadcast_braille_update()
                        
            elif message_type == MessageType.DOT_RELEASE.value:
                if len(data) >= 1:
                    dot_index = data[0]
                    if 0 <= dot_index < 6:
                        self.braille_dots[dot_index] = False
                        logger.info(f"👆 점 릴리즈: {dot_index + 1}번 점 비활성화")
                        await self.broadcast_braille_update()
                        
            elif message_type == MessageType.PATTERN_COMPLETE.value:
                if len(data) >= 1:
                    byte_value = data[0]
                    # 바이트를 6개 점으로 변환
                    for i in range(6):
                        self.braille_dots[i] = (byte_value & (1 << i)) != 0
                    
                    pattern = self.get_braille_pattern()
                    character = self.get_braille_character()
                    logger.info(f"📝 패턴 완성: {pattern} ({character})")
                    await self.broadcast_braille_update()
                    
            elif message_type == MessageType.CLEAR.value:
                self.braille_dots = [False] * 6
                logger.info("🧹 점자 상태 초기화")
                await self.broadcast_braille_update()
                
            elif message_type == MessageType.TEXT_MESSAGE.value:
                try:
                    text = data.decode('utf-8')
                    logger.info(f"📝 텍스트 메시지: '{text}'")
                    await self.broadcast_to_clients({
                        "type": "text_message",
                        "text": text,
                        "timestamp": time.time()
                    })
                except UnicodeDecodeError:
                    logger.error("텍스트 디코딩 실패")
                    
            elif message_type == MessageType.SETTINGS.value:
                if len(data) >= 2:
                    speed = data[0]
                    mode = data[1]
                    if 0 <= speed <= 2 and 0 <= mode <= 2:
                        self.current_settings.speed = speed
                        self.current_settings.mode = mode
                        logger.info(f"⚙️ 설정 업데이트: speed={speed}, mode={mode}")
                        await self.broadcast_to_clients({
                            "type": "settings_updated",
                            "settings": {
                                "speed": speed,
                                "mode": mode
                            },
                            "timestamp": time.time()
                        })
                        
        except Exception as e:
            logger.error(f"ESP32 메시지 처리 오류: {e}")

    async def broadcast_braille_update(self):
        """점자 상태 업데이트 브로드캐스트"""
        pattern = BraillePattern(
            dots=self.braille_dots.copy(),
            timestamp=time.time(),
            source="esp32"
        )
        self.pattern_history.append(pattern)
        
        # 최근 100개 패턴만 유지
        if len(self.pattern_history) > 100:
            self.pattern_history = self.pattern_history[-100:]
        
        await self.broadcast_to_clients({
            "type": "braille_pattern",
            "pattern": self.braille_dots,
            "binary": self.get_braille_pattern(),
            "character": self.get_braille_character(),
            "visual": self.get_visual_pattern(),
            "timestamp": pattern.timestamp
        })

    async def handle_websocket_message(self, websocket: websockets.WebSocketServerProtocol, message: str):
        """WebSocket 메시지 처리"""
        try:
            data = json.loads(message)
            message_type = data.get("type")
            
            if message_type == "ping":
                # 핑-퐁 응답
                await websocket.send(json.dumps({
                    "type": "pong",
                    "t0": data.get("t0", time.time() * 1000)
                }))
                
            elif message_type == "sequence":
                # 시퀀스 메시지 처리
                steps = data.get("steps", [])
                loop = data.get("loop", False)
                
                if self.validate_sequence({"steps": steps, "loop": loop}):
                    logger.info(f"🎬 시퀀스 수신: {len(steps)}개 스텝, 루프={loop}")
                    await self.broadcast_to_clients({
                        "type": "sequence",
                        "steps": steps,
                        "loop": loop,
                        "timestamp": time.time()
                    })
                else:
                    logger.warning("잘못된 시퀀스 형식")
                    
            elif message_type == "get_status":
                # 현재 상태 요청
                await websocket.send(json.dumps({
                    "type": "status",
                    "braille_dots": self.braille_dots,
                    "pattern": self.get_braille_pattern(),
                    "character": self.get_braille_character(),
                    "settings": {
                        "speed": self.current_settings.speed,
                        "mode": self.current_settings.mode
                    },
                    "connected_clients": len(self.connected_clients),
                    "timestamp": time.time()
                }))
                
        except json.JSONDecodeError:
            logger.error("잘못된 JSON 메시지")
        except Exception as e:
            logger.error(f"WebSocket 메시지 처리 오류: {e}")

    def validate_sequence(self, seq: Dict[str, Any]) -> bool:
        """시퀀스 유효성 검사"""
        if not seq or not isinstance(seq.get("steps"), list) or len(seq["steps"]) == 0:
            return False
            
        for step in seq["steps"]:
            if not isinstance(step.get("cell"), list) or len(step["cell"]) != 6:
                return False
            if not all(isinstance(v, int) and v in [0, 1] for v in step["cell"]):
                return False
            if not isinstance(step.get("ms"), (int, float)) or step["ms"] <= 0:
                return False
                
        return True

    async def register_client(self, websocket: websockets.WebSocketServerProtocol):
        """클라이언트 등록"""
        self.connected_clients.add(websocket)
        logger.info(f"🔗 클라이언트 연결: {websocket.remote_address}")
        logger.info(f"📊 연결된 클라이언트 수: {len(self.connected_clients)}")
        
        # 현재 상태 전송
        await websocket.send(json.dumps({
            "type": "connected",
            "status": "connected",
            "braille_dots": self.braille_dots,
            "pattern": self.get_braille_pattern(),
            "character": self.get_braille_character(),
            "timestamp": time.time()
        }))

    async def unregister_client(self, websocket: websockets.WebSocketServerProtocol):
        """클라이언트 등록 해제"""
        self.connected_clients.discard(websocket)
        logger.info(f"🔌 클라이언트 연결 해제: {websocket.remote_address}")
        logger.info(f"📊 연결된 클라이언트 수: {len(self.connected_clients)}")

    async def handle_client(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """클라이언트 연결 처리"""
        await self.register_client(websocket)
        
        try:
            async for message in websocket:
                await self.handle_websocket_message(websocket, message)
        except websockets.exceptions.ConnectionClosed:
            logger.info("클라이언트 연결이 정상적으로 종료됨")
        except Exception as e:
            logger.error(f"클라이언트 처리 오류: {e}")
        finally:
            await self.unregister_client(websocket)

    async def start_server(self):
        """서버 시작"""
        try:
            self.running = True
            logger.info(f"🚀 Web Braille Server 시작!")
            logger.info(f"📡 WebSocket 서버: ws://{self.host}:{self.port}/ws")
            logger.info(f"💡 종료하려면 Ctrl+C를 누르세요")
            logger.info("=" * 50)
            
            async with websockets.serve(
                self.handle_client, 
                self.host, 
                self.port,
                ping_interval=20,
                ping_timeout=10
            ):
                await asyncio.Future()  # 서버를 계속 실행
                
        except Exception as e:
            logger.error(f"서버 시작 실패: {e}")
        finally:
            self.running = False

    def stop_server(self):
        """서버 종료"""
        self.running = False
        logger.info("🔚 Web Braille Server 종료")

async def main():
    """메인 함수"""
    print("🌐 Web Braille Visualizer Server")
    print("=" * 50)
    
    server = WebBrailleServer()
    
    try:
        await server.start_server()
    except KeyboardInterrupt:
        print(f"\n🛑 사용자에 의한 종료")
    except Exception as e:
        print(f"❌ 예상치 못한 오류: {e}")
    finally:
        server.stop_server()

if __name__ == "__main__":
    asyncio.run(main())
