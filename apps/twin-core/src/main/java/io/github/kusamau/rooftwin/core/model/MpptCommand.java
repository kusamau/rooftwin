package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Commands declared by {@code dtmi:io:github:kusamau:rooftwin:MpptChargeController;1}. */
public sealed interface MpptCommand extends Command permits MpptCommand.ResetDailyCounters {

  @Override
  @JsonIgnore
  default String interfaceDtmi() {
    return Dtmi.MPPT_CHARGE_CONTROLLER;
  }

  /** DTDL {@code resetDailyCounters}: no request, no response. */
  record ResetDailyCounters() implements MpptCommand {
    @Override
    @JsonIgnore
    public String name() {
      return "resetDailyCounters";
    }
  }
}
