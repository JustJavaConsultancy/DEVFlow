package com.justjava.devFlow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Delegate that extracts generated Thymeleaf and Controller artifacts
 * from process variable "artifact" and writes them into the correct
 * Spring Boot project folders. The project root path is retrieved
 * from process variable "appPath".
 */
@Component
public class WriteGeneratedArtifactsDelegate implements JavaDelegate {

    // Patterns to identify different file types
    private static final Pattern HTML_FILE_PATTERN = Pattern.compile("<!--\\s*(src/main/resources/templates/[^>]+)\\.html\\s*-->([\\s\\S]*?)(?=<!--|$)");
    private static final Pattern JAVA_FILE_PATTERN = Pattern.compile("java\\s*//\\s*(src/main/java/[^>]+\\.java)\\s*([\\s\\S]*?)(?=java\\s*//|<!--|$)");

    @Override
    public void execute(DelegateExecution execution) {
        try {
            System.out.println("Starting file extraction process...");

            // Retrieve process variables
            String artifact = (String) execution.getVariable("artifact");
            String appPath = (String) execution.getVariable("appPath");

            if (artifact == null || artifact.trim().isEmpty()) {
                throw new IllegalArgumentException("Process variable 'artifact' is null or empty");
            }

            if (appPath == null || appPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Process variable 'appPath' is null or empty");
            }

            System.out.println("Application root path: " + appPath);

            // Extract and write files
            Map<String, String> extractedFiles = extractFilesFromArtifact(artifact);
            writeFilesToDisk(appPath, extractedFiles);

            // Set process variable with extraction results
            execution.setVariable("filesExtracted", extractedFiles.size());
            execution.setVariable("extractionStatus", "SUCCESS");

            System.out.println("Successfully extracted " + extractedFiles.size() + " files to " + appPath);

        } catch (Exception e) {
            System.out.println("Error during file extraction: " + e.getMessage());
            execution.setVariable("extractionStatus", "FAILED");
            execution.setVariable("errorMessage", e.getMessage());
            throw new RuntimeException("File extraction failed", e);
        }
    }

    private Map<String, String> extractFilesFromArtifact(String artifact) {
        Map<String, String> files = new HashMap<>();

        // Extract HTML files (Thymeleaf templates)
        extractHtmlFiles(artifact, files);

        // Extract Java files
        extractJavaFiles(artifact, files);

        return files;
    }

    private void extractHtmlFiles(String artifact, Map<String, String> files) {
        Matcher matcher = HTML_FILE_PATTERN.matcher(artifact);

        System.out.println(" The matcher ==="+matcher.toString());
        while (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = matcher.group(2).trim();

            System.out.println(" The file content==="+fileContent + "and  filePath==="+filePath);
            // Clean up the file path and content
            filePath = filePath.replace("<!--", "").replace("-->", "").trim();

            // Remove the file path comment from content if present
            if (fileContent.contains("<!DOCTYPE html>")) {
                files.put(filePath, fileContent);
                System.out.println("Extracted HTML file: " + filePath);
            }
        }
    }

    private void extractJavaFiles(String artifact, Map<String, String> files) {
        Matcher matcher = JAVA_FILE_PATTERN.matcher(artifact);

        while (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = matcher.group(2).trim();

            // Clean up the file path
            filePath = filePath.replace("java", "").replace("//", "").trim();

            // Ensure the content starts with proper Java syntax
            if (fileContent.startsWith("package") || fileContent.contains("@Controller")) {
                files.put(filePath, fileContent);
                System.out.println("Extracted Java file: " + filePath);
            }
        }

        // Alternative extraction for Java files if the first pattern doesn't match
        if (files.isEmpty()) {
            extractJavaFilesAlternative(artifact, files);
        }
    }

    private void extractJavaFilesAlternative(String artifact, Map<String, String> files) {
        // Look for Java file declarations in the artifact
        Pattern altJavaPattern = Pattern.compile("//\\s*(src/main/java/[^\\n]+\\.[a-zA-Z]+)\\s*([\\s\\S]*?)(?=//\\s*src/main/java/|<!--|$)");
        Matcher matcher = altJavaPattern.matcher(artifact);

        while (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = matcher.group(2).trim();

            if (fileContent.startsWith("package") || fileContent.contains("@")) {
                files.put(filePath, fileContent);
                System.out.println("Extracted Java file (alternative): " + filePath);
            }
        }
    }

    private void writeFilesToDisk(String appPath, Map<String, String> files) throws IOException {
        Path rootPath = Paths.get(appPath);

        // Ensure root directory exists
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
            System.out.println("Created root directory: " + rootPath.toAbsolutePath());
        }

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String relativePath = entry.getKey();
            String content = entry.getValue();

            Path fullPath = rootPath.resolve(relativePath);
            Path parentDir = fullPath.getParent();

            // Create parent directories if they don't exist
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                System.out.println("Created directory: " + parentDir.toAbsolutePath());
            }

            // Write file content
            Files.writeString(fullPath, content);
            System.out.println("Written file: " + fullPath.toAbsolutePath());
        }
    }
}