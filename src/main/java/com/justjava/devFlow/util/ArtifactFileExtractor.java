package com.justjava.devFlow.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ArtifactFileExtractor {

    public static class ExtractedFile {
        private final String filePath;
        private final String content;
        private final FileType fileType;

        public ExtractedFile(String filePath, String content, FileType fileType) {
            this.filePath = filePath;
            this.content = content;
            this.fileType = fileType;
        }

        public String getFilePath() { return filePath; }
        public String getContent() { return content; }
        public FileType getFileType() { return fileType; }
    }

    public enum FileType {
        HTML_TEMPLATE,
        JAVA_CLASS,
        YAML_CONFIG,
        UNKNOWN
    }

    public List<ExtractedFile> extractFiles(String artifact) {
        List<ExtractedFile> extractedFiles = new ArrayList<>();

        System.out.println("Starting file extraction from artifact...");

        // Extract HTML templates
        extractedFiles.addAll(extractHtmlFiles(artifact));

        // Extract Java classes
        extractedFiles.addAll(extractJavaFiles(artifact));

        // Extract YAML configuration files
        extractedFiles.addAll(extractYamlFiles(artifact));

        System.out.println("Successfully extracted " + extractedFiles.size() + " files from artifact");

        // Debug: Print extracted file paths
        for (ExtractedFile file : extractedFiles) {
            System.out.println("Found: " + file.getFilePath() + " (" + file.getFileType() + ")");
        }

        return extractedFiles;
    }

    private List<ExtractedFile> extractHtmlFiles(String artifact) {
        List<ExtractedFile> files = new ArrayList<>();

        // Pattern for HTML files
        Pattern htmlPattern = Pattern.compile(
                "<!--\\s*(src/main/resources/templates/[^>]+\\.html)\\s*-->\\s*" +
                        "(<!DOCTYPE[\\s\\S]*?</html>\\s*)",
                Pattern.DOTALL
        );

        Matcher matcher = htmlPattern.matcher(artifact);

        while (matcher.find()) {
            String filePath = cleanHtmlFilePath(matcher.group(1));
            String content = matcher.group(2).trim();

            if (isValidHtmlContent(content)) {
                files.add(new ExtractedFile(filePath, content, FileType.HTML_TEMPLATE));
                System.out.println("Extracted HTML file: " + filePath);
            }
        }

        return files;
    }

    private String cleanHtmlFilePath(String filePath) {
        // Remove only the comment markers, not content within the path
        return filePath.replace("<!--", "").replace("-->", "").trim();
    }

    private List<ExtractedFile> extractJavaFiles(String artifact) {
        List<ExtractedFile> files = new ArrayList<>();

        // Pattern for Java files - FIXED: better path extraction
        Pattern javaPattern = Pattern.compile(
                "java\\s*//\\s*(src/main/java/[^\\s]+\\.[a-zA-Z]+)\\s*" +
                        "([\\s\\S]*?})(?=\\s*(?:java\\s*//|<!--|\\z))",
                Pattern.DOTALL
        );

        Matcher matcher = javaPattern.matcher(artifact);

        while (matcher.find()) {
            String filePath = cleanJavaFilePath(matcher.group(1));
            String content = matcher.group(2).trim();

            if (isValidJavaContent(content)) {
                files.add(new ExtractedFile(filePath, content, FileType.JAVA_CLASS));
                System.out.println("Extracted Java file: " + filePath);
            }
        }

        return files;
    }

    private List<ExtractedFile> extractYamlFiles(String artifact) {
        List<ExtractedFile> files = new ArrayList<>();

        // Pattern for YAML files - matches both yaml and yml extensions
        Pattern yamlPattern = Pattern.compile(
                "yaml\\s*#\\s*(src/main/resources/[^\\s]+\\.ya?ml)\\s*" +
                        "([\\s\\S]*?)(?=yaml\\s*#\\s*src/main/|java\\s*//|<!--|\\z)",
                Pattern.DOTALL
        );

        Matcher matcher = yamlPattern.matcher(artifact);

        while (matcher.find()) {
            String filePath = cleanYamlFilePath(matcher.group(1));
            String content = matcher.group(2).trim();

            if (isValidYamlContent(content)) {
                files.add(new ExtractedFile(filePath, content, FileType.YAML_CONFIG));
                System.out.println("Extracted YAML file: " + filePath);
            }
        }

        // Alternative pattern for YAML files without the "yaml" prefix
        Pattern altYamlPattern = Pattern.compile(
                "#\\s*(src/main/resources/[^\\s]+\\.ya?ml)\\s*" +
                        "([\\s\\S]*?)(?=#\\s*src/main/|java\\s*//|<!--|\\z)",
                Pattern.DOTALL
        );

        Matcher altMatcher = altYamlPattern.matcher(artifact);
        while (altMatcher.find()) {
            String filePath = cleanYamlFilePath(altMatcher.group(1));
            String content = altMatcher.group(2).trim();

            if (isValidYamlContent(content) && !isYamlFileAlreadyExtracted(files, filePath)) {
                files.add(new ExtractedFile(filePath, content, FileType.YAML_CONFIG));
                System.out.println("Extracted YAML file (alternative): " + filePath);
            }
        }

        return files;
    }

    private String cleanJavaFilePath(String filePath) {
        // FIXED: Only remove the "java //" prefix, not "java" within the path
        // Remove "java //" from the beginning of the path declaration
        String cleaned = filePath.replaceFirst("^java\\s*//\\s*", "").trim();

        System.out.println("Cleaned Java file path: '" + filePath + "' -> '" + cleaned + "'");
        return cleaned;
    }

    private String cleanYamlFilePath(String filePath) {
        // Remove "yaml #" or "#" prefix from YAML file paths
        String cleaned = filePath.replaceFirst("^yaml\\s*#\\s*", "").replaceFirst("^#\\s*", "").trim();
        System.out.println("Cleaned YAML file path: '" + filePath + "' -> '" + cleaned + "'");
        return cleaned;
    }

    private boolean isYamlFileAlreadyExtracted(List<ExtractedFile> files, String filePath) {
        return files.stream().anyMatch(file -> file.getFilePath().equals(filePath));
    }

    private boolean isValidHtmlContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        return content.contains("<!DOCTYPE") || content.contains("<html");
    }

    private boolean isValidJavaContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        return content.contains("package") || content.contains("class") || content.contains("@");
    }

    private boolean isValidYamlContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        // YAML validation - check for common YAML patterns
        boolean hasYamlStructure = content.contains(":") || content.contains("-");
        boolean hasSpringProperties = content.contains("spring:") || content.contains("server:") || content.contains("logging:");
        boolean hasKeyValuePairs = content.matches(".*\\w+:\\s*.+");

        return hasYamlStructure && (hasSpringProperties || hasKeyValuePairs);
    }

    public void writeFiles(String appPath, List<ExtractedFile> files) throws IOException {
        Path rootPath = Paths.get(appPath);

        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
            System.out.println("Created application root directory: " + rootPath.toAbsolutePath());
        }

        for (ExtractedFile file : files) {
            Path fullPath = rootPath.resolve(file.getFilePath());
            Path parentDir = fullPath.getParent();

            System.out.println("Writing file to: " + fullPath.toAbsolutePath());

            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                System.out.println("Created parent directory: " + parentDir.toAbsolutePath());
            }

            Files.writeString(fullPath, file.getContent());
            System.out.println("Successfully written: " + fullPath.toAbsolutePath() +
                    " (" + file.getFileType() + ", " + file.getContent().length() + " characters)");
        }
    }

    // Enhanced extraction method that handles multiple artifact formats
    public List<ExtractedFile> extractFilesRobust(String artifact) {
        List<ExtractedFile> files = new ArrayList<>();

        // Split artifact by common separators to handle multiple file formats
        String[] sections = artifact.split("---\\s*");

        for (String section : sections) {
            files.addAll(extractFilesFromSection(section));
        }

        // If no files found with section splitting, try the original method
        if (files.isEmpty()) {
            files.addAll(extractFiles(artifact));
        }

        return files;
    }

    private List<ExtractedFile> extractFilesFromSection(String section) {
        List<ExtractedFile> files = new ArrayList<>();

        // Try to extract HTML files from this section
        Pattern htmlPattern = Pattern.compile(
                "<!--\\s*(src/main/resources/templates/[^>]+\\.html)\\s*-->\\s*" +
                        "(<!DOCTYPE[\\s\\S]*?</html>\\s*)",
                Pattern.DOTALL
        );

        Matcher htmlMatcher = htmlPattern.matcher(section);
        while (htmlMatcher.find()) {
            String filePath = htmlMatcher.group(1).trim().replace("<!--", "").replace("-->", "").trim();
            String content = htmlMatcher.group(2).trim();
            if (isValidHtmlContent(content)) {
                files.add(new ExtractedFile(filePath, content, FileType.HTML_TEMPLATE));
            }
        }

        // Try to extract Java files from this section
        Pattern javaPattern = Pattern.compile(
                "java\\s*//\\s*(src/main/java/[^\\n]+\\.java)\\s*" +
                        "([\\s\\S]*?})(?=\\s*(?:java\\s*//|<!--|$))",
                Pattern.DOTALL
        );

        Matcher javaMatcher = javaPattern.matcher(section);
        while (javaMatcher.find()) {
            String filePath = javaMatcher.group(1).trim().replaceFirst("^java\\s*//\\s*", "").trim();
            String content = javaMatcher.group(2).trim();
            if (isValidJavaContent(content)) {
                files.add(new ExtractedFile(filePath, content, FileType.JAVA_CLASS));
            }
        }

        // Try to extract YAML files from this section
        Pattern yamlPattern = Pattern.compile(
                "(?:yaml\\s*)?#\\s*(src/main/resources/[^\\n]+\\.ya?ml)\\s*" +
                        "([\\s\\S]*?)(?=(?:yaml\\s*)?#\\s*src/main/|java\\s*//|<!--|$)",
                Pattern.DOTALL
        );

        Matcher yamlMatcher = yamlPattern.matcher(section);
        while (yamlMatcher.find()) {
            String filePath = yamlMatcher.group(1).trim().replaceFirst("^(?:yaml\\s*)?#\\s*", "").trim();
            String content = yamlMatcher.group(2).trim();
            if (isValidYamlContent(content)) {
                files.add(new ExtractedFile(filePath, content, FileType.YAML_CONFIG));
            }
        }

        return files;
    }
}