/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.qrcode.decoding;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;


import com.google.zxing.qrcode.camera.CameraManager;
import com.google.zxing.qrcode.camera.PlanarYUVLuminanceSource;
import com.milanac007.scancode.QRCodeScanActivity;
import com.milanac007.scancode.R;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final QRCodeScanActivity activity;

    private Hashtable<DecodeHintType, Object> mHints = null;
    private QRCodeReader mQrCodeReader;

  private  MultiFormatReader multiFormatReader;

  DecodeHandler(QRCodeScanActivity activity, Hashtable<DecodeHintType, Object> hints) {
//    multiFormatReader = new MultiFormatReader();
//    multiFormatReader.setHints(hints);
/**
* 如果项目仅仅用来解析二维码，完全没必要支持所有的格式，也没有必要使用MultiFormatReader来解析。
 * 所以在配置的过程中，我移除了所有与二维码不相关的代码。直接使用QRCodeReader类来解析，字符集采用utf-8，
 * 使用Harder模式，并且把可能的解析格式只定义为BarcodeFormat.QR_CODE，这对于直接二维码扫描解析无疑是帮助最大的。
*/
    mQrCodeReader = new QRCodeReader();
    mHints = hints;
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
      if(message.what == R.id.decode){
          //Log.d(TAG, "Got decode message");
          decode((byte[]) message.obj, message.arg1, message.arg2);
      }else if(message.what == R.id.quit){
          Looper.myLooper().quit();
      }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {

    long start = System.currentTimeMillis();
    byte[] rotatedData = new byte[data.length];  
    for (int y = 0; y < height; y++) {  
        for (int x = 0; x < width; x++)  
        rotatedData[x * height + height - y - 1] = data[x + y * width];  
    }  
    int tmp = width; // Here we are swapping, that's the difference to #11  
    width = height;  
    height = tmp;  
    data = rotatedData;  
    
    Result rawResult = null;
      // 构造基于平面的YUV亮度源，即包含二维码区域的数据源
    PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(data, width, height);

    try {
        // 构造二值图像比特流，使用HybridBinarizer算法解析数据源
//    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        // 采用MultiFormatReader解析图像，可以解析多种数据格式
//      rawResult = multiFormatReader.decodeWithState(bitmap);

    /**
     * HybridBinarizer算法使用了更高级的算法，但使用GlobalHistogramBinarizer识别效率确实比HybridBinarizer要高一些。
     *
     * GlobalHistogram算法：（http://kuangjianwei.blog.163.com/blog/static/190088953201361015055110/）
     *
     * 二值化的关键就是定义出黑白的界限，我们的图像已经转化为了灰度图像，每个点都是由一个灰度值来表示，就需要定义出一个灰度值，大于这个值就为白（0），低于这个值就为黑（1）。
     * 在GlobalHistogramBinarizer中，是从图像中均匀取5行（覆盖整个图像高度），每行取中间五分之四作为样本；以灰度值为X轴，每个灰度值的像素个数为Y轴建立一个直方图，
     * 从直方图中取点数最多的一个灰度值，然后再去给其他的灰度值进行分数计算，按照点数乘以与最多点数灰度值的距离的平方来进行打分，选分数最高的一个灰度值。接下来在这两个灰度值中间选取一个区分界限，
     * 取的原则是尽量靠近中间并且要点数越少越好。界限有了以后就容易了，与整幅图像的每个点进行比较，如果灰度值比界限小的就是黑，在新的矩阵中将该点置1，其余的就是白，为0。
     */
        BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
        rawResult = mQrCodeReader.decode(bitmap, mHints);

    } catch (ReaderException re) {
    } finally {
//      multiFormatReader.reset();
        mQrCodeReader.reset();
    }

    if (rawResult != null) {
      long end = System.currentTimeMillis();
      Log.e(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());
      Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, rawResult);
      Bundle bundle = new Bundle();
      bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
      message.setData(bundle);
      message.sendToTarget();
    } else {
      Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
      message.sendToTarget();
    }
  }
  
  private Bitmap suoxiaoBitmap(Bitmap bm){
	  // 获得图片的宽高
	    int width = bm.getWidth();
	    int height = bm.getHeight();
	    // 设置想要的大小
	    int newWidth = 320;
	    int newHeight = 320;
	    // 计算缩放比例
	    float scaleWidth = ((float) newWidth) / width;
	    float scaleHeight =scaleWidth;// ((float) newHeight) / height;
	    // 取得想要缩放的matrix参数
	    Matrix matrix = new Matrix();
	    matrix.postScale(scaleWidth, scaleHeight);
	    // 得到新的图片
	    Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
	    return newbm;
  }
	public static void saveBitmap_JPEG(String bitName,Bitmap bitmap,int quality)throws Exception {
		File f = new File(bitName);
		if (!f.exists())
			f.createNewFile();
		CompressFormat format= CompressFormat.JPEG;
		OutputStream stream = null;
		try {
			stream = new FileOutputStream(bitName);
			bitmap.compress(format, quality, stream);
			stream.flush();
			stream.close();			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	 
	}
}
