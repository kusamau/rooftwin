package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** The one Jackson configuration for twin JSON, so every module reads and writes the same form. */
public final class TwinJson {

  private TwinJson() {}

  /**
   * A new mapper for twin JSON.
   *
   * <ul>
   *   <li>Times as ISO-8601 strings ({@code "2026-11-19T12:00:00Z"}), never epoch numbers.
   *   <li><b>Strict:</b> unknown fields fail. A field the model doesn't declare is drift, and drift
   *       should be loud in tests. An ingestion path that must survive newer producers can relax
   *       this explicitly, where the decision is visible.
   *   <li>A JSON {@code null} for a primitive fails instead of silently becoming 0. A
   *       <em>missing</em> primitive fails too, because each always-present reading is marked
   *       {@code @JsonProperty(required = true)} on its record. (The global {@code
   *       FAIL_ON_MISSING_CREATOR_PROPERTIES} would also reject absent <em>optional</em> fields.)
   * </ul>
   *
   * @return a configured, thread-safe mapper
   */
  public static ObjectMapper mapper() {
    return JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .build();
  }
}
