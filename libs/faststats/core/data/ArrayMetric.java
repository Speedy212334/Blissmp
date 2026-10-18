package dev.xoperr.blissgems.libs.faststats.core.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

final class ArrayMetric<T> extends SimpleMetric<T[]> {
   public ArrayMetric(@SourceId String id, Callable<T @Nullable []> callable) throws IllegalArgumentException {
      super(id, callable);
   }

   @Override
   public Optional<JsonElement> getData() throws Exception {
      return this.compute().map(data -> {
         JsonArray elements = new JsonArray(data.length);

         for (T d : data) {
            switch (d) {
               case Boolean b:
                  elements.add(b);
                  break;
               case Number n:
                  elements.add(n);
                  break;
               default:
                  elements.add(d.toString());
            }
         }

         return elements;
      });
   }
}
