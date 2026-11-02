# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.1] - 2026-11-02

### Added

- Embedded Python 3.14 runtime via Chaquopy 17.2 with yt-dlp and yt-dlp-ejs integration.
- Native standalone executables (`libffmpeg.so` and `libqjs.so`) compiled for `arm64-v8a` with 16 KB page-size alignment for Android 15+.
- Video download support with multiplexed audio/video tracks or single-stream extraction.
- Audio extraction and conversion to M4A (AAC), OPUS, lossless FLAC, and native stream.
- Soft-embedded subtitles for MP4 and MKV video containers.
- Automatic metadata tagging and cover art thumbnail embedding.
- Asynchronous media thumbnail loading and caching via Coil with offline frame extraction fallback.
- Foreground download service with persistent notifications, real-time transfer speed, and ETA calculation.
- Media persistence via Android MediaStore API saving to `Movies/Vidown` and `Music/Vidown`.
- Direct URL input with dynamic paste/clear action and external URL sharing support via `ACTION_SEND`.
- In-app yt-dlp updater fetching updates directly from PyPI.
- Jetpack Compose Material 3 interface with Phosphor Icons.
- Automated GitHub Actions workflows for continuous integration and signed release builds.
