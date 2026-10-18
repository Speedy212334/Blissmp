package dev.xoperr.blissgems.libs.faststats.core.concurrent;

import java.util.concurrent.ThreadFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

@ApiStatus.NonExtendable
public interface TrackingThreadFactory extends ThreadFactory {
   @Contract(value = "_ -> new", pure = true)
   @Override
   Thread newThread(Runnable var1);

   @Contract(value = "_, _ -> new", pure = true)
   Thread newThread(@NonNls String var1, Runnable var2);
}
