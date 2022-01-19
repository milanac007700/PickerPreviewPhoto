/*
 * Copyright 2009 ZXing authors
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

import com.google.zxing.LuminanceSource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * This object extends LuminanceSource around an array of YUV data returned from the camera driver,
 * with the option to crop to a rectangle within the full data. This can be used to exclude
 * superfluous pixels around the perimeter and speed up decoding.
 *
 * It works for any pixel format where the Y channel is planar and appears first, including
 * YCbCr_420_SP and YCbCr_422_SP.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class PlanarYUVLuminanceSource extends LuminanceSource {
  private final byte[] yuvData;
  private final int dataWidth;
  private final int dataHeight;
  private final int left;
  private final int top;

  /**
   * 再来看看YUV亮度源是怎么构造的，在CameraManager里，首先获取预览图像的聚焦框矩形getFramingRect()，这个聚焦框的矩形大小是根据屏幕的宽高值来做计算的，
   * 官方定义了最小和最大的聚焦框大小，分别是240*240和1200*675，即最多的聚焦框大小为屏幕宽高的5/8。
   * 获取屏幕的聚焦框大小后，还需要做从屏幕分辨率到相机分辨率的转换才能得到预览聚焦框的大小，
   * 这个转换在getFramingRectInPreview()里完成。这样便完成了亮度源的构造。
   */
  public PlanarYUVLuminanceSource(byte[] yuvData, int dataWidth, int dataHeight, int left, int top,
      int width, int height) {
    super(width, height);

    if (left + width > dataWidth || top + height > dataHeight) {
      throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
    }

    this.yuvData = yuvData;
    this.dataWidth = dataWidth;
    this.dataHeight = dataHeight;
    this.left = left;
    this.top = top;
  }

  @Override
  public byte[] getRow(int y, byte[] row) {
    if (y < 0 || y >= getHeight()) {
      throw new IllegalArgumentException("Requested row is outside the image: " + y);
    }
    int width = getWidth();
    if (row == null || row.length < width) {
      row = new byte[width];
    }
    int offset = (y + top) * dataWidth + left;
    System.arraycopy(yuvData, offset, row, 0, width);
    return row;
  }

  @Override
  public byte[] getMatrix() {
    int width = getWidth();
    int height = getHeight();

    // If the caller asks for the entire underlying image, save the copy and give them the
    // original data. The docs specifically warn that result.length must be ignored.
    if (width == dataWidth && height == dataHeight) {
      return yuvData;
    }

    int area = width * height;
    byte[] matrix = new byte[area];
    int inputOffset = top * dataWidth + left;

    // If the width matches the full width of the underlying data, perform a single copy.
    if (width == dataWidth) {
      System.arraycopy(yuvData, inputOffset, matrix, 0, area);
      return matrix;
    }

    // Otherwise copy one cropped row at a time.
    byte[] yuv = yuvData;
    for (int y = 0; y < height; y++) {
      int outputOffset = y * width;
      System.arraycopy(yuv, inputOffset, matrix, outputOffset, width);
      inputOffset += dataWidth;
    }
    return matrix;
  }

  @Override
  public boolean isCropSupported() {
    return true;
  }

  public int getDataWidth() {
    return dataWidth;
  }

  public int getDataHeight() {
    return dataHeight;
  }

  public Bitmap renderCroppedGreyscaleBitmap() {
    int width = getWidth();
    int height = getHeight();
    int[] pixels = new int[width * height];
    byte[] yuv = yuvData;
    int inputOffset = top * dataWidth + left;

    for (int y = 0; y < height; y++) {
      int outputOffset = y * width;
      for (int x = 0; x < width; x++) {
        int grey = yuv[inputOffset + x] & 0xff;
        pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
      }
      inputOffset += dataWidth;
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

//    Matrix matrix = new Matrix();
//    matrix.reset();
//    matrix.setRotate(90);
//    Bitmap tmpBitmap = Bitmap.createBitmap(bitmap,0,0, bitmap.getWidth(), bitmap.getHeight(),matrix, true);
    return bitmap;
  }
}
