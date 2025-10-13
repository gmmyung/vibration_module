// 네트워크 유틸리티 함수들
export class NetworkUtils {
  // WiFi IP 주소 감지 (WebRTC 방식, 무선 어댑터 우선)
  static async getLocalIP(): Promise<string> {
    return new Promise((resolve, reject) => {
      const pc = new RTCPeerConnection({
        iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
      });
      
      const candidates: string[] = [];
      
      pc.createDataChannel('');
      pc.createOffer().then(offer => pc.setLocalDescription(offer));
      
      pc.onicecandidate = (event) => {
        if (event.candidate) {
          const candidate = event.candidate.candidate;
          const ipMatch = candidate.match(/([0-9]{1,3}(\.[0-9]{1,3}){3})/);
          if (ipMatch) {
            const ip = ipMatch[1];
            // 로컬 네트워크 IP만 수집
            if (this.isLocalIP(ip)) {
              candidates.push(ip);
            }
          }
        }
      };
      
      // 모든 후보를 수집한 후 무선 어댑터 IP 우선 선택
      setTimeout(() => {
        pc.close();
        
        if (candidates.length === 0) {
          reject(new Error('IP 주소를 찾을 수 없습니다'));
          return;
        }
        
        // 무선 어댑터 IP 우선 선택 (192.168.x.x가 일반적으로 WiFi)
        const wifiIP = candidates.find(ip => ip.startsWith('192.168.'));
        const selectedIP = wifiIP || candidates[0];
        
        console.log('발견된 IP 후보들:', candidates);
        console.log('선택된 WiFi IP:', selectedIP);
        
        resolve(selectedIP);
      }, 3000);
    });
  }

  // 로컬 네트워크 IP인지 확인
  private static isLocalIP(ip: string): boolean {
    const parts = ip.split('.').map(Number);
    if (parts.length !== 4) return false;
    
    // 192.168.x.x
    if (parts[0] === 192 && parts[1] === 168) return true;
    // 10.x.x.x
    if (parts[0] === 10) return true;
    // 172.16.x.x - 172.31.x.x
    if (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) return true;
    
    return false;
  }

  // WebSocket URL 생성
  static getWebSocketURL(ip: string, port: number = 8888): string {
    return `ws://${ip}:${port}/ws`;
  }

  // HTTP URL 생성 (QR 코드용)
  static getHTTPURL(ip: string, port: number = 3000): string {
    return `http://${ip}:${port}`;
  }


  // 네트워크 상태 확인
  static async checkNetworkConnection(): Promise<boolean> {
    try {
      const response = await fetch('https://www.google.com', { 
        method: 'HEAD',
        mode: 'no-cors'
      });
      return true;
    } catch {
      return false;
    }
  }
}
