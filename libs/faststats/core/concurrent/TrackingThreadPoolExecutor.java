package dev.xoperr.blissgems.libs.faststats.core.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;

@ApiStatus.NonExtendable
public interface TrackingThreadPoolExecutor {
   ThreadPoolExecutor create(
      @Range(from = 0L, to = 2147483647L) int var1,
      @Range(from = 0L, to = 2147483647L) int var2,
      @Range(from = 0L, to = 2147483647L) long var3,
      TimeUnit var5,
      BlockingQueue<Runnable> var6
   );

   ThreadPoolExecutor create(
      @Range(from = 0L, to = 2147483647L) int var1,
      @Range(from = 0L, to = 2147483647L) int var2,
      @Range(from = 0L, to = 2147483647L) long var3,
      TimeUnit var5,
      BlockingQueue<Runnable> var6,
      ThreadFactory var7
   );

   ThreadPoolExecutor create(
      @Range(from = 0L, to = 2147483647L) int var1,
      @Range(from = 0L, to = 2147483647L) int var2,
      @Range(from = 0L, to = 2147483647L) long var3,
      TimeUnit var5,
      BlockingQueue<Runnable> var6,
      RejectedExecutionHandler var7
   );

   ThreadPoolExecutor create(
      @Range(from = 0L, to = 2147483647L) int var1,
      @Range(from = 0L, to = 2147483647L) int var2,
      @Range(from = 0L, to = 2147483647L) long var3,
      TimeUnit var5,
      BlockingQueue<Runnable> var6,
      ThreadFactory var7,
      RejectedExecutionHandler var8
   );
}
