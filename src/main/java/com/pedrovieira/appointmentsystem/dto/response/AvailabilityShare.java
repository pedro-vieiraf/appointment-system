package com.pedrovieira.appointmentsystem.dto.response;

import java.util.UUID;

/**
 * The relative path used to query the available intervals for a shared agenda.
 */
public record AvailabilityShare(String slotsPath) {

  /** Builds the public path from the active link identifier. */
  public static AvailabilityShare forLink(UUID linkId) {
    return new AvailabilityShare("/public/availability/" + linkId + "/slots");
  }
}
