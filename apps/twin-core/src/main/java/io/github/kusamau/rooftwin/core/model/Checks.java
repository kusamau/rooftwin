package io.github.kusamau.rooftwin.core.model;

/** Validation helpers for telemetry and twin records. Package-private on purpose. */
final class Checks {

  private Checks() {}

  static <T> T requireNonNull(String name, T value) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return value;
  }

  static String requireId(String name, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be null or blank");
    }
    return value;
  }

  static void requireFinite(String name, double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite, was " + value);
    }
  }

  /** A missing optional reading is fine; a present one must be a real number. */
  static void requireFiniteIfPresent(String name, Double value) {
    if (value != null) {
      requireFinite(name, value);
    }
  }

  static void requireNonNegative(String name, double value) {
    if (!Double.isFinite(value) || value < 0.0) {
      throw new IllegalArgumentException(name + " must be a finite value >= 0, was " + value);
    }
  }

  static void requireNonNegativeIfPresent(String name, Double value) {
    if (value != null) {
      requireNonNegative(name, value);
    }
  }

  static void requireRange(String name, double value, double min, double max) {
    if (!(value >= min && value <= max)) {
      throw new IllegalArgumentException(
          name + " must be in [" + min + ", " + max + "], was " + value);
    }
  }

  static void requireRangeIfPresent(String name, Double value, double min, double max) {
    if (value != null) {
      requireRange(name, value, min, max);
    }
  }
}
