package com.salofresh.validation;

import com.salofresh.constant.ValidationPatterns;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class GstValidator implements ConstraintValidator<ValidGst, String> {

    private static final Pattern PATTERN = Pattern.compile(ValidationPatterns.GST_NUMBER);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PATTERN.matcher(value).matches();
    }
}
