# Android Podman Build

Build Android APKs using Podman containers - no Android Studio required!

## Project Structure

```
.
├── app/
│   ├── build.gradle              # App-level build configuration
│   └── src/main/
│       ├── AndroidManifest.xml   # App manifest
│       ├── java/com/example/androidapp/
│       │   └── MainActivity.java # Main activity (WebView)
│       └── res/                  # Resources (layouts, strings, icons)
├── build.gradle                  # Project-level build configuration
├── settings.gradle               # Project settings
├── gradle.properties             # Gradle properties
├── gradlew                       # Gradle wrapper script
├── gradle/wrapper/               # Gradle wrapper files
├── Dockerfile                    # Podman/Docker image definition
└── podman-build.sh               # Build automation script
```

## Prerequisites

- [Podman](https://podman.io/) installed on your system
- Internet connection for downloading SDK components

## Usage

### Build the Docker Image (First Time Only)

```bash
./podman-build.sh build-image
```

This creates an Ubuntu-based image with Android SDK 34 and OpenJDK 17.

### Build Debug APK

```bash
./podman-build.sh build-apk
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

```bash
./podman-build.sh build-release
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Clean Build Artifacts

```bash
./podman-build.sh clean
```

### Open Shell in Container

```bash
./podman-build.sh shell
```

Useful for debugging or running custom gradle commands.

### Show Help

```bash
./podman-build.sh help
```

## App Details

- **Package**: `com.example.androidapp`
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Features**: Simple WebView app that loads example.com

## Customization

Edit these files to customize the app:

- `app/src/main/java/com/example/androidapp/MainActivity.java` - App logic
- `app/src/main/res/layout/activity_main.xml` - UI layout
- `app/src/main/res/values/strings.xml` - App strings
- `app/build.gradle` - Dependencies and build settings
- `Dockerfile` - Build environment configuration

## How It Works

1. The `Dockerfile` sets up an Ubuntu container with:
   - OpenJDK 17
   - Android SDK Command Line Tools
   - Android Platform 34 and Build Tools

2. The `podman-build.sh` script:
   - Mounts your project directory into the container
   - Runs Gradle commands to build the APK
   - Outputs the APK to your local filesystem

3. No Android Studio required - everything runs in the container!

## Troubleshooting

**Permission issues**: The script uses `:Z` flag for SELinux compatibility.

**Build fails**: Try cleaning first with `./podman-build.sh clean`

**Slow builds**: First build downloads Gradle and dependencies. Subsequent builds are faster.

**Out of space**: The Android SDK image is ~2GB. Ensure you have enough disk space.
