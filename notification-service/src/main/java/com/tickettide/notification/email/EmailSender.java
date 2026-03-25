package com.tickettide.notification.email;

/**
 * Outbound email channel. The default implementation logs messages; swap in an
 * SES or SMTP sender by providing another bean of this type.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
