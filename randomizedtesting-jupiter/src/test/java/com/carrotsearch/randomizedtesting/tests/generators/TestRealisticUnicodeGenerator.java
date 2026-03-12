package com.carrotsearch.randomizedtesting.tests.generators;

import com.carrotsearch.randomizedtesting.jupiter.generators.RealisticUnicodeGenerator;

public class TestRealisticUnicodeGenerator extends StringGeneratorTestBase {
  public TestRealisticUnicodeGenerator() {
    super(new RealisticUnicodeGenerator());
  }
}
