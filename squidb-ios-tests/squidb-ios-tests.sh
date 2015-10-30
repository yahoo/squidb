#!/bin/zsh
if [ ! -f "${J2OBJC_HOME}/j2objc" ]; then echo "J2OBJC_HOME not correctly defined, currently set to '${J2OBJC_HOME}'"; exit 1; fi;

# clean up build directories
rm *.o
if [ -d bin ]; then rm -r bin; fi;

if [ -d intermediate ]; then rm -r intermediate; fi;

if [ -d gen ]; then rm -r gen; fi;

if [ -f invoke_ios_tests ]; then rm invoke_ios_tests; fi;

mkdir bin
mkdir intermediate
mkdir gen

SQUIDB_SRC="../squidb/src"
SQUIDB_ANNOTATIONS_SRC="../squidb-annotations/src"
SQUIDB_IOS_SRC="../squidb-ios/src"
SQUIDB_IOS_TESTS_SRC="../squidb-ios-tests/src"

SQUIDB_TESTS_ROOT="../squidb-tests/src"
SQUIDB_TESTS_SRC="../squidb-tests/src/com/yahoo/squidb"
SQUIDB_TESTS_DATA_SRC="${SQUIDB_TESTS_SRC}/data"
SQUIDB_TESTS_SQL_SRC="${SQUIDB_TESTS_SRC}/sql"
SQUIDB_TESTS_TEST_SRC="${SQUIDB_TESTS_SRC}/test"
SQUIDB_TESTS_UTILITY_SRC="${SQUIDB_TESTS_SRC}/utility"
GEN_SRC="./gen"

SOURCEPATH="${GEN_SRC}:${SQUIDB_SRC}:${SQUIDB_ANNOTATIONS_SRC}:${SQUIDB_IOS_SRC}:${SQUIDB_IOS_TESTS_SRC}:${SQUIDB_TESTS_ROOT}"
#echo ${SOURCEPATH}

# invoke annotation processing, output to gen folder
javac -AsquidbOptions=iosModels -classpath "${J2OBJC_HOME}/lib/j2objc_junit.jar:./squidb-annotations-2.0.1.jar:./squidb-processor-2.0.1.jar:./apt-utils-1.0.0.jar" -s gen/ -proc:only -sourcepath "${SOURCEPATH}" ${SQUIDB_TESTS_TEST_SRC}/**/*.java
javacResult=$?
if [ ! $javacResult -eq 0 ]
then
    echo "javac exited with error code $javacResult"
    exit $javacResult
fi

# invoke j2objc to translate java sources
${J2OBJC_HOME}/j2objc -classpath "${J2OBJC_HOME}/lib/j2objc_junit.jar:${J2OBJC_HOME}/lib/jre_emul.jar" -d intermediate --no-package-directories -use-arc -sourcepath "${SOURCEPATH}" ${SQUIDB_SRC}/**/*.java \
    ${SQUIDB_IOS_SRC}/**/*.java ${SQUIDB_TESTS_TEST_SRC}/*.java ${GEN_SRC}/**/*.java \
    ${SQUIDB_IOS_TESTS_SRC}/**/*.java ${SQUIDB_TESTS_DATA_SRC}/*.java ${SQUIDB_TESTS_SQL_SRC}/*.java ${SQUIDB_TESTS_UTILITY_SRC}/*.java
j2objcResult=$?
if [ ! $j2objcResult -eq 0 ]
then
    echo "j2objc exited with code $j2objcResult"
    exit $j2objcResult
fi

# compile translated Obj-C sources
for f in ../squidb-ios/native/**/*.m **/*.m ${SQUIDB_IOS_TESTS_SRC}/**/*.m
do
    echo "Compiling $f"
    ${J2OBJC_HOME}/j2objcc -fobjc-arc -Iintermediate -I../squidb-ios/native -c $f # output to bin folder
    j2objccResult=$?
    if [ ! $j2objccResult -eq 0 ]
    then
        echo "j2objcc failed to compile $f, exiting with code $j2objccResult"
        rm *.o
        exit $j2objccResult
    fi
done

# build test executable
mv *.o bin/
${J2OBJC_HOME}/j2objcc -L ${J2OBJC_HOME}/lib/macosx -l jre_emul -l junit -l sqlite3 -ObjC -o run_squidb_ios_tests bin/*.o # link with libraries

# run tests
./run_squidb_ios_tests
testResults=$?
rm run_squidb_ios_tests
if [ ! $testResults -eq 0 ]
then
    echo "Unit test failures, exiting with code $testResults"
    exit $testResults
fi
exit
