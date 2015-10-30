#!/bin/zsh
if [ ! -f "${J2OBJC_HOME}/j2objc" ]; then echo "J2OBJC_HOME not correctly defined, currently set to '${J2OBJC_HOME}'"; exit 1; fi;

BUILD_DIR="build/ios-tests"
# clean up build directories
if [ -d $BUILD_DIR ]; then rm -r $BUILD_DIR; fi;
if [ -f run_squidb_ios_tests ]; then rm run_squidb_ios_tests; fi;

mkdir -p $BUILD_DIR

BIN="$BUILD_DIR/bin"
INTERMEDIATE="$BUILD_DIR/intermediate"
GEN="$BUILD_DIR/gen"

mkdir $BIN
mkdir $INTERMEDIATE
mkdir $GEN

SQUIDB_SRC="squidb/src"
SQUIDB_ANNOTATIONS_SRC="squidb-annotations/src"
SQUIDB_IOS_SRC="squidb-ios/src"
SQUIDB_IOS_NATIVE="squidb-ios/native"
SQUIDB_IOS_TESTS="squidb-ios-tests"
SQUIDB_IOS_TESTS_SRC="$SQUIDB_IOS_TESTS/src"

SQUIDB_TESTS_ROOT="squidb-tests/src"
SQUIDB_TESTS_SRC="squidb-tests/src/com/yahoo/squidb"
SQUIDB_TESTS_DATA_SRC="${SQUIDB_TESTS_SRC}/data"
SQUIDB_TESTS_SQL_SRC="${SQUIDB_TESTS_SRC}/sql"
SQUIDB_TESTS_TEST_SRC="${SQUIDB_TESTS_SRC}/test"
SQUIDB_TESTS_UTILITY_SRC="${SQUIDB_TESTS_SRC}/utility"

SOURCEPATH="${GEN}:${SQUIDB_SRC}:${SQUIDB_ANNOTATIONS_SRC}:${SQUIDB_IOS_SRC}:${SQUIDB_IOS_TESTS_SRC}:${SQUIDB_TESTS_ROOT}"
#echo ${SOURCEPATH}

# invoke annotation processing, output to gen folder
javac -AsquidbOptions=iosModels -classpath "${J2OBJC_HOME}/lib/j2objc_junit.jar:$SQUIDB_IOS_TESTS/squidb-annotations-2.0.1.jar:$SQUIDB_IOS_TESTS/squidb-processor-2.0.1.jar:$SQUIDB_IOS_TESTS/apt-utils-1.0.0.jar" \
    -s $GEN -proc:only -sourcepath "${SOURCEPATH}" ${SQUIDB_TESTS_TEST_SRC}/**/*.java
javacResult=$?
if [ ! $javacResult -eq 0 ]
then
    echo "javac exited with error code $javacResult"
    exit $javacResult
fi

# invoke j2objc to translate java sources
${J2OBJC_HOME}/j2objc -classpath "${J2OBJC_HOME}/lib/j2objc_junit.jar:${J2OBJC_HOME}/lib/jre_emul.jar" -d $INTERMEDIATE \
    --no-package-directories -use-arc -sourcepath "${SOURCEPATH}" \
    ${SQUIDB_SRC}/**/*.java ${SQUIDB_IOS_SRC}/**/*.java ${SQUIDB_TESTS_TEST_SRC}/*.java ${GEN}/**/*.java \
    ${SQUIDB_IOS_TESTS_SRC}/**/*.java ${SQUIDB_TESTS_DATA_SRC}/*.java ${SQUIDB_TESTS_SQL_SRC}/*.java ${SQUIDB_TESTS_UTILITY_SRC}/*.java
j2objcResult=$?
if [ ! $j2objcResult -eq 0 ]
then
    echo "j2objc exited with code $j2objcResult"
    exit $j2objcResult
fi

# compile translated Obj-C sources
for f in squidb-ios/native/**/*.m $INTERMEDIATE/*.m ${SQUIDB_IOS_TESTS_SRC}/**/*.m
do
    echo "Compiling $f"
    # output .o file to bin folder
    ${J2OBJC_HOME}/j2objcc -fobjc-arc -I$INTERMEDIATE -I$SQUIDB_IOS_NATIVE -o "$BIN/${$(basename $f)%.*}.o" -c $f
    j2objccResult=$?
    if [ ! $j2objccResult -eq 0 ]
    then
        echo "j2objcc failed to compile $f, exiting with code $j2objccResult"
        exit $j2objccResult
    fi
done

# build test executable
${J2OBJC_HOME}/j2objcc -L ${J2OBJC_HOME}/lib/macosx -l jre_emul -l junit -l sqlite3 -ObjC -o run_squidb_ios_tests $BIN/*.o # link with libraries

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
