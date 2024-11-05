/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
// WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

#ifndef _FAITH_FAITH_H
#define _FAITH_FAITH_H

#include <vector>
#include <string>

typedef enum {
    /// Run in a standard mode.
    /// This mode will be deprecated in the future and replaced with BALANCED.
    SNPE_PERFORMANCE_PROFILE_DEFAULT = 0,
    /// Run in a balanced mode.
    SNPE_PERFORMANCE_PROFILE_BALANCED = 0,

    /// Run in high performance mode
    SNPE_PERFORMANCE_PROFILE_HIGH_PERFORMANCE = 1,

    /// Run in a power sensitive mode, at the expense of performance.
    SNPE_PERFORMANCE_PROFILE_POWER_SAVER = 2,

    /// Use system settings.  SNPE makes no calls to any performance related APIs.
    SNPE_PERFORMANCE_PROFILE_SYSTEM_SETTINGS = 3,

    /// Run in sustained high performance mode
    SNPE_PERFORMANCE_PROFILE_SUSTAINED_HIGH_PERFORMANCE = 4,

    /// Run in burst mode
    SNPE_PERFORMANCE_PROFILE_BURST = 5,

    /// Run in lower clock than POWER_SAVER, at the expense of performance.
    SNPE_PERFORMANCE_PROFILE_LOW_POWER_SAVER = 6,

    /// Run in higher clock and provides better performance than POWER_SAVER.
    SNPE_PERFORMANCE_PROFILE_HIGH_POWER_SAVER = 7,

    /// Run in lower balanced mode
    SNPE_PERFORMANCE_PROFILE_LOW_BALANCED = 8,
} Snpe_PerformanceProfile_t;

namespace faith {

using TensorShape = std::vector<unsigned int>;

/** CPU affinity policy */
enum class BackendType {
    CPU_HIGH_PERFORMANCE = 0,  ///< performance is high priority(use big core)
    CPU_LOW_POWER = 1,         ///< power is high priority(use small core)
    GPU = 2,                   ///< use GPU
    CLML = 3,                  ///< use qcom adreno opencl ml sdk
    HTP = 4                    ///< use snpe
};

/** data precision */
enum class TensorType {
    FP32 = 0,    ///< 32 bit float
    FP16 = 1,    ///< 16 bit float
    INT32 = 2,   ///<  32 bit integer
    UINT32 = 3,  ///<  32 bit unsigned integer
    UINT8 = 4,   ///< 8 bit unsigned integer
};

/** multi-dimension data format */
enum class TensorLayout {
    NCHW = 0,       ///< batch->channel->height->width data order
    NHWC = 1,       ///< batch->height->width->channel data order
    NCHWC8 = 2,     ///< batch->channel/8->height->width->8 data order
    ROW_MAJOR = 3,  ///< batch->unit data order
    RNN_MTK = 4,    ///< batch->time->unit data order
    NCHWC4 = 5      ///< batch->channel/4->width->high->channel four element data order
};

// forward declaration
struct Model;

struct Buffer {
    void *data;
    size_t bytes;
};

// IOTensor
struct IOTensorSpec {
    std::string name;
    TensorType type;
    TensorLayout layout;  // 0=NCHW
    TensorShape shape;    // vector{1, 2, 3}
    uint64_t offset;
    float scale;
};

// For model and algo config, either both use stream (default) or both use path
struct ModelConfig {
    BackendType backend;
    Buffer modelStream;
    Buffer algoStream;
    std::string modelPath;
    std::string algoPath;
    bool useFileStream;
    // the directory of libSnpeVxxSkel.so, if skel.so exist in "/data/path/libSnpeVxxSkel.so", it should be setted as "/data/path"
    std::string adsp_dir;
    // the path of libSNPE.so, if so exist in "/data/path/libSNPE.so", it should be setted as "/data/path/libSNPE.so"
    std::string snpe_path;
    // the performance of snpe when infer
    Snpe_PerformanceProfile_t performance;
    // For Adreno GPU, recordable queue can lower the overhead of enqueuing.
    bool useRecordableQueue;
};

// Return status
enum class ReturnStatus {
    SUCCESS = 0,  ///< SUCCESS
    FAIL = -1,    ///< FAIL
    NULLPTR = -2  ///< NULLPTR
};

// create model by pre-setted ModelConfig
Model *CreateModelFromFileStream(const ModelConfig &modelConfig);

/**
 * @brief Query inputs/outputs tensor specs from model. `inputs` and `outputs` can be NULL.
 *
 * @param model
 * @param inputs
 * @param outputs
 * @return ReturnStatus
 */
ReturnStatus GetIOTensorSpecFromModel(Model *model, std::vector<IOTensorSpec> *inputs,
                                      std::vector<IOTensorSpec> *outputs);

// run model, the data of inputBuffers should be uint8, if use snpe
ReturnStatus RunModel(Model *model, const std::vector<Buffer> &inputBuffers);

std::vector<Buffer> GetOutputBuffers(Model *model);

ReturnStatus DestroyModel(Model *model);

}  // namespace faith

#endif  // _FAITH_FAITH_H
