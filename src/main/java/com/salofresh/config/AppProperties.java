package com.salofresh.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String version;
    private String frontendUrl;
    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Otp otp = new Otp();
    private final Security security = new Security();
    private final File file = new File();
    private final Payment payment = new Payment();
    private final Sms sms = new Sms();
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs;
        private long refreshTokenExpirationMs;
        private long rememberMeRefreshTokenExpirationMs;
        private String issuer;
    }

    @Getter
    @Setter
    public static class Otp {
        private int length;
        private int expiryMinutes;
        private int maxAttempts;
        private int resendCooldownSeconds;
    }

    @Getter
    @Setter
    public static class Security {
        private int maxFailedLoginAttempts;
        private int accountLockDurationMinutes;
    }

    @Getter
    @Setter
    public static class File {
        private String uploadDir;
        private long maxSizeBytes;
        private List<String> allowedExtensions;
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class Payment {
        private final Razorpay razorpay = new Razorpay();
        private final Stripe stripe = new Stripe();

        @Getter
        @Setter
        public static class Razorpay {
            private String keyId;
            private String keySecret;
        }

        @Getter
        @Setter
        public static class Stripe {
            private String secretKey;
            private String publishableKey;
        }
    }

    @Getter
    @Setter
    public static class Sms {
        private String provider;
        private final Twilio twilio = new Twilio();

        @Getter
        @Setter
        public static class Twilio {
            private String accountSid;
            private String authToken;
            private String fromNumber;
        }
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled;
        private int capacity;
        private int refillTokens;
        private int refillDurationSeconds;
    }
}
