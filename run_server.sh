#!/bin/bash
export ANDROID_USER_HOME=$(pwd)/.gradle_tmp/.android
export HOME=$(pwd)/.gradle_tmp
mkdir -p $ANDROID_USER_HOME
# Use a local gradle user home to avoid permission issues in the global one
export GRADLE_USER_HOME=$(pwd)/.gradle_user_home
mkdir -p $GRADLE_USER_HOME
./gradlew :server:run --no-daemon > server_output.log 2>&1
