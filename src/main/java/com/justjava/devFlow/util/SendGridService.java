package com.justjava.devFlow.util;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.apache.http.NoHttpResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridService {
    private final SendGrid sendGridClient;
    private final String fromEmail;

    @Value("${sendgrid.template-id}")
    private String TEMPLATE_ID ;

    public SendGridService(
            @Value("${sendgrid.api-key}") String apiKey,
            @Value("${sendgrid.from-email}") String fromEmail
    ) {
        this.sendGridClient = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
    }

    @Async
    public void sendTemplateEmail(String toEmail, String subject, String password, String webUrl) {
        if (TEMPLATE_ID == null || TEMPLATE_ID.isEmpty()) {
            return;
        }
        int maxRetries = 2;
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                Email from = new Email(fromEmail, "Just Java");
                Email to = new Email(toEmail);

                Mail mail = new Mail();
                mail.setFrom(from);
                mail.setTemplateId(TEMPLATE_ID);

                Personalization personalization = new Personalization();
                personalization.addTo(to);
                personalization.addDynamicTemplateData("email", toEmail);
                personalization.addDynamicTemplateData("password", password);
                personalization.addDynamicTemplateData("webUrl", webUrl);
                mail.addPersonalization(personalization);

                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());

                Response response = sendGridClient.api(request);
                System.out.println("\nEmail Status Code: " + response.getStatusCode());
                if (response.getStatusCode() >= 400) {
                    throw new RuntimeException(response.getBody());
                }
                return;
            } catch (NoHttpResponseException e) {
                System.err.println("No response from SendGrid (attempt " + (attempts + 1) + "): " + e.getMessage());
                attempts++;
                if (attempts >= maxRetries) {
                    throw new RuntimeException("SendGrid failed after retries: " + e.getMessage(), e);
                }
            } catch(Exception e){
                e.printStackTrace();
                throw new RuntimeException("Failed to send email: " + e.getMessage());
            }
        }
    }
}
