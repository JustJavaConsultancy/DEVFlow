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
        UNKNOWN
    }

    public List<ExtractedFile> extractFiles(String artifact) {
        List<ExtractedFile> extractedFiles = new ArrayList<>();

        System.out.println("Starting file extraction from artifact...");

        // Extract HTML templates
        extractedFiles.addAll(extractHtmlFiles(artifact));

        // Extract Java classes
        extractedFiles.addAll(extractJavaFiles(artifact));

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

    private String cleanJavaFilePath(String filePath) {
        // FIXED: Only remove the "java //" prefix, not "java" within the path
        // Remove "java //" from the beginning of the path declaration
        String cleaned = filePath.replaceFirst("^java\\s*//\\s*", "").trim();

        System.out.println("Cleaned Java file path: '" + filePath + "' -> '" + cleaned + "'");
        return cleaned;
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
            System.out.println("Successfully written: " + fullPath.toAbsolutePath());
        }
    }
}