# Native Binaries for Vidown (arm64-v8a)

Vidown executes FFmpeg and QuickJS directly from `nativeLibraryDir` via Android's native packaging mechanism (`useLegacyPackaging = true`) to comply with Android 10+ W^X (Write XOR Execute) security policies.

Because Android only extracts files matching `lib*.so` into `nativeLibraryDir`, the standalone CLI executables are compiled and named:
- `libffmpeg.so`: FFmpeg CLI executable
- `libqjs.so`: QuickJS CLI executable

Both binaries are linked with `-Wl,-z,max-page-size=16384` for compatibility with 16 KB page-size devices on Android 15+.

---

## Component Details

### FFmpeg (`libffmpeg.so`)
- **Version**: FFmpeg 7.1 (LGPL v2.1+)
- **Purpose**: Audio extraction, format conversion (AAC, OPUS, FLAC), subtitle soft-embedding (MP4/MKV), cover art embedding, and video/audio track multiplexing.
- **Build Configuration**: Compiled with `--enable-small`, `--disable-programs` (except ffmpeg), `--disable-doc`, native `aac`, `opus`, and `flac` encoders, all muxers/demuxers, and without `--enable-gpl` or `--enable-nonfree` to preserve LGPL redistribution rights.

### QuickJS (`libqjs.so`)
- **Version**: QuickJS 2024-01-13 (MIT License by Fabrice Bellard and Charlie Gordon)
- **Purpose**: Evaluates obfuscated YouTube JavaScript signatures and decryption algorithms on-device via `yt-dlp-ejs` and yt-dlp `--js-runtimes quickjs`.

---

## Building in WSL2 (Debian / Ubuntu)

### Prerequisites

```bash
sudo apt update && sudo apt install -y \
    build-essential clang git curl wget unzip pkg-config make ninja-build yasm nasm cmake python3
```

Set the Android NDK path (NDK r26d or r27b):

```bash
export ANDROID_NDK_ROOT="$HOME/android/android-ndk-r27b"
```

### Build Execution

From the `tools/native` directory:

```bash
chmod +x build-ffmpeg.sh build-quickjs.sh
./build-ffmpeg.sh
./build-quickjs.sh
```

The scripts compile both binaries targeting `aarch64-linux-android24` with 16 KB page alignment and copy the outputs to:
```
../../app/src/main/jniLibs/arm64-v8a/
├── libffmpeg.so
└── libqjs.so
```

---

## Third-Party Licenses

- **FFmpeg**: Licensed under GNU Lesser General Public License (LGPL) version 2.1 or later.
- **QuickJS**: Copyright (c) 2017-2024 Fabrice Bellard, Charlie Gordon. Licensed under the MIT License.
