#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESP8266WebServer.h>
#include <ESP8266Wifi.h>
#include <RestClient.h>
#include <PubSubClient.h>
#include "DHT.h"
#include "IRsend.h"

#define DHTPIN 5
#define DHTTYPE DHT11

DHT dht(DHTPIN, DHTTYPE);


WiFiClient espClient;
PubSubClient pubsubClient(espClient);
char msg[50];
const char* ssid = "RedPrueba";
const char* password = "pruebaDAD";
const char* serverIP = "192.168.137.1";

const char* nombreSensor = "isidro baño";

ESP8266WebServer http_rest_server(8080);
RestClient client =	RestClient(serverIP,8083);

IRsend irsend(4);

void enviaIR(int code);

void enviaIRTemp(int code);

void cambiaTemperatura(const char* nTemp, const char* user, const char* sensor){
	Serial.print("Cambia temperatura del sensor ");
	Serial.print(sensor);
	Serial.print(" a ");
	Serial.print(nTemp);
	Serial.println(" grados.");
	char codigo[5];
	int code = 0x01;
	snprintf_P(codigo, sizeof(code), "%X%s", code, nTemp);
	int IRCode = atoi(codigo);
	Serial.print("Codigo temperatura INFRARROJO: ");
	Serial.println(IRCode);
	enviaIRTemp(IRCode);

}

void mideTemperatura(const char* user, const char* sensor){

  float t = dht.readTemperature();

  // Check if any reads failed and exit early (to try again).
  if (isnan(t)) {
    Serial.println("Error al leer del sensor!");
    return;
  }

	char temp[10];
	snprintf_P(temp, sizeof(temp), "%.2f",t);

 	const size_t capacity = JSON_OBJECT_SIZE(3) + JSON_ARRAY_SIZE(2) + 60;
  DynamicJsonBuffer jsonBuffer(capacity);

  JsonObject& newJson = jsonBuffer.createObject();
  newJson["id"] = "";
  newJson["temp"] = temp;
  newJson["user"] = user;
  newJson["fechaHora"] = 0;
  newJson["sensor"] = sensor;
  char jsonStr[100];
  newJson.printTo(jsonStr);

	Serial.println(jsonStr);

  String response = "";
  int statusCode = client.put("/ISS/guardaTemperatura", jsonStr,
	&response);
	Serial.print("Status code: ");
	Serial.println(statusCode);
	Serial.print("Respuesta: ");
	Serial.println(response);
}

void enciende(const char* user, const char* sensor){
	Serial.print("Sensor ");
	Serial.print(sensor);
	Serial.println(" encendido!");
	enviaIR(0xA90);

}

void apaga(const char* user, const char* sensor){
	Serial.print("Sensor ");
	Serial.print(sensor);
	Serial.println(" apagado!");
	enviaIR(0xA90);
}

unsigned int hexToDec(int hex) {

	String hexString = String(hex);
  unsigned int decValue = 0;
  int nextInt;

  for (uint i = 0; i < hexString.length(); i++) {

    nextInt = int(hexString.charAt(i));
    if (nextInt >= 48 && nextInt <= 57) nextInt = map(nextInt, 48, 57, 0, 9);
    if (nextInt >= 65 && nextInt <= 70) nextInt = map(nextInt, 65, 70, 10, 15);
    if (nextInt >= 97 && nextInt <= 102) nextInt = map(nextInt, 97, 102, 10, 15);
    nextInt = constrain(nextInt, 0, 15);

    decValue = (decValue * 16) + nextInt;
  }

  return decValue;
}

void enviaIRTemp(int code){
	Serial.println("Enviando código IR");
	for (int i = 0; i < 3; i++) {
	  irsend.sendSony(hexToDec(code), 12); //0xa90 es para apagar y encender
		delay(40);
	}
  delay(1000); //1 second delay between each signal burst
}

void enviaIR(int code){
	Serial.println("Enviando código IR");
	for (int i = 0; i < 3; i++) {
	  irsend.sendSony(code, 12); //0xa90 es para apagar y encender
		delay(40);
	}
  delay(1000); //1 second delay between each signal burst
}

void callback(char* topic, byte* payload, unsigned int length) {
	// Serial.print("Mensaje recibido [");
	// Serial.print(topic);
	// Serial.print("] ");
	String message = String((char *)payload);

	// Trabajar con el mensaje
	DynamicJsonBuffer jsonBuffer;
	JsonObject& mensajeJ = jsonBuffer.parseObject(message);
	if(mensajeJ["sensor"]==nombreSensor){
		//Serial.print("Json recibido: ");
		//Serial.println(message);
		const char* temp = mensajeJ["temp"];
		const char* user = mensajeJ["user"];
		const char* sensor = mensajeJ["sensor"];

		if(mensajeJ["tipo"]=="cambiaTemperatura"){
			cambiaTemperatura(temp,user,sensor);
		}
		if(mensajeJ["tipo"]=="mideTemperatura"){
			mideTemperatura(user,sensor);
		}
		if(mensajeJ["tipo"]=="enciende"){
			enciende(user, sensor);
		}
		if(mensajeJ["tipo"]=="apaga"){
			apaga(user, sensor);
		}
	}
	// else{
	// 	Serial.println("Este mensaje no es para mi");
	// }
}



void setup() {
    Serial.begin(115200);
    delay(10);
		dht.begin();

    Serial.println();
    Serial.print("Conectando a ");
    Serial.println(ssid);
		pinMode(4,OUTPUT);

    // Modo cliente
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);

    while (WiFi.status() != WL_CONNECTED) {
      delay(500);
      Serial.print(".");
    }

    Serial.println("");
    Serial.print("Red conectada. Dirección IP: ");
    Serial.println(WiFi.localIP());

    pubsubClient.setServer(serverIP, 1883);
    pubsubClient.setCallback(callback);
}

void reconnect() {
	while (!pubsubClient.connected()) {
		Serial.print("Conectando al servidor MQTT");
		if (pubsubClient.connect("ESP8266Client")) {
			Serial.println("Conectado");
			pubsubClient.publish("topic_2", "Hola a todos");
			pubsubClient.subscribe("topic_2");
		} else {
			Serial.print("Error, rc=");
			Serial.print(pubsubClient.state());
			Serial.println(" Reintentando en 5 segundos");
			delay(5000);
		}
	}
}

void loop() {
  // MQTT
  if (!pubsubClient.connected()) {
    reconnect();
  }

  pubsubClient.loop();
  delay(5000);
  snprintf (msg, 75, "Son las %ld", millis());
  pubsubClient.publish("topic_2", msg);

}
