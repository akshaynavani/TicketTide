package com.tickettide.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingEmailSender {

    private static final Logger log = LoggerFactory.getLogger("tickettide.email");

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    public EmailSender loggingSender(@Value("${tickettide.email.from}") String from) {
        return (to, subject, body) -> log.info("EMAIL from={} to={} subject=\"{}\"\n{}", from, to, subject, body);
    }
}
