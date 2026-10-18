package dev.xoperr.blissgems.libs.faststats.core.data;

import com.google.gson.JsonElement;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public interface Metric<T> {
   @SourceId
   @Contract(pure = true)
   String getId();

   @Contract(pure = true)
   Optional<T> compute() throws Exception;

   @Contract(pure = true)
   Optional<JsonElement> getData() throws Exception;

   @Contract(value = "_, _ -> new", pure = true)
   static Metric<String[]> stringArray(@SourceId String id, Callable<String @Nullable []> callable) throws IllegalArgumentException {
      return new ArrayMetric<>(id, callable);
   }

   @Contract(value = "_, _ -> new", pure = true)
   static Metric<Boolean[]> booleanArray(@SourceId String id, Callable<Boolean @Nullable []> callable) throws IllegalArgumentException {
      return new ArrayMetric<>(id, callable);
   }

   @Contract(value = "_, _ -> new", pure = true)
   static Metric<Number[]> numberArray(@SourceId String id, Callable<Number @Nullable []> callable) throws IllegalArgumentException {
      return new ArrayMetric<>(id, callable);
   }

   @Contract(value = "_, _ -> new", pure = true)
   static Metric<Boolean> bool(@SourceId String id, Callable<@Nullable Boolean> callable) throws IllegalArgumentException {
      return new SingleValueMetric<>(id, callable);
   }

   @Contract(value = "_, _ -> new", pure = true)
   static Metric<String> string(@SourceId String id, Callable<@Nullable String> callable) throws IllegalArgumentException {
      return new SingleValueMetric<>(id, callable);
   }

   @Contract(value = "_, _ -> new", pure = true)
   static Metric<Number> number(@SourceId String id, Callable<@Nullable Number> callable) throws IllegalArgumentException {
      return new SingleValueMetric<>(id, callable);
   }
}
