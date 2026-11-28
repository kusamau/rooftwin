package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A command declared in a DTDL interface. The JSON form names the command in a {@code "command"}
 * field; request parameters, if any, are the record's components.
 *
 * <p>A type here is a <em>declaration</em>, not a permission. rooftwin never sends commands to the
 * real Victron devices (shadow mode); in Lesson 9 a policy decides who may even ask.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "command")
@JsonSubTypes({
  @JsonSubTypes.Type(value = MpptCommand.ResetDailyCounters.class, name = "resetDailyCounters")
})
public sealed interface Command permits MpptCommand {

  /** DTMI of the interface that declares the command. */
  String interfaceDtmi();

  /** DTDL name of the command. */
  String name();
}
