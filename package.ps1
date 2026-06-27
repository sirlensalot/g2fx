$ErrorActionPreference = "Stop"

./gradlew installDist

Remove-Item -Recurse -Force build/jre, build/sdk -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force build/sdk | Out-Null

Set-Location build/sdk

Invoke-WebRequest -Uri "https://download.java.net/java/GA/javafx26.0.1/8e81b911af59415286d745a64d36b878/4/openjfx-26.0.1_windows-x64_bin-jmods.zip" -OutFile "openjfx-26.0.1_windows-x64_bin-jmods.zip"
Expand-Archive "openjfx-26.0.1_windows-x64_bin-jmods.zip" -DestinationPath .

Set-Location ../..

jlink -v `
  --module-path build/sdk/javafx-jmods-26.0.1/ `
  --add-modules java.base,java.desktop,java.logging,javafx.controls `
  --output build/jre

Copy-Item build/install/g2fx/lib/*.jar build/jre/lib
Remove-Item build/jre/lib/libusb4java-1.3.0-* -ErrorAction SilentlyContinue
Copy-Item build/install/g2fx/lib/libusb4java-1.3.0-win32-x86-64.jar build/jre/lib

Remove-Item -Recurse -Force build/app-image -ErrorAction SilentlyContinue

$APP_VERSION = ./gradlew -q printVersion
$BUILD_NUMBER = $env:BUILD_NUMBER
if ([string]::IsNullOrWhiteSpace($BUILD_NUMBER)) { $BUILD_NUMBER = $env:GITHUB_RUN_NUMBER }
if ([string]::IsNullOrWhiteSpace($BUILD_NUMBER)) { $BUILD_NUMBER = "0" }
$BUILD_NUMBER = "{0:D4}" -f [int]$BUILD_NUMBER
$ARTIFACT_NAME = "G2FX-$APP_VERSION`_$BUILD_NUMBER-windows-x64"

if ($env:GITHUB_ENV) {
  "ARTIFACT_NAME=$ARTIFACT_NAME" | Out-File -FilePath $env:GITHUB_ENV -Append -Encoding utf8
}

jpackage --type app-image `
  --input build/jre/lib `
  --win-console `
  --name G2FX `
  --main-jar "g2fx-$APP_VERSION.jar" `
  --main-class org.g2fx.g2gui.G2GuiApplication `
  --runtime-image build/jre `
  --dest build/app-image `
  --icon data/icon/G2FX.ico `
  --verbose

Set-Location build/app-image
$zip = "$ARTIFACT_NAME.zip"
Compress-Archive -Path G2FX\* -DestinationPath $zip -Force
Write-Output $zip
