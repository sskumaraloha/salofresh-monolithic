package com.salofresh.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
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
    private final Push push = new Push();
    private final Whatsapp whatsapp = new Whatsapp();
    private final RateLimit rateLimit = new RateLimit();
    private final Payout payout = new Payout();

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

    /**
     * Push notification configuration. Delivery uses the Firebase Cloud Messaging HTTP v1 API
     * ({@code https://fcm.googleapis.com/v1/projects/{projectId}/messages:send}), which requires an
     * OAuth2 bearer token minted from a Google service-account credential rather than a static server key.
     */
    @Getter
    @Setter
    public static class Push {
        private final Fcm fcm = new Fcm();

        @Getter
        @Setter
        public static class Fcm {
            /** GCP/Firebase project id that owns the messaging service account. */
            private String projectId;
            /**
             * The service-account credential used to obtain FCM OAuth2 access tokens. May be either:
             * <ul>
             *     <li>the raw JSON content of the service-account key (starts with '{'), or</li>
             *     <li>a filesystem path to the service-account JSON key file.</li>
             * </ul>
             * Left blank in default/dev configuration; push sends are skipped (logged and swallowed)
             * until this is configured.
             */
            private String serviceAccountJson;
        }
    }

    @Getter
    @Setter
    public static class Whatsapp {
        private String provider;
        private final Meta meta = new Meta();

        @Getter
        @Setter
        public static class Meta {
            private String accessToken;
            private String phoneNumberId;
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

    /**
     * Fallback platform commission percentage used when generating a salon payout for a
     * salon owner who has no active {@code PlatformSubscription} (or when that module is
     * unavailable), applied in place of the subscription plan's commission percentage.
     */
    @Getter
    @Setter
    public static class Payout {
        private BigDecimal defaultCommissionPercentage = new BigDecimal("10.0");
    }
}
