// SMS-Only Fish Feeder Schedule Parser
// Focuses purely on SMS parsing and schedule management
#include <SoftwareSerial.h>
#include <EEPROM.h>

// Uncomment to enable Test Mode
// #define TEST_MODE
 
// Pin Definitions
constexpr uint8_t GSM_RX_PIN = 10;
constexpr uint8_t GSM_TX_PIN = 11;

// Configuration Constants
constexpr uint8_t MAX_SCHEDULES = 10;
constexpr int EEPROM_BASE_ADDR = 0;

// Schedule structure
struct FeedingSchedule {
  uint8_t hour;
  uint8_t minute;
  float amount;
  bool active;
  uint32_t startDay;
  uint32_t endDay;
};

// Globals
SoftwareSerial gsm(GSM_RX_PIN, GSM_TX_PIN);
FeedingSchedule schedules[MAX_SCHEDULES];
uint8_t scheduleCount = 0;
String lastSender = "";

// SMS assembly state
String pendingSMSHeader = "";
String pendingSMSBody = "";
bool awaitingSMSBody = false;
unsigned long smsLastByteMs = 0;
String aggSender = "";

// Forward declarations
void loadSchedules();
void saveSchedules();
void printSchedulesFromEEPROM();
void setupGSM();
void sendAT(const char* cmd);
void sendSMS(const String& to, const String& text);
void checkSMS();
void handleCMTI(const String& line);
void readSMSByIndex(int index);
void processSMS(const String& header, const String& msg);
uint32_t dateToDays(int y, int m, int d);
uint32_t parseDate(const String&);

void setup() {
  Serial.begin(9600);
  gsm.begin(9600);
  gsm.setTimeout(5000);

  #ifdef TEST_MODE
    Serial.println(F("TEST_MODE IS ACTIVE - SMS PARSING ONLY"));
  #else
    Serial.println(F("SMS PARSING MODE ACTIVE"));
  #endif

  Serial.println(F("Loading schedules from EEPROM..."));
  loadSchedules();
  printSchedulesFromEEPROM();
  
  #ifndef TEST_MODE
    Serial.println(F("Starting GSM setup..."));
    setupGSM();
  #endif

  Serial.println(F("=== SMS Parser Initialized ==="));

  #ifdef TEST_MODE
    Serial.println(F("\n=== RUNNING TEST CASES ==="));
    
    // Test Case: Your message format
    String testHeader = "+CMT: \"+1234567890\",,\"24/08/09,15:00:00+08\"";
    String testMessage =
      "New feeding schedule for Tilapia\n\n"
      "Aug 24, 2025 - Aug 30, 2025:\n"
      "- 5:00 AM - 118.0g\n"
      "- 9:00 PM - 115.0g";
    
    Serial.println(F("\n--- TEST: Your Message Format ---"));
    Serial.println(F("Input Message:"));
    Serial.println(testMessage);
    Serial.println(F("\n--- Processing ---"));
    processSMS(testHeader, testMessage);
    
    // Show final results
    Serial.println(F("\n=== FINAL RESULTS ==="));
    printSchedulesFromEEPROM();
  #endif
}

void loop() {
  #ifndef TEST_MODE
    if (Serial.available()) {
      String cmd = Serial.readString();
      cmd.trim();
      if (cmd == "SCHEDULES") {
        printSchedulesFromEEPROM();
      }
    }
    checkSMS();
  #else
    static unsigned long lastPrompt = 0;
    if (millis() - lastPrompt > 15000) {
      Serial.println(F("Commands: SCHEDULES"));
      lastPrompt = millis();
    }
    
    if (Serial.available()) {
      String cmd = Serial.readString();
      cmd.trim();
      if (cmd == "SCHEDULES") {
        printSchedulesFromEEPROM();
      }
    }
  #endif
}

