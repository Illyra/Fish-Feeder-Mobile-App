# Fish Feeder (Arduino UNO R3)

Automated fish feeder controlled by SMS. Parses a human-readable feeding schedule from incoming SMS, stores schedules/logs in EEPROM, keeps time with a DS3231 RTC, dispenses food with a servo, and monitors weight via an HX711 load cell amplifier.

File: `Arduino_Setup/Fish_Feeder_ARD/Fish_Feeder_ARD.ino`

## Features

- Parses SMS-formatted schedules and stores up to 10 feeding times.
- Date range support per schedule (start day to end day).
- Servo-based dispensing with time-proportional dosing (calibrate grams/sec).
- DS3231 RTC for accurate timekeeping.
- HX711 load cell readings (currently for monitoring; can be used for closed-loop dosing).
- EEPROM-backed circular logs (20 entries × 80 chars) and schedule persistence.
- Serial commands for diagnostics: `LOGS`, `CLEAR`, `STATUS`.

## Hardware and Power Notes (12V Battery)

- Use a buck regulator to step 12V to:
  - 5V for Arduino and logic.
  - 5–6V for the servo on a dedicated rail (do NOT power servo from Arduino 5V pin).
- GSM modules (e.g., SIM800L) draw high burst currents (up to ~2A). Use a separate regulator and large bulk capacitors near the module. Tie all grounds together.
- Relay output: use a relay module or a transistor + flyback diode if driving a coil directly.

## Pin Map

- GSM (SoftwareSerial): `D10` = Arduino RX (from GSM TX), `D11` = Arduino TX (to GSM RX)
- Servo: `D9`
- HX711: `D4` = DOUT, `D5` = SCK
- Relay: `D6`
- DS3231 RTC (I2C): `A4` = SDA, `A5` = SCL

## Libraries

- `SoftwareSerial` — GSM serial on pins 10/11.
- `HX711_ADC` — HX711 weight readings with calibration.
- `Servo` — Servo control.
- `RTClib` — DS3231 RTC and `DateTime` utilities.
- `EEPROM` — Persistence for schedules and logs.

Optional: define `TEST_MODE` in the sketch to inject a sample SMS during setup for parser testing.

## How It Works (Flow)

1. Setup initializes Serial, GSM, RTC, HX711, servo, relay, loads schedules/logs from EEPROM, and configures GSM (text mode, new SMS indications, deletes stored SMS).
2. Loop (when not in TEST_MODE):
   - Updates load cell every 250ms.
   - Handles USB serial commands.
   - Logs GSM registration/signal every 2 minutes.
   - Reads incoming `+CMT:` SMS and processes schedules.
   - Checks RTC time against schedules and dispenses within the first 5 seconds of the matching minute.

## SMS Format (Example)

```
New feeding schedule for Tilapia

Aug 24, 2025 - Aug 30, 2025:
• 4:00 PM - 5.0g
• 7:00 PM - 10.0g
• 6:00 AM - 15.0g
```

Notes:

- Bullets can be `•` or `-`.
- Time supports AM/PM and is converted to 24-hour internally.
- Amount accepts a trailing `g` and uses `toFloat()`.
- Current parser looks specifically for the month string in the date line. In the provided code, it searches for `"Aug"` (change this to be month-agnostic if needed).

## Schedules and EEPROM

- Max schedules: `10`.
- Each schedule: `{ hour, minute, amount(g), active, startDay, endDay }`.
- EEPROM map (summary):
  - `0` — `scheduleCount` (1 byte)
  - `1..` — schedule blocks (`sizeof(FeedingSchedule)` each)
  - `500` — log index (1 byte)
  - `501..` — 20 log slots × 80 bytes

## Logging

- `logMessage()` writes `HH:MM message` into a circular EEPROM buffer.
- `printLogs()` dumps logs in chronological order.
- `clearLogs()` clears all log entries and resets index.

## Serial Commands

- `LOGS` — print EEPROM logs
- `CLEAR` — clear logs
- `STATUS` — query quick GSM status (`AT+CREG?`, `AT+CSQ`)

## GSM Setup

During `setupGSM()` the sketch sends:

- `AT`, `AT+CMGF=1` (text mode), `AT+CNMI=1,2,0,0,0` (push new SMS as `+CMT:`), `AT+CMGDA="DEL ALL"` (cleanup), and queries `AT+CREG?`, `AT+CSQ`, `AT+CPIN?`, `AT+CCID`.

## Dispensing Logic

- `dispense(grams)` uses a simple timing model: `GRAMS_PER_SEC = 1.0f`.
- Servo goes to 90° (open) for `grams / GRAMS_PER_SEC` seconds, then returns to 0° (closed).
- Calibrate `GRAMS_PER_SEC` to your mechanism.

## HX711 Calibration

- Uses `CALIBRATION_FACTOR = -7050.0f` by default (sign depends on wiring/orientation).
- Calibrate: place known weights, adjust factor until `scale.getData()` matches grams.
- Current code reads weight periodically but does not stop dispensing based on live weight.

## Known Caveats / Improvements

- Date parsing currently searches for a specific month token (e.g., `"Aug"`). Generalize by scanning for any month or using a more flexible parser.
- Avoid double-reading GSM bytes: the raw GSM dump can consume data before `checkSMS()`. Consider disabling raw dump in production or buffering more carefully.
- Persist last-fed timestamp to avoid duplicate feeds on resets within the same minute.
- Optional: closed-loop dosing using load cell delta mass instead of time-only control.

## Hardware Tips

- Separate regulators for GSM and servo; add large capacitors near GSM (e.g., 470–1000 µF) and proper decoupling.
- Common ground for all modules.
- Ensure DS3231 coin cell is installed so time persists across power cycles.

## Building & Uploading

1. Install required Arduino libraries: `HX711_ADC`, `RTClib`, `Servo` (bundled), `SoftwareSerial` (bundled).
2. Select Arduino/Genuino UNO in the IDE.
3. Open `Fish_Feeder_ARD.ino` and upload.
4. With GSM SIM active and signal present, send an SMS matching the format above.

## Safety

- Confirm servo motion does not jam or overfeed.
- Test with small amounts first.
- Ensure relay-driven loads are wired safely and within ratings.

---

If you want, consider enhancements: month-agnostic date parsing, schedule viewing via SMS, RTC time setting via SMS, and closed-loop dispensing using the load cell.

