package com.carrotsearch.randomizedtesting.jupiter;

record Seed(long value) {
  private static final char[] HEX = "0123456789ABCDEF".toCharArray();
  static final Seed UNSPECIFIED = new Seed(0);

  @Override
  public String toString() {
    long seed = value;
    StringBuilder b = new StringBuilder(Long.BYTES * 2);
    do {
      b.append(HEX[(int) (seed & 0xF)]);
      seed = seed >>> 4;
    } while (seed != 0);
    return b.reverse().toString();
  }

  public boolean isUnspecified() {
    return this == UNSPECIFIED;
  }
}
