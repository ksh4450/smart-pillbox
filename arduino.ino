#include <Wire.h>
#include <SoftwareSerial.h>

#define BT_RXD 8
#define BT_TXD 7
SoftwareSerial bluetooth(BT_RXD, BT_TXD);

const int IN_A0 = A0; // 첫 번째 IR 센서의 아날로그 입력
const int IN_D0 = 9;  // 첫 번째 IR 센서의 디지털 입력
const int IN_A1 = A1; // 두 번째 IR 센서의 아날로그 입력
const int IN_D1 = 10;  //
const int IN_A2 = A2; //
const int IN_D2 = 11;
const int IN_A3 = A3; //
const int IN_D3 = 12;

void setup() {
  pinMode(LED_PIN, OUTPUT);
  pinMode(IN_A0, INPUT);    
  pinMode(IN_D0, INPUT);    
  pinMode(IN_A1, INPUT);    
  pinMode(IN_D1, INPUT);    
  pinMode(IN_A2, INPUT);    
  pinMode(IN_D2, INPUT);    
  pinMode(IN_A3, INPUT);    
  pinMode(IN_D3, INPUT);    
  pinMode(2, OUTPUT);
  pinMode(3, OUTPUT);
  pinMode(4, OUTPUT);
  pinMode(6, OUTPUT);
  Serial.begin(9600);
  bluetooth.begin(9600);
}

int value_A0;
bool value_D0;
int value_A1;
bool value_D1;
int value_A2;
bool value_D2;
int value_A3;
bool value_D3;

void loop() {

  if (bluetooth.available()) {
    Serial.write(bluetooth.read());
  }
  if (Serial.available()) {
    bluetooth.write(Serial.read());
  }

  int value_A0 = analogRead(IN_A0); // 첫 번째 IR 센서의 아날로그 입력 읽기
  int value_D0 = digitalRead(IN_D0); // 첫 번째 IR 센서의 디지털 입력 읽기
  int value_A1 = analogRead(IN_A1); // 두 번째 IR 센서의 아날로그 입력 읽기
  int value_D1 = digitalRead(IN_D1); // 두 번째 IR 센서의 디지털 입력 읽기
  int value_A2 = analogRead(IN_A2); // 세 번째 IR 센서의 아날로그 입력 읽기
  int value_D2 = digitalRead(IN_D2); // 세 번째 IR 센서의 디지털 입력 읽기
  value_A3 = analogRead(IN_A3); // 네 번째 IR 센서의 아날로그 입력 읽기
  value_D3 = digitalRead(IN_D3); // 네 번째 IR 센서의 디지털 입력 읽기

  Serial.print("999");
  Serial.print(", ");
  Serial.print(value_A0);
  Serial.print(", ");
  Serial.print(value_A1);
  Serial.print(", ");
  Serial.print(value_A2);
  Serial.print(", ");
  Serial.print(value_A3);
  Serial.print(", ");
  Serial.println("888");

  bluetooth.print("999");
  bluetooth.print(",");
  bluetooth.print(value_A0);
  bluetooth.print(",");
  bluetooth.print(value_A1);
  bluetooth.print(",");
  bluetooth.print(value_A2);
  bluetooth.print(",");
  bluetooth.print(value_A3);
  bluetooth.print(",");
  bluetooth.println("888");
 
  if (value_A0 < 350)
    digitalWrite(4, HIGH);
  else
    digitalWrite(4, LOW);

  if (value_A1 < 350)
    digitalWrite(6, HIGH);
  else
   digitalWrite(6, LOW);

  if (value_A2 < 350)
    digitalWrite(3, HIGH);
  else
    digitalWrite(3, LOW);

  if (value_A3 < 350)
    digitalWrite(2, HIGH);
  else
    digitalWrite(2, LOW);

  delay(500);
}
