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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import android.app.Activity;

/**
 * Finishes an activity after a period of inactivity.
 */
public final class InactivityTimer {

  private static final int INACTIVITY_DELAY_SECONDS = 5 * 60;

  private final ScheduledExecutorService inactivityTimer =
      Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
  private final Activity activity;
  private ScheduledFuture<?> inactivityFuture = null;

  public InactivityTimer(Activity activity) {
    this.activity = activity;
    onActivity();
  }

  public void onActivity() {
    cancel();
    inactivityFuture = inactivityTimer.schedule(new FinishListener(activity),
                                                INACTIVITY_DELAY_SECONDS,
                                                TimeUnit.SECONDS);
  }

  private void cancel() {
    if (inactivityFuture != null) {
      inactivityFuture.cancel(true);
      inactivityFuture = null;
    }
  }

  public void shutdown() {
    cancel();
    inactivityTimer.shutdown();
  }

  /*

* 守护线程在没有用户线程可服务时自动离开

* 在Java中比较特殊的线程是被称为守护（Daemon）线程的低级别线程。

* 这个线程具有最低的优先级，用于为系统中的其它对象和线程提供服务。

* 将一个用户线程设置为守护线程的方式是在线程对象创建之前调用线程对象的setDaemon方法。

* 典型的守护线程例子是JVM中的系统资源自动回收线程，

* 我们所熟悉的Java垃圾回收线程就是一个典型的守护线程，

* 当我们的程序中不再有任何运行中的Thread，

* 程序就不会再产生垃圾，垃圾回收器也就无事可做，

* 所以当垃圾回收线程是Java虚拟机上仅剩的线程时，Java虚拟机会自动离开。

* 它始终在低级别的状态中运行，用于实时监控和管理系统中的可回收资源。

* 守护进程（Daemon）是运行在后台的一种特殊进程。它独立于控制终端并且周期性地执行某种任务或等待处理某些发生的事件。

* 也就是说守护线程不依赖于终端，但是依赖于系统，与系统“同生共死”。

* 那Java的守护线程是什么样子的呢。

* 当JVM中所有的线程都是守护线程的时候，JVM就可以退出了；

* 如果还有一个或以上的非守护线程则JVM不会退出。

*/
  private static final class DaemonThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);
      return thread;
    }
  }

}
