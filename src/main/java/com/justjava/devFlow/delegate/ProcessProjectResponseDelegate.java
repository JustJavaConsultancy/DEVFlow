package com.justjava.devFlow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class ProcessProjectResponseDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        try {
            // Get the response from HTTP task
            Object response = execution.getVariable("springInitializrResponse");

            String stringResponse = (String) response;
            System.out.println("String length: " + stringResponse.length());
            System.out.println("First 200 characters: " + stringResponse.substring(0, Math.min(200, stringResponse.length())));

            // Check if it's an error message
            if (stringResponse.contains("error") || stringResponse.contains("Error") ||
                    stringResponse.contains("exception") || stringResponse.contains("Exception")) {
                System.out.println("RESPONSE APPEARS TO BE AN ERROR MESSAGE");
            }
            // Check if it's HTML
            if (stringResponse.contains("<!DOCTYPE") || stringResponse.contains("<html") || stringResponse.contains("<body")) {
                System.out.println("RESPONSE APPEARS TO BE HTML (POSSIBLE ERROR PAGE)");
            }

            // Show the actual content
            System.out.println("Full response content:");
            System.out.println(stringResponse);
            byte[] projectZip = stringResponse.getBytes(StandardCharsets.UTF_8);

            if (projectZip instanceof byte[]) {

                String artifactId = (String) execution.getVariable("artifactId");
                String timestamp = String.valueOf(new Date().getTime());
                String filename = (artifactId==null?"projectX":artifactId) + "-" + timestamp + ".zip";

                try (FileOutputStream fos = new FileOutputStream(filename)) {
                    fos.write(projectZip);
                    System.out.println(" The full path=="+fos.getFD());
                }

                execution.setVariable("generatedProjectPath", filename);
                execution.setVariable("projectSize", projectZip.length);
                execution.setVariable("generationStatus", "SUCCESS");

                System.out.println("Project generated successfully: " + filename);
                System.out.println("File size: " + projectZip.length + " bytes");

            } else {
                execution.setVariable("generationStatus", "ERROR");
                execution.setVariable("errorMessage", "Invalid response format - expected byte array");
            }

        } catch (IOException e) {
            execution.setVariable("generationStatus", "ERROR");
            execution.setVariable("errorMessage", e.getMessage());
            throw new RuntimeException("Failed to save generated project", e);
        }
    }
}
