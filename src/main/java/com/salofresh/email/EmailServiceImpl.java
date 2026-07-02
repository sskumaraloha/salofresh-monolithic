package com.salofresh.email;

import com.salofresh.entity.EmailLog;
import com.salofresh.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    @Override
    @Async("taskExecutor")
    public void sendSimpleEmail(String to, String subject, String body) {
        EmailLog.EmailLogBuilder logBuilder = EmailLog.builder()
                .recipient(to)
                .subject(subject)
                .body(body);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            saveLog(logBuilder.status(STATUS_SENT).sentAt(Instant.now()));
        } catch (Exception ex) {
            log.error("Failed to send email to {}", to, ex);
            saveLog(logBuilder.status(STATUS_FAILED).errorMessage(ex.getMessage()));
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        EmailLog.EmailLogBuilder logBuilder = EmailLog.builder()
                .recipient(to)
                .subject(subject)
                .body(htmlBody);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            saveLog(logBuilder.status(STATUS_SENT).sentAt(Instant.now()));
        } catch (Exception ex) {
            log.error("Failed to send HTML email to {}", to, ex);
            saveLog(logBuilder.status(STATUS_FAILED).errorMessage(ex.getMessage()));
        }
    }

    private void saveLog(EmailLog.EmailLogBuilder builder) {
        emailLogRepository.save(builder.build());
    }
}
