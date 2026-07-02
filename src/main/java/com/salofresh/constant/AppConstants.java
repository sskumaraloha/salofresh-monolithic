package com.salofresh.constant;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String API_BASE_PATH = "/api/v1";

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    public static final String ROLE_PREFIX = "ROLE_";

    public static final int DEFAULT_SLOT_DURATION_MINUTES = 30;
    public static final int DEFAULT_BUFFER_TIME_MINUTES = 5;

    public static final String CURRENCY_INR = "INR";
    public static final double DEFAULT_GST_PERCENTAGE = 18.0;

    public static final int REFERRAL_BONUS_POINTS = 100;
    public static final int SIGNUP_BONUS_POINTS = 50;
    public static final double POINTS_TO_CURRENCY_RATIO = 1.0;

    public static final String INVOICE_PREFIX = "INV";
    public static final String BOOKING_PREFIX = "BK";
    public static final String TRANSACTION_PREFIX = "TXN";
    public static final String REFERRAL_CODE_LENGTH_KEY = "referral.code.length";
    public static final int REFERRAL_CODE_LENGTH = 8;
}
