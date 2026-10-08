#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper script for UNIX
# ----------------------------------------------------------------------------

which mvn >/dev/null 2>&1
if [ $? -eq 0 ]; then
    exec mvn "$@"
fi

if [ -n "$M2_HOME" ] && [ -x "$M2_HOME/bin/mvn" ]; then
    exec "$M2_HOME/bin/mvn" "$@"
fi

if [ -n "$MAVEN_HOME" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
    exec "$MAVEN_HOME/bin/mvn" "$@"
fi

echo "[ERROR] Apache Maven was not found on your PATH, M2_HOME, or MAVEN_HOME."
echo "[ERROR] Please install Apache Maven and JDK 21 to run."
exit 1
