# Native Binaries for Vidown (arm64-v8a)

Vidown executes FFmpeg and QuickJS directly from `nativeLibraryDir` via Android's `extractNativeLibs="true"` mechanism to comply with Android 10+ W^X (Write XOR Execute) security policies.

Because Android only extracts files named `lib*.so` into `nativeLibraryDir`, the compiled standalone executables are named:
- `libffmpeg.so` (FFmpeg CLI)
- `libqjs.so` (QuickJS CLI)

Both binaries must be linked with `-Wl,-z,max-page-size=16384` to guarantee full compatibility with 16 KB page size devices on Android 15+.

## Building in WSL2 (Debian)

### Prerequisites
In Debian WSL2:
```bash
sudo apt update && sudo apt install -y \
    build-essential clang git curl wget unzip pkg-config make ninja-build yasm nasm cmake python3
```

Set your Android NDK path (NDK r26d or r27b recommended):
```bash
export ANDROID_NDK_ROOT="$HOME/android/android-ndk-r27b"
```

### Running the build scripts
From this directory (`tools/native`):
```bash
chmod +x build-ffmpeg.sh build-quickjs.sh
./build-ffmpeg.sh
./build-quickjs.sh
```

The scripts compile the binaries for `aarch64-linux-android24` and automatically copy the resulting `libffmpeg.so` and `libqjs.so` into:
`../../app/src/main/jniLibs/arm64-v8a/`
