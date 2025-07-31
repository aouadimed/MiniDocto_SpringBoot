package com.minidocto.availability.dto;

import com.minidocto.availability.model.AvailabilitySlot;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.*;

/**
 * DTO for updating a doctor's availability slots (added/removed).
 */
@Data
@AvailabilityUpdateRequest.ValidAvailabilityUpdate
public class AvailabilityUpdateRequest {
    @NotNull(message = "Added slots list cannot be null")
    private List<AvailabilitySlot> addedSlots;
    @NotNull(message = "Removed slots list cannot be null")
    private List<AvailabilitySlot> removedSlots;

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = AvailabilityUpdateRequestValidator.class)
    public @interface ValidAvailabilityUpdate {
        String message() default "At least one of addedSlots or removedSlots must be non-empty";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class AvailabilityUpdateRequestValidator implements ConstraintValidator<ValidAvailabilityUpdate, AvailabilityUpdateRequest> {
        @Override
        public boolean isValid(AvailabilityUpdateRequest value, ConstraintValidatorContext context) {
            if (value == null) return false;
            boolean addedEmpty = value.addedSlots == null || value.addedSlots.isEmpty();
            boolean removedEmpty = value.removedSlots == null || value.removedSlots.isEmpty();
            return !(addedEmpty && removedEmpty);
        }
    }
} 