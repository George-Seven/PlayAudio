# Description

Play audio files on Android using ADB Shell, or Root.

# Installation

Use pre-built, or compile it:-

### Using pre-built:-

Download [PlayAudio.dex](https://github.com/George-Seven/PlayAudio/raw/refs/heads/main/PlayAudio.dex) and copy it to Download folder of the phone.

&nbsp;

### Compiling:-

In Termux:-

    apt update && apt install -y git && rm -rf ~/PlayAudio && git clone --depth 1 https://github.com/George-Seven/PlayAudio ~/PlayAudio && bash ~/PlayAudio/build.sh

&nbsp;

# Usage

Play audio from ADB Shell in Termux or PC:-

    adb shell CLASSPATH=/storage/emulated/0/Download/PlayAudio.dex /system/bin/app_process /system/bin PlayAudio /system/media/audio/ringtones/Beep.ogg

&nbsp;

Viewing only the help text:-

    adb shell CLASSPATH=/storage/emulated/0/Download/PlayAudio.dex /system/bin/app_process /system/bin PlayAudio

&nbsp;

Help text:-

    Usage: PlayAudio [-s streamtype] <audio-file>
    
    The audio stream type (which affects the volume)
    may be specified as:
        alarm
        media (default)
        notification
        ring
        system
        voice

&nbsp;

Using the `-s streamtype` option:-

    adb shell CLASSPATH=/storage/emulated/0/Download/PlayAudio.dex /system/bin/app_process /system/bin PlayAudio -s media /system/media/audio/ringtones/Beep.ogg

&nbsp;

If root is available, replace `adb shell` with `su -c` and run it.
