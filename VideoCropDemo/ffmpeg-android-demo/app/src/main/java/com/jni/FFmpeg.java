package com.jni;


public class FFmpeg {
//    static {
//        System.loadLibrary("avcodec-59");
//        System.loadLibrary("avformat-59");
//        System.loadLibrary("swscale-6");
//        System.loadLibrary("avutil-57");
//        System.loadLibrary("avfilter-8");
//        System.loadLibrary("avformat-59");
//        System.loadLibrary("swresample-4");
//        System.loadLibrary("avdevice-59");
//        System.loadLibrary("ffmpeg");
//    }

    static {
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("avutil");
        System.loadLibrary("avfilter");
        System.loadLibrary("avformat");
        System.loadLibrary("swresample");
        System.loadLibrary("avdevice");
        System.loadLibrary("ffmpeg");
    }

    public static native String run();
    public static native int runCmd(String[] cmd);
//    public static native int runCmd(int cmdSum, String[] cmd);

//    private static OnCmdExecListener sOnCmdExecListener;
//
//    public static void exec(String[] cmds, OnCmdExecListener listener)
//    {
//        sOnCmdExecListener = listener;
//        runCmd(cmds.length, cmds);
//    }
//
//    public static void onExecuted(int ret)
//    {
//        if (sOnCmdExecListener != null)
//        {
//            if (ret == 0)
//            {
//                sOnCmdExecListener.onSuccess();
//            }
//            else
//            {
//                sOnCmdExecListener.onFailure();
//            }
//        }
//    }
//
//    public static void onProgress(float progress)
//    {
//
//    }
//
//
//    public interface OnCmdExecListener
//    {
//        void onSuccess();
//
//        void onFailure();
//
//        void onProgress(float progress);
//    }
}
