package dev.xoperr.blissgems.libs.faststats.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingBase;
import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingExecutors;
import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingThreadFactory;
import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingThreadPoolExecutor;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

final class SimpleErrorTracker implements ErrorTracker {
   private final Map<String, Integer> collected = new ConcurrentHashMap<>();
   private final Map<String, JsonObject> reports = new ConcurrentHashMap<>();
   private final TrackingBase base = new SimpleTrackingBase(this);
   private final TrackingExecutors executors = new SimpleTrackingExecutors(this);
   private final TrackingThreadFactory threadFactory = new SimpleTrackingThreadFactory(this);
   private final TrackingThreadPoolExecutor threadPoolExecutor = new SimpleTrackingThreadPoolExecutor(this);
   private volatile @Nullable BiConsumer<@Nullable ClassLoader, Throwable> errorEvent = null;
   private volatile @Nullable UncaughtExceptionHandler originalHandler = null;

   @Override
   public void trackError(String message) {
      this.trackError(new RuntimeException(message));
   }

   @Override
   public void trackError(Throwable error) {
      try {
         JsonObject compiled = ErrorHelper.compile(error, null);
         String hashed = MurmurHash3.hash(compiled);
         if (this.collected.compute(hashed, (k, v) -> v == null ? 1 : v + 1) > 1) {
            return;
         }

         this.reports.put(hashed, compiled);
      } catch (NoClassDefFoundError var4) {
      }
   }

   public JsonArray getData() {
      JsonArray report = new JsonArray(this.reports.size());
      this.reports.forEach((hash, object) -> {
         JsonObject copy = object.deepCopy();
         copy.addProperty("hash", hash);
         Integer count = this.collected.getOrDefault(hash, 1);
         if (count > 1) {
            copy.addProperty("count", count);
         }

         report.add(copy);
      });
      this.collected.forEach((hash, count) -> {
         if (count > 0 && !this.reports.containsKey(hash)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("hash", hash);
            if (count > 1) {
               entry.addProperty("count", count);
            }

            report.add(entry);
         }
      });
      return report;
   }

   public void clear() {
      this.collected.replaceAll((k, v) -> 0);
      this.reports.clear();
   }

   public boolean needsFlushing() {
      if (!this.reports.isEmpty()) {
         return true;
      }

      for (Integer value : this.collected.values()) {
         if (value > 0) {
            return true;
         }
      }

      return false;
   }

   @Override
   public synchronized void attachErrorContext(@Nullable ClassLoader loader) throws IllegalStateException {
      if (this.originalHandler != null) {
         throw new IllegalStateException("Error context already attached");
      }

      this.originalHandler = Thread.getDefaultUncaughtExceptionHandler();
      Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
         UncaughtExceptionHandler handler = this.originalHandler;
         if (handler != null) {
            handler.uncaughtException(thread, error);
         }

         try {
            if (loader != null && !ErrorTracker.isSameLoader(loader, error)) {
               return;
            }

            BiConsumer<ClassLoader, Throwable> event = this.errorEvent;
            if (event != null) {
               event.accept(loader, error);
            }

            this.trackError(error);
         } catch (Throwable t) {
            this.trackError(t);
         }
      });
   }

   @Override
   public synchronized void detachErrorContext() {
      if (this.originalHandler != null) {
         Thread.setDefaultUncaughtExceptionHandler(this.originalHandler);
         this.originalHandler = null;
      }
   }

   @Override
   public synchronized boolean isContextAttached() {
      return this.originalHandler != null;
   }

   @Override
   public synchronized void setContextErrorHandler(@Nullable BiConsumer<@Nullable ClassLoader, Throwable> errorEvent) {
      this.errorEvent = errorEvent;
   }

   @Override
   public synchronized Optional<BiConsumer<@Nullable ClassLoader, Throwable>> getContextErrorHandler() {
      return Optional.ofNullable(this.errorEvent);
   }

   @Override
   public TrackingBase base() {
      return this.base;
   }

   @Override
   public TrackingExecutors executors() {
      return this.executors;
   }

   @Override
   public TrackingThreadFactory threadFactory() {
      return this.threadFactory;
   }

   @Override
   public TrackingThreadPoolExecutor threadPoolExecutor() {
      return this.threadPoolExecutor;
   }
}
