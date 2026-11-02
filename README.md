# Vidown

Vidown is a lightweight video and audio downloader for Android embedding yt-dlp and FFmpeg to provide on-device extraction, format conversion, and track multiplexing.

## Features

- **Video Extraction**: Download video streams up to available native resolutions with audio multiplexing or fast single-stream extraction.
- **Audio Conversion**: Direct audio extraction to M4A (AAC), OPUS, lossless FLAC, or native stream without third-party converters.
- **Soft-Embedded Subtitles**: Embed available caption tracks into MP4/MKV video containers for toggleable in-player playback.
- **Metadata and Artwork**: Automatic embedding of title, uploader, and video thumbnail as cover art into media containers.
- **Background Downloads**: Foreground download service providing persistent status notifications, transfer speed, and ETA calculation.
- **In-App Engine Updates**: Direct in-app updates for the bundled yt-dlp package from PyPI.
- **Modern Android Architecture**: Jetpack Compose UI with Material 3 styling, Phosphor Icons, Coil image loading, Android 10+ W^X execution compliance, and 16 KB memory page compatibility for Android 15+.

## Tech Stack

- **UI & Presentation**: Jetpack Compose, Material 3, Adaptive NavigationSuiteScaffold, Phosphor Icons, Coil 2.7.0.
- **Runtime Bridge**: Chaquopy 17.2 running Python 3.14 on `arm64-v8a`.
- **Media Extraction**: yt-dlp with yt-dlp-ejs.
- **JavaScript Engine**: Standalone QuickJS binary (`libqjs.so`) for YouTube JavaScript challenge decryption.
- **Media Processing**: Custom LGPL FFmpeg binary (`libffmpeg.so`) compiled with `aac`, `opus`, and `flac` encoders.
- **Persistence**: SQLite via Android SQLiteOpenHelper and Android MediaStore API (`Movies/Vidown` and `Music/Vidown`).

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- Android SDK with platform tools for API 37
- Python 3.14 installed and available on system `PATH`

### Build Commands

Clone the repository and build using the Gradle wrapper:

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK (app.vidown.debug)
./gradlew assembleDebug

# Assemble Release APK (app.vidown)
./gradlew assembleRelease
```

Generated APKs are located at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Release Signing

To sign release builds locally, copy the example properties template and provide your keystore credentials:

```bash
cp keystore.properties.example keystore.properties
```

Edit `keystore.properties` with your keystore path, alias, and passwords. Both `keystore.properties` and `*.jks` files are ignored by version control.
