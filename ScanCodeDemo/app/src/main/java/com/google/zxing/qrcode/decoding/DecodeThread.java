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

package com.google.zxing.qrcode.decoding;

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import android.os.Handler;
import android.os.Looper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;
import com.milanac007.scancode.QRCodeScanActivity;


/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {

  public static final String BARCODE_BITMAP = "barcode_bitmap";

  private final QRCodeScanActivity activity;
  private final Hashtable<DecodeHintType, Object> hints;
  private Handler handler;
  /**
   * CountDownLatch，一个同步辅助类，在完成一组正在其他线程中执行的操作之前，它允许一个或多个线程一直等待。
   * 构造方法参数指定了计数的次数
   * countDown方法，当前线程调用此方法，则计数减一
   * awaint方法，调用此方法会一直阻塞当前线程，直到计时器的值为0
   */
  private final CountDownLatch handlerInitLatch;

  DecodeThread(QRCodeScanActivity activity,
               Vector<BarcodeFormat> decodeFormats,
               String characterSet,
               ResultPointCallback resultPointCallback) {

    this.activity = activity;
    handlerInitLatch = new CountDownLatch(1);

//    hints = new Hashtable<>(3);
//
//    if (decodeFormats == null || decodeFormats.isEmpty()) {
//    	 decodeFormats = new Vector<>();
//    	 decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
//    	 decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
//    	 decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
//    }
//
//    hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
//
//    if (characterSet != null) {
//      hints.put(DecodeHintType.CHARACTER_SET, characterSet);
//    }
//
//    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);

    hints = new Hashtable<>(4);
    hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
    // TRY_HARDER(Void.class)
    // 是否使用HARDER模式来解析数据，如果启用，则会花费更多的时间去解析二维码，对精度有优化，对速度则没有。
    hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    hints.put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.QR_CODE);
    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);

  }

  Handler getHandler() {
    try {
      handlerInitLatch.await();
    } catch (InterruptedException ie) {
      // continue?
    }
    return handler;
  }

  @Override
  public void run() {
    Looper.prepare();//非主线程中默认没有创建Looper对象，需要先调用Looper.prepare()启用Looper。
    handler = new DecodeHandler(activity, hints);
    handlerInitLatch.countDown();
    Looper.loop();//写在Looper.loop()之后的代码不会被执行，这个函数内部应该是一个循环，当调用mHandler.getLooper().quit()后，loop才会中止，其后的代码才能得以运行。
  }

}
