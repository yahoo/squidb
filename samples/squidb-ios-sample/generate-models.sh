#!/bin/zsh
GEN="./squidb-ios-sample/gen"
JARS="./jars"
SQUIDB_ROOT="../.."
SQUIDB_SRC="$SQUIDB_ROOT/squidb/src"
SQUIDB_ANNOTATIONS_SRC="$SQUIDB_ROOT/squidb-annotations/src"
SQUIDB_SAMPLE_CORE_SRC="../squidb-sample-core/src/main/java"

SOURCEPATH="${GEN}:${SQUIDB_SRC}:${SQUIDB_ANNOTATIONS_SRC}:${SQUIDB_SAMPLE_CORE_SRC}"
echo $SOURCEPATH

javac -classpath "$JARS/*" -s $GEN -proc:only -sourcepath "${SOURCEPATH}" \
    $SQUIDB_SAMPLE_CORE_SRC/com/yahoo/squidb/sample/models/*.java
