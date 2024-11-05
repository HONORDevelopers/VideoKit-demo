# 荣耀视频服务示例代码

[![Apache-2.0](https://img.shields.io/badge/license-Apache-blue)](http://www.apache.org/licenses/LICENSE-2.0)
[![Open Source Love](https://img.shields.io/static/v1?label=Open%20Source&message=%E2%9D%A4%EF%B8%8F&color=green)](https://developer.hihonor.com/demos/)
[![Java Language](https://img.shields.io/badge/language-java-green.svg)](https://www.java.com/en/)

[English](README.md) | 中文

## 目录

 * [简介](#简介)
 * [环境要求](#环境要求)
 * [硬件要求](#硬件要求)
 * [开发准备](#开发准备)
 * [安装](#安装)
 * [技术支持](#技术支持)
 * [授权许可](#授权许可)

## 简介

本示例代码中，你将使用已创建的代码工程来调用荣耀视频服务（Video Kit）的接口。通过该工程，你将：

1. HDR 视频播放场景：通过 HDR 视频增强能力，使得视频画面高亮细节更有层次感，暗部细节有明显提升，达到更好的观看体验。

2. 视频超分场景：视频分辨率不大于360P 时，提高视频分辨率，视频分辨率在360P~720P 之间时，启用自适应锐化，提升视频画面质量，使得视频播放更清晰；借助 NPU 硬件强大的计算能力，使得业务的推理模型在 NPU 上的推理速度更快、功耗更低，视频播放更流畅、更清晰，为用户提供更好的视频播放体验。

3. 设备不支持 HDR 播放场景：通过将 HDR 格式的视频转码为 SDR 格式，确保视频在不同设备上正常播放，提供最佳的兼容性。

4. SDR 视频播放场景：通过超动态显示能力，呈现更大的亮度范围，使明暗对比更加真实，达到更好的观看体验。

 更多内容，请参见[业务简介](https://developer.honor.com/cn/docs/11023/guides/introduction)

## 环境要求

推荐使用的安卓targetSdk版本为29及以上，JDK版本为1.8及以上。

## 硬件要求

安装有Windows 10/Windows 7操作系统的计算机（台式机或者笔记本），
带USB数据线的荣耀MagicOS 8.0及以上版本手机，用于业务调试。

## 开发准备

1.	注册荣耀帐号，成为荣耀开发者。
2.	创建应用，启动接口。
3.	构建本示例代码，需要先把它导入安卓集成开发环境（Android Studio的版本为2021.2.1及以上）。然后从[荣耀开发者服务平台](https://developer.honor.com/)下载应用的mcs-services.json文件，并添加到对应示例代码根目录下。另外，需要生成签名证书指纹并将证书文件添加到项目中，然后将配置添加到build.gradle。详细信息，请参见[集成指南](https://developer.honor.com/cn/docs/11032/guides/intergrate)集成准备。


## 安装
* 方法1：在Android Studio中进行代码的编译构建。构建APK完成后，将APK安装到手机上，并调试APK。
* 方法2：在Android Studio中生成APK。使用ADB（Android Debug Bridge）工具通过adb install {YourPath/YourApp.apk} 命令将APK安装到手机，并调试APK。

## 技术支持

如果您对该示例代码还处于评估阶段，可在[荣耀开发者社区](https://developer.honor.com/cn/forum/?navation=dh11614886576872095748%2F1)获取关于MediaData Kit的最新讯息，并与其他开发者交流见解。

如果您对使用该示例代码有疑问，请尝试：
- 开发过程遇到问题上[Stack Overflow](https://stackoverflow.com/questions/tagged/honor-developer-services?tab=Votes)，在`honor-developer-services`标签下提问，有荣耀研发专家在线一对一解决您的问题。

如果您在尝试示例代码中遇到问题，请向仓库提交[issue](https://github.com/HONORDevelopers/Honor-Push-Client-Demo-demo/issues)，也欢迎您提交[Pull Request](https://github.com/HONORDevelopers/Honor-Push-Client-Demo-demo/pulls)。

## 授权许可

该示例代码经过[Apache 2.0授权许可](http://www.apache.org/licenses/LICENSE-2.0)。