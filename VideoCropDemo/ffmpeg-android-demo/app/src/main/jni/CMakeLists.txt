# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("ffmpeg_android_demo")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#IF(APPLE)
#        # tell ranlib to ignore empty compilation units
#        SET(CMAKE_C_ARCHIVE_FINISH   "<CMAKE_RANLIB> -no_warning_for_no_symbols -c <TARGET>")
#        SET(CMAKE_CXX_ARCHIVE_FINISH "<CMAKE_RANLIB> -no_warning_for_no_symbols -c <TARGET>")
#        # prevents ar from invoking ranlib, let CMake do it
#        SET(CMAKE_C_ARCHIVE_CREATE   "<CMAKE_AR> qc -S <TARGET> <LINK_FLAGS> <OBJECTS>")
#        SET(CMAKE_CXX_ARCHIVE_CREATE "<CMAKE_AR> qc -S <TARGET> <LINK_FLAGS> <OBJECTS>")
#
#        add_compile_definitions(FORCE_POSIX)
#ENDIF()
#
#set(can_use_assembler TRUE)
#enable_language(ASM)
#IF("${ANDROID_ABI}" STREQUAL "arm64-v8a")
#        SET(ASM_OPTIONS "-x assembler-with-cpp")
#        SET(CMAKE_ASM_FLAGS "${CFLAGS} ${ASM_OPTIONS} -march=armv8+crypto -D__ANDROID__")
#ELSEIF("${ANDROID_ABI}" STREQUAL "armeabi-v7a")
#        SET(ASM_OPTIONS "-x assembler-with-cpp")
#        SET(CMAKE_ASM_FLAGS "${CFLAGS} ${ASM_OPTIONS} -march=armv7a -D__ANDROID__")
#ELSEIF("${ANDROID_ABI}" STREQUAL "armeabi")
#        SET(ASM_OPTIONS "-x assembler-with-cpp")
#        SET(CMAKE_ASM_FLAGS "${CFLAGS} ${ASM_OPTIONS} -march=armv5 -D__ANDROID__")
#ENDIF()

###############################方式一：#####################################################

set(my_lib_path ${CMAKE_SOURCE_DIR}/../../../libs)

MESSAGE(STATUS "Project Path: ${PROJECT_SOURCE_DIR}\nCurrent Path: ${CMAKE_SOURCE_DIR}")
MESSAGE(STATUS "CMAKE_ANDROID_ARCH_ABI: ${CMAKE_ANDROID_ARCH_ABI}\nINCLUDE_PATH: ${my_lib_path}/include")

set(fftools_path ${my_lib_path}/${ANDROID_ABI}/include/fftools)
include_directories("${my_lib_path}/${ANDROID_ABI}/include"})
include_directories("${fftools_path}"})
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
link_directories("${my_lib_path}/${ANDROID_ABI}")

link_libraries(
        avcodec
        avformat
        swscale
        avutil
        avfilter
        swresample
        avdevice
        )

find_library( # Sets the name of the path variable.
        log-lib
        log )

add_library(
        ffmpeg
        SHARED
        ${fftools_path}/ffmpeg.c
        ${fftools_path}/cmdutils.c
        ${fftools_path}/ffmpeg_filter.c
        ${fftools_path}/ffmpeg_hw.c
#        ${fftools_path}/ffmpeg_mux.c
        ${fftools_path}/ffmpeg_opt.c
#        ${fftools_path}/opt_common.c
        com_jni_FFmpeg.c
)


target_link_libraries( # Specifies the target library.
        ffmpeg
        -Wl,--start-group
        # Links the target library to the log library
        # included in the NDK.
        avcodec
        avformat
        swscale
        avutil
        avfilter
        swresample
        avdevice
        -Wl,--end-group
        ${log-lib} )

###############################方式二：#####################################################


#set(my_lib_path ${CMAKE_SOURCE_DIR}/../../../libs) #定义变量my_lib_path，${CMAKE_SOURCE_DIR}为当前CMakeList.txt所在路径
#
## 打印功能，在.cxx/cmake/debug/arm64-v8a/cmake_build_output.txt里面会有显示。
## ${PROJECT_SOURCE_DIR} = ${CMAKE_SOURCE_DIR}，都为当前CMakeList.txt所在路径;${CMAKE_ANDROID_ARCH_ABI}为CPU架构类型
#MESSAGE(STATUS "Project Path: ${PROJECT_SOURCE_DIR}\nCurrent Path: ${CMAKE_SOURCE_DIR}")
#MESSAGE(STATUS "CMAKE_ANDROID_ARCH_ABI: ${CMAKE_ANDROID_ARCH_ABI}\nINCLUDE_PATH: ${my_lib_path}/include")
#
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
#include_directories("${my_lib_path}/include"}) #包含的头文件路径
#
#add_library(avcodec
#        SHARED
#        IMPORTED)
#set_target_properties(avcodec
#        PROPERTIES IMPORTED_LOCATION
#        ${my_lib_path}/armeabi-v7a/libavcodec.so)
#
#add_library(avformat
#        SHARED
#        IMPORTED)
#set_target_properties(avformat
#        PROPERTIES IMPORTED_LOCATION
#        ${my_lib_path}/armeabi-v7a/libavformat.so)
#
#add_library(swscale
#        SHARED
#        IMPORTED)
#set_target_properties(swscale
#        PROPERTIES IMPORTED_LOCATION
#        ${my_lib_path}/armeabi-v7a/libswscale.so)
#
#add_library(avutil
#        SHARED
#        IMPORTED)
#set_target_properties(avutil
#        PROPERTIES IMPORTED_LOCATION
#        ${my_lib_path}/armeabi-v7a/libavutil.so)
#
#add_library(avfilter
#        SHARED
#        IMPORTED)
#set_target_properties(avfilter
#        PROPERTIES IMPORTED_LOCATION
#        ${my_lib_path}/armeabi-v7a/libavfilter.so)
#
#add_library(swresample
#        SHARED
#        IMPORTED)
#set_target_properties(swresample
#        PROPERTIES IMPORTED_LOCATION
#        ${my_lib_path}/armeabi-v7a/libswresample.so)
#
#add_library(avdevice
#        SHARED
#        IMPORTED)
#set_target_properties(avdevice
#        PROPERTIES IMPORTED_LOCATION
#        ${my_lib_path}/armeabi-v7a/libavdevice.so)
#
#add_library(
#        ffmpeg
#        SHARED
#        com_jni_FFmpeg.c
#)
#
#find_library( # Sets the name of the path variable.
#        log-lib
#        log )
#
#
#target_link_libraries( # Specifies the target library.
#        ffmpeg
#        -landroid #native_window
#        -ljnigraphics #bitmap
#        -lOpenSLES #openSLES
#        # Links the target library to the log library
#        # included in the NDK.
#        avcodec
#        avformat
#        swscale
#        avutil
#        avfilter
#        swresample
#        avdevice
#        ${log-lib} )
