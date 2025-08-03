// Refactored Fish Feeder Sketch with Dynamic Reply and Test Mode
#include <SoftwareSerial.h>
#include <HX711_ADC.h>
#include <Servo.h>
#include <RTClib.h>
#include <EEPROM.h>

// Uncomment to enable Test Mode
//#define TEST_MODE
 
// Pin Definitions
constexpr uint8_t GSM_RX_PIN       = 10;
constexpr uint8_t GSM_TX_PIN       = 11;
constexpr uint8_t SERVO_PIN        = 9;
constexpr uint8_t LOADCELL_DOUT    = 4;
constexpr uint8_t LOADCELL_SCK     = 5;
constexpr uint8_t RELAY_PIN        = 6;

// Configuration Constants
constexpr uint8_t  MAX_SCHEDULES      = 10;
constexpr int      EEPROM_BASE_ADDR   = 0;
constexpr float    CALIBRATION_FACTOR = -7050.0f;
constexpr uint16_t CELL_STABLE_MS     = 2000;
constexpr uint16_t LOADCELL_UPDATE_MS = 250;

// Schedule structure
struct FeedingSchedule {
  uint8_t  hour;
  uint8_t  minute;
  float    amount;
  bool     active;
  uint32_t startDay;
  uint32_t endDay;
};

// Globals
SoftwareSerial gsm(GSM_RX_PIN, GSM_TX_PIN);
HX711_ADC       scale(LOADCELL_DOUT, LOADCELL_SCK);
Servo            servo;
RTC_DS3231       rtc;
FeedingSchedule  schedules[MAX_SCHEDULES];
uint8_t          scheduleCount = 0;
float            currentWeight  = 0;
String           lastSender     = "";
unsigned long    lastLoadcellMS = 0;

// Forward declarations
void loadSchedules();
void saveSchedules();
void printSchedulesFromEEPROM();
void setupGSM();
void sendAT(const char* cmd);
void sendSMS(const String& to, const String& text);
void checkSMS();
void processSMS(const String& header, const String& msg);
void checkFeed();
uint32_t dateToDays(int y, int m, int d);
uint32_t parseDate(const String&);
void dispense(float grams);

void setup() {
  Serial.begin(9600);
  gsm.begin(9600);

  #ifdef TEST_MODE
    Serial.println(F("TEST_MODE IS ACTIVE"));
  #else
    Serial.println(F("TEST_MODE IS INACTIVE"));
  #endif

  if (!rtc.begin()) {
    Serial.println(F("RTC not found"));
    while (true);
  }

  scale.begin();
  scale.start(CELL_STABLE_MS);
  scale.setCalFactor(CALIBRATION_FACTOR);

  servo.attach(SERVO_PIN);
  servo.write(0);
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);

  loadSchedules();
  printSchedulesFromEEPROM();
  setupGSM();

  Serial.println(F("=== Fish Feeder Initialized ==="));

#ifdef TEST_MODE
  String testHeader = "+CMT: \"+1234567890\",,\"24/07/25,16:00:00+08\"";
  String testMessage =
    "New feeding schedule for Tilapia\n\n"
    "Jul 24, 2025 - Jul 30, 2025:\n"
    "• 4:00 PM - 5.0g\n"
    "• 7:00 PM - 10.0g\n"
    "• 6:00 AM - 15.0g";
  processSMS(testHeader, testMessage);
#endif
}

void loop() {
#ifndef TEST_MODE
  unsigned long now = millis();
  if (now - lastLoadcellMS >= LOADCELL_UPDATE_MS) {
    scale.update();
    currentWeight = scale.getData();
    lastLoadcellMS = now;
  }
  
  // DEBUG: Print ALL GSM module output
  if (gsm.available()) {
    Serial.print("GSM RAW: ");
    while (gsm.available()) {
      char c = gsm.read();
      Serial.print(c);
    }
    Serial.println();
  }
  
  checkSMS();
  checkFeed();
#endif
}

void setupGSM() {
  Serial.println(F("Setting up GSM..."));
  
  const char* cmds[] = {
    "AT", 
    "AT+CMGF=1", 
    "AT+CNMI=1,2,0,0,0", 
    "AT+CMGDA=\"DEL ALL\"",
    "AT+CREG?",     // Network registration status
    "AT+CSQ",       // Signal quality
    "AT+CPIN?",     // SIM card status
    "AT+CCID"       // SIM card ID
  };
  
  for (auto cmd : cmds) {
    Serial.print(F("Sending: "));
    Serial.println(cmd);
    sendAT(cmd);
    delay(1000);  // Increased delay
  }
  
  Serial.println(F("GSM setup complete"));
}

