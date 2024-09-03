/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

#include "Utils.h"
#include "external/loadpng.h"
#include <android/log.h>
#include <fstream>
#include <iostream>

#define HLOGI(TAG, ...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define HLOGW(TAG, ...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define HLOGE(TAG, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define TAG             "util"

void TransformToRaw(std::string directory, uint8_t *data, size_t size)
{
    if (directory.empty()) {
        std::cout << "error:输入文件为空" << std::endl;
        return;
    }
    std::string raw_file = directory;
    raw_file.replace(raw_file.size() - 3, 3, "raw");
    std::cout << "size:" << size << std::endl;
    std::cout << "raw_file:" << raw_file << std::endl;
    FILE *fp = NULL;
    do {
        fp = fopen(directory.c_str(), "wb");
        if (fp == NULL) {
            std::cout << "Failed to open output results file " << directory << std::endl;
            ;
            break;
        }
        std::cout << "output size:" << size << std::endl;
        ;
        if (data != nullptr) {
            fwrite(data, 1, size, fp);
        } else {
            std::cout << "Output buffer is NULL, test results not saved to file " << directory << std::endl;
            break;
        }
    } while (0);
    if (fp != NULL) {
        fclose(fp);
        fp = NULL;
    }
}

int PreProcessImg(const std::string &imgPath, std::vector<faith::Buffer> &inputBuffers)
{
    std::vector<unsigned char> image;  // the raw pixels
    unsigned width, height;
    // decode
    unsigned error = lodepng::decode(image, width, height, imgPath.c_str());
    if (error != 0) {
        HLOGE(TAG, "decode failed %d", error);
        return -1;
    }
    // preprocess
    char *bgr_img = new char[width * height * sizeof(float)];
    float *float_img = reinterpret_cast<float *>(bgr_img);
    for (size_t i = 0; i < height; i++) {
        for (size_t j = 0; j < width; j++) {
            size_t image_index = i * (252 * 4) + j * 4;
            float sum =
                ((image[image_index + 2] * 24.966 + image[image_index + 1] * 128.553 + image[image_index] * 65.481) /
                     255.0 +
                 16.0) /
                255.0;
            size_t bgr_index = i * 252 + j;
            float_img[bgr_index] = sum;
        }
    }

    inputBuffers.push_back({bgr_img, width * height * sizeof(float)});

    return 0;
}

void PostProcess(const std::string &imgPath, const faith::Buffer &outputBuffer)
{
    std::vector<unsigned char> image;  // the raw pixels
    unsigned width, height;
    // decode
    unsigned error = lodepng::decode(image, width, height, imgPath.c_str());
    if (error != 0) {
        HLOGE(TAG, "decode failed");
        return;
    }
    std::vector<std::vector<float>> factor = {
        {24.966, 112.0, -18.214}, {128.553, -74.203, -93.786}, {65.481, -37.797, 112.0}};
    // preprocess
    float *rgb_img = new float[width * height * 3];
    for (size_t i = 0; i < height; i++) {
        for (size_t j = 0; j < width; j++) {
            size_t index = i * 252 * 4 + j * 4;
            float b = image[index + 2];
            float g = image[index + 1];
            float r = image[index];

            size_t rgb_index = i * 252 * 3 + j * 3;
            rgb_img[rgb_index + 0] = ((b * factor[0][2] + g * factor[1][2] + r * factor[2][2]) / 255.0 + 128.0) / 255.0;
            rgb_img[rgb_index + 1] = ((b * factor[0][1] + g * factor[1][1] + r * factor[2][1]) / 255.0 + 128.0) / 255.0;
            rgb_img[rgb_index + 2] = ((b * factor[0][0] + g * factor[1][0] + r * factor[2][0]) / 255.0 + 16.0) / 255.0;
        }
    }

    // process output and merge to rgb_img
    for (size_t i = 0; i < width * height; i++) {
        float *ptr = reinterpret_cast<float *>(outputBuffer.data);
        ptr[i] = ptr[i] > 1 ? 1 : (ptr[i] < 0 ? 0 : ptr[i]);
        rgb_img[i * 3 + 2] = ptr[i];
    }

    // postprocess
    std::vector<std::vector<float>> new_factor = {
        {0.00456621, 0.00456621, 0.00456621}, {0.00791071, -0.00153632, 0}, {0, -0.00318811, 0.00625893}};
    std::vector<uint8_t> result;
    result.resize(width * height * 4);
    for (size_t i = 0; i < height; i++) {
        for (size_t j = 0; j < width; j++) {
            size_t index = i * 252 * 3 + j * 3;
            float b = rgb_img[index + 2] * 255.0;
            float g = rgb_img[index + 1] * 255.0;
            float r = rgb_img[index] * 255.0;

            size_t result_index = i * 252 * 4 + j * 4;
            float r_r = (b * new_factor[0][2] + g * new_factor[1][2] + r * new_factor[2][2]) * 255.0 - 222.921;
            float r_g = (b * new_factor[0][1] + g * new_factor[1][1] + r * new_factor[2][1]) * 255.0 + 135.576;
            float r_b = (b * new_factor[0][0] + g * new_factor[1][0] + r * new_factor[2][0]) * 255.0 - 276.836;
            result[result_index + 0] = r_r > 255 ? 255 : (r_r < 0 ? 0 : r_r);
            result[result_index + 1] = r_g > 255 ? 255 : (r_g < 0 ? 0 : r_g);
            result[result_index + 2] = r_b > 255 ? 255 : (r_b < 0 ? 0 : r_b);
            result[result_index + 3] = 255;
        }
    }

    error = lodepng::encode("/sdcard/Download/result0.png", result, width, height);
    if (error != 0) {
        HLOGE(TAG, "encode failed %d", error);
        return;
    }
    delete[] rgb_img;
}