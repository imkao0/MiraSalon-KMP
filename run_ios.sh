#!/bin/bash

# Configuration
BUNDLE_ID="iz.mkao.mirasalon.MiraSalon-KMP"
SCHEME="iosApp"
PROJECT_PATH="iosApp/iosApp.xcodeproj"
DERIVED_DATA_PATH="build/ios"

# 1. Find booted simulator
BOOTED_DEVICE_ID=$(xcrun simctl list devices available | grep "(Booted)" | head -1 | sed -E 's/.*\(([-A-Z0-9]+)\).*/\1/')

if [ -z "$BOOTED_DEVICE_ID" ]; then
    echo "❌ No booted simulator found. Please start a simulator first."
    exit 1
fi

DEVICE_NAME=$(xcrun simctl list devices available | grep "$BOOTED_DEVICE_ID" | sed -E 's/^[[:space:]]+(.*) \([-A-Z0-9]+\) \(Booted\).*/\1/')

echo "📱 Target Simulator: $DEVICE_NAME ($BOOTED_DEVICE_ID)"

# 2. Build Kotlin Multiplatform Framework
echo "⚙️  Building Kotlin Multiplatform Framework..."
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -DskipAndroid=true

if [ $? -ne 0 ]; then
    echo "❌ Kotlin build failed."
    exit 1
fi

# 3. Build iOS App
echo "🏗️  Building iOS App ($SCHEME)..."
export GRADLE_OPTS="-DskipAndroid=true"
xcodebuild -project "$PROJECT_PATH" \
           -scheme "$SCHEME" \
           -configuration Debug \
           -sdk iphonesimulator \
           -derivedDataPath "$DERIVED_DATA_PATH" \
           -destination "platform=iOS Simulator,id=$BOOTED_DEVICE_ID" \
           build

if [ $? -ne 0 ]; then
    echo "❌ iOS Build failed."
    exit 1
fi

# 4. Install on Simulator
APP_PATH=$(find "$DERIVED_DATA_PATH/Build/Products" -name "*.app" | head -1)

if [ -z "$APP_PATH" ]; then
    echo "❌ Could not find the compiled .app bundle."
    exit 1
fi

echo "💾 Installing app on simulator..."
xcrun simctl install "$BOOTED_DEVICE_ID" "$APP_PATH"

# 5. Launch App
echo "🚀 Launching $BUNDLE_ID..."
xcrun simctl launch "$BOOTED_DEVICE_ID" "$BUNDLE_ID"

echo "✅ Done!"
