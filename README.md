[![CodeQL Advanced](https://github.com/Harishhari0525/upitracker/actions/workflows/codeql.yml/badge.svg)](https://github.com/Harishhari0525/upitracker/actions/workflows/codeql.yml)

## Overview

UPI Tracker is an Android application built with Kotlin and Jetpack Compose that automatically parses UPI (Unified Payments Interface) SMS alerts, stores transaction details locally, and presents insightful spending analytics. The app includes:

- Secure entry via PIN and optional biometric authentication  
- Customizable regex patterns for parsing UPI messages  
- Onboarding flow to import historical SMS data  
- Transaction history and category-based views  
- Interactive charts (pie and line) for daily and monthly spending  
- Export functionality for CSV backups  
- Dark/light theme support and user preferences for customization  

Below are representative screenshots of key screens in the app:

- **PIN Setup Screen**
  Users create a 4-digit PIN to secure their data. 
  ![ChatGPT Image Jun 2, 2025, 02_21_57 PM](https://github.com/user-attachments/assets/63f3e5a1-12ae-4ecd-8c2b-f2ff434d71b8) 

- **Transaction History Screen**  
  Lists parsed UPI transactions chronologically with merchant name, category, date, and amount (green = credit, red = debit).  
  ![ChatGPT Image Jun 2, 2025, 02_22_04 PM](https://github.com/user-attachments/assets/f5dead39-ba29-405c-a7dd-890d2776614b) 
  
---

## Key Features

1. **Secure Access**  
   - **PIN Setup & Lock**: Enforce a 4-digit PIN on first launch; subsequent access requires PIN entry (or biometric).  
   - **Biometric Support** (optional): Use device fingerprint/face to unlock.

2. **SMS Parsing & Import**  
   - **Real-Time Listener**: A `BroadcastReceiver` monitors incoming SMS messages; filters and parses ones matching UPI patterns.  
   - **Custom Regex Editor**: Users can view and modify default regex patterns under Settings → “Regex Editor” to handle new banks or changed SMS formats.  
   - **Import Historical SMS**: During onboarding, the app reads existing SMS inbox entries via `ImportOldSmsActivity` to backfill past transactions.

3. **Local Data Storage**  
   - **Room Database**: Stores three entities:  
     - `Transaction` (parsed UPI transactions)  
     - `UpiLiteSummary` (condensed summary for quick overviews)  
     - `ArchivedSmsMessage` (older raw SMS entries, periodically cleaned)  
   - **DAOs** provide insert, query, and delete operations.

4. **Transaction Listing & Categorization**  
   - **Tabbed Home Screen** (`TabbedHomeScreen.kt`): Contains three primary tabs:  
     - **Current Month Expenses** (`CurrentMonthExpensesScreen.kt`): Pie chart showing category-wise distribution.  
     - **Transaction History** (`TransactionHistoryScreen.kt`): Scrollable list of all parsed transactions, with merchant, date, category, and amount.  
     - **Analytics** (`GraphsScreen.kt`): Line chart of daily spend and a pie chart for monthly categories.  

5. **User Preferences & Settings**  
   - **Theme Preference**: Light/dark mode selection stored via `ThemePreference.kt`.  
   - **Regex Preference**: Persist custom regex expressions in `RegexPreference.kt` to adjust parsing logic.  
   - **Onboarding Flags**: `OnboardingPreference.kt` tracks whether the user has completed initial setup.  
   - **Backup & Restore Rules** (`backup_rules.xml`) allow automated backups of data using Android’s native backup API.

6. **Export & Maintenance Utilities**  
   - **CSV Exporter** (`CsvExporter.kt`): Allows exporting all transactions into a CSV file saved on device storage.  
   - **Archived SMS Cleanup** (`CleanupArchivedSmsWorker.kt`): A WorkManager `PeriodicWorkRequest` runs daily to remove archived SMS entries older than 30 days, keeping storage optimized.  
   - **Biometric Helper** (`BiometricHelper.kt`): Encapsulates biometric prompt logic for fallback unlocking.

7. **Pin & Biometric Flow**  
   - **`PinSetupScreen.kt`**: First-time PIN creation.  
   - **`PinLockScreen.kt` & `OldPinVerificationComponent.kt`**: Prompt for stored PIN or biometric on every app launch.  
   - **`PinStorage.kt`**: Securely stores hashed PIN in encrypted `SharedPreferences`.

---

## Project Structure

## Project Structure

```plaintext
app/                                ← Android application module
├── build.gradle.kts                ← Module-level Gradle build script
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml     ← Permissions (e.g., READ_SMS) & activity declarations
│   │   ├── java/com/example/upitracker/
│   │   │   ├── MainActivity.kt     ← Hosts navigation & top-level permission logic
│   │   │   ├── data/
│   │   │   │   ├── AppDatabase.kt            ← Room database definition
│   │   │   │   ├── Transaction.kt             ← Entity for UPI transactions
│   │   │   │   ├── TransactionDao.kt
│   │   │   │   ├── UpiLiteSummary.kt           ← Entity for condensed transaction summaries
│   │   │   │   ├── UpiLiteSummaryDao.kt
│   │   │   │   ├── ArchivedSmsMessage.kt       ← Entity for raw archived SMS messages
│   │   │   │   └── ArchivedSmsMessageDao.kt
│   │   │   ├── sms/
│   │   │   │   ├── SmsReceiver.kt              ← BroadcastReceiver for incoming SMS
│   │   │   │   ├── UpiSmsParser.kt             ← Full-message parsing logic
│   │   │   │   └── UpiLiteSummaryParser.kt     ← Condensed parsing for quick summary
│   │   │   ├── ui/
│   │   │   │   ├── components/                 ← Reusable Compose UI elements
│   │   │   │   │   ├── CategorySpendingPieChart.kt
│   │   │   │   │   ├── DeleteDialogs.kt
│   │   │   │   │   ├── EditCategoryDialog.kt
│   │   │   │   │   ├── OldPinVerificationComponent.kt
│   │   │   │   │   ├── PinLockScreen.kt
│   │   │   │   │   ├── PinSetupScreen.kt
│   │   │   │   │   ├── RegexEditor.kt
│   │   │   │   │   ├── TransactionCard.kt
│   │   │   │   │   ├── TransactionTable.kt
│   │   │   │   │   └── UpiLiteSummaryCard.kt
│   │   │   │   ├── screens/                     ← Full-screen Composables
│   │   │   │   │   ├── BottomNavItem.kt
│   │   │   │   │   ├── CurrentMonthExpensesScreen.kt
│   │   │   │   │   ├── GraphsScreen.kt
│   │   │   │   │   ├── MainAppScreen.kt
│   │   │   │   │   ├── MainNavHost.kt            ← Compose Navigation Graph
│   │   │   │   │   ├── OnboardingScreen.kt
│   │   │   │   │   ├── RegexEditorScreen.kt
│   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   ├── TabbedHomeScreen.kt
│   │   │   │   │   └── TransactionHistoryScreen.kt
│   │   │   ├── util/
│   │   │   │   ├── BiometricHelper.kt
│   │   │   │   ├── CsvExporter.kt
│   │   │   │   ├── CleanupArchivedSmsWorker.kt
│   │   │   │   ├── OnboardingPreference.kt
│   │   │   │   ├── PinStorage.kt
│   │   │   │   ├── RegexPreference.kt
│   │   │   │   ├── SampleUpiRegex.kt             ← Default regex patterns
│   │   │   │   ├── Theme.kt
│   │   │   │   └── ThemePreference.kt
│   │   ├── res/
│   │   │   ├── drawable/                        ← App launcher icons, vector assets
│   │   │   ├── mipmap-*/                        ← App icons at various densities
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   ├── values-night/                    ← Dark theme overrides
│   │   │   └── xml/                             ← Backup rules, data extraction rules
│   └── test/                                   ← Unit tests (basic examples)
├── build.gradle.kts                            ← Project-level Gradle definitions (Kotlin DSL)
├── gradle.properties                           ← Gradle properties config
└── settings.gradle.kts                         ← Includes the “app” module in the build
```
