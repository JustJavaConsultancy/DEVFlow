package com.justjava.devFlow.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmailUtil {
    public static Map<String, Object> buildEmailData (String to, String from, String subject, String password,
                                                      String webUrl) {
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("subject", subject);
        emailData.put("toEmail", to);
        emailData.put("fromEmail", from);
        emailData.put("email", to);
        emailData.put("password", password);
        emailData.put("webUrl", webUrl);
        return emailData;
    }
}
