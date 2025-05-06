package com.hind.spring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sendinblue.ApiClient;
import sendinblue.ApiException;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.*;
import jakarta.mail.MessagingException;
import java.util.Arrays;

@Service
public class BrevoEmailService implements EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    private TransactionalEmailsApi getApiInstance() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKeyAuth.setApiKey(apiKey);
        return new TransactionalEmailsApi();
    }

    @Override
    public void sendVerificationEmail(String to, String subject, String htmlContent) throws MessagingException {
        try {
            TransactionalEmailsApi api = getApiInstance();

            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(fromEmail);
            sender.setName(fromName);

            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(to);

            SendSmtpEmail email = new SendSmtpEmail();
            email.setSender(sender);
            email.setTo(Arrays.asList(recipient));
            email.setHtmlContent(htmlContent);
            email.setSubject(subject);

            api.sendTransacEmail(email);
        } catch (ApiException e) {
            throw new MessagingException("Failed to send email: " + e.getMessage());
        }
    }

    // Optional: Method for sending emails with attachments
    public void sendEmailWithAttachment(String to, String subject, String htmlContent, byte[] attachment, String fileName) throws ApiException {
        // Implementation for sending emails with attachments can be added here if needed
    }
} 