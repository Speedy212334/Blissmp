package dev.xoperr.blissgems.libs.faststats.core.data;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

abstract class SimpleMetric<T> implements Metric<T> {
   @SourceId
   private final String id;
   private final Callable<@Nullable T> callable;

   public SimpleMetric(@SourceId String id, Callable<@Nullable T> callable) throws IllegalArgumentException {
      if (!id.matches("[a-z_]+")) {
         throw new IllegalArgumentException("Invalid source id '" + id + "', must match '[a-z_]+'");
      }

      this.id = id;
      this.callable = callable;
   }

   @SourceId
   @Override
   public final String getId() {
      return this.id;
   }

   @Override
   public final Optional<T> compute() throws Exception {
      return Optional.ofNullable(this.callable.call());
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         SimpleMetric<?> that = (SimpleMetric<?>)o;
         return Objects.equals(this.id, that.id);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.id);
   }

   @Override
   public String toString() {
      return "SimpleMetric{id='" + this.id + "'}";
   }
}
