#!/bin/sh
if [ -z "$CI_IOS_TESTS" ]
then
    sudo apt-get -qq update
    sudo apt-get install python3

    echo no | android create avd --force -n test -t $ANDROID_TARGET --abi armeabi-v7a
    emulator -avd test -no-skin -no-audio -no-window &
    android-wait-for-emulator
    adb shell input keyevent 82 &
    ./gradlew squidb-tests:installDebug squidb-tests:installDebugAndroidTest
else
    wget https://github.com/google/j2objc/releases/download/$J2OBJC_VERSION/j2objc-$J2OBJC_VERSION.zip -O /tmp/j2objc-$J2OBJC_VERSION.zip
    unzip -oq /tmp/j2objc-$J2OBJC_VERSION.zip -d /tmp
fi