void setupGSM() {
  Serial.println(F("Setting up GSM..."));
  const char* cmds[] = {
    "AT",                    // Test AT command
    "AT+CMGF=1",            // Set SMS text mode
    "AT+CSCS=\"GSM\"",      // Set character set to GSM 7-bit
    "AT+CPMS=\"SM\"",       // Set SMS storage to SIM card
    "AT+CNMI=2,1,0,0,0"     // Set SMS notification mode
  };
  for (auto cmd : cmds) {
    Serial.print(F("Sending: "));
    Serial.println(cmd);
    sendAT(cmd);
    delay(1000);
  }
  Serial.println(F("GSM setup complete"));
}

void sendAT(const char* cmd) {
  gsm.println(cmd);
  delay(500);
  Serial.print(F("Response: "));
  unsigned long timeout = millis() + 2000;
  while (millis() < timeout) {
    if (gsm.available()) {
      Serial.write(gsm.read());
    }
  }
  Serial.println();
}

void sendSMS(const String& to, const String& text) {
  #ifndef TEST_MODE
    sendAT("AT+CMGF=1");
    String cmd = "AT+CMGS=\"" + to + "\"";
    sendAT(cmd.c_str());
    gsm.print(text);
    gsm.write(26);
    delay(500);
  #else
    Serial.print(F("TEST: Would send SMS to "));
    Serial.print(to);
    Serial.print(F(": "));
    Serial.println(text);
  #endif
}

void checkSMS() {
  while (gsm.available()) {
    String line = gsm.readStringUntil('\n');
    line.trim();
    if (line.length() == 0) continue;

    if (line.startsWith("+CMTI:")) {
      handleCMTI(line);
      continue;
    }

    if (line.startsWith("+CMT:")) {
      int p1 = line.indexOf('"');
      int p2 = line.indexOf('"', p1 + 1);
      String newSender = "";
      if (p1 >= 0 && p2 > p1) {
        newSender = line.substring(p1 + 1, p2);
      }

      if (awaitingSMSBody) {
        if (newSender.length() && newSender == aggSender) {
          smsLastByteMs = millis();
          if (pendingSMSBody.length() && pendingSMSBody[pendingSMSBody.length()-1] != '\n') {
            pendingSMSBody += '\n';
          }
        } else {
          String msg = pendingSMSBody; msg.trim();
          if (msg.length()) {
            processSMS(pendingSMSHeader, msg);
          }
          awaitingSMSBody = false;
          pendingSMSHeader = "";
          pendingSMSBody = "";
          aggSender = "";
        }
      }

      pendingSMSHeader = line;
      awaitingSMSBody = true;
      if (aggSender.length() == 0) aggSender = newSender;
      lastSender = newSender;
      smsLastByteMs = millis();
    } else if (awaitingSMSBody) {
      pendingSMSBody += line;
      pendingSMSBody += '\n';
      smsLastByteMs = millis();
    }
  }

  if (awaitingSMSBody && (millis() - smsLastByteMs) > 5000) {
    String msg = pendingSMSBody;
    msg.trim();
    if (msg.length()) {
      processSMS(pendingSMSHeader, msg);
    }
    awaitingSMSBody = false;
    pendingSMSHeader = "";
    pendingSMSBody = "";
    aggSender = "";
  }
}

void handleCMTI(const String& line) {
  int comma = line.lastIndexOf(',');
  if (comma < 0) return;
  int idx = line.substring(comma + 1).toInt();
  if (idx <= 0) return;
  readSMSByIndex(idx);
}

