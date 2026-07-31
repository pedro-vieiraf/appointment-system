package com.pedrovieira.appointmentsystem.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

@GroupSequence({Default.class, SizeCheck.class})
public interface OrderedValidation {
}