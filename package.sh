#!/bin/bash

set -e

./gradlew installDist

rm -rf build/jre
rm -rf build/sdk

mkdir build/sdk
cd build/sdk

wget https://download.java.net/java/GA/javafx26.0.1/8e81b911af59415286d745a64d36b878/4/openjfx-26.0.1_macos-aarch64_bin-jmods.tar.gz
tar xzvf openjfx-26.0.1_macos-aarch64_bin-jmods.tar.gz

wget https://download.java.net/java/GA/javafx26.0.1/8e81b911af59415286d745a64d36b878/4/openjfx-26.0.1_macos-aarch64_bin-sdk.tar.gz
tar xzvf openjfx-26.0.1_macos-aarch64_bin-sdk.tar.gz

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

jpackage --type app-image \
         --input build/jre/lib \
         --name G2FX \
         --main-jar g2fx-1.0-SNAPSHOT.jar \
         --main-class org.g2fx.g2gui.G2GuiApplication \
         --runtime-image build/jre \
         --dest build/app-image \
         --icon data/icon/G2FX.icns \
         --verbose

cd build/app-image
zip -r G2FX-macos-aarch64.zip G2FX.app