void sendAT(const char* cmd) {
  gsm.println(cmd);
  delay(500);  // Give more time for response
  
  Serial.print(F("Response: "));
  String response = "";
  unsigned long timeout = millis() + 2000; // 2 second timeout
  
  while (millis() < timeout) {
    if (gsm.available()) {
      char c = gsm.read();
      Serial.write(c);
      response += c;
    }
  }
  
  if (response.length() == 0) {
    Serial.println(F("NO RESPONSE!"));
  }
  
  Serial.println();
}

void sendSMS(const String& to, const String& text) {
  sendAT("AT+CMGF=1");
  String cmd = "AT+CMGS=\"" + to + "\"";
  sendAT(cmd.c_str());
  gsm.print(text);
  gsm.write(26);
  delay(500);
}

void checkSMS() {
  while (gsm.available()) {
    String header = gsm.readStringUntil('\n');
    if (header.indexOf("+CMT:") >= 0) {
      int p1 = header.indexOf('"');
      int p2 = header.indexOf('"', p1 + 1);
      lastSender = header.substring(p1 + 1, p2);
      delay(200);
      String msg = gsm.readString();
      processSMS(header, msg);
    }
  }
}

void processSMS(const String& header, const String& msg) {
  Serial.println(F("\nProcessing SMS..."));
  if (!msg.startsWith("New feeding schedule")) return;

  scheduleCount = 0;
  String text = msg;
  text.replace("\r", "");

  // Parse date range
  int dateStart = text.indexOf("Jul");
  int dateEnd = text.indexOf(":", dateStart);
  if (dateStart > 0 && dateEnd > dateStart) {
    String dateLine = text.substring(dateStart, dateEnd);
    int dashPos = dateLine.indexOf("-");
    if (dashPos > 0) {
      String startDate = dateLine.substring(0, dashPos);
      String endDate = dateLine.substring(dashPos + 1);
      
      uint32_t startDay = parseDate(startDate);
      uint32_t endDay = parseDate(endDate);
      
      // Find all schedule lines
      int bulletPos = text.indexOf("•");
      while (bulletPos > 0 && scheduleCount < MAX_SCHEDULES) {
        int endLine = text.indexOf('\n', bulletPos);
        if (endLine < 0) endLine = text.length();
        
        String line = text.substring(bulletPos, endLine);
        line.replace("•", "");
        line.trim();
        
        int dashPos = line.indexOf("-");
        if (dashPos > 0) {
          String timePart = line.substring(0, dashPos);
          String amountPart = line.substring(dashPos + 1);
          
          // Parse time with AM/PM
          String upperTime = timePart;
          upperTime.toUpperCase();
          
          bool isPM = upperTime.indexOf("PM") >= 0;
          bool isAM = upperTime.indexOf("AM") >= 0;
          
          // Remove AM/PM from time string
          timePart.replace(" PM", "");
          timePart.replace(" AM", "");
          timePart.replace(" pm", "");
          timePart.replace(" am", "");
          
          int colonPos = timePart.indexOf(':');
          if (colonPos >= 0) {
            int hour = timePart.substring(0, colonPos).toInt();
            int minute = timePart.substring(colonPos + 1).toInt();
            
            if (isPM && hour != 12) hour += 12;
            if (isAM && hour == 12) hour = 0;
              
            amountPart.replace("g", "");
            float amount = amountPart.toFloat();
            
            // Display in 12-hour format
            int displayHour = hour;
            String period = "AM";
            if (hour >= 12) {
              period = "PM";
              if (hour > 12) displayHour = hour - 12;
            }
            if (hour == 0) {
              displayHour = 12;
              period = "AM";
            }
            
            Serial.print(F("Added schedule: "));
            Serial.print(displayHour);
            Serial.print(':');
            if (minute < 10) Serial.print('0');
            Serial.print(minute);
            Serial.print(' ');
            Serial.print(period);
            Serial.print(F(" — "));
            Serial.print(amount);
            Serial.println(F("g"));
            
            schedules[scheduleCount++] = { 
              uint8_t(hour), 
              uint8_t(minute), 
              amount, 
              true, 
              startDay, 
              endDay 
            };
            
            bulletPos = text.indexOf("•", endLine);
          }
        }
      }
    }
  }
  
  saveSchedules();
  printSchedulesFromEEPROM();
  if (lastSender.length()) sendSMS(lastSender, "Feeding schedule updated successfully!");
}

