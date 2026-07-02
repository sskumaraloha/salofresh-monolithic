package com.salofresh.constant;

public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PHONE = "^[6-9]\\d{9}$";
    public static final String PASSWORD =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,64}$";
    public static final String GST_NUMBER =
            "^\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";
    public static final String OTP = "^\\d{4,6}$";
    public static final String PIN_CODE = "^\\d{6}$";
}
