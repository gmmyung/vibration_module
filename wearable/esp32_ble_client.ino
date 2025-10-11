#include <BLEDevice.h>
#include <BLEClient.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// Android 앱과 동일한 UUID 사용
#define SERVICE_UUID        "3e9c71e3-ba94-4efa-ba8f-23ad496706da"
#define CHARACTERISTIC_UUID "42f8b4ab-deca-4c41-a2b2-ade173695cbd"

BLEClient* pClient = nullptr;
BLERemoteCharacteristic* pRemoteCharacteristic = nullptr;
bool doConnect = false;
bool connected = false;

class MyClientCallback : public BLEClientCallbacks {
    void onConnect(BLEClient* pclient) {
        Serial.println("ESP32: Android 앱 연결됨!");
        connected = true;
    }

    void onDisconnect(BLEClient* pclient) {
        Serial.println("ESP32: Android 앱 연결 해제됨");
        connected = false;
    }
};

class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) {
        Serial.print("ESP32: 발견된 디바이스: ");
        Serial.println(advertisedDevice.toString().c_str());
        
        // 모든 디바이스 정보 출력
        Serial.print("ESP32: 디바이스 이름: ");
        Serial.println(advertisedDevice.getName().c_str());
        Serial.print("ESP32: RSSI: ");
        Serial.println(advertisedDevice.getRSSI());
        
        // 서비스 UUID 확인
        if (advertisedDevice.haveServiceUUID()) {
            Serial.print("ESP32: 서비스 UUID: ");
            Serial.println(advertisedDevice.getServiceUUID().toString().c_str());
        }
        
        // Android 앱 찾기 (서비스 UUID로 확인)
        if (advertisedDevice.haveServiceUUID() && 
            advertisedDevice.getServiceUUID().toString() == SERVICE_UUID) {
            Serial.println("ESP32: Android 앱 발견! 연결 시도...");
            BLEDevice::getScan()->stop();
            pClient = BLEDevice::createClient();
            pClient->setClientCallbacks(new MyClientCallback());
            pClient->connect(&advertisedDevice);
            doConnect = true;
        }
    }
};

void setup() {
    Serial.begin(115200);
    delay(1000); // 시리얼 초기화 대기
    Serial.println("ESP32: BLE 클라이언트 시작");
    Serial.println("ESP32: Android 앱을 찾는 중...");
    
    BLEDevice::init("ESP32_Braille_Client");
    BLEScan* pBLEScan = BLEDevice::getScan();
    pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
    pBLEScan->setInterval(1349);
    pBLEScan->setWindow(449);
    pBLEScan->setActiveScan(true);
    pBLEScan->start(5, false);
    
    Serial.println("ESP32: 스캔 시작됨");
}

void loop() {
    if (doConnect) {
        if (connectToServer()) {
            Serial.println("ESP32: Android 앱 연결 성공!");
        } else {
            Serial.println("ESP32: Android 앱 연결 실패");
        }
        doConnect = false;
    }
    
    if (connected) {
        // Android 앱에 메시지 전송
        String message = "ESP32에서 보낸 점자 데이터: " + String(millis());
        sendMessageToAndroid(message);
        delay(5000);
    } else {
        // 연결되지 않은 상태에서 주기적으로 스캔 재시작
        static unsigned long lastScanTime = 0;
        if (millis() - lastScanTime > 10000) { // 10초마다 스캔 재시작
            Serial.println("ESP32: 스캔 재시작...");
            BLEScan* pBLEScan = BLEDevice::getScan();
            pBLEScan->start(5, false);
            lastScanTime = millis();
        }
    }
    
    delay(1000);
}

bool connectToServer() {
    Serial.println("ESP32: Android 앱 연결 중...");
    
    if (!pClient->isConnected()) {
        Serial.println("ESP32: 클라이언트가 연결되지 않음");
        return false;
    }
    
    BLERemoteService* pRemoteService = pClient->getService(SERVICE_UUID);
    if (pRemoteService == nullptr) {
        Serial.println("ESP32: 서비스 찾을 수 없음");
        return false;
    }
    
    pRemoteCharacteristic = pRemoteService->getCharacteristic(CHARACTERISTIC_UUID);
    if (pRemoteCharacteristic == nullptr) {
        Serial.println("ESP32: 특성 찾을 수 없음");
        return false;
    }
    
    if (pRemoteCharacteristic->canNotify()) {
        pRemoteCharacteristic->registerForNotify([](BLERemoteCharacteristic* pBLERemoteCharacteristic, uint8_t* pData, size_t length, bool isNotify) {
            Serial.print("ESP32: Android 앱에서 수신된 데이터: ");
            for (int i = 0; i < length; i++) {
                Serial.print((char)pData[i]);
            }
            Serial.println();
        });
    }
    
    return true;
}

void sendMessageToAndroid(String message) {
    if (pRemoteCharacteristic != nullptr && connected) {
        pRemoteCharacteristic->writeValue(message.c_str(), message.length());
        Serial.println("ESP32: Android 앱에 메시지 전송됨: " + message);
    }
}