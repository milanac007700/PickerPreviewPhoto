//
// Created by apple on 2022/4/18.
//

#include <android/log.h>
#include "com_jni_FFmpeg.h"

#include <stdlib.h>
#include <stdbool.h>
#include <stdio.h>
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavfilter/avfilter.h"
#include "fftools/ffmpeg.h"

//#include "../../../../../../../../../../Library/Android/sdk/ndk/20.1.5948944/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/jni.h"


JNIEXPORT jstring JNICALL Java_com_jni_FFmpeg_run(JNIEnv *env, jclass obj){
    const char *conf = avcodec_configuration();
    __android_log_print(ANDROID_LOG_INFO, "myTag", "avcodec_configuration: %s", conf);
    return (*env)->NewStringUTF(env, conf); // C
//    env->NewStringUTF(conf); //C++
}

JNIEXPORT jint JNICALL Java_com_jni_FFmpeg_runCmd(JNIEnv *env, jclass obj, jobjectArray cmds){
    int len = (*env)->GetArrayLength(env, cmds);
    const char *argv[len];

    int i;
    for(i = 0; i<len; i++) {
       jstring cmd =(jstring)(*env)->GetObjectArrayElement(env, cmds, i);
        argv[i] = (*env)->GetStringUTFChars(env, cmd, 0);
    }
    return runCmd(len, argv);
}