void readSMSByIndex(int index) {
  String cmd = "AT+CMGR=" + String(index);
  gsm.println(cmd);

  String header = "";
  String body = "";
  bool headerSeen = false;
  unsigned long start = millis();
  while (millis() - start < 8000) {
    String line = gsm.readStringUntil('\n');
    line.trim();
    if (line.length() == 0) continue;
    if (!headerSeen) {
      if (line.startsWith("+CMGR:")) {
        header = line;
        headerSeen = true;
        int q[8]; int qi = 0; int pos = -1;
        while (qi < 8) {
          pos = header.indexOf('"', pos + 1);
          if (pos < 0) break; q[qi++] = pos;
        }
        if (qi >= 4) {
          String sender = header.substring(q[2] + 1, q[3]);
          if (sender.length()) {
            lastSender = sender;
          }
        }
      }
    } else {
      if (line == "OK") break;
      body += line;
      body += '\n';
    }
  }

  body.trim();
  if (body.length()) {
    processSMS(header, body);
  }

  gsm.print(F("AT+CMGD=")); gsm.println(index);
  delay(200);
}

void processSMS(const String& header, const String& msg) {
  Serial.println(F("\n=== PROCESSING SMS ==="));
  
  String msgUpper = msg;
  msgUpper.trim();
  msgUpper.toUpperCase();
  if (!msgUpper.startsWith("NEW FEEDING")) {
    Serial.println(F("ERROR: Message must start with 'New feeding'"));
    Serial.print(F("Received message: '"));
    Serial.print(msg);
    Serial.println(F("'"));
    return;
  }

  Serial.println(F("SMS format OK - parsing schedules..."));
  scheduleCount = 0;
  String text = msg;
  text.replace("\r", "");

  Serial.println(F("Full message:"));
  Serial.println(text);
  Serial.println(F("=== PARSING ==="));

  // Parse date range
  static const char* MONTHS[] = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
  int dateStart = -1;
  for (int i = 0; i < 12; i++) {
    int idx = text.indexOf(MONTHS[i]);
    if (idx >= 0) { 
      dateStart = idx; 
      Serial.print(F("Found month: "));
      Serial.println(MONTHS[i]);
      break; 
    }
  }
  
  int dateEnd = -1;
  if (dateStart >= 0) {
    int searchPos = dateStart;
    while (searchPos < text.length()) {
      int lineEnd = text.indexOf('\n', searchPos);
      if (lineEnd < 0) lineEnd = text.length();
      
      String line = text.substring(searchPos, lineEnd);
      line.trim();
      if (line.endsWith(":")) {
        dateEnd = lineEnd;
        Serial.print(F("Found date line: "));
        Serial.println(line);
        break;
      }
      searchPos = lineEnd + 1;
    }
  }
  
  if (dateStart >= 0 && dateEnd > dateStart) {
    String dateLine = text.substring(dateStart, dateEnd);
    dateLine.replace(":", "");
    dateLine.trim();
    Serial.print(F("Date range: "));
    Serial.println(dateLine);
    
    int dashPos = dateLine.indexOf("-");
    if (dashPos > 0) {
      String startDate = dateLine.substring(0, dashPos);
      String endDate = dateLine.substring(dashPos + 1);
      startDate.trim();
      endDate.trim();
      
      uint32_t startDay = parseDate(startDate);
      uint32_t endDay = parseDate(endDate);
      
      Serial.print(F("Start day: "));
      Serial.print(startDay);
      Serial.print(F(", End day: "));
      Serial.println(endDay);
      
      // Find schedule lines
      int searchPos = dateEnd + 1;
      while (searchPos < text.length() && scheduleCount < MAX_SCHEDULES) {
        int lineStart = searchPos;
        int lineEnd = text.indexOf('\n', searchPos);
        if (lineEnd < 0) lineEnd = text.length();
        
        String line = text.substring(lineStart, lineEnd);
        line.trim();
        
        if (line.startsWith("- ")) {
          line = line.substring(2);
          line.trim();
          
          Serial.print(F("Processing: "));
          Serial.println(line);
          
          int timeDashPos = line.indexOf(" - ");
          if (timeDashPos > 0) {
            String timePart = line.substring(0, timeDashPos);
            String amountPart = line.substring(timeDashPos + 3);
            
            timePart.trim();
            amountPart.trim();
            
            String upperTime = timePart;
            upperTime.toUpperCase();
            
            bool isPM = upperTime.indexOf("PM") >= 0;
            bool isAM = upperTime.indexOf("AM") >= 0;
            
            timePart.replace(" PM", "");
            timePart.replace(" AM", "");
            timePart.replace(" pm", "");
            timePart.replace(" am", "");
            timePart.trim();
            
            int colonPos = timePart.indexOf(':');
            if (colonPos >= 0) {
              int hour = timePart.substring(0, colonPos).toInt();
              int minute = timePart.substring(colonPos + 1).toInt();
              
              if (isPM && hour != 12) hour += 12;
              if (isAM && hour == 12) hour = 0;
                
              amountPart.replace("g", "");
              float amount = amountPart.toFloat();
              
              if (amount > 0) {
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
                
                String schedMsg = "Added: " + String(displayHour) + ":" + (minute<10?"0":"") + String(minute) + " " + period + " - " + String(amount) + "g";
                Serial.print(F("SUCCESS: "));
                Serial.println(schedMsg);
                
                schedules[scheduleCount++] = { 
                  uint8_t(hour), 
                  uint8_t(minute), 
                  amount, 
                  true, 
                  startDay, 
                  endDay 
                };
              }
            }
          }
        }
        
        searchPos = lineEnd + 1;
      }
    }
  }
  
  Serial.print(F("Total schedules parsed: "));
  Serial.println(scheduleCount);
  
  if (scheduleCount > 0) {
    Serial.println(F("Saving to EEPROM..."));
    saveSchedules();
    if (lastSender.length()) {
      String confirmMsg = "Schedule updated! Added " + String(scheduleCount) + " feeding times.";
      sendSMS(lastSender, confirmMsg);
    }
    printSchedulesFromEEPROM();
  } else {
    Serial.println(F("No schedules parsed"));
  }
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
    Serial.print(displayHour); Serial.print(":");
    if (sc.minute < 10) Serial.print("0"); Serial.print(sc.minute);
    Serial.print(' '); Serial.print(period);
    Serial.print(F(" — ")); Serial.print(sc.amount); Serial.print(F("g "));
    Serial.print(F("[Day ")); Serial.print(sc.startDay);
    Serial.print(F(" to ")); Serial.print(sc.endDay); Serial.println(F("]"));
  }
  Serial.println(F("---------------------------"));
}

