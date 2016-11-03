#!/bin/sh
runAndroidTestWithBinding () {
    echo "Running tests on platform $ANDROID_TARGET with binding $1"
    python3 ./scripts/adb-wrapper.py "adb shell am instrument -w -e squidb_binding $1 com.yahoo.squidb.test.test/com.yahoo.squidb.android.SquidInstrumentationTestRunner"
    testResult=$?
    if [ ! $testResult -eq 0 ]
    then
        echo "Test suite failed on $ANDROID_TARGET with binding $1"
        exit $testResult
    fi
}

runAndroidTestWithBinding android
if [ ! "$ANDROID_TARGET" = "android-15" ]
then
    runAndroidTestWithBinding sqlite
fi
