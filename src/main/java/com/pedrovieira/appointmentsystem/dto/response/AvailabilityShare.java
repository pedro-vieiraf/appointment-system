package com.pedrovieira.appointmentsystem.dto.response;

import java.util.UUID;

/**
 * Relative paths for the booking page and its availability API.
 */
public record AvailabilityShare(String slotsPath, String bookingPath) {

  /** Builds the public path from the active link identifier. */
  public static AvailabilityShare forLink(UUID linkId) {
    String path = "/public/availability/" + linkId;
    return new AvailabilityShare(path + "/slots", path);
  }
}
