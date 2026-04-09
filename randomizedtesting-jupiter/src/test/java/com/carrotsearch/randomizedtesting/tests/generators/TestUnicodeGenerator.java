package com.carrotsearch.randomizedtesting.tests.generators;

import com.carrotsearch.randomizedtesting.jupiter.generators.UnicodeGenerator;

public class TestUnicodeGenerator extends StringGeneratorTestBase {
  public TestUnicodeGenerator() {
    super(new UnicodeGenerator());
  }
}
