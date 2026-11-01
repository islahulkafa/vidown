#!/usr/bin/env bash
set -euo pipefail

# QuickJS build script for Vidown (Android arm64-v8a)
# Builds a standalone libqjs.so executable compliant with Android 10+ W^X and 16 KB pages.

if [ -z "${ANDROID_NDK_ROOT:-}" ]; then
    echo "ERROR: ANDROID_NDK_ROOT environment variable is not set." >&2
    echo "Please set it, e.g.: export ANDROID_NDK_ROOT=\$HOME/android/android-ndk-r27b" >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK_DIR="${SCRIPT_DIR}/quickjs_build"
OUTPUT_DIR="${SCRIPT_DIR}/../../app/src/main/jniLibs/arm64-v8a"

mkdir -p "${WORK_DIR}"
cd "${WORK_DIR}"

if [ ! -d "quickjs" ]; then
    echo "Cloning QuickJS repository..."
    git clone --depth 1 https://github.com/quickjs-ng/quickjs.git
fi

cd quickjs

echo "Configuring QuickJS with CMake and Android NDK..."
cmake -B build-android \
    -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_ROOT}/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="arm64-v8a" \
    -DANDROID_PLATFORM="android-24" \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_EXE_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
    -DBUILD_QJS=ON \
    -DBUILD_SHARED_LIBS=OFF

echo "Building qjs executable..."
cmake --build build-android --target qjs_exe -j"$(nproc)"

# Find the built qjs binary
QJS_BIN="$(find build-android -type f -name "qjs" -perm /111 | head -n 1)"
if [ -z "${QJS_BIN}" ]; then
    # Fallback check
    if [ -f "build-android/qjs_exe" ]; then
        QJS_BIN="build-android/qjs_exe"
    elif [ -f "build-android/qjs" ]; then
        QJS_BIN="build-android/qjs"
    else
        echo "ERROR: Could not locate built qjs binary in build-android/" >&2
        ls -la build-android/
        exit 1
    fi
fi

echo "Found binary at: ${QJS_BIN}"
mkdir -p "${OUTPUT_DIR}"
cp "${QJS_BIN}" "${OUTPUT_DIR}/libqjs.so"
echo "Successfully built and copied libqjs.so to ${OUTPUT_DIR}/libqjs.so"
