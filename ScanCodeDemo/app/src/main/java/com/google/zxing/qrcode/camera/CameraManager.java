/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.zxing.qrcode.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.milanac007.scancode.StatusBarUtil;

import java.io.IOException;
import java.util.List;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

  private static final String TAG = CameraManager.class.getSimpleName();

  public static int MIN_FRAME_WIDTH = 240;
  public static int MIN_FRAME_HEIGHT = 240;  
  public static int MAX_FRAME_WIDTH = 320;     
  public static int MAX_FRAME_HEIGHT = 320;  

  private static CameraManager cameraManager;

  static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
  static {
    int sdkInt;
    try {
      sdkInt = Integer.parseInt(Build.VERSION.SDK);
    } catch (NumberFormatException nfe) {
      // Just to be safe
      sdkInt = 10000;
    }
    SDK_INT = sdkInt;
  }

  private final Context context;
  private final CameraConfigurationManager configManager;
  private int screenWidth;
  private int screenHeight;
  private int previewWidth;
  private int previewHeight;
  private Camera camera;
  private Rect framingRect;
  private Rect framingRectInPreview;
  private boolean initialized;
  private boolean previewing;
  private final boolean useOneShotPreviewCallback;
  /**
   * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
   * clear the handler so it will only receive one message.
   */
  private final PreviewCallback previewCallback;
  /** Autofocus callbacks arrive here, and are dispatched to the Handler which requested them. */
  private final AutoFocusCallback autoFocusCallback;
  private boolean mIsHorizonal;

  /**
   * Initializes this static object with the Context of the calling Activity.
   *
   * @param context The Activity which wants to use the camera.
   */
  public static void init(Context context) {
    if (cameraManager == null) {
      cameraManager = new CameraManager(context);
    }
  }

  /**
   * Gets the CameraManager singleton instance.
   *
   * @return A reference to the CameraManager singleton.
   */
  public synchronized static CameraManager get() {
    return cameraManager;
  }

  private CameraManager(Context context) {

    this.context = context;
    this.configManager = new CameraConfigurationManager(context);

    // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
    // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
    // the more efficient one shot callback, as the older one can swamp the system and cause it
    // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
    //useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > Build.VERSION_CODES.CUPCAKE;
    useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; // 3 = Cupcake

    previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
    autoFocusCallback = new AutoFocusCallback();


    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    int rotation = windowManager.getDefaultDisplay().getRotation();
    mIsHorizonal = false;
    if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
      mIsHorizonal = true;
    }
    if (!mIsHorizonal) {
      screenWidth = getScreenWidth();
      screenHeight = getScreenHeight() + StatusBarUtil.getStatusBarHeight(context);
    } else {
      screenWidth = getScreenWidth() + StatusBarUtil.getStatusBarHeight(context);;
      screenHeight = getScreenHeight();
    }
  }

  public int getScreenWidth() { // 每次都重新获取， 适用于screenOrientation="sensor"，且发生了屏幕旋转的情形
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    return dm.widthPixels;
  }

  public int getScreenHeight() {
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    return dm.heightPixels;
  }

  private Size getBestPreviewSize(Camera.Parameters parameters) {
    int viewWidth = mIsHorizonal ? screenWidth: screenHeight;
    int viewHeight = mIsHorizonal ? screenHeight: screenWidth;
    List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
    int bestPreviewWidth = 1920;
    int bestPreviewHeight = 1080;
    int diffs = Integer.MAX_VALUE;
    for (Camera.Size previewSize: supportedPreviewSizes) {
      int newDiffs = Math.abs(previewSize.width - viewWidth) + Math.abs(previewSize.height - viewHeight);
      if (newDiffs == 0) {
        bestPreviewWidth = previewSize.width;
        bestPreviewHeight = previewSize.height;
        break;
      }
      if (diffs > newDiffs) {
        bestPreviewWidth = previewSize.width;
        bestPreviewHeight = previewSize.height;
        diffs = newDiffs;
      }
    }
    Log.i(TAG, "最佳预览宽高：" + bestPreviewWidth + ", " + bestPreviewHeight);
    return new Size(bestPreviewWidth, bestPreviewHeight);
  }

  public void setPreviewSize(int previewWidth, int previewHeight) {
    this.previewWidth = previewWidth;
    this.previewHeight = previewHeight;
  }

  public FrameLayout.LayoutParams getLayoutParams(int previewWidth, int previewHeight) {
    float scale = previewWidth * 1.0f / previewHeight;
    // 以屏幕高为布局高，计算布局宽: 竖屏手机的高度对应相机预览尺寸的宽度
    int layout_height = screenHeight;
    int layout_width = (int)(layout_height * 1.0f /  scale);
    if (mIsHorizonal) {
      layout_width = screenWidth;
      layout_height = (int)(layout_width * 1.0f / scale);
    }
    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(layout_width, layout_height);
    layoutParams.gravity = Gravity.CENTER;//相机水平居中
    return layoutParams;
  }

  public void setCameraDisplayOrientation(Context context, int cameraId, Camera camera) {
    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, cameraInfo);
    WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
    int orientation = windowManager.getDefaultDisplay().getOrientation();
    int degree = 0;
    switch (orientation) {
      case Surface.ROTATION_0: degree = 0;break;
      case Surface.ROTATION_90: degree = 90;break;
      case Surface.ROTATION_180: degree = 180;break;
      case Surface.ROTATION_270: degree = 270;break;
    }

    int result;
    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (cameraInfo.orientation + degree) % 360;
      result = (360 - result) % 360; // compensate the mirror
    } else {
      result = (cameraInfo.orientation - degree + 360) % 360;
    }
    camera.setDisplayOrientation(result);
  }

  /**
   * Opens the camera driver and initializes the hardware parameters.
   *
   * @param holder The surface object which the camera will draw preview frames into.
   * @throws IOException Indicates the camera driver failed to open.
   */
  public void openDriver(SurfaceHolder holder, TextureView textureView) throws IOException {
    if (camera == null) {
      camera = Camera.open();
      if (camera == null) {
        throw new IOException();
      }

//      Camera.Parameters parameters = camera.getParameters();
//      Size bestPreviewSize = getBestPreviewSize(parameters);
//      parameters.setPreviewSize(bestPreviewSize.getWidth(), bestPreviewSize.getHeight());
//      setPreviewSize(bestPreviewSize.getWidth(), bestPreviewSize.getHeight());
//      // 这里直接设置为预览尺寸，因为发现有些手机的拍照尺寸大于预览尺寸，导致输出的图片和预览拍照那一刻不一致。
//      parameters.setPictureSize(bestPreviewSize.getWidth(), bestPreviewSize.getHeight());
//      setCameraDisplayOrientation(context, 0, camera);
//      camera.setParameters(parameters);

      if (holder != null) {
        camera.setPreviewDisplay(holder);
      }

      if (textureView != null) {
//        textureView.setLayoutParams(getLayoutParams());
//        textureView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
//        int layout_width = textureView.getLayoutParams().width;
//        int layout_height = textureView.getLayoutParams().height;
//        if (layout_width >  screenWidth || layout_height > screenHeight) { // matrix变换，使画面拉近但不拉伸
//          Matrix matrix = new Matrix();
//          RectF viewRect = new RectF(0, 0, screenWidth, screenHeight);
//          RectF bufferRect = new RectF(0, 0, layout_width, layout_height);
//          float centerX = viewRect.centerX();
//          float centerY = viewRect.centerY();
//          bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY()); //将预览图像外框bufferRect的中心点移动到手机屏幕的物理中心点centerX, centerY
//          matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);//矩阵变换,从viewRect变换到bufferRect，表现为画面拉近，但不拉伸变形
//          float scale = Math.max( //找到水平和竖直方向最大的缩放倍数
//                  (float) screenWidth / layout_width,
//                  (float) screenHeight / layout_height
//          );
//          matrix.postScale(scale, scale, centerX, centerY); // 以(centerX, centerY)为中心，x、y方向各缩放scale倍
//          textureView.setTransform(matrix);
//        }
        camera.setPreviewTexture(textureView.getSurfaceTexture());
      }
      //TODO
//      if (!initialized) {
//        initialized = true;
//        configManager.initFromCameraParameters(camera);
//      }
//      configManager.setDesiredCameraParameters(camera);


      if (!initialized) {
        initialized = true;
        configManager.initFromCameraParameters2(camera);
      }
      configManager.setDesiredCameraParameters2(camera);

      Point screenResolution = configManager.getScreenResolution();
      screenWidth = screenResolution.x;
      screenHeight = screenResolution.y;
      Point cameraResolution = configManager.getCameraResolution();

      if (textureView != null) {
        textureView.setLayoutParams(getLayoutParams(cameraResolution.x, cameraResolution.y));
        int layout_width = textureView.getLayoutParams().width;
        int layout_height = textureView.getLayoutParams().height;
        if (layout_width >  screenWidth || layout_height > screenHeight) { // matrix变换，使画面拉近但不拉伸
          Matrix matrix = new Matrix();
          RectF viewRect = new RectF(0, 0, screenWidth, screenHeight);
          RectF bufferRect = new RectF(0, 0, layout_width, layout_height);
          float centerX = viewRect.centerX();
          float centerY = viewRect.centerY();
          bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY()); //将预览图像外框bufferRect的中心点移动到手机屏幕的物理中心点centerX, centerY
          matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);//矩阵变换,从viewRect变换到bufferRect，表现为画面拉近，但不拉伸变形
          float scale = Math.max( //找到水平和竖直方向最大的缩放倍数
                  (float) screenWidth / layout_width,
                  (float) screenHeight / layout_height
          );
          matrix.postScale(scale, scale, centerX, centerY); // 以(centerX, centerY)为中心，x、y方向各缩放scale倍
          textureView.setTransform(matrix);
        }
//        camera.setPreviewTexture(textureView.getSurfaceTexture());
      }

      FlashlightManager.enableFlashlight();
    }
  }

  /**
   * Closes the camera driver if still in use.
   */
  public void closeDriver() {
    if (camera != null) {
      FlashlightManager.disableFlashlight();
      camera.release();
      camera = null;
    }
  }

  /**
   * 打开或关闭闪光灯
   *
   * @param open 控制是否打开
   * @return 打开或关闭失败，则返回false。
   */
  public boolean setFlashLight(boolean open) {
    if (camera == null) {
      return false;
    }
    Camera.Parameters parameters = camera.getParameters();
    if (parameters == null) {
      return false;
    }
    List<String> flashModes = parameters.getSupportedFlashModes();
    // Check if camera flash exists
    if (null == flashModes || 0 == flashModes.size()) {
      // Use the screen as a flashlight (next best thing)
      return false;
    }
    String flashMode = parameters.getFlashMode();
    if (open) {
      if (Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
        return true;
      }
      // Turn on the flash
      if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(parameters);
        return true;
      } else {
        return false;
      }
    } else {
      if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
        return true;
      }
      // Turn on the flash
      if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(parameters);
        return true;
      } else
        return false;
    }
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   */
  public void startPreview() {
    if (camera != null && !previewing) {
      camera.startPreview();
      previewing = true;
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   */
  public void stopPreview() {
    if (camera != null && previewing) {
      if (!useOneShotPreviewCallback) {
        camera.setPreviewCallback(null);
      }
      camera.stopPreview();
      previewCallback.setHandler(null, 0);
      autoFocusCallback.setHandler(null, 0);
      previewing = false;
    }
  }

  /**
   * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
   * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
   * respectively.
   *
   * @param handler The handler to send the message to.
   * @param message The what field of the message to be sent.
   */
  public void requestPreviewFrame(Handler handler, int message) {
    if (camera != null && previewing) {
      previewCallback.setHandler(handler, message);
      if (useOneShotPreviewCallback) {
        camera.setOneShotPreviewCallback(previewCallback);
      } else {
        camera.setPreviewCallback(previewCallback);
      }
    }
  }

  /**
   * Asks the camera hardware to perform an autofocus.
   *
   * @param handler The Handler to notify when the autofocus completes.
   * @param message The message to deliver.
   */
  public void requestAutoFocus(Handler handler, int message) {
    if (camera != null && previewing) {
      autoFocusCallback.setHandler(handler, message);
      //Log.d(TAG, "Requesting auto-focus callback");
      camera.autoFocus(autoFocusCallback);
    }
  }

  /**
   * Calculates the framing rect which the UI should draw to show the user where to place the
   * barcode. This target helps with alignment as well as forces the user to hold the device
   * far enough away to ensure the image will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public Rect getFramingRect() {
    Point screenResolution = configManager.getScreenResolution();
    // TODO
    if (screenResolution == null) {
      screenResolution = new Point(screenWidth, screenHeight);
    }


    if (framingRect == null) {
      if (camera == null) {
        return null;
      }
//      int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);

//      int height = fisndDesiredDimensionInRange(screenResolution.y,MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);
      
      int width = screenResolution.x * 3 / 4;
      if (width < MIN_FRAME_WIDTH) {
        width = MIN_FRAME_WIDTH;
      } else if (width > MAX_FRAME_WIDTH) {
        width = MAX_FRAME_WIDTH;
      }
      int height = screenResolution.y * 3 / 4;
      if (height < MIN_FRAME_HEIGHT) {
        height = MIN_FRAME_HEIGHT;
      } else if (height > MAX_FRAME_HEIGHT) {
        height = MAX_FRAME_HEIGHT;
      }
      int leftOffset = (screenResolution.x - width) / 2;
      int topOffset = (screenResolution.y - height) / 2;
      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }
    return framingRect;
  }

  /**
   * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
   * not UI / screen.
   */
  public Rect getFramingRectInPreview() {
    if (framingRectInPreview == null) {
      Rect rect = new Rect(getFramingRect());
      Point cameraResolution = configManager.getCameraResolution();
      Point screenResolution = configManager.getScreenResolution();
      rect.left = rect.left * cameraResolution.y / screenResolution.x;  
      rect.right = rect.right * cameraResolution.y / screenResolution.x;  
      rect.top = rect.top * cameraResolution.x / screenResolution.y;  
      rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y; 
      
      Log.e("tag","getFRIP "+rect.left +"  "+rect.right+" "+rect.top+ "  "+rect.bottom);
//      rect.left = rect.left * cameraResolution.x / screenResolution.x;
//      rect.right = rect.right * cameraResolution.x / screenResolution.x;
//      rect.top = rect.top * cameraResolution.y / screenResolution.y;
//      rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
      framingRectInPreview = rect;
    }
    return framingRectInPreview;
  }

  /**
   * Converts the result points from still resolution coordinates to screen coordinates.
   *
   * @param points The points returned by the Reader subclass through Result.getResultPoints().
   * @return An array of Points scaled to the size of the framing rect and offset appropriately
   *         so they can be drawn in screen coordinates.
   */
  /*
  public Point[] convertResultPoints(ResultPoint[] points) {
    Rect frame = getFramingRectInPreview();
    int count = points.length;
    Point[] output = new Point[count];
    for (int x = 0; x < count; x++) {
      output[x] = new Point();
      output[x].x = frame.left + (int) (points[x].getX() + 0.5f);
      output[x].y = frame.top + (int) (points[x].getY() + 0.5f);
    }
    return output;
  }
   */

  /**
   * A factory method to build the appropriate LuminanceSource object based on the format
   * of the preview buffers, as described by Camera.Parameters.
   *
   * @param data A preview frame.
   * @param width The width of the image.
   * @param height The height of the image.
   * @return A PlanarYUVLuminanceSource instance.
   */
  public PlanarYUVLuminanceSource buildLuminanceSource_bak(byte[] data, int width, int height) {
    Rect rect = getFramingRectInPreview();
    int previewFormat = configManager.getPreviewFormat();
    String previewFormatString = configManager.getPreviewFormatString();
    switch (previewFormat) {
      // This is the standard Android format which all devices are REQUIRED to support.
      // In theory, it's the only one we should ever care about.
      case PixelFormat.YCbCr_420_SP:
      // This format has never been seen in the wild, but is compatible as we only care
      // about the Y channel, so allow it.
      case PixelFormat.YCbCr_422_SP:
//    	  Log.e("tag", width +" "+ height +" "+  rect.left +" "+ rect.top +" "+ rect.width() +" "+ rect.height());
        /**
         * 为什么在扫描的时候这么难扫到二维码呢，原因在于官方为了减少解码的数据，提高解码效率和速度，采用了裁剪无用区域的方式。这样会带来一定的问题，
         * 整个二维码数据需要完全放到聚焦框里才有可能被识别，并且在buildLuminanceSource(byte[],int,int)这个方法签名中，传入的byte数组便是图像的数据，
         * 并没有因为裁剪而使数据量减小，而是采用了取这个数组中的部分数据来达到裁剪的目的。对于目前CPU性能过剩的大多数智能手机来说，这种裁剪显得没有
         * 必要。如果把解码数据换成采用全幅图像数据，这样在识别的过程中便不再拘束于聚焦框，也使得二维码数据可以铺满整个屏幕。
         * 这样用户在使用程序来扫描二维码时，尽管不完全对准聚焦框，也可以识别出来。这属于一种策略上的让步，给用户造成了错觉，但提高了识别的精度。
         解决办法很简单，就是不仅仅使用聚焦框里的图像数据，而是采用全幅图像的数据。
         */
//        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height());
        return new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, width);
      default:
        // The Samsung Moment incorrectly uses this variant instead of the 'sp' version.
        // Fortunately, it too has all the Y data up front, so we can read it.
        if ("yuv420p".equals(previewFormatString)) {
//          return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height());
          return new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, width);
        }
    }
    throw new IllegalArgumentException("Unsupported picture format: " +
        previewFormat + '/' + previewFormatString);
  }

  /**
   * A factory method to build the appropriate LuminanceSource object based on
   * the format of the preview buffers, as described by Camera.Parameters.
   *
   * @param data A preview frame.
   * @param width The width of the image.
   * @param height The height of the image.
   * @return A PlanarYUVLuminanceSource instance.
   */
  public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
    Rect rect = getFramingRectInPreview();
    if (rect == null) {
      return null;
    }
    // Go ahead and assume it's YUV rather than die.
    return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(),
            rect.height());
  }

}
