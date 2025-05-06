package com.hind.spring.service;

import jakarta.mail.MessagingException;

public interface EmailService {
    void sendVerificationEmail(String to, String subject, String htmlContent) throws MessagingException;
}
