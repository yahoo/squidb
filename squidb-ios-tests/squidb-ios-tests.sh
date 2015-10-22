#!/bin/zsh
J2OBJC_HOME="${HOME}/workspace/j2objc-dist"
rm -r bin
rm -r intermediate
rm -r gen

rm invoke_ios_tests

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
javac -AsquidbOptions=iosModels -classpath "${J2OBJC_HOME}/lib/j2objc_junit.jar:./squidb-annotations-2.0.0.jar:./squidb-processor-2.0.0.jar:./apt-utils-1.0.0.jar" -s gen/ -proc:only -sourcepath "${SOURCEPATH}" ${SQUIDB_TESTS_TEST_SRC}/**/*.java

if [ ! -f "${J2OBJC_HOME}/j2objc" ]; then echo "J2OBJC_HOME not correctly defined, currently set to '${J2OBJC_HOME}'"; exit 1; fi;
${J2OBJC_HOME}/j2objc -classpath "${J2OBJC_HOME}/lib/j2objc_junit.jar" -d intermediate -sourcepath "${SOURCEPATH}" ${SQUIDB_SRC}/**/*.java \
    ${SQUIDB_IOS_SRC}/**/*.java ${SQUIDB_TESTS_TEST_SRC}/*.java ${GEN_SRC}/**/*.java \
    ${SQUIDB_IOS_TESTS_SRC}/**/*.java ${SQUIDB_TESTS_DATA_SRC}/*.java ${SQUIDB_TESTS_SQL_SRC}/*.java  ${SQUIDB_TESTS_UTILITY_SRC}/*.java

${J2OBJC_HOME}/j2objcc -Iintermediate -I../squidb-ios/native -c ../squidb-ios/native/**/*.m **/*.m # output to bin folder

mv *.o bin/

#j2objcc -o invoke_ios_tests bin/*.o # link with libraries

#./invoke_ios_tests com.yahoo.squidb.test.SquidbTestRunner
