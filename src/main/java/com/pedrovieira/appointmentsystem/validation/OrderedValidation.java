package com.pedrovieira.appointmentsystem.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/**
 * Defines the validation group execution order for DTOs that require deterministic validation
 * feedback.
 */
@GroupSequence({Default.class, SizeCheck.class})
public interface OrderedValidation {

}