void checkFeed() {
  DateTime now = rtc.now();
  uint32_t today = dateToDays(now.year(), now.month(), now.day());

  for (uint8_t i = 0; i < scheduleCount; i++) {
    auto& sc = schedules[i];
    if (!sc.active || today < sc.startDay || today > sc.endDay) continue;

    if (now.hour() == sc.hour && now.minute() == sc.minute && now.second() < 5) {
      dispense(sc.amount);
      if (lastSender.length()) {
        String msg = "Fed " + String(sc.amount) + "g at " + String(sc.hour) + ":" + (sc.minute<10?"0":"") + String(sc.minute);
        sendSMS(lastSender, msg);
      }
      delay(1000);
    }
  }
}

void dispense(float grams) {
  constexpr float GRAMS_PER_SEC = 1.0f;
  unsigned long runMs = (unsigned long)(grams / GRAMS_PER_SEC * 1000);
  servo.write(90);
  delay(runMs);
  servo.write(0);
}

void saveSchedules() {
  EEPROM.put(EEPROM_BASE_ADDR, scheduleCount);
  for (uint8_t i = 0; i < scheduleCount; i++) {
    EEPROM.put(EEPROM_BASE_ADDR + 1 + i * sizeof(FeedingSchedule), schedules[i]);
  }
}

void loadSchedules() {
  EEPROM.get(EEPROM_BASE_ADDR, scheduleCount);
  if (scheduleCount > MAX_SCHEDULES) scheduleCount = 0;
  for (uint8_t i = 0; i < scheduleCount; i++) {
    EEPROM.get(EEPROM_BASE_ADDR + 1 + i * sizeof(FeedingSchedule), schedules[i]);
  }
}

void printSchedulesFromEEPROM() {
  Serial.println(F("\n--- Schedules in EEPROM ---"));
  Serial.print(F("Schedule Count: ")); Serial.println(scheduleCount);
  for (uint8_t i = 0; i < scheduleCount; i++) {
    FeedingSchedule sc;
    EEPROM.get(EEPROM_BASE_ADDR + 1 + i * sizeof(FeedingSchedule), sc);
    Serial.print(F("Schedule ")); Serial.print(i + 1); Serial.print(F(": "));
    // Display in 12-hour format
    int displayHour = sc.hour;
    String period = "AM";
    if (sc.hour >= 12) {
      period = "PM";
      if (sc.hour > 12) displayHour = sc.hour - 12;
    }
    if (sc.hour == 0) {
      displayHour = 12;
      period = "AM";
    }
    Serial.print(displayHour); Serial.print(":" );
    if (sc.minute < 10) Serial.print("0"); Serial.print(sc.minute);
    Serial.print(' '); Serial.print(period);
    Serial.print(F(" — ")); Serial.print(sc.amount); Serial.print(F("g "));
    DateTime startDT(sc.startDay * 86400);
    DateTime endDT(sc.endDay * 86400);
    Serial.print(F("["));
    Serial.print(startDT.year()); Serial.print("-");
    Serial.print(startDT.month() < 10 ? "0" : ""); Serial.print(startDT.month()); Serial.print("-");
    Serial.print(startDT.day() < 10 ? "0" : ""); Serial.print(startDT.day());
    Serial.print(F(" to "));
    Serial.print(endDT.year()); Serial.print("-");
    Serial.print(endDT.month() < 10 ? "0" : ""); Serial.print(endDT.month()); Serial.print("-");
    Serial.print(endDT.day() < 10 ? "0" : ""); Serial.print(endDT.day());
    Serial.println(F("]"));
  }
  Serial.println(F("---------------------------"));
}

uint32_t dateToDays(int y, int m, int d) {
  DateTime dt(y, m, d, 0, 0, 0);
  return dt.unixtime() / 86400;
}

uint32_t parseDate(const String& s) {
  static const char* M[] = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
  String str = s;
  str.trim();
  int mon = 1;
  for (int i = 0; i < 12; i++) if (str.startsWith(M[i])) { mon = i + 1; break; }
  int day = str.substring(4, 6).toInt();
  int year = str.substring(8, 12).toInt();
  return dateToDays(year, mon, day);
}