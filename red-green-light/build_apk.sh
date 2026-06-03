#!/usr/bin/env bash
set -euo pipefail

ROOT="/home/pan/matrix_ws"
PROJECT="$ROOT/robot_edu_platform/edu_software/zsibot/red_light_green_light_android"
SDK="$ROOT/weilan/android_tools/android-sdk"
JBR="$ROOT/weilan/android_tools/android-studio/jbr"
KEYSTORE="$ROOT/weilan/android_tools/debug.keystore"
BUILD="$PROJECT/manual-build"
PKG="com.matrix.zsibotrlgl"
APK="$BUILD/zsibot-red-light-green-light.apk"
INSTALL=false

if [[ "${1:-}" == "--install" ]]; then
  INSTALL=true
fi

AAPT2="$SDK/build-tools/35.0.0/aapt2"
D8="$SDK/build-tools/35.0.0/d8"
ZIPALIGN="$SDK/build-tools/35.0.0/zipalign"
APKSIGNER="$SDK/build-tools/35.0.0/apksigner"
ADB="$SDK/platform-tools/adb"
ANDROID_JAR="$SDK/platforms/android-35/android.jar"
JAVAC="$JBR/bin/javac"
KEYTOOL="$JBR/bin/keytool"

rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/generated" "$BUILD/dex"

cp "$PROJECT/app/src/main/AndroidManifest.xml" "$BUILD/AndroidManifest.xml"

"$AAPT2" compile --dir "$PROJECT/app/src/main/res" -o "$BUILD/compiled.zip"
"$AAPT2" link \
  -o "$BUILD/app-unsigned.apk" \
  -I "$ANDROID_JAR" \
  --min-sdk-version 26 \
  --target-sdk-version 35 \
  --manifest "$BUILD/AndroidManifest.xml" \
  --java "$BUILD/generated" \
  "$BUILD/compiled.zip"

"$JAVAC" -source 8 -target 8 \
  -classpath "$ANDROID_JAR" \
  -d "$BUILD/classes" \
  "$PROJECT/app/src/main/java/com/matrix/zsibotrlgl/DogFaceView.java" \
  "$PROJECT/app/src/main/java/com/matrix/zsibotrlgl/SignalView.java" \
  "$PROJECT/app/src/main/java/com/matrix/zsibotrlgl/MainActivity.java" \
  "$BUILD/generated/com/matrix/zsibotrlgl/R.java"

"$D8" --min-api 26 --output "$BUILD/dex" $(find "$BUILD/classes" -name '*.class' | sort)

cp "$BUILD/app-unsigned.apk" "$BUILD/app-with-dex-unsigned.apk"
zip -j "$BUILD/app-with-dex-unsigned.apk" "$BUILD/dex/classes.dex"
"$ZIPALIGN" -f 4 "$BUILD/app-with-dex-unsigned.apk" "$BUILD/app-aligned.apk"

if [[ ! -f "$KEYSTORE" ]]; then
  "$KEYTOOL" -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname 'CN=Android Debug,O=Android,C=US'
fi

"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$APK" \
  "$BUILD/app-aligned.apk"

"$APKSIGNER" verify --verbose "$APK"

if [[ "$INSTALL" == true ]]; then
  "$ADB" install -r "$APK"
  "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
fi

echo "Built: $APK"
