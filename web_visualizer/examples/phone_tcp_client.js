#!/usr/bin/env node
/**
 * 핸드폰 TCP 클라이언트 예제
 * Node.js로 핸드폰에서 TCP 서버로 점자 데이터를 전송하는 예제
 */

const net = require('net');

class PhoneTCPClient {
  constructor(serverIP, serverPort = 8888) {
    this.serverIP = serverIP;
    this.serverPort = serverPort;
    this.socket = null;
    this.isConnected = false;
  }

  // 체크섬 계산
  calculateChecksum(data) {
    let checksum = 0;
    for (const byte of data) {
      checksum ^= byte;
    }
    return checksum & 0xFF;
  }

  // GATT 호환 메시지 생성
  createMessage(messageType, data) {
    const dataLength = data.length;
    const checksum = this.calculateChecksum(data);
    
    // 헤더: [messageType, dataLength(2bytes), checksum]
    const header = Buffer.alloc(4);
    header[0] = messageType;
    header[1] = dataLength & 0xFF;        // LSB
    header[2] = (dataLength >> 8) & 0xFF; // MSB
    header[3] = checksum;
    
    return Buffer.concat([header, data]);
  }

  // 점 터치 메시지 전송
  sendDotTouch(dotIndex) {
    const data = Buffer.from([dotIndex]);
    const message = this.createMessage(0x01, data); // DOT_TOUCH
    this.sendMessage(message);
    console.log(`👆 점 터치 전송: ${dotIndex + 1}번 점`);
  }

  // 점 릴리즈 메시지 전송
  sendDotRelease(dotIndex) {
    const data = Buffer.from([dotIndex]);
    const message = this.createMessage(0x02, data); // DOT_RELEASE
    this.sendMessage(message);
    console.log(`👆 점 릴리즈 전송: ${dotIndex + 1}번 점`);
  }

  // 패턴 완성 메시지 전송
  sendPatternComplete(dots) {
    // 6개 점을 1바이트로 압축
    let dotsByte = 0;
    for (let i = 0; i < 6; i++) {
      if (dots[i]) {
        dotsByte |= (1 << i);
      }
    }
    
    const data = Buffer.from([dotsByte]);
    const message = this.createMessage(0x03, data); // PATTERN_COMPLETE
    this.sendMessage(message);
    console.log(`📝 패턴 완성 전송: ${dots.map(d => d ? '1' : '0').join('')}`);
  }

  // 초기화 메시지 전송
  sendClear() {
    const data = Buffer.from([0x00]);
    const message = this.createMessage(0x04, data); // CLEAR
    this.sendMessage(message);
    console.log(`🧹 초기화 전송`);
  }

  // 텍스트 메시지 전송
  sendTextMessage(text) {
    const textBytes = Buffer.from(text, 'utf-8');
    const data = Buffer.concat([
      Buffer.from([textBytes.length]),
      textBytes
    ]);
    const message = this.createMessage(0x10, data); // TEXT_MESSAGE
    this.sendMessage(message);
    console.log(`📝 텍스트 메시지 전송: "${text}"`);
  }

  // 설정 메시지 전송
  sendSettings(speed, mode) {
    const data = Buffer.from([speed, mode, 0x00]); // [speed, mode, flags]
    const message = this.createMessage(0x20, data); // SETTINGS
    this.sendMessage(message);
    console.log(`⚙️ 설정 전송: speed=${speed}, mode=${mode}`);
  }

  // 메시지 전송
  sendMessage(message) {
    if (this.socket && this.isConnected) {
      this.socket.write(message);
    } else {
      console.error('❌ 서버에 연결되지 않음');
    }
  }

  // 서버 연결
  connect() {
    return new Promise((resolve, reject) => {
      this.socket = net.createConnection(this.serverPort, this.serverIP, () => {
        this.isConnected = true;
        console.log(`🔗 TCP 서버 연결됨: ${this.serverIP}:${this.serverPort}`);
        resolve();
      });

      this.socket.on('error', (error) => {
        console.error('❌ TCP 연결 오류:', error.message);
        this.isConnected = false;
        reject(error);
      });

      this.socket.on('close', () => {
        console.log('🔌 TCP 연결 종료');
        this.isConnected = false;
      });

      this.socket.on('data', (data) => {
        console.log('📨 서버 응답:', data.toString());
      });
    });
  }

  // 연결 종료
  disconnect() {
    if (this.socket) {
      this.socket.end();
      this.isConnected = false;
    }
  }
}

// 사용 예제
async function main() {
  const serverIP = process.argv[2] || '192.168.1.100';
  const client = new PhoneTCPClient(serverIP);

  try {
    // 서버 연결
    await client.connect();

    // 점자 패턴 시뮬레이션
    console.log('\n🎯 점자 패턴 시뮬레이션 시작...\n');

    // 1. 점 터치 시뮬레이션
    client.sendDotTouch(0); // 1번 점 터치
    await sleep(500);
    
    client.sendDotTouch(3); // 4번 점 터치
    await sleep(500);
    
    client.sendDotTouch(4); // 5번 점 터치
    await sleep(1000);

    // 2. 패턴 완성 (A 문자: 100000)
    client.sendPatternComplete([true, false, false, false, false, false]);
    await sleep(2000);

    // 3. 초기화
    client.sendClear();
    await sleep(1000);

    // 4. 다른 패턴 (B 문자: 110000)
    client.sendPatternComplete([true, true, false, false, false, false]);
    await sleep(2000);

    // 5. 텍스트 메시지
    client.sendTextMessage('Hello Braille!');
    await sleep(1000);

    // 6. 설정 전송
    client.sendSettings(1, 0); // speed=1 (NORMAL), mode=0 (AUTO)
    await sleep(1000);

    console.log('\n✅ 시뮬레이션 완료!');
    
  } catch (error) {
    console.error('❌ 오류 발생:', error.message);
  } finally {
    client.disconnect();
  }
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// 스크립트 실행
if (require.main === module) {
  console.log('📱 핸드폰 TCP 클라이언트 예제');
  console.log('사용법: node phone_tcp_client.js [서버IP]');
  console.log('예제: node phone_tcp_client.js 192.168.1.100\n');
  
  main().catch(console.error);
}

module.exports = PhoneTCPClient;
