package com.justjava.devFlow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component("downloadSpringProjectDelegate")
public class DownloadSpringProjectDelegate implements JavaDelegate {

    private static final String SPRING_INITIALIZR_URL = "https://start.spring.io/starter.zip";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void execute(DelegateExecution execution) {
        // ✅ Get project parameters from process variables
        String groupId = (String) execution.getVariable("groupId");
        String artifactId = (String) execution.getVariable("artifactId");
        //String packageName = (String) execution.getVariable("packageName");
        String javaVersion = (String) execution.getVariable("javaVersion");
        String springBootVersion = (String) execution.getVariable("springBootVersion");


        String dependencies = (String) execution.getVariable("dependencies"); // e.g. "web,data-jpa"
        String outputDir = (String) execution.getVariable("outputDir");       // e.g. "C:/projects"

        try {
            // ✅ Build POST request body
            String body = "type=maven-project" +
                    "&language=java" +
                    "&bootVersion="+springBootVersion +   // adapt to latest
                    "&javaVersion="+javaVersion +
                    "&baseDir=" + artifactId +
                    "&groupId=" + groupId +
                    "&artifactId=" + artifactId +
                    "&name=" + artifactId +
                    "&packageName=" + groupId +
                    "&dependencies=" + dependencies;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            // ✅ Send POST request
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    SPRING_INITIALIZR_URL,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            // ✅ Validate response
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Failed to download Spring Boot project. Status: " + response.getStatusCode());
            }

            byte[] zipData = response.getBody();

            // ✅ Validate zip signature
            if (zipData.length < 4 ||
                    zipData[0] != 'P' || zipData[1] != 'K') {
                throw new IOException("Invalid ZIP file received from Spring Initializr");
            }

            // ✅ Ensure output directory exists
            Path targetDir = Paths.get(outputDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // ✅ Unzip into outputDir
            try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
                 ZipInputStream zis = new ZipInputStream(bais)) {

                ZipEntry entry;
                byte[] buffer = new byte[1024];

                while ((entry = zis.getNextEntry()) != null) {
                    Path outPath = targetDir.resolve(entry.getName());

                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                    } else {
                        // ensure parent directories exist
                        if (outPath.getParent() != null) {
                            Files.createDirectories(outPath.getParent());
                        }
                        try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }

                    zis.closeEntry();
                }
            }

            execution.setVariable("appPath",outputDir+"/"+artifactId);
            //System.out.println("✅ Project zip saved at: " + zipFilePath);

        } catch (Exception e) {
            throw new RuntimeException("Error downloading Spring Boot project from Initializr", e);
        }
    }
}
