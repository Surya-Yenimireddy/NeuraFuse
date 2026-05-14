# 🧠 NeuraFuse — Digital Focus & Wellbeing Companion
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Room Database](https://img.shields.io/badge/Room_DB-4285F4?style=for-the-badge&logo=sqlite&logoColor=white)
![Material Design](https://img.shields.io/badge/Material_Design_3-757575?style=for-the-badge&logo=material-design&logoColor=white)

A powerful Android application designed to enforce focus sessions, block digital distractions, and track screen time. Built with a modern architecture to gamify productivity and help users take back control of their digital lives.

## 📌 Project Overview
NeuraFuse goes beyond simple screen time tracking. It uses a combination of **Accessibility Services** and a **Local VPN Tunnel** to actively block distracting applications during dedicated "Focus Sessions". With a premium Dark Mode UI, Room Database persistence, and built-in gamification (Streaks), it acts as a complete productivity suite.

## ✨ Key Features
* 🛡️ **Active Distraction Blocking**: Intercepts and blocks non-primary apps during focus sessions.
* 🔥 **Gamification & Streaks**: Builds user habits by tracking consecutive days of completed focus sessions.
* 📊 **Digital Wellbeing Dashboard**: Analyzes daily/weekly screen time with MPAndroidChart visualizations.
* 🏷️ **Smart App Categorization**: Breaks down usage by Social Media, Entertainment, Productivity, etc.
* 💾 **Session History**: Persists all past focus sessions (Pomodoro, Deep Work, Study) using SQLite/Room.
* 🌙 **Premium Material UI**: Glassmorphism cards, glowing timer animations, and a sleek dark theme.

## 🧠 Core Architecture
| Component | Implementation | Role |
| :--- | :--- | :--- |
| **Data Layer** | Room DB + LiveData | Local persistence for Focus Sessions and Streaks. |
| **App Blocking** | `AccessibilityService` | Detects when a user opens a distracting app and redirects them. |
| **Network Blocking** | `VpnService` | Creates a local tunnel to drop internet packets for blacklisted apps. |
| **Usage Analytics** | `UsageStatsManager` | securely fetches OS-level screen time statistics. |
| **UI/UX** | Material 3 + XML | Bottom Navigation, ViewBinding, and MPAndroidChart. |

## 📸 Screenshots (Placeholders)
*(Add your actual screenshots to the repository and replace these links later!)*

| Dashboard & Streaks | Focus Mode Active | Digital Wellbeing |
|:---:|:---:|:---:|
| <img src="https://via.placeholder.com/250x500.png?text=Dashboard" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Focus+Timer" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Analytics" width="200"/> |

## 🔒 Required Permissions & Why
NeuraFuse relies on deep Android system integrations to function effectively:
1. **Accessibility Service**: Required to detect when the user opens an app that isn't on their "Primary Apps" whitelist.
2. **VPN Service**: Required to cut off internet access to distracting apps (Local VPN only, no external servers used).
3. **Usage Access**: Required to read the device's screen time and generate the Digital Wellbeing charts.
4. **Exact Alarms**: Required to trigger scheduled Focus Mode tasks accurately.

## ▶️ How to Run Locally

### Step 1 — Clone the Repository
```bash
git clone https://github.com/Surya-Yenimireddy/NeuraFuse.git
```

### Step 2 — Open in Android Studio
1. Open **Android Studio**.
2. Select **File > Open** and choose the `NeuraFuse` directory.
3. Allow Gradle to sync the dependencies (Room, MPAndroidChart, Material Components).

### Step 3 — Build and Run
1. Connect an Android device (via USB Debugging) or start an Emulator.
2. Click the **Run ▶️** button (or press `Shift + F10`).
3. Upon first launch, grant the necessary permissions when prompted.

## 📂 Repository Structure
```text
NeuraFuse/
├── app/src/main/java/com/example/neurafuse/
│   ├── data/                 # Room Database Entities & DAOs
│   ├── services/             # Accessibility and VPN Services
│   ├── utils/                # UsageStats and Dialog Helpers
│   ├── MainActivity.java     # Dashboard & Streak Engine
│   └── FocusModeActivity.java# Core Timer & Blocking Logic
├── app/src/main/res/         # XML Layouts, Drawables, Themes
├── build.gradle.kts          # App dependencies
└── README.md
```

## 👨‍💻 Author
**Surya Yenimireddy**
* [GitHub Profile](https://github.com/Surya-Yenimireddy)
* *(Add your LinkedIn link here)*

## 📄 License
This project is licensed under the MIT License.
