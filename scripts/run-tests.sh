#!/bin/sh
if [ ! -z "$CI_IOS_TESTS" ]
then
    ./scripts/squidb-ios-tests.sh
elif [ ! -z "$CI_CODE_GENERATOR_TESTS" ]
then
    ./gradlew squidb-processor:test
else
    ./scripts/squidb-android-tests.sh
fi
