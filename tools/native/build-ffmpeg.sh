#!/usr/bin/env bash
set -euo pipefail

# FFmpeg build script for Vidown (Android arm64-v8a)
# Builds a standalone libffmpeg.so executable compliant with Android 10+ W^X and 16 KB pages.

if [ -z "${ANDROID_NDK_ROOT:-}" ]; then
    echo "ERROR: ANDROID_NDK_ROOT environment variable is not set." >&2
    echo "Please set it, e.g.: export ANDROID_NDK_ROOT=\$HOME/android/android-ndk-r27b" >&2
    exit 1
fi

FFMPEG_VERSION="7.1"
ARCH="aarch64"
TARGET_API=24
HOST_TAG="linux-x86_64"
TOOLCHAIN="${ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/${HOST_TAG}"
CC="${TOOLCHAIN}/bin/aarch64-linux-android${TARGET_API}-clang"
AR="${TOOLCHAIN}/bin/llvm-ar"
RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
STRIP="${TOOLCHAIN}/bin/llvm-strip"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK_DIR="${SCRIPT_DIR}/ffmpeg_build"
SRC_DIR="${WORK_DIR}/ffmpeg-${FFMPEG_VERSION}"
OUTPUT_DIR="${SCRIPT_DIR}/../../app/src/main/jniLibs/arm64-v8a"

mkdir -p "${WORK_DIR}"
cd "${WORK_DIR}"

if [ ! -d "${SRC_DIR}" ]; then
    TAR_NAME="ffmpeg-${FFMPEG_VERSION}.tar.xz"
    if [ ! -f "${TAR_NAME}" ]; then
        echo "Downloading FFmpeg ${FFMPEG_VERSION}..."
        curl -LO "https://ffmpeg.org/releases/${TAR_NAME}"
    fi
    echo "Extracting FFmpeg ${FFMPEG_VERSION}..."
    tar -xf "${TAR_NAME}"
fi

cd "${SRC_DIR}"

echo "Configuring FFmpeg for Android arm64-v8a (LGPL)..."
./configure \
    --prefix="${WORK_DIR}/dist" \
    --target-os=android \
    --arch="${ARCH}" \
    --cpu=armv8-a \
    --cc="${CC}" \
    --ar="${AR}" \
    --ranlib="${RANLIB}" \
    --strip="${STRIP}" \
    --enable-cross-compile \
    --sysroot="${TOOLCHAIN}/sysroot" \
    --extra-cflags="-O3 -fPIC" \
    --extra-ldflags="-Wl,-z,max-page-size=16384" \
    --enable-static \
    --disable-shared \
    --enable-small \
    --disable-programs \
    --enable-ffmpeg \
    --disable-doc \
    --disable-debug \
    --disable-gpl \
    --disable-nonfree \
    --disable-network \
    --enable-protocol=file \
    --enable-protocol=pipe \
    --enable-demuxers \
    --enable-muxers \
    --enable-parsers \
    --enable-bsfs \
    --enable-decoder=aac \
    --enable-decoder=opus \
    --enable-decoder=mp3 \
    --enable-decoder=flac \
    --enable-decoder=vorbis \
    --enable-decoder=h264 \
    --enable-decoder=hevc \
    --enable-decoder=vp9 \
    --enable-decoder=av1 \
    --enable-encoder=aac \
    --enable-encoder=opus \
    --enable-encoder=flac \
    --enable-swresample \
    --enable-swscale \
    --enable-avfilter \
    --disable-postproc \
    --disable-avdevice

echo "Building FFmpeg..."
make -j"$(nproc)"

echo "Stripping binary..."
"${STRIP}" ffmpeg

mkdir -p "${OUTPUT_DIR}"
cp ffmpeg "${OUTPUT_DIR}/libffmpeg.so"
echo "Successfully built and copied libffmpeg.so to ${OUTPUT_DIR}/libffmpeg.so"
