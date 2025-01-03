
# quickpose-android-sdk


[![Swift Package Manager](https://img.shields.io/badge/Swift%20Package%20Manager-compatible-brightgreen.svg)](https://github.com/apple/swift-package-manager)

QuickPose provides developer-oriented cutting edge ML features of MediaPipe and BlazePose, with easy integration and production ready code, which dramatically improves the speed of implementation of pose estimation, skeleton tracking and fitness counting features into mobile applications. 

See our [Features](#features) below or [checkout our full documentation](https://docs.quickpose.ai/docs/MobileSDK)  on our website [docs.quickpose.ai/docs/MobileSDK](https://docs.quickpose.ai/docs/MobileSDK)

| Range Of Motion Example | Leg Raises Counter Example | 
| --------------- |:-----------------:| 
|![health-shoulder-right-rom](docs/v0.3/health-shoulder-right-rom.gif) |![fitness-leg-raises](docs/v1.1.0/fitness-leg-raises.gif) |

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Register an SDK Key](#register-an-sdk-key)
- [How it works](#how-it-works)
- [Features](#features)
- [Meta Features](#meta-features)
- [Supported Platforms](#supported-platforms)
- [Requirements](#requirements)
- [Installing the SDK](#installing-the-sdk)
  - [Swift Package Manager](#swift-package-manager)
  - [CocoaPods](#cocoapods)
- [Getting Started](#getting-started)
  - [Getting Started with Newer Macs M1/M2](#getting-started-with-newer-macs-m1m2)
  - [Getting Started with Older Intel Macs](#getting-started-with-older-intel-macs)
  - [SwiftUI Example](#swiftui-example)
- [Documentation](#documentation)
- [Troubleshooting](#troubleshooting)
  - [No Such Module](#no-such-module)
  - [Cannot find type 'QuickPoseCaptureAVAssetOutputSampleBufferDelegate' in scope](#cannot-find-type-quickposecaptureavassetoutputsamplebufferdelegate-in-scope)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Register an SDK Key
------------------

Get your free SDK key on [https://dev.quickpose.ai](https://dev.quickpose.ai), usage limits may apply. 
SDK Keys are linked to your bundle ID, please check Key before distributing to the Play Store.

How it works
------------------

QuickPose process a video frame and makes it easy for developers to perform complex AI features to the image, such as overlaying markings to the output image to highlight the user's pose.

```swift
+----------+          +-------------+          +-----------------+
|          |          |             |          |  Overlay View   |
|  Camera  |--------->|  QuickPose  |--------->|        +        |
|          |          |             |          |     Results     |
+----------+          +-------------+          +-----------------+
```

Features
------------------

| Feature       | Example       | Supported |
| ------------- |:-------------:| ---------:|
| Joint Positions  | ![MediaPipe Landmarks](docs/v0.2/overlay-all-points.gif) | v0.1        |
| <p><b>Overlays</b></p><p>Whole Body</p><p>Upper Body</p><p>Lower Body</p><p>Shoulder</p><p>Left Arm</p><p>Right Arm</p><p>Left Leg</p><p>Right Leg</p>       |  ![Whole Body Overlay](docs/v0.1/overlay-whole-body.gif) | v0.1        |
| <p><b>Health - Range Of Motion</b></p><p>Left Shoulder</p><p>Right Shoulder</p><p>Left Hip</p><p>Right Hip</p><p>Left Knee</p><p>Right Knee</p><p>Back</p><p>Neck</p>       |  ![health-shoulder-right-rom](docs/v0.3/health-shoulder-right-rom.gif)  | v0.1      |
   

Meta Features
------------------

| Feature       | Example       | Supported |
| ------------- |:-------------:| ---------:|
| Stacked Feature Styling | ![shoulder-conditional-image](docs/v0.4/bike-demo.gif)<br />[<small>Bike Side View Video by Tariq Ali</small>](https://www.youtube.com/watch?v=LRA4N5cGnLU) | v0.1 |   
| Conditional Styling | ![knee-conditional-image](docs/v0.4/health-knee-rom-conditional.gif)  | v0.1 |   



Supported Platforms
------------------

| Android Device | Emulator x86_64 | Emulator arm64  | 
| --------------:| ---------------:|----------------:|
| ✅ Runs        |  ⚙ Compiles |        ⚙ Compiles  |

Requirements
------------------

- Android SDK 28+

Installing the SDK
------------------

### Install Android Libraries

In your app's `build.gradle`, copy the quickpose dependencies

```gradle
dependencies {
    // recommended for starting quickpose
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1'")
    
    // CameraX core library
    val camerax_version = ("1.3.0-alpha05")
    
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    implementation("com.google.flogger:flogger:latest.release")
    implementation("com.google.flogger:flogger-system-backend:latest.release")
    implementation("com.google.guava:guava:27.0.1-android")
    implementation("com.google.protobuf:protobuf-javalite:3.19.1")

    implementation("com.microsoft.onnxruntime:onnxruntime-android:latest.release")
    implementation("ai.quickpose:quickpose-mp:0.1")
    implementation("ai.quickpose:quickpose-core:0.2")
}

```


Getting Started
------------------

See code examples below or download our [Sample Apps](/SampleApps).


__Step 1__: Download/Clone Repo

__Step 2__: Open Basic Demo

__Step 3__: Build to your device

__Step 4__: Run

__Step 5__: Explore the features and returned results

```kotlin
quickPose.start(
    arrayOf(
        Feature.Overlay(group = Landmarks.Group.Arm(Side.LEFT))
    ),
    onFrame = { status, featureResults, feedback, landmarks ->
    	println("$status, $featureResults")
    }
 )               
```


Documentation
------------------
Checkout our full documentation at [https://docs.quickpose.ai/docs/MobileSDK](https://docs.quickpose.ai/docs/MobileSDK)
