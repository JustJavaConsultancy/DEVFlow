package com.justjava.devFlow.util;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResendService {
    private final Resend resend;

    @Value("${resend.default-from}")
    private String fromEmail;

    @Value("${resend.template-id-devFlow}")
    private String templateId;

    public ResendService(@Value("${resend.api-key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public void sendMailWithTemplate(String toEmail, String subject,String password, String webUrl) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("email", toEmail);
        templateData.put("password", password);
        templateData.put("webUrl", webUrl);

        try {
            // 2. Build the request using the template ID
            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject(subject)
                    .template(com.resend.services.emails.model.Template.builder()
                            .id(templateId)
                            .variables(templateData)
                            .build())
                    .build();

            // 3. Send the email
            CreateEmailResponse response = resend.emails().send(request);
            System.out.println("Template email sent. ID: " + response.getId());

        } catch (ResendException e) {
            System.err.println("Failed to send template email: " + e.getMessage());
            // Handle exception (log, throw, etc.)
        }
    }

    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from(fromEmail) // verified domain or @resend.dev for testing
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(request);
        } catch (ResendException e) {
            System.err.println("Failed to send html email: " + e.getMessage());
        }
    }
}
