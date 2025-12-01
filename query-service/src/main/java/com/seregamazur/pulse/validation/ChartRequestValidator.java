package com.seregamazur.pulse.validation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.seregamazur.pulse.dto.DataForPeriodRequest;
import com.seregamazur.pulse.dto.Period;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ChartRequestValidator implements ConstraintValidator<ValidDateRequest, DataForPeriodRequest> {

    @Override
    public boolean isValid(DataForPeriodRequest request, ConstraintValidatorContext ctx) {
        ctx.disableDefaultConstraintViolation();

        LocalDate from = request.from();
        LocalDate to = request.to();
        Period period = request.period();

        if (from == null || to == null || period == null) {
            add(ctx, "from, to and period must be provided");
            return false;
        }

        long days = ChronoUnit.DAYS.between(from, to);
        if (days < 0) {
            add(ctx, "from must be <= to");
            return false;
        }

        long maxAllowed = MaxResolutionRules.MAX_DAYS.get(period);

        if (days > maxAllowed) {
            add(ctx,
                "Selected period " + period +
                    " is too detailed for range " + days + " days. " +
                    "Maximum allowed is " + maxAllowed + " days."
            );
            return false;
        }
        return true;
    }

    private void add(ConstraintValidatorContext ctx, String message) {
        ctx.buildConstraintViolationWithTemplate(message)
            .addConstraintViolation();
    }
}
