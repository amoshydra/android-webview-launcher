#!/bin/bash
set -e

# Podman build script for Android APK

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="android-build"
IMAGE_NAME="android-sdk:34"
APK_OUTPUT_DIR="$SCRIPT_DIR/app/build/outputs/apk"

echo "=== Android Podman Build Script ==="
echo ""

# Function to show usage
show_usage() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  build-image    - Build the Android SDK Docker image"
    echo "  build-apk      - Build the debug APK"
    echo "  build-release  - Build the release APK"
    echo "  clean          - Clean build artifacts"
    echo "  shell          - Open a shell in the build container"
    echo "  help           - Show this help message"
    echo ""
}

# Build the Docker image
build_image() {
    echo "Building Android SDK Docker image..."
    podman build -t "$IMAGE_NAME" -f "$SCRIPT_DIR/Dockerfile" "$SCRIPT_DIR"
    echo "Image built successfully: $IMAGE_NAME"
}

# Build debug APK
build_apk() {
    echo "Building debug APK..."
    
    # Check if image exists
    if ! podman image exists "$IMAGE_NAME"; then
        echo "Docker image not found. Building first..."
        build_image
    fi
    
    # Run build in container
    podman run --rm \
        -v "$SCRIPT_DIR:/workspace:Z" \
        -w /workspace \
        "$IMAGE_NAME" \
        bash -c "./gradlew assembleDebug --no-daemon"
    
    echo ""
    echo "=== Build Complete ==="
    echo "APK location: $APK_OUTPUT_DIR/debug/app-debug.apk"
    echo ""
}

# Build release APK
build_release() {
    echo "Building release APK..."
    
    # Check if image exists
    if ! podman image exists "$IMAGE_NAME"; then
        echo "Docker image not found. Building first..."
        build_image
    fi
    
    # Run build in container
    podman run --rm \
        -v "$SCRIPT_DIR:/workspace:Z" \
        -w /workspace \
        "$IMAGE_NAME" \
        bash -c "./gradlew assembleRelease --no-daemon"
    
    echo ""
    echo "=== Build Complete ==="
    echo "APK location: $APK_OUTPUT_DIR/release/app-release-unsigned.apk"
    echo ""
}

# Clean build artifacts
clean() {
    echo "Cleaning build artifacts..."
    
    if podman image exists "$IMAGE_NAME"; then
        podman run --rm \
            -v "$SCRIPT_DIR:/workspace:Z" \
            -w /workspace \
            "$IMAGE_NAME" \
            bash -c "./gradlew clean --no-daemon"
    else
        echo "Removing local build directories..."
        rm -rf "$SCRIPT_DIR/app/build"
        rm -rf "$SCRIPT_DIR/.gradle"
    fi
    
    echo "Clean complete."
}

# Open shell in container
open_shell() {
    echo "Opening shell in Android build container..."
    
    # Check if image exists
    if ! podman image exists "$IMAGE_NAME"; then
        echo "Docker image not found. Building first..."
        build_image
    fi
    
    podman run -it --rm \
        -v "$SCRIPT_DIR:/workspace:Z" \
        -w /workspace \
        "$IMAGE_NAME" \
        bash
}

# Main script logic
case "${1:-build-apk}" in
    build-image)
        build_image
        ;;
    build-apk|debug)
        build_apk
        ;;
    build-release|release)
        build_release
        ;;
    clean)
        clean
        ;;
    shell)
        open_shell
        ;;
    help|--help|-h)
        show_usage
        ;;
    *)
        echo "Unknown command: $1"
        show_usage
        exit 1
        ;;
esac
