#!/bin/bash

./gradlew installDist

rm -rf build/jre


jlink -v \
      --module-path /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/jmods/:/Users/stuart/dev/javafx-sdk-21.0.7/lib/ \
      --add-modules java.base,java.desktop,java.logging,javafx.controls \
      --output /Users/stuart/dev/g2fx/build/jre

libdir=build/jre/lib
dist=build/install/g2fx/lib

cp ../javafx-sdk-21.0.7/lib/*.dylib $libdir
cp $dist/g2fx-1.0-SNAPSHOT.jar $libdir
cp $dist/jackson*.jar $libdir
cp $dist/guava*.jar $libdir
cp $dist/controlsfx*.jar $libdir
cp $dist/richtextfx*.jar $libdir
cp $dist/snakeyaml*.jar $libdir
cp $dist/libusb4java-1.3.0-darwin-aarch64.jar $libdir
cp $dist/usb4java*.jar $libdir
cp $dist/flowless*.jar $libdir
cp $dist/jline*.jar $libdir
cp $dist/reactfx*.jar $libdir
cp $dist/undofx*.jar $libdir
cp $dist/wellbehavedfx*.jar $libdir
cp $dist/commons-lang3*.jar $libdir


rm -rf build/app-image

jpackage --type app-image \
         --input /Users/stuart/dev/g2fx/build/jre/lib \
         --name G2FX \
         --main-jar g2fx-1.0-SNAPSHOT.jar \
         --main-class org.g2fx.g2gui.G2GuiApplication \
         --runtime-image /Users/stuart/dev/g2fx/build/jre \
         --dest /Users/stuart/dev/g2fx/build/app-image \
         --verbose
