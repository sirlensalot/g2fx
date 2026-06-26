#!/bin/bash

set -e

./gradlew installDist

rm -rf build/jre

jlink -v \
      --module-path /Users/stuart/dev/javafx-jmods-26.0.1/ \
      --add-modules java.base,java.desktop,java.logging,javafx.controls \
      --output /Users/stuart/dev/g2fx/build/jre

libdir=build/jre/lib

cp ../javafx-sdk-26.0.1/lib/*.dylib $libdir

cp build/install/g2fx/lib/*.jar $libdir

rm $libdir/libusb4java-1.3.0-*
cp build/install/g2fx/lib/libusb4java-1.3.0-darwin-aarch64.jar $libdir

rm -rf build/app-image

jpackage --type app-image \
         --input /Users/stuart/dev/g2fx/build/jre/lib \
         --name G2FX \
         --main-jar g2fx-1.0-SNAPSHOT.jar \
         --main-class org.g2fx.g2gui.G2GuiApplication \
         --runtime-image /Users/stuart/dev/g2fx/build/jre \
         --dest /Users/stuart/dev/g2fx/build/app-image \
         --icon data/icon/G2FX.icns \
         --verbose

echo "build/app-image/G2FX.app"
