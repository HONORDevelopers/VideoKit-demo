/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

#ifndef VIDEOKIT_NPU_RESOLUTIONENHANCEMENT_H
#define VIDEOKIT_NPU_RESOLUTIONENHANCEMENT_H

#include <cstddef>

#ifdef __cplusplus
extern "C" {
#endif
enum VideoStatusCode {
    VIDEO_SUCCESS = 0,
    VIDEO_UNKNOWN_FAIL,
    VIDEO_CREATE_MODEL_FAIL,
    VIDEO_GL_OPERATION_FAIL,
    VIDEO_INFER_FAIL,
    VIDEO_FAITH_MODEL_NULL,
    VIDEO_PARAMETER_INVALID
};

// 提供Image信息
struct ImageInfo {
    int textureId;
    int width;
    int height;
};

// 基于模型配置信息，初始化创建 model
/*
 * handle 用于标识当前模型推理对象实例
 * dlcBuffer .dlc文件的内容
 * dlcLength .dlc文件内容大小
 * skeLibraryPath libSnpeVxxSkel.so相关so的路径
 * skeLibraryPathLength libSnpeVxxSkel.so相关so的路径长度
 */
VideoStatusCode Initialize(long handle, const void *dlcBuffer, size_t dlcLength, const char *skeLibraryPath,
                           size_t skeLibraryPathLength);
// 对纹理进行推理
/*
 * handle 用于标识当前模型推理对象实例
 * inOutImage 作为输入和输出image数据的载体
 */
VideoStatusCode Infer(long handle, ImageInfo *inOutImage);

// 释放相关的资源
/*
 * handle用于标识当前模型推理对象实例
 */
VideoStatusCode Release(long handle);
#ifdef __cplusplus
}
#endif

#endif  // VIDEOKIT_NPU_RESOLUTIONENHANCEMENT_H
