#include <IRrecv.h>

int RECV_PIN = 14;

IRrecv irrecv(RECV_PIN);

decode_results results;

void setup2()
{
  Serial.begin(115200);
  // In case the interrupt driver crashes on setup, give a clue
  // to the user what's going on.
  Serial.println("Enabling IRin");
  irrecv.enableIRIn(); // Start the receiver
  Serial.println("Enabled IRin");
}

void loop2() {
  char codigo[5];
  if (irrecv.decode(&results)) {
    snprintf_P(codigo, sizeof(codigo), "%X", (int)results.value);
  	Serial.println(codigo);
    irrecv.resume(); // Receive the next value
  }
  delay(100);
}
