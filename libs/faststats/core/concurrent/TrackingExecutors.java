package dev.xoperr.blissgems.libs.faststats.core.concurrent;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface TrackingExecutors {
   ExecutorService newFixedThreadPool(int var1);

   ExecutorService newWorkStealingPool(int var1);

   ExecutorService newWorkStealingPool();

   ExecutorService newFixedThreadPool(int var1, ThreadFactory var2);

   ExecutorService newSingleThreadExecutor();

   ExecutorService newSingleThreadExecutor(ThreadFactory var1);

   ExecutorService newCachedThreadPool();

   ExecutorService newCachedThreadPool(ThreadFactory var1);

   ExecutorService newThreadPerTaskExecutor(ThreadFactory var1);

   ExecutorService newVirtualThreadPerTaskExecutor();

   ScheduledExecutorService newSingleThreadScheduledExecutor();

   ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory var1);

   ScheduledExecutorService newScheduledThreadPool(int var1);

   ScheduledExecutorService newScheduledThreadPool(int var1, ThreadFactory var2);

   ExecutorService unconfigurableExecutorService(ExecutorService var1);

   ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService var1);

   ThreadFactory defaultThreadFactory();

   <T> Callable<T> callable(Runnable var1, T var2);

   Callable<Object> callable(Runnable var1);

   Callable<Object> callable(PrivilegedAction<?> var1);

   Callable<Object> callable(PrivilegedExceptionAction<?> var1);
}