uint32_t dateToDays(int y, int m, int d) {
  uint32_t days = 0;
  for (int year = 2000; year < y; year++) {
    if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
      days += 366;
    } else {
      days += 365;
    }
  }
  
  uint8_t daysInMonth[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
  if ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)) {
    daysInMonth[1] = 29;
  }
  
  for (int month = 1; month < m; month++) {
    days += daysInMonth[month - 1];
  }
  
  days += d - 1;
  return days;
}

uint32_t parseDate(const String& s) {
  static const char* M[] = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
  String str = s;
  str.trim();

  int mon = 1;
  int monPos = -1;
  for (int i = 0; i < 12; i++) {
    int pos = str.indexOf(M[i]);
    if (pos >= 0) { 
      mon = i + 1; 
      monPos = pos; 
      break; 
    }
  }

  int i = (monPos >= 0) ? monPos + 3 : 0;
  while (i < (int)str.length() && (str[i] == ' ' || str[i] == ',')) i++;
  int dayStart = i;
  while (i < (int)str.length() && (str[i] >= '0' && str[i] <= '9')) i++;
  int day = str.substring(dayStart, i).toInt();

  while (i < (int)str.length() && !(str[i] >= '0' && str[i] <= '9')) i++;
  int yearStart = i;
  while (i < (int)str.length() && (str[i] >= '0' && str[i] <= '9')) i++;
  int year = str.substring(yearStart, i).toInt();

  if (day <= 0 || year <= 0) return 0;
  
  return dateToDays(year, mon, day);
}