#!/usr/bin/env sh
# Gradle start up script for UN*X
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`
APP_HOME=`( cd "$APP_HOME" ; pwd -P )`
MAX_FD="maximum"
warn () { echo "$*"; }
die () { echo; echo "ERROR: $*"; echo; exit 1; }
OS_TYPE=`uname`
case $OS_TYPE in Darwin*) OS_TYPE="Mac";; Linux*) OS_TYPE="Linux";; CYGWIN*|MINGW*) OS_TYPE="Cygwin";; *) OS_TYPE="Other";; esac
if [ "$OS_TYPE" = "Mac" ] || [ "$OS_TYPE" = "Linux" ]; then
    if [ "$MAX_FD" = "maximum" ] || [ "$MAX_FD" = "max" ]; then
        MAX_FD_LIMIT=`ulimit -H -n`
        [ $? -eq 0 ] && MAX_FD=$MAX_FD_LIMIT || warn "Could not query maximum file descriptor limit"
    fi
    [ "$MAX_FD" != "unlimited" ] && ulimit -n $MAX_FD
fi
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
[ -z "$JAVA_HOME" ] && JAVA_CMD=java || JAVA_CMD="$JAVA_HOME/bin/java"
exec "$JAVA_CMD" \
    $JAVA_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
