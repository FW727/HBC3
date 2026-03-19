# HBC to Bytecode

<p align="center">
  <img src="app/src/main/res/mipmap/ic_app.png" width="96" alt="HBC to Bytecode icon"/>
</p>

<p align="center">
  <strong>Hermes Bytecode Toolkit for Android</strong><br/>
  Disassemble, assemble and search inside <code>.hbc</code> files — directly from your device, no PC needed.
</p>

<p align="center">
  <a href="https://github.com/Fireway727/HBCTool/releases/latest">
    <img src="https://img.shields.io/github/v/release/Fireway727/HBCTool?style=flat-square&color=00C8FF&label=release" alt="Latest Release"/>
  </a>
  <img src="https://img.shields.io/badge/status-alpha-FF6B00?style=flat-square" alt="Alpha"/>
  <img src="https://img.shields.io/badge/maintenance-archived-red?style=flat-square" alt="Archived"/>
  <img src="https://img.shields.io/badge/platform-Android-brightgreen?style=flat-square&color=00E57A" alt="Platform"/>
  <img src="https://img.shields.io/badge/minSdk-26%20(Oreo)-orange?style=flat-square" alt="minSdk"/>
  <img src="https://img.shields.io/badge/language-Kotlin-7B3FFF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License"/>
  </a>
  <a href="https://t.me/Fireway727">
    <img src="https://img.shields.io/badge/Telegram-Fireway727-2CA5E0?style=flat-square&logo=telegram&logoColor=white" alt="Telegram"/>
  </a>
</p>

---

> [!WARNING]
> **Este proyecto está archivado y ya no tiene mantenimiento activo.**
> Esta es una **versión alpha** — es funcional pero puede contener bugs y estar incompleta.
> El creador original (Fireway727) no publicará más actualizaciones.
> El código fuente se publica tal cual, para que la comunidad lo use, forkee o continúe.

---

## What is it?

**HBC to Bytecode** is an Android app that wraps [hbctool](https://github.com/P1sec/hermes-dec) to let you work with Hermes Bytecode (`.hbc`) files from React Native Android apps — entirely on-device using [Termux](https://f-droid.org/packages/com.termux/).

No laptop. No ADB. Just your phone.

---

## Features

| Feature | Description |
|---|---|
| 🔵 **Disassemble** | Convert `.hbc` bundles into readable `.hasm` assembly files |
| 🟢 **Assemble** | Pack edited `.hasm` files back into a `.hbc` bundle |
| 🟣 **Hermes Finder** | Search for keywords, opcodes and strings inside `.hasm` files |
| ⚙️ **Auto Setup** | Detects Termux + Python and installs `hbctool` automatically |
| 🌙 **Dark / Light** | Theme follows system setting, switches instantly |
| 🌍 **EN / ES** | Full English and Spanish UI |
| 🎨 **Material You** | Animated worm-border cards, custom canvas animations |

---

## Requirements

- **Android 8.0+** (minSdk 26)
- **[Termux](https://f-droid.org/packages/com.termux/)** — install from **F-Droid**, not the Play Store
- **Python** inside Termux (`pkg install python`)
- `hbctool` — the app can install this automatically via Setup

---

## Getting Started

### 1 — Install Termux
Download Termux from **[F-Droid](https://f-droid.org/packages/com.termux/)**.  
⚠️ The Play Store version is outdated and will not work correctly.

### 2 — Install Python
Open Termux and run:
```bash
pkg install python
```

### 3 — Install the APK
Download the latest `.apk` from the [Releases](https://github.com/Fireway727/HBCTool/releases/latest) page and install it.

### 4 — Run Setup
Open the app → tap **Setup** → tap **Install hbctool**.  
The app will install `hbctool` via `pip` inside Termux automatically.

---

## Build from Source

### Prerequisites
- **AndroidIDE** (on-device) or Android Studio (PC)
- Java 17 / Kotlin 2.x

### Clone & Build
```bash
git clone https://github.com/FW727/HBC3.git
cd HBC3
./gradlew assembleDebug
```

The output APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Project Structure

```
HBC3/
├── app/src/main/
│   ├── kotlin/com/hbctool/
│   │   ├── MainActivity.kt          # Home screen, status bar
│   │   ├── DisasmActivity.kt        # HBC → HASM
│   │   ├── AsmActivity.kt           # HASM → HBC
│   │   ├── HermesFinderActivity.kt  # Keyword search
│   │   ├── SetupActivity.kt         # Termux / hbctool installer
│   │   ├── GuideActivity.kt         # Step-by-step guide
│   │   ├── AboutActivity.kt         # Credits & links
│   │   ├── CrashActivity.kt         # Crash reporter
│   │   ├── ui/
│   │   │   ├── AnimatedBorderCardView.kt   # Animated worm-border card
│   │   │   └── SearchResultAdapter.kt
│   │   └── util/
│   │       ├── HbcRunner.kt         # Runs hbctool via Termux
│   │       ├── HbcSetup.kt          # Detects env (Termux, Python, hbctool)
│   │       ├── CommandRunner.kt
│   │       ├── TermuxFinder.kt
│   │       ├── LanguageManager.kt
│   │       ├── PrefsManager.kt
│   │       ├── CrashHandler.kt
│   │       └── RunEvent.kt
│   └── res/
│       ├── values/strings.xml       # English strings
│       └── values-es/strings.xml    # Spanish strings
```

---

## How it Works

The app communicates with Termux via the **Termux:API run-command intent**, passing shell commands to `hbctool` installed inside Termux's Python environment. File access uses the Android Storage Access Framework (SAF) for scoped storage compatibility.

```
Your .hbc file
      │
      ▼
  Android SAF ──► App ──► Termux intent ──► hbctool (Python)
                                                   │
                                                   ▼
                                            .hasm output files
```

---

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

`hbctool` is a separate open-source project. See its own license at [P1sec/hermes-dec](https://github.com/P1sec/hermes-dec).

---

## Author

**Fireway727** · FWXFTP Team

- Telegram: [@Fireway727](https://t.me/Fireway727)
- GitHub: [github.com/Fireway727](https://github.com/Fireway727)

---

<p align="center"><sub>Made by FW · FWXFTP Team</sub></p>
