#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Start Up Batch script
#
# Required ENV Vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV Vars
# -----------------
#   MAVEN_OPTS - parameters to passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ]; then
  if [ -f /etc/mavenrc ]; then
    . /etc/mavenrc
  fi
  if [ -f "$HOME/.mavenrc" ]; then
    . "$HOME/.mavenrc"
  fi
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
mingw=false
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true;;
  MINGW*) mingw=true;;
esac

## Resolve links - $0 may be a link to the script
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done

SAVED="`pwd`"
cd "`dirname \"$PRG\"`" >/dev/null
APPDIR="`pwd`"
cd "$SAVED" >/dev/null

JAVACMD="`which java 2>/dev/null`"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ];  then
    JAVACMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "The JAVA_HOME environment variable is not defined correctly" >&2
  echo "This environment variable is needed to run this program" >&2
  echo "NB: JAVA_HOME should point to a JDK not a JRE" >&2
  exit 1
fi

WRAPPER_JAR="$APPDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER="org.apache.maven.wrapper.MavenWrapperMain"

# Extension to allow automatically downloading the maven-wrapper.jar
if [ ! -r "$WRAPPER_JAR" ]; then
    if [ -r "$APPDIR/.mvn/wrapper/maven-wrapper.properties" ]; then
        WRAPPER_URL=`grep "^wrapperUrl" "$APPDIR/.mvn/wrapper/maven-wrapper.properties" | cut -d= -f2`
    fi
    if [ -z "$WRAPPER_URL" ]; then
        WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    fi
    if command -v curl >/dev/null 2>&1; then
        curl -s -f -L -o "$WRAPPER_JAR" "$WRAPPER_URL" 2>/dev/null
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL" 2>/dev/null
    fi
fi

# Fallback to local mvn if wrapper jar couldn't be loaded
if [ ! -r "$WRAPPER_JAR" ] && command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
fi

exec "$JAVACMD" \
  $MAVEN_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.home=$APPDIR/.mvn/wrapper/dists" \
  "-Dmaven.multiModuleProjectDirectory=$APPDIR" \
  $WRAPPER_LAUNCHER "$@"
