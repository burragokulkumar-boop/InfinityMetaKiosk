# Infinity Meta Kiosk

Portrait Android kiosk-style launcher based on the supplied reference screenshots.

## Included
- Supplied portrait screenshot used directly as the wallpaper/background.
- Infinity Meta label and number `268065734` are already present in that wallpaper.
- Bottom-left `i` hotspot opens the Kiosk menu.
- Kiosk menu contains Settings, Exit Kiosk, Support, Open source, Version 1.0.5, Installed date 260808, and Done.
- Exit Kiosk asks for confirmation and calls Android `stopLockTask()`.
- Back is consumed by the activity while running.
- GitHub Actions workflow builds `app-debug.apk` and uploads it as an artifact.

## Important Android kiosk limitation
Android's full Lock Task mode is controlled by the operating system. A normal app can call `startLockTask()`, but device-owner provisioning is required for a fully managed, silent kiosk configuration. This project calls the official Android Lock Task APIs and falls back to the platform's allowed behavior on devices where the app is not allowlisted.
