# Contributing to HBC to Bytecode

Thanks for your interest in contributing! Here's how to get started.

---

## Reporting Bugs

1. Check [existing issues](../../issues) first to avoid duplicates.
2. Open a new issue and include:
   - Android version and device model
   - App version (visible in About screen)
   - Steps to reproduce
   - Expected vs actual behavior
   - The crash log if available (copy it from the Crash screen)

You can also reach me directly on **[Telegram @Fireway727](https://t.me/Fireway727)**.

---

## Suggesting Features

Open an issue with the `enhancement` label and describe:
- What problem it solves
- How you'd expect it to work

---

## Code Contributions

### Setup

The project is built with **AndroidIDE** (on-device) or Android Studio. It uses:
- Kotlin
- Jetpack ViewBinding
- Material 3 components
- Coroutines

### Rules

- Follow the existing code style (Kotlin conventions, 4-space indent)
- Keep Material You design language consistent
- All user-facing strings must be added to both `values/strings.xml` (EN) **and** `values-es/strings.xml` (ES)
- Test on both light and dark themes before submitting

### Pull Request Process

1. Fork the repository
2. Create a branch: `git checkout -b feature/your-feature-name`
3. Commit with a clear message
4. Push and open a Pull Request against `main`
5. Describe what you changed and why

---

## Translations

Want to add a new language? Add a new `values-xx/strings.xml` folder (where `xx` is the ISO 639-1 code) mirroring all keys from `values/strings.xml`.

---

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).

---

<sub>Made by FW · FWXFTP Team</sub>
