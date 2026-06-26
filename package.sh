#!/bin/bash

set -e

./gradlew installDist

rm -rf build/jre
rm -rf build/sdk

mkdir build/sdk
cd build/sdk

wget -q https://download.java.net/java/GA/javafx26.0.1/8e81b911af59415286d745a64d36b878/4/openjfx-26.0.1_macos-aarch64_bin-jmods.tar.gz
tar xzf openjfx-26.0.1_macos-aarch64_bin-jmods.tar.gz

wget -q https://download.java.net/java/GA/javafx26.0.1/8e81b911af59415286d745a64d36b878/4/openjfx-26.0.1_macos-aarch64_bin-sdk.tar.gz
tar xzf openjfx-26.0.1_macos-aarch64_bin-sdk.tar.gz

cd ../..

jlink -v \
      --module-path build/sdk/javafx-jmods-26.0.1/ \
      --add-modules java.base,java.desktop,java.logging,javafx.controls \
      --output build/jre

libdir=build/jre/lib

cp build/sdk/javafx-sdk-26.0.1/lib/*.dylib $libdir

cp build/install/g2fx/lib/*.jar $libdir

rm $libdir/libusb4java-1.3.0-*
cp build/install/g2fx/lib/libusb4java-1.3.0-darwin-aarch64.jar $libdir

rm -rf build/app-image

version=`./gradlew printVersion -q`
build="$GITHUB_RUN_NUMBER"
if [ -z "$build" ]; then build="0000"; fi

jpackage --type app-image \
         --input build/jre/lib \
         --name G2FX \
         --main-jar g2fx-$version.jar \
         --main-class org.g2fx.g2gui.G2GuiApplication \
         --runtime-image build/jre \
         --dest build/app-image \
         --icon data/icon/G2FX.icns \
         --verbose

cd build/app-image
zf="G2FX-macos-aarch64-$version-$build.zip"
zip -q -r $zf G2FX.app

echo $zf
