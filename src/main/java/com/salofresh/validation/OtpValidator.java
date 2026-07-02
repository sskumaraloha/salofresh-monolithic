package com.salofresh.validation;

import com.salofresh.constant.ValidationPatterns;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class OtpValidator implements ConstraintValidator<ValidOtp, String> {

    private static final Pattern PATTERN = Pattern.compile(ValidationPatterns.OTP);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return PATTERN.matcher(value).matches();
    }
}
