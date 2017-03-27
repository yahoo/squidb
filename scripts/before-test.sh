#!/bin/sh
if [ ! -z "$CI_IOS_TESTS" ]
then
    wget https://github.com/google/j2objc/releases/download/$J2OBJC_VERSION/j2objc-$J2OBJC_VERSION.zip -O /tmp/j2objc-$J2OBJC_VERSION.zip
    unzip -oq /tmp/j2objc-$J2OBJC_VERSION.zip -d /tmp
elif [ ! -z "$CI_CODE_GENERATOR_TESTS" ]
then
    echo "Running code generator tests"
else
    ANDROID_SDK_HOME=$(dirname $(dirname $(which android)))
    EMULATOR="system-images;$ANDROID_TARGET;google_apis;armeabi-v7a"
    echo y | $ANDROID_SDK_HOME/tools/bin/sdkmanager "tools"
    echo y | $ANDROID_SDK_HOME/tools/bin/sdkmanager "emulator"
    echo y | $ANDROID_SDK_HOME/tools/bin/sdkmanager "$EMULATOR"

    sudo apt-get -qq update
    sudo apt-get install python3

    echo no | $ANDROID_SDK_HOME/tools/bin/avdmanager create avd --force --name test --package "$EMULATOR" --tag google_apis --abi armeabi-v7a
    $ANDROID_SDK_HOME/emulator/emulator -avd test -no-skin -no-audio -no-window &
    android-wait-for-emulator
    adb shell input keyevent 82 &
    ./gradlew squidb-tests:installDebug squidb-tests:installDebugAndroidTest
fi
