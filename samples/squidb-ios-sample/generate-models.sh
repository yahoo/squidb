#!/bin/sh
if [[ -z "$SRCROOT" ]]
then
    SRCROOT="."
fi
GEN="${SRCROOT}/squidb-ios-sample/squidb-gen"
JARS="${SRCROOT}/jars"
SQUIDB_ROOT="${SRCROOT}/../.."
SQUIDB_SRC="$SQUIDB_ROOT/squidb/src"
SQUIDB_ANNOTATIONS_SRC="$SQUIDB_ROOT/squidb-annotations/src"
SQUIDB_SAMPLE_CORE_SRC="${SRCROOT}/../squidb-sample-core/src/main/java"

SOURCEPATH="${GEN}:${SQUIDB_SRC}:${SQUIDB_ANNOTATIONS_SRC}:${SQUIDB_SAMPLE_CORE_SRC}"

javac -classpath "$JARS/*" -s $GEN -proc:only -sourcepath "${SOURCEPATH}" \
    $SQUIDB_SAMPLE_CORE_SRC/com/yahoo/squidb/sample/models/*.java
