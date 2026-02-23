package com.carrotsearch.randomizedtesting.jupiter;

/** Static hashing utilities. */
public final class Hashing {
  /** Bit mixer for {@code long} values. */
  public static long mix64(long k) {
    k ^= k >>> 33;
    k *= 0xff51afd7ed558ccdL;
    k ^= k >>> 33;
    k *= 0xc4ceb9fe1a85ec53L;
    k ^= k >>> 33;
    return k;
  }

  /** String hash function redistributing over a {@code long}. */
  public static long longHash(String v) {
    long h = 0;
    int length = v.length();
    for (int i = 0; i < length; i++) {
      h = 31 * h + v.charAt(i);
    }
    return mix64(h);
  }
}
