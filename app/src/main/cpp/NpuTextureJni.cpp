/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

#include "ResolutionEnhancement.h"
#include "Utils.h"
#include <GLES3/gl32.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <string>
#include <vector>

#define HLOGI(TAG, ...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define HLOGW(TAG, ...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define HLOGE(TAG, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define TAG             "JNI"

// 存放资源的路径
std::string imagePath = "/data/data/com.hihonor.videokit/sources/butterfly_lr.png";

ImageInfo inOutPicture;

// 声明VideoKitNpu接口函数指针
VideoStatusCode (*FuncInitialize)(long handle, const void *dlcBuffer, size_t dlcLength, const char *skeLibraryPath,
                                  size_t skeLibraryPathLength);
VideoStatusCode (*FuncInfer)(long handle, ImageInfo *inPicture);
VideoStatusCode (*FuncRelease)(long handle);

extern "C" JNIEXPORT int JNICALL Java_com_hihonor_videokit_npu_NpuActivity_nativeGetTextureID(JNIEnv *env, jobject thiz,
                                                                                          jint in_width, jint in_height,
                                                                                          jobject assetManager)
{
    AAssetManager *nativeasset = AAssetManager_fromJava(env, assetManager);

    AAsset *asset = AAssetManager_open(nativeasset, "model_quant_sm8650.dlc", AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        HLOGE(TAG, "open dlc failed");
        return VIDEO_UNKNOWN_FAIL;
    }

    const void *buffer = AAsset_getBuffer(asset);
    off_t length = AAsset_getLength(asset);

    std::vector<faith::Buffer> inputBuffers;

    // 输入数据进行预处理操作
    int resPro = PreProcessImg(imagePath, inputBuffers);
    if (VIDEO_SUCCESS != resPro) {
        HLOGE(TAG, "PreProcessImg failed resPro = %d", resPro);
        return VIDEO_UNKNOWN_FAIL;
    }

    // 纹理对象创建
    GLuint textureId;
    glGenTextures(1, &textureId);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, in_width, in_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, inputBuffers[0].data);
    glBindTexture(GL_TEXTURE_2D, 0);

    // 加载动态库  libmedianpu编译时需要依赖libbolt 因此先加载
    void *handleBoltPtr = dlopen("/data/data/com.hihonor.videokit/lib/arm64-v8a/libbolt.so", RTLD_NOW);
    if (handleBoltPtr == nullptr) {
        HLOGE(TAG, "open libbolt so failed: %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }

    void *handleMediaNpuPtr = dlopen("/data/data/com.hihonor.videokit/lib/arm64-v8a/libmedianpu.so", RTLD_NOW);
    if (handleMediaNpuPtr == nullptr) {
        HLOGE(TAG, "open libmedianpu so failed: %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }

    void *handleSharePtr = dlopen("/data/data/com.hihonor.videokit/lib/arm64-v8a/libc++_shared.so", RTLD_NOW);
    if (handleSharePtr == nullptr) {
        HLOGE(TAG, "open libc++_shared.so failed: %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }
    void *handleSnpePtr = dlopen("/data/data/com.hihonor.videokit/lib/arm64-v8a/libSNPE.so", RTLD_NOW);
    if (handleSnpePtr == nullptr) {
        HLOGE(TAG, "open libSNPE.so failed: %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }
    void *handle73StubPtr = dlopen("/data/data/com.hihonor.videokit/lib/arm64-v8a/libSnpeHtpV73Stub.so", RTLD_NOW);
    if (handle73StubPtr == nullptr) {
        HLOGE(TAG, "open libSnpeHtpV73Stub.so failed: %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }
    void *handle75StubPtr = dlopen("/data/data/com.hihonor.videokit/lib/arm64-v8a/libSnpeHtpV75Stub.so", RTLD_NOW);
    if (handle75StubPtr == nullptr) {
        HLOGE(TAG, "open libSnpeHtpV75Stub.so failed: %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }

    // 获取VideoKitNpu接口函数
    *reinterpret_cast<void **>(&FuncInitialize) = ::dlsym(handleMediaNpuPtr, "Initialize");
    if (FuncInitialize == nullptr) {
        HLOGE(TAG, "Could not dlsym 'initialize': %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }
    *reinterpret_cast<void **>(&FuncInfer) = ::dlsym(handleMediaNpuPtr, "Infer");
    if (FuncInfer == nullptr) {
        HLOGE(TAG, "Could not dlsym 'infer': %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }
    *reinterpret_cast<void **>(&FuncRelease) = ::dlsym(handleMediaNpuPtr, "Release");
    if (FuncRelease == nullptr) {
        HLOGE(TAG, "Could not dlsym 'release': %s", dlerror());
        return VIDEO_UNKNOWN_FAIL;
    }

    // 输入图片信息
    inOutPicture.textureId = (int) textureId;
    inOutPicture.height = in_height;
    inOutPicture.width = in_width;

    int64_t handle = -1;

    // libSnpeVxxSkel.so相关so的路径
    const char *skelPath = "/data/data/com.hihonor.videokit/lib/arm64-v8a/";
    size_t skeLibraryPathLength = strlen(skelPath);

    // 调用初始化model函数
    int npuResult = FuncInitialize(handle, buffer, length, skelPath, skeLibraryPathLength);
    if (VIDEO_SUCCESS != npuResult) {
        HLOGE(TAG, "initialize failed");
        return npuResult;
    }

    // 推理函数, 处理数据
    npuResult = FuncInfer(handle, &inOutPicture);
    if (VIDEO_SUCCESS != npuResult) {
        HLOGE(TAG, "infer failed");
        return npuResult;
    }

    // 释放资源
    if (VIDEO_SUCCESS != FuncRelease(handle)) {
        HLOGW(TAG, "no resources used");
    }

    // 关闭动态库
    dlclose(handleBoltPtr);
    dlclose(handleMediaNpuPtr);
    dlclose(handleSharePtr);
    dlclose(handleSnpePtr);
    dlclose(handle73StubPtr);
    dlclose(handle75StubPtr);

    // 创建一个帧缓冲对象（Framebuffer Object，FBO）并绑定它
    GLuint fbo;
    glGenFramebuffers(1, &fbo);
    glBindFramebuffer(GL_FRAMEBUFFER, fbo);

    // 将纹理附加到帧缓冲对象的颜色附件上
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, inOutPicture.textureId, 0);

    // 申请用于存储像素数据的缓冲区 若宽 高不变 则不需在申请内存
    char *pixels = new char[inOutPicture.width * inOutPicture.height * 4];
    HLOGI(TAG, "glReadPixels enter width = %d,height = %d,", inOutPicture.width, inOutPicture.height);

    // 3.读取纹理数据到 CPU 缓冲区中  GL_RGBA
    glReadPixels(0, 0, inOutPicture.width, inOutPicture.height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

    std::vector<faith::Buffer> outputFloatBuffers(1);
    outputFloatBuffers[0].data = pixels;

    outputFloatBuffers[0].bytes = inOutPicture.width * inOutPicture.height * 4;

    PostProcess(imagePath, outputFloatBuffers[0]);

    HLOGI(TAG, "PostProcess end ");

    return 0;
}
