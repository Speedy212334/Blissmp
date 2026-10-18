package dev.xoperr.blissgems.libs.faststats.core;

import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingThreadPoolExecutor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Range;

final class SimpleTrackingThreadPoolExecutor implements TrackingThreadPoolExecutor {
   private final ErrorTracker tracker;

   public SimpleTrackingThreadPoolExecutor(ErrorTracker tracker) {
      this.tracker = tracker;
   }

   @Override
   public ThreadPoolExecutor create(
      @Range(from = 0L, to = 2147483647L) int corePoolSize,
      @Range(from = 0L, to = 2147483647L) int maximumPoolSize,
      @Range(from = 0L, to = 2147483647L) long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue
   ) {
      return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, this.tracker.threadFactory()) {
         @Override
         protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            SimpleTrackingThreadPoolExecutor.this.tracker.trackError(t);
         }
      };
   }

   @Override
   public ThreadPoolExecutor create(
      @Range(from = 0L, to = 2147483647L) int corePoolSize,
      @Range(from = 0L, to = 2147483647L) int maximumPoolSize,
      @Range(from = 0L, to = 2147483647L) long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      ThreadFactory threadFactory
   ) {
      return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory) {
         @Override
         protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            SimpleTrackingThreadPoolExecutor.this.tracker.trackError(t);
         }
      };
   }

   @Override
   public ThreadPoolExecutor create(
      @Range(from = 0L, to = 2147483647L) int corePoolSize,
      @Range(from = 0L, to = 2147483647L) int maximumPoolSize,
      @Range(from = 0L, to = 2147483647L) long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      RejectedExecutionHandler handler
   ) {
      return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler) {
         @Override
         protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            SimpleTrackingThreadPoolExecutor.this.tracker.trackError(t);
         }
      };
   }

   @Override
   public ThreadPoolExecutor create(
      @Range(from = 0L, to = 2147483647L) int corePoolSize,
      @Range(from = 0L, to = 2147483647L) int maximumPoolSize,
      @Range(from = 0L, to = 2147483647L) long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      ThreadFactory threadFactory,
      RejectedExecutionHandler handler
   ) {
      return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler) {
         @Override
         protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            SimpleTrackingThreadPoolExecutor.this.tracker.trackError(t);
         }
      };
   }
}
