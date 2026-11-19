package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A location, serialized as the GeoJSON Point that DTDL's {@code point} schema expects.
 *
 * <p>GeoJSON (RFC 7946) orders coordinates <b>longitude first</b>: {@code [lon, lat]}. Getting it
 * backwards still parses, still validates, and puts the house in the wrong hemisphere or the ocean.
 * Named components in Java and one place that writes the array keep that bug out.
 *
 * @param longitude degrees east, in [-180, 180]
 * @param latitude degrees north, in [-90, 90]
 */
public record GeoPoint(double longitude, double latitude) {

  /** Validates ranges. */
  public GeoPoint {
    Checks.requireRange("longitude", longitude, -180.0, 180.0);
    Checks.requireRange("latitude", latitude, -90.0, 90.0);
  }

  /**
   * Reads {@code {"type":"Point","coordinates":[lon, lat]}}.
   *
   * @param type must be {@code "Point"}
   * @param coordinates {@code [longitude, latitude]}; an optional altitude is not supported
   * @return the point
   */
  @JsonCreator
  public static GeoPoint fromGeoJson(
      @JsonProperty("type") String type, @JsonProperty("coordinates") List<Double> coordinates) {
    if (!"Point".equals(type)) {
      throw new IllegalArgumentException("type must be \"Point\", was " + type);
    }
    if (coordinates == null || coordinates.size() != 2) {
      throw new IllegalArgumentException("coordinates must be [longitude, latitude]");
    }
    return new GeoPoint(coordinates.get(0), coordinates.get(1));
  }

  /** Writes {@code {"type":"Point","coordinates":[lon, lat]}}. */
  @JsonValue
  public Map<String, Object> toGeoJson() {
    Map<String, Object> json = new LinkedHashMap<>();
    json.put("type", "Point");
    json.put("coordinates", List.of(longitude, latitude));
    return json;
  }
}
