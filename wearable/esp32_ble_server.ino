#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// Android 앱과 동일한 UUID 사용
#define SERVICE_UUID        "3e9c71e3-ba94-4efa-ba8f-23ad496706da"
#define CHARACTERISTIC_UUID "42f8b4ab-deca-4c41-a2b2-ade173695cbd"

BLEServer* pServer = nullptr;
BLECharacteristic* pCharacteristic = nullptr;
bool deviceConnected = false;
bool oldDeviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        Serial.println("ESP32: 클라이언트 연결됨");
    }

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println("ESP32: 클라이언트 연결 해제됨");
    }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string rxValue = pCharacteristic->getValue();
        if (rxValue.length() > 0) {
            Serial.print("ESP32: 수신된 데이터: ");
            for (int i = 0; i < rxValue.length(); i++) {
                Serial.print(rxValue[i]);
            }
            Serial.println();
        }
    }
};

void setup() {
    Serial.begin(115200);
    delay(1000); // 시리얼 초기화 대기
    Serial.println("ESP32: BLE 서버 시작");
    
    BLEDevice::init("MyBrailleDevice");  // 원하는 이름으로 변경 가능
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());
    
    BLEService* pService = pServer->createService(SERVICE_UUID);
    
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_WRITE |
        BLECharacteristic::PROPERTY_NOTIFY
    );
    
    pCharacteristic->setCallbacks(new MyCallbacks());
    pCharacteristic->addDescriptor(new BLE2902());
    
    pService->start();
    
    BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMaxPreferred(0x12);
    pAdvertising->start();
    
    Serial.println("ESP32: BLE 광고 시작됨");
    Serial.println("ESP32: 디바이스 이름: MyBrailleDevice");
    Serial.println("ESP32: 서비스 UUID: " + String(SERVICE_UUID));
}

void loop() {
    // 연결 상태 확인
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // 클라이언트가 연결을 끊을 시간을 줌
        pServer->startAdvertising(); // 다시 광고 시작
        Serial.println("ESP32: 광고 재시작");
        oldDeviceConnected = deviceConnected;
    }
    
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
    }
    
    if (deviceConnected) {
        // Android 앱에 메시지 전송
        String message = "{\"text\":\"ESP32에서 보낸 메시지\",\"braille\":\"⠑⠎⠏⠼⠉⠼⠃⠼⠃\",\"timestamp\":" + String(millis()) + "}";
        pCharacteristic->setValue(message.c_str());
        pCharacteristic->notify();
        Serial.println("ESP32: 메시지 전송됨: " + message);
    }
    
    delay(5000);
}
