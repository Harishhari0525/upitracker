# UPI Tracker

[![CodeQL Advanced](https://github.com/Harishhari0525/upitracker/actions/workflows/codeql.yml/badge.svg)](https://github.com/Harishhari0525/upitracker/actions/workflows/codeql.yml)

## Overview

UPI Tracker is a modern Android app (Kotlin + Jetpack Compose) that automatically parses SMS alerts for UPI (Unified Payments Interface) transactions, securely stores your financial data on-device, and presents comprehensive analytics and visualizations of your spending.

## Features

- **Secure Access:** PIN and optional biometric authentication.
- **Customizable Regex:** Modify parsing patterns for new or updated UPI SMS formats.
- **Import Historical Data:** Onboarding flow to backfill transactions from existing SMS messages.
- **Transaction Management:** View, search, and categorize all expenses.
- **Analytics:** Interactive pie and line charts for daily/monthly spending.
- **CSV Export:** Export your transaction history for backup or analysis.
- **Themes:** Light and dark mode with additional color schemes.

## Key Components

- **SMS Listener:** Real-time `BroadcastReceiver` filters and parses incoming UPI alerts.
- **Room Database:** Stores transactions, summaries, and archived SMS messages.
- **Regex Editor:** Edit and test custom regex patterns within app settings.
- **Onboarding:** Import historical SMS upon first launch.
- **Data Security:** PIN is securely hashed and stored via encrypted `SharedPreferences` (see `PinStorage.kt`). Biometric unlock supported.
- **Backup/Restore:** Leverages Android’s backup APIs.

## Build Instructions

**Prerequisites:**
- Android Studio Hedgehog/Koala or newer.
- JDK 17 or higher.
- Android SDK Platform 33+ (compileSdk = 36, minSdk = 31).

**Steps:**

1. **Clone the Repository:**
   ```sh
   git clone https://github.com/Harishhari0525/upitracker.git
   cd upitracker
   ```

2. **Open in Android Studio:**
   - Open the root folder in Android Studio.
   - Let Gradle sync and download dependencies.

3. **Configure Signing (Optional):**
   - For a debug build, no configuration is needed.
   - For release builds, set up a signing config in `app/build.gradle.kts`.

4. **Build & Run:**
   - Connect an Android device (API 31+) or start an emulator.
   - Click "Run" in Android Studio or use:
     ```sh
     ./gradlew assembleDebug
     ```
   - Install the APK on your device or run directly from the IDE.

5. **Grant Permissions:**
   - On first launch, allow SMS and storage permissions for full functionality.

**Notes:**
- The app uses the latest Jetpack Compose and Material 3.
- All data is stored locally—no server or cloud interaction.
- To modify regex for SMS parsing, use the in-app Regex Editor under Settings.

---

**MIT License**

Copyright (c) 2025 Harishhari0525

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
