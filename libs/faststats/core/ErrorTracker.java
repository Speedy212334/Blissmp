package dev.xoperr.blissgems.libs.faststats.core;

import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingBase;
import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingExecutors;
import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingThreadFactory;
import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingThreadPoolExecutor;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public sealed interface ErrorTracker permits SimpleErrorTracker {
   @Contract(" -> new")
   static ErrorTracker contextAware() {
      SimpleErrorTracker tracker = new SimpleErrorTracker();
      tracker.attachErrorContext(ErrorTracker.class.getClassLoader());
      return tracker;
   }

   @Contract(" -> new")
   static ErrorTracker contextUnaware() {
      return new SimpleErrorTracker();
   }

   @Contract(mutates = "this")
   void trackError(String var1);

   @Contract(mutates = "this")
   void trackError(Throwable var1);

   void attachErrorContext(@Nullable ClassLoader var1) throws IllegalStateException;

   void detachErrorContext();

   boolean isContextAttached();

   @Contract(mutates = "this")
   void setContextErrorHandler(@Nullable BiConsumer<@Nullable ClassLoader, Throwable> var1);

   @Contract(pure = true)
   Optional<BiConsumer<@Nullable ClassLoader, Throwable>> getContextErrorHandler();

   @Contract(pure = true)
   TrackingBase base();

   @Contract(pure = true)
   TrackingExecutors executors();

   @Contract(pure = true)
   TrackingThreadFactory threadFactory();

   @Contract(pure = true)
   TrackingThreadPoolExecutor threadPoolExecutor();

   @Contract(pure = true)
   static boolean isSameLoader(ClassLoader loader, Throwable error) {
      return ErrorHelper.isSameLoader(loader, error);
   }
}
