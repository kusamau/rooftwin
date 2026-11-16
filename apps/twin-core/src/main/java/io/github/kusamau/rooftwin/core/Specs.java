package io.github.kusamau.rooftwin.core;

/** Small validation helpers shared by the spec records. Package-private on purpose. */
final class Specs {

  private Specs() {}

  static String requireId(String name, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be null or blank");
    }
    return value;
  }

  static double requirePositive(String name, double value) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException(name + " must be a finite value > 0, was " + value);
    }
    return value;
  }

  static int requirePositive(String name, int value) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be > 0, was " + value);
    }
    return value;
  }

  static int requireNonNegative(String name, int value) {
    if (value < 0) {
      throw new IllegalArgumentException(name + " must be >= 0, was " + value);
    }
    return value;
  }
}
