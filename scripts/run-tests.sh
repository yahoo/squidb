#!/bin/sh
if [ -z "$CI_IOS_TESTS" ]
then
    adb shell am instrument -w -e squidb_binding $SQUIDB_BINDING com.yahoo.squidb.test/com.yahoo.squidb.android.SquidInstrumentationTestRunner
else
    ./scripts/squidb-ios-tests.sh
fi
