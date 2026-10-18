package dev.xoperr.blissgems.libs.faststats.core.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

final class SingleValueMetric<T> extends SimpleMetric<T> {
   public SingleValueMetric(@SourceId String id, Callable<@Nullable T> callable) throws IllegalArgumentException {
      super(id, callable);
   }

   @Override
   public Optional<JsonElement> getData() throws Exception {
      return this.compute().map(data -> {
         return switch (data) {
            case Boolean bool -> new JsonPrimitive(bool);
            case Number number -> new JsonPrimitive(number);
            default -> new JsonPrimitive(data.toString());
         };
      });
   }
}
