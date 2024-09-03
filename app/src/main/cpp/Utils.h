/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

#pragma once
#include<string>
#include<vector>
#include"Faith.h"

int PreProcessImg(const std::string &imgPath, std::vector<faith::Buffer> &inputBuffers);
void PostProcess(const std::string &imgPath, const faith::Buffer &outputBuffer);