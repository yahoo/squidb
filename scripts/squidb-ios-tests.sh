#!/bin/zsh
if [ ! -f "${J2OBJC_HOME}/j2objc" ]; then echo "J2OBJC_HOME not correctly defined, currently set to '${J2OBJC_HOME}'"; exit 1; fi;

BUILD_DIR="build/ios-tests"
# clean up build directories
if [ -d $BUILD_DIR ]; then rm -r $BUILD_DIR; fi;
if [ -f run_squidb_ios_tests ]; then rm run_squidb_ios_tests; fi;

mkdir -p $BUILD_DIR

BIN="$BUILD_DIR/bin"; mkdir $BIN;
INTERMEDIATE="$BUILD_DIR/intermediate"; mkdir $INTERMEDIATE;
GEN="$BUILD_DIR/gen"; mkdir $GEN;

SQUIDB_SRC="squidb/src"
SQUIDB_ANNOTATIONS_SRC="squidb-annotations/src"
SQUIDB_JSON_SRC="squidb-addons/squidb-json/squidb-json-plugin/src"
SQUIDB_JSON_ANNOTATIONS_SRC="squidb-addons/squidb-json/squidb-json-annotations/src"
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

SOURCEPATH="${GEN}:${SQUIDB_SRC}:${SQUIDB_ANNOTATIONS_SRC}:${SQUIDB_JSON_SRC}:${SQUIDB_JSON_ANNOTATIONS_SRC}:${SQUIDB_IOS_SRC}:${SQUIDB_IOS_TESTS_SRC}:${SQUIDB_TESTS_ROOT}"
#echo ${SOURCEPATH}

function buildTestExecutable () {
    ${J2OBJC_HOME}/j2objcc -L${J2OBJC_HOME}/lib/macosx "${LINK_ARGS[@]}" -ObjC -o run_squidb_ios_tests $BIN/*.o # link with libraries
    linkerResult=$?
    if [ ! $linkerResult -eq 0 ]
    then
        echo "Linker failed with error code $linkerResult"
        exit $linkerResult
    fi
}

function runTests () {
    ./run_squidb_ios_tests
    testResults=$?
    rm run_squidb_ios_tests
    if [ ! $testResults -eq 0 ]
    then
        echo "Unit test failures, exiting with code $testResults"
        exit $testResults
    fi
}

function downloadSQLiteAmalgamation () {
    SQLITE_VERSION="sqlite-amalgamation-3150000"
    echo "Downloading $SQLITE_VERSION"
    rm -rf $SQUIDB_IOS_NATIVE/sqlite
    mkdir $SQUIDB_IOS_NATIVE/sqlite

    if [ -z "$CI_IOS_TESTS" ] # local build
    then
        DESTINATION=$SQUIDB_IOS_NATIVE/sqlite
    else
        DESTINATION=/tmp
    fi

    wget https://www.sqlite.org/2016/$SQLITE_VERSION.zip -O $DESTINATION/$SQLITE_VERSION.zip
    unzip -oq $DESTINATION/$SQLITE_VERSION.zip -d $DESTINATION
    mv $DESTINATION/$SQLITE_VERSION/sqlite3.c $SQUIDB_IOS_NATIVE/sqlite
    mv $DESTINATION/$SQLITE_VERSION/sqlite3.h $SQUIDB_IOS_NATIVE/sqlite
}

# Build annotation and processor jars
if [ -z "$CI_IOS_TESTS" ] # only build annotation processors from scratch when not on CI
then
    ./gradlew squidb-annotations:jar squidb-processor:jar squidb-json-annotations:jar squidb-json-compiler:jar
    for f in squidb-annotations/build/libs/*.jar squidb-processor/build/libs/*.jar squidb-addons/squidb-json/squidb-json-annotations/build/libs/*.jar squidb-addons/squidb-json/squidb-json-compiler/build/libs/*.jar
    do
        rsync -rc -t $f $SQUIDB_IOS_TESTS
    done
    rm $SQUIDB_IOS_TESTS/*-javadoc.jar
    rm $SQUIDB_IOS_TESTS/*-sources.jar
fi

# invoke annotation processing, output to gen folder
javac -classpath "${J2OBJC_HOME}/lib/j2objc_junit.jar:$SQUIDB_IOS_TESTS/*" \
    -s $GEN -proc:only -AsquidbPlugins=com.yahoo.squidb.json.JSONPlugin -sourcepath "${SOURCEPATH}" ${SQUIDB_TESTS_TEST_SRC}/**/*.java
javacResult=$?
if [ ! $javacResult -eq 0 ]
then
    echo "javac exited with error code $javacResult"
    exit $javacResult
fi

# invoke j2objc to translate java sources
${J2OBJC_HOME}/j2objc -classpath "${J2OBJC_HOME}/lib/j2objc_junit.jar:${J2OBJC_HOME}/lib/jre_emul.jar" -d $INTERMEDIATE \
    --no-package-directories -use-arc -sourcepath "${SOURCEPATH}" \
    ${SQUIDB_SRC}/**/*.java ${SQUIDB_IOS_SRC}/**/*.java ${SQUIDB_JSON_SRC}/**/*.java ${SQUIDB_JSON_ANNOTATIONS_SRC}/**/*.java \
    ${SQUIDB_TESTS_TEST_SRC}/*.java ${GEN}/**/*.java ${SQUIDB_IOS_TESTS_SRC}/**/*.java ${SQUIDB_TESTS_DATA_SRC}/*.java \
    ${SQUIDB_TESTS_SQL_SRC}/*.java ${SQUIDB_TESTS_UTILITY_SRC}/*.java
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
    ${J2OBJC_HOME}/j2objcc -fobjc-arc -I$INTERMEDIATE -I$SQUIDB_IOS_NATIVE -I$SQUIDB_IOS_NATIVE/sqlite -o "$BIN/${$(basename $f)%.*}.o" -c $f
    j2objccResult=$?
    if [ ! $j2objccResult -eq 0 ]
    then
        echo "j2objcc failed to compile $f, exiting with code $j2objccResult"
        exit $j2objccResult
    fi
done

# When using the -ObjC flag, the -ljre_core, -ljre_util, and -ljre_concurrent flags are the ones SquiDB requires.
# If not using the flag, it should be safe to use -ljre_emul, because unused symbols will be stripped
# the android_util lib is used for testing json functions using the org.json package, and in turn requires jre_net
LINK_ARGS_BASE=(-ljre_core -ljre_util -ljre_concurrent -ljunit -landroid_util -ljre_net)
LINK_ARGS_SQLITE=("${LINK_ARGS_BASE[@]}")
LINK_ARGS_SQLITE+=(-lsqlite3)

echo "Building test executable for default SQLite"
LINK_ARGS=("${LINK_ARGS_SQLITE[@]}")
buildTestExecutable
runTests

echo "Building test executable for custom built SQLite"
downloadSQLiteAmalgamation
f=$SQUIDB_IOS_NATIVE/sqlite/sqlite3.c
echo "Compiling $f"
gcc -DSQLITE_ENABLE_FTS3 -DSQLITE_ENABLE_JSON1 -DSQLITE_TEMP_STORE=3 -DHAVE_STRCHRNUL=0 \
    -I$SQUIDB_IOS_NATIVE/sqlite -o "$BIN/${$(basename $f)%.*}.o" -c $f
LINK_ARGS=("${LINK_ARGS_BASE[@]}")
buildTestExecutable
runTests

exit 0
