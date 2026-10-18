package dev.xoperr.blissgems.libs.faststats.core;

import dev.xoperr.blissgems.libs.faststats.core.concurrent.TrackingThreadFactory;
import java.util.concurrent.ThreadFactory;

final class WrappedTrackingThreadFactory implements ThreadFactory {
   private final ErrorTracker tracker;
   private final ThreadFactory factory;

   public WrappedTrackingThreadFactory(ErrorTracker tracker, ThreadFactory factory) {
      this.tracker = tracker;
      this.factory = factory;
   }

   @Override
   public Thread newThread(Runnable runnable) {
      return this.factory.newThread(this.tracker.base().tracked(runnable));
   }

   public static ThreadFactory wrap(ErrorTracker tracker, ThreadFactory factory) {
      return !(factory instanceof TrackingThreadFactory) && !(factory instanceof WrappedTrackingThreadFactory)
         ? new WrappedTrackingThreadFactory(tracker, factory)
         : factory;
   }
}
