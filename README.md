# Honor Video Kit Sample Code (Android)
[![Apache-2.0](https://img.shields.io/badge/license-Apache-blue)](http://www.apache.org/licenses/LICENSE-2.0)
[![Open Source Love](https://img.shields.io/static/v1?label=Open%20Source&message=%E2%9D%A4%EF%B8%8F&color=green)](https://developer.hihonor.com/demos/)
[![Java Language](https://img.shields.io/badge/language-java-green.svg)](https://www.java.com/en/)

English | [中文](README_ZH.md)

## Contents

* [Introduction](#Introduction)
* [Preparations](#Preparations)
* [Environment Requirements](#Environment-Requirements)
* [Hardware Requirements](#Hardware-Requirements)
* [Installation](#Installation)
* [Technical Support](#Technical Support)
* [License](#License)

## Introduction

In this sample code, you will use the created demo project to call APIs of VideoKit-demo. Through the demo project, you will:

1.  HDR video playback scene: Through HDR video enhancement capabilities, the highlight details of the video screen are more layered, and the dark details are significantly improved, achieving a better viewing experience.

2.  Video super-resolution scene: When the video resolution is not greater than 360P, the video resolution is increased. When the video resolution is between 360P and 720P, adaptive sharpening is enabled to improve the video picture quality and make the video playback clearer; with the powerful computing power of NPU hardware, the inference speed of the business inference model on NPU is faster and the power consumption is lower, and the video playback is smoother and clearer, providing users with a better video playback experience.

3.  The device does not support HDR playback scene: By transcoding HDR format videos to SDR format, ensure that the video is played normally on different devices and provide the best compatibility.

4.  SDR video playback scene: Through ultra-dynamic display capabilities, a larger brightness range is presented, making the contrast between light and dark more realistic, achieving a better viewing experience.

For more, see[Business introduction](https://developer.honor.com/cn/docs/11023/guides/introduction)


## Environment Requirements

Android targetSdkVersion 29 or later and JDK 1.8 or later are recommended.

## Hardware Requirements

A computer (desktop or laptop) running Windows 10 or Windows 7
A Honor MagicOS 8.0 and above phones with USB data cable, which is used for debugging.

## Preparations

1.  Register as a Honor developer.
2.  Create an app and start APIs.
3.  Import your demo project to Android Studio (Chipmunk | 2021.2.1) or later. Download the mcs-services.json file of the app from Honor Developer Site, and add the file to the root directory of your project. Generate a signing certificate fingerprint, add the certificate file to your project, and add the configuration to the build.gradle file. For details, please refer to the integration preparations.

## Installation
Method 1: Compile and build the APK in Android Studio. Then, install the APK on your phone and debug it.
Method 2: Generate the APK in Android Studio. Use the Android Debug Bridge (ADB) tool to run the **adb install {*YourPath/YourApp.apk*}** command to install the APK on your phone and debug it.

## Technical Support

If you have any questions about the sample code, try the following:
- Visit [Stack Overflow](https://stackoverflow.com/questions/tagged/honor-developer-services?tab=Votes), submit your questions, and tag them with `honor-developer-services`. Honor experts will answer your questions.

If you encounter any issues when using the sample code, submit your [issues](https://github.com/HONORDevelopers/Honor-Push-Client-Demo-demo/issues) or submit a [pull request](https://github.com/HONORDevelopers/Honor-Push-Client-Demo-demo/pulls).

## License
The sample code is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).