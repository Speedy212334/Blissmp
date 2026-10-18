package dev.xoperr.blissgems.libs.faststats.core.concurrent;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import org.jetbrains.annotations.Contract;

public interface TrackingBase {
   @Contract(value = "_ -> new", pure = true)
   Runnable tracked(Runnable var1);

   @Contract(value = "_ -> new", pure = true)
   <T> PrivilegedAction<T> tracked(PrivilegedAction<T> var1);

   @Contract(value = "_ -> new", pure = true)
   <T> PrivilegedExceptionAction<T> tracked(PrivilegedExceptionAction<T> var1);
}
