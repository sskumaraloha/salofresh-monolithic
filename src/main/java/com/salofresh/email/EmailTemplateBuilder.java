package com.salofresh.email;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildOtpEmail(String recipientName, String otpCode, int expiryMinutes) {
        return """
                <html>
                <body style="font-family:Arial,sans-serif;">
                  <h2>SaloFresh Verification Code</h2>
                  <p>Hi %s,</p>
                  <p>Your One-Time Password (OTP) is:</p>
                  <h1 style="letter-spacing:4px;">%s</h1>
                  <p>This code will expire in %d minutes. Do not share this code with anyone.</p>
                  <p>Regards,<br/>Team SaloFresh</p>
                </body>
                </html>
                """.formatted(recipientName, otpCode, expiryMinutes);
    }

    public String buildBookingConfirmationEmail(String customerName, String bookingNumber, String salonName,
                                                 String appointmentDate, String appointmentTime) {
        return """
                <html>
                <body style="font-family:Arial,sans-serif;">
                  <h2>Booking Confirmed!</h2>
                  <p>Hi %s,</p>
                  <p>Your appointment at <strong>%s</strong> has been confirmed.</p>
                  <ul>
                    <li>Booking Number: %s</li>
                    <li>Date: %s</li>
                    <li>Time: %s</li>
                  </ul>
                  <p>We look forward to seeing you!</p>
                  <p>Regards,<br/>Team SaloFresh</p>
                </body>
                </html>
                """.formatted(customerName, salonName, bookingNumber, appointmentDate, appointmentTime);
    }

    public String buildGenericNotificationEmail(String recipientName, String title, String message) {
        return """
                <html>
                <body style="font-family:Arial,sans-serif;">
                  <h2>%s</h2>
                  <p>Hi %s,</p>
                  <p>%s</p>
                  <p>Regards,<br/>Team SaloFresh</p>
                </body>
                </html>
                """.formatted(title, recipientName, message);
    }
}
