package com.justjava.devFlow.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ArtifactFileExtractor {

    private final SpringBootProjectGitHubService projectGitHubService;

    public ArtifactFileExtractor(SpringBootProjectGitHubService projectGitHubService) {
        this.projectGitHubService = projectGitHubService;
    }

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

    /**
     * Extracts files from artifact and pushes them to GitHub repository
     */
    public GitHubPushResult extractAndPushToGitHub(String artifact, String repositoryName,
                                                   String githubUsername, String githubToken,
                                                   String repositoryDescription, boolean isPrivateRepo)
            throws FileExtractionException, GitHubPushException {

        try {
            System.out.println("Starting file extraction from artifact and pushing to GitHub...");

            // Extract files from artifact
            List<ExtractedFile> extractedFiles = extractFiles(artifact);

            if (extractedFiles.isEmpty()) {
                throw new FileExtractionException("No files were extracted from the artifact");
            }

            System.out.println("Extracted " + extractedFiles.size() + " files, now pushing to GitHub...");

            // Convert ExtractedFile to GitHubFile and push to GitHub
            List<SpringBootProjectGitHubService.GitHubFile> githubFiles = convertToGitHubFiles(extractedFiles);

            // Use SpringBootProjectGitHubService to push to GitHub
            SpringBootProjectGitHubService.GitHubRepositoryResult result =
                    projectGitHubService.pushFilesToGitHubRepository(
                            githubFiles, repositoryName, repositoryDescription, isPrivateRepo,
                            githubUsername, githubToken
                    );

            return new GitHubPushResult(
                    result.getRepositoryUrl(),
                    result.getRepositoryName(),
                    result.getFilesCount(),
                    extractedFiles.size()
            );

        } catch (SpringBootProjectGitHubService.GitHubPushException e) {
            throw new GitHubPushException("Failed to push extracted files to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new FileExtractionException("Error during file extraction: " + e.getMessage(), e);
        }
    }

    /**
     * Converts internal ExtractedFile to SpringBootProjectGitHubService.GitHubFile
     */
    private List<SpringBootProjectGitHubService.GitHubFile> convertToGitHubFiles(List<ExtractedFile> extractedFiles) {
        List<SpringBootProjectGitHubService.GitHubFile> githubFiles = new ArrayList<>();

        for (ExtractedFile extractedFile : extractedFiles) {
            githubFiles.add(new SpringBootProjectGitHubService.GitHubFile(
                    extractedFile.getFilePath(),
                    extractedFile.getContent()
            ));
        }

        return githubFiles;
    }

    // All your existing extraction methods remain exactly the same
    public List<ExtractedFile> extractFiles(String artifact) {
        List<ExtractedFile> extractedFiles = new ArrayList<>();

        System.out.println("Starting file extraction from artifact...");

        // Extract HTML templates
        extractedFiles.addAll(extractHtmlFiles(artifact));

        // Extract Java classes
        extractedFiles.addAll(extractJavaFiles(artifact));

        // Extract YAML configuration files
        extractedFiles.addAll(extractYamlFiles(artifact));

        // New architecture-doc extraction
        extractedFiles.addAll(extractFromArchitectureDoc(artifact));

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

    /**
     * @deprecated Use extractAndPushToGitHub instead for GitHub integration
     */
    @Deprecated
    public void writeFiles(String appPath, List<ExtractedFile> files) throws IOException {
        System.out.println("Warning: writeFiles method is deprecated. Use extractAndPushToGitHub for GitHub integration.");
        // Keep existing implementation for backward compatibility
        // ... (your existing writeFiles implementation)
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

    // New result class for GitHub push operations
    public static class GitHubPushResult {
        private final String repositoryUrl;
        private final String repositoryName;
        private final int filesPushed;
        private final int filesExtracted;

        public GitHubPushResult(String repositoryUrl, String repositoryName, int filesPushed, int filesExtracted) {
            this.repositoryUrl = repositoryUrl;
            this.repositoryName = repositoryName;
            this.filesPushed = filesPushed;
            this.filesExtracted = filesExtracted;
        }

        public String getRepositoryUrl() { return repositoryUrl; }
        public String getRepositoryName() { return repositoryName; }
        public int getFilesPushed() { return filesPushed; }
        public int getFilesExtracted() { return filesExtracted; }
    }

    // Custom exceptions
    public static class FileExtractionException extends Exception {
        public FileExtractionException(String message) { super(message); }
        public FileExtractionException(String message, Throwable cause) { super(message, cause); }
    }

    public static class GitHubPushException extends Exception {
        public GitHubPushException(String message) { super(message); }
        public GitHubPushException(String message, Throwable cause) { super(message, cause); }
    }




    // New addition to manage Mistral
    // Add inside ArtifactFileExtractor

    /**
     * Extract files from IEEE/ISO/IEC architecture-style Markdown deliverables.
     */
    public List<ExtractedFile> extractFromArchitectureDoc(String artifact) {
        List<ExtractedFile> files = new ArrayList<>();

        // Regex for ### heading with filename in backticks
        Pattern headingPattern = Pattern.compile(
                "###\\s+.*?\\(`([^`]+)`\\)\\s*\\n+([\\s\\S]*?)(?=###|$)",
                Pattern.DOTALL
        );
        Matcher matcher = headingPattern.matcher(artifact);

        while (matcher.find()) {
            String fileName = matcher.group(1).trim();
            String sectionBody = matcher.group(2).trim();

            // Extract fenced code block if present
            Pattern codeBlockPattern = Pattern.compile("(?:(?:java|yaml|html)?\\s*\\n)?([\\s\\S]*)", Pattern.DOTALL);
            Matcher cbMatcher = codeBlockPattern.matcher(sectionBody);
            String content = sectionBody;
            if (cbMatcher.find()) {
                content = cbMatcher.group(1).trim();
            }

            // Map filename to correct project path
            String filePath = mapToProjectPath(fileName);

            // Detect file type
            FileType fileType = detectFileType(fileName);

            if (isValidFileContent(content, fileType)) {
                files.add(new ExtractedFile(filePath, content, fileType));
                System.out.println("Extracted from architecture doc: " + filePath);
            }
        }

        // Also handle "// File: …" or "# File: …" markers
        Pattern fileMarkerPattern = Pattern.compile(
                "(?://|#)\\s*File:\\s*([^\\n]+)\\n([\\s\\S]*?)(?=(?://|#)\\s*File:|###|$)",
                Pattern.DOTALL
        );
        Matcher fm = fileMarkerPattern.matcher(artifact);
        while (fm.find()) {
            String filePath = fm.group(1).trim();
            String content = fm.group(2).trim();
            FileType fileType = detectFileType(filePath);

            if (isValidFileContent(content, fileType)) {
                files.add(new ExtractedFile(filePath, content, fileType));
                System.out.println("Extracted from file marker: " + filePath);
            }
        }

        return files;
    }

    private String mapToProjectPath(String fileName) {
        if (fileName.equalsIgnoreCase("layout.html") || fileName.equalsIgnoreCase("home.html")) {
            return "src/main/resources/templates/" + fileName;
        }
        if (fileName.equalsIgnoreCase("application.yml")) {
            return "src/main/resources/application.yml";
        }
        // fallback: just return fileName
        return fileName;
    }

    private FileType detectFileType(String fileName) {
        if (fileName.endsWith(".html")) return FileType.HTML_TEMPLATE;
        if (fileName.endsWith(".java")) return FileType.JAVA_CLASS;
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return FileType.YAML_CONFIG;
        return FileType.UNKNOWN;
    }

    private boolean isValidFileContent(String content, FileType type) {
        switch (type) {
            case HTML_TEMPLATE: return isValidHtmlContent(content);
            case JAVA_CLASS: return isValidJavaContent(content);
            case YAML_CONFIG: return isValidYamlContent(content);
            default: return content != null && !content.isBlank();
        }
    }

}