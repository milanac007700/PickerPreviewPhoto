//
// Created by apple on 2024/11/5.
//

#ifndef FFMPEG_ANDROID_DEMO_ANDROID_LOG_H
#define FFMPEG_ANDROID_DEMO_ANDROID_LOG_H

#include <jni.h>
#include <android/log.h>
#define TAG "FFmpeg"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...)   __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#endif //FFMPEG_ANDROID_DEMO_ANDROID_LOG_H
