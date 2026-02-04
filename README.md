# Android WebView Launcher

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/API-26%2B-blue.svg)](https://developer.android.com/about/versions)
[![Podman](https://img.shields.io/badge/Built%20with-Podman-892CA0.svg)](https://podman.io)

A fullscreen WebView launcher with JavaScript injection capabilities, built without Android Studio using Podman containers.

![android-webview-launcher](https://github.com/user-attachments/assets/e34f5141-2b42-4bf0-a717-658b7ab53a20)


## Features

- **Custom URL Input** - Enter any website URL to load
- **JavaScript Injection** - Execute custom JS code 2 seconds after page load
- **Two-Activity Flow** - Clean separation: Settings form leads to fullscreen WebView
- **Containerized Builds** - Build APKs using Podman (no Android Studio required)
- **Fullscreen Experience** - Immersive browsing without UI chrome
- **Monospace Editor** - Code-friendly JavaScript input field

## Quick Start

```bash
# Build and install the APK
./podman-build.sh build-apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## How It Works

The app uses a two-activity architecture:

### SettingsActivity (Launcher)
- URL input field (default: https://example.com)
- JavaScript code textarea with monospace font
- Launch WebView button

### MainActivity (Fullscreen WebView)
- Receives URL and JavaScript via Intent
- Loads the specified URL
- Executes JavaScript 2 seconds after page finishes loading
- Press back button to return to SettingsActivity

## Building

### Prerequisites

- [Podman](https://podman.io/) installed on your system
- Internet connection for downloading SDK components

### Build Commands

```bash
# Build the Docker image (first time only)
./podman-build.sh build-image

# Build debug APK
./podman-build.sh build-apk

# Build release APK (unsigned)
./podman-build.sh build-release

# Clean build artifacts
./podman-build.sh clean

# Open shell in build container
./podman-build.sh shell
```

## Installation

```bash
# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## App Details

- **Package**: `com.example.androidapp`
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle with Podman containerization

## Customization

Edit these files to customize the app:

- `app/src/main/java/com/example/androidapp/SettingsActivity.java` - Settings form logic
- `app/src/main/java/com/example/androidapp/MainActivity.java` - WebView and JS execution
- `app/src/main/res/layout/activity_settings.xml` - Settings form layout
- `app/src/main/res/layout/activity_main.xml` - WebView layout
- `app/build.gradle` - Dependencies and build settings
- `Dockerfile` - Build environment configuration

## Architecture

```
SettingsActivity (Form)
  - URL Input: https://example.com
  - JavaScript: alert('Hello!');
  - [Launch WebView] Button
         |
         v
MainActivity (Fullscreen WebView)
  - Loads specified URL
  - Waits 2 seconds
  - Executes JavaScript
```

## How It Works (Build System)

1. **Dockerfile** sets up an Ubuntu container with:
   - OpenJDK 17
   - Android SDK Command Line Tools
   - Android Platform 34 and Build Tools

2. **podman-build.sh** script:
   - Mounts your project directory into the container
   - Runs Gradle commands to build the APK
   - Outputs the APK to your local filesystem

3. No Android Studio required - everything runs in the container!

## Troubleshooting

**Permission issues**: The script uses `:Z` flag for SELinux compatibility.

**Build fails**: Try cleaning first with `./podman-build.sh clean`

**Slow builds**: First build downloads Gradle and dependencies. Subsequent builds are faster.

**Out of space**: The Android SDK image is approximately 2GB. Ensure you have enough disk space.

**Signature mismatch on reinstall**: If you get `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall first:
```bash
adb uninstall com.example.androidapp
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
.
├── app/
│   ├── build.gradle              # App-level build configuration
│   └── src/main/
│       ├── AndroidManifest.xml   # App manifest
│       ├── java/com/example/androidapp/
│       │   ├── MainActivity.java      # Fullscreen WebView
│       │   └── SettingsActivity.java  # Settings form
│       └── res/                  # Resources (layouts, strings, icons)
├── build.gradle                  # Project-level build configuration
├── settings.gradle               # Project settings
├── gradle.properties             # Gradle properties
├── gradlew                       # Gradle wrapper script
├── gradle/wrapper/               # Gradle wrapper files
├── Dockerfile                    # Podman/Docker image definition
├── podman-build.sh               # Build automation script
├── LICENSE                       # MIT License
└── README.md                     # This file
```

## License

[MIT](LICENSE) - Copyright (c) 2026 amoshydra
