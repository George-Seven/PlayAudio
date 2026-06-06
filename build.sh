#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

CURRENT_DIR="$PWD"

TMP_DIR="$(mktemp -d)"

cleanup() {
    rm -rf "$TMP_DIR"
}

trap cleanup EXIT

cd "$TMP_DIR"

if [ -n "$TERMUX_VERSION" ]; then
    yes | termux-setup-storage &>/dev/null
    apt update
    yes | apt install -y wget d8 android-tools
fi

if command -v wget &>/dev/null; then
    wget -O android.jar https://github.com/Sable/android-platforms/raw/refs/heads/master/android-36/android.jar
elif command -v curl &>/dev/null; then
    curl -L -o android.jar https://github.com/Sable/android-platforms/raw/refs/heads/master/android-36/android.jar
else
    echo "Missing wget or curl"
    exit 1
fi

if ! command -v javac &>/dev/null; then
    echo "Missing java compiler"
    exit 1
fi

if command -v d8 &>/dev/null; then
    javac --release 11 -cp android.jar -d . "$CURRENT_DIR/PlayAudio.java"
    d8 --release *.class --output .
elif command -v dx &>/dev/null; then
    javac --release 8 -cp android.jar -d . -Xlint:none "$CURRENT_DIR/PlayAudio.java"
    dx --dex --output classes.dex *.class
else
    echo "Missing d8 or dx"
    exit 1
fi

rm -rf "$CURRENT_DIR/PlayAudio.dex"

mv classes.dex "$CURRENT_DIR/PlayAudio.dex"

cp -f "$CURRENT_DIR/PlayAudio.dex" /storage/emulated/0/Download/PlayAudio.dex
