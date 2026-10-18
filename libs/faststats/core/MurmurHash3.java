package dev.xoperr.blissgems.libs.faststats.core;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.Contract;

final class MurmurHash3 {
   public static String hash(JsonObject object) {
      long[] hash = hash(object.toString());
      return Long.toHexString(hash[0]) + Long.toHexString(hash[1]);
   }

   @Contract(value = "_ -> new", pure = true)
   private static long[] hash(String data) {
      byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
      long h1 = 0L;
      long h2 = 0L;
      long c1 = -8663945395140668459L;
      long c2 = 5545529020109919103L;
      int length = bytes.length;
      int blocks = length / 16;

      for (int i = 0; i < blocks; i++) {
         int k1 = getInt(bytes, i * 16);
         int k2 = getInt(bytes, i * 16 + 4);
         int k3 = getInt(bytes, i * 16 + 8);
         int k4 = getInt(bytes, i * 16 + 12);
         k1 *= 289559509;
         k1 = Integer.rotateLeft(k1, 31);
         k1 *= 658871167;
         long var17 = h1 ^ k1;
         long var18 = Long.rotateLeft(var17, 27);
         long var19 = var18 + h2;
         h1 = var19 * 5L + 1390208809L;
         k2 *= 658871167;
         k2 = Integer.rotateLeft(k2, 33);
         k2 *= 289559509;
         h2 ^= k2;
         h2 = Long.rotateLeft(h2, 31);
         h2 += h1;
         h2 = h2 * 5L + 944331445L;
      }

      int k1 = 0;
      int k2 = 0;
      int k3 = 0;
      int k4 = 0;
      int tail = blocks * 16;
      switch (length & 15) {
         case 15:
            k4 ^= (bytes[tail + 14] & 255) << 16;
         case 14:
            k4 ^= (bytes[tail + 13] & 255) << 8;
         case 13:
            k4 ^= bytes[tail + 12] & 255;
            k4 *= 658871167;
            k4 = Integer.rotateLeft(k4, 33);
            k4 *= 289559509;
            h2 ^= k4;
         case 12:
            k3 ^= (bytes[tail + 11] & 255) << 24;
         case 11:
            k3 ^= (bytes[tail + 10] & 255) << 16;
         case 10:
            k3 ^= (bytes[tail + 9] & 255) << 8;
         case 9:
            k3 ^= bytes[tail + 8] & 255;
            k3 *= 289559509;
            k3 = Integer.rotateLeft(k3, 31);
            k3 *= 658871167;
            h1 ^= k3;
         case 8:
            k2 ^= (bytes[tail + 7] & 255) << 24;
         case 7:
            k2 ^= (bytes[tail + 6] & 255) << 16;
         case 6:
            k2 ^= (bytes[tail + 5] & 255) << 8;
         case 5:
            k2 ^= bytes[tail + 4] & 255;
            k2 *= 658871167;
            k2 = Integer.rotateLeft(k2, 33);
            k2 *= 289559509;
            h2 ^= k2;
         case 4:
            k1 ^= (bytes[tail + 3] & 255) << 24;
         case 3:
            k1 ^= (bytes[tail + 2] & 255) << 16;
         case 2:
            k1 ^= (bytes[tail + 1] & 255) << 8;
         case 1:
            k1 ^= bytes[tail] & 255;
            k1 *= 289559509;
            k1 = Integer.rotateLeft(k1, 31);
            k1 *= 658871167;
            h1 ^= k1;
         default:
            h1 ^= length;
            h2 ^= length;
            h1 += h2;
            h2 += h1;
            h1 = fmix64(h1);
            h2 = fmix64(h2);
            h1 += h2;
            h2 += h1;
            return new long[]{h1, h2};
      }
   }

   @Contract(pure = true)
   private static long fmix64(long k) {
      k ^= k >>> 33;
      k *= -49064778989728563L;
      k ^= k >>> 33;
      k *= -4265267296055464877L;
      return k ^ k >>> 33;
   }

   @Contract(pure = true)
   private static int getInt(byte[] bytes, int offset) {
      return bytes[offset] & 0xFF | (bytes[offset + 1] & 0xFF) << 8 | (bytes[offset + 2] & 0xFF) << 16 | (bytes[offset + 3] & 0xFF) << 24;
   }
}
