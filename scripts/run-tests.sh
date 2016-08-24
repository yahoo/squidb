#!/bin/sh
if [ -z "$CI_IOS_TESTS" ]
then
    ./scripts/squidb-android-tests.sh
else
    ./scripts/squidb-ios-tests.sh
fi
