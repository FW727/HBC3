# Changelog

All notable changes to **HBC to Bytecode** will be documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [1.0.0-alpha] — 2025

> ⚠️ **Alpha release.** Functional but may contain bugs. No further updates planned from the original creator.

### Added
- **Disassemble** screen — convert `.hbc` bundles to `.hasm` files via hbctool
- **Assemble** screen — pack `.hasm` files back into `.hbc` bundles
- **Hermes Finder** — multi-file keyword and opcode search with highlighted results
- **Setup** screen — auto-detects Termux + Python, installs `hbctool` via pip
- **Guide** — 9-step interactive walkthrough covering all features (EN + ES)
- **About** screen — version info, Telegram and GitHub links
- **Animated worm-border cards** (`AnimatedBorderCardView`) with crawl and wiggle loading modes
- **Dark / Light theme** — follows system setting automatically
- **EN / ES language toggle** — full bilingual UI
- **Crash reporter** — catches uncaught exceptions, lets user copy and report the stack trace
- Live Termux status indicator on the main screen
- Support for custom Termux binary path
- Real-time log output during run operations
- Open in Termux — exports the generated hbctool command for manual use
- minSdk 26 (Android 8.0 Oreo)
