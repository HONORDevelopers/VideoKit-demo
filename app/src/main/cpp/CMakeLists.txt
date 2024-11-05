
cmake_minimum_required(VERSION 3.18.1)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -std=gnu++11")

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

project("npu")

file(GLOB securec_file "external/*.cpp")

add_library(
        npu
        SHARED
        NpuTextureJni.cpp
        Utils.cpp
        ${securec_file}
        )

find_library(
        log-lib
        log )

target_link_libraries(
        npu
        ${log-lib}
        android
        GLESv3
        EGL
        )
