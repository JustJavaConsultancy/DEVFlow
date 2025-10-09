package com.justjava.devFlow.util;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ArtifactFileExtractor {

    private final SpringBootProjectGitHubService projectGitHubService;

    public ArtifactFileExtractor(SpringBootProjectGitHubService projectGitHubService) {
        this.projectGitHubService = projectGitHubService;
    }

    // ======================================================
    // 📦 Model Classes
    // ======================================================

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

    // ======================================================
    // 🚀 Enhanced Unified Extraction Logic
    // ======================================================

    /**
     * Extracts files from artifact text and pushes them to GitHub.
     */
    public GitHubPushResult extractAndPushToGitHub(
            String artifact, String repositoryName, String githubUsername,
            String githubToken, String repositoryDescription, boolean isPrivateRepo)
            throws FileExtractionException, GitHubPushException {

        try {
            System.out.println("🚀 Starting extraction + GitHub push process...");

            List<ExtractedFile> extractedFiles = extractFilesFromUnifiedFormat(artifact);

            if (extractedFiles.isEmpty()) {
                throw new FileExtractionException("No files were extracted from the artifact");
            }

            System.out.println("✅ Extracted " + extractedFiles.size() + " files. Now pushing to GitHub...");

            List<SpringBootProjectGitHubService.GitHubFile> githubFiles = convertToGitHubFiles(extractedFiles);

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
     * Enhanced extraction logic supporting:
     * - **File Path:** `path`
     * - <!-- src/... -->
     * - # src/...
     * - Inline `src/main/...`
     */
    public List<ExtractedFile> extractFilesFromUnifiedFormat(String artifact) {
        List<ExtractedFile> extractedFiles = new ArrayList<>();
        System.out.println("🔍 Scanning artifact for file markers...");

        // Matches various path formats
        Pattern pathPattern = Pattern.compile(
                "(?:\\*\\*File Path:\\*\\*\\s*`([^`]+?\\.\\w+)`|" +    // **File Path:** `...`
                        "<!--\\s*((?:[\\w.-]+/)+[\\w.-]+\\.[a-z]+)\\s*-->|" +  // <!-- src/... -->
                        "#\\s*((?:[\\w.-]+/)+[\\w.-]+\\.[a-z]+)|" +            // # src/...
                        "//\\s*((?:[\\w.-]+/)+[\\w.-]+\\.[a-z]+)|" +           // // src/...
                        "^(?:((?:[\\w.-]+/)+[\\w.-]+\\.[a-z]+)))",             // Plain src/...
                Pattern.MULTILINE
        );

        Matcher matcher = pathPattern.matcher(artifact);
        List<FileMarker> markers = new ArrayList<>();

        while (matcher.find()) {
            String path = Optional.ofNullable(matcher.group(1))
                    .orElse(Optional.ofNullable(matcher.group(2))
                            .orElse(Optional.ofNullable(matcher.group(3))
                                    .orElse(matcher.group(4))));
            if (path != null) {
                markers.add(new FileMarker(path.trim(), matcher.start()));
            }
        }

        for (int i = 0; i < markers.size(); i++) {
            FileMarker current = markers.get(i);
            int start = current.index;
            int end = (i + 1 < markers.size()) ? markers.get(i + 1).index : artifact.length();

            String section = artifact.substring(start, end);
            String content = extractFileContent(section);
            FileType type = determineFileType(current.path);

            if (content != null && !content.isBlank()) {
                extractedFiles.add(new ExtractedFile(current.path, content, type));
                System.out.println("✅ Extracted: " + current.path + " (" + type + ")");
            }
        }

        System.out.println("📦 Total files extracted: " + extractedFiles.size());
        return extractedFiles;
    }

    private record FileMarker(String path, int index) {}

    /**
     * Extract file content (between code fences or directly under marker)
     */
    private String extractFileContent(String section) {
        // Code fence block (```java, ```html, etc.)
        Pattern codeFence = Pattern.compile("(?s)```[a-zA-Z]*\\s*([\\s\\S]+?)```");
        Matcher fenceMatcher = codeFence.matcher(section);
        if (fenceMatcher.find()) {
            return fenceMatcher.group(1).trim();
        }

        // Otherwise extract everything after first newline
        String[] lines = section.split("\\r?\\n", 2);
        if (lines.length > 1) {
            return lines[1].trim();
        }

        return null;
    }

    /**
     * Determine file type based on extension
     */
    private FileType determineFileType(String filePath) {
        if (filePath.endsWith(".java")) return FileType.JAVA_CLASS;
        if (filePath.endsWith(".html")) return FileType.HTML_TEMPLATE;
        if (filePath.endsWith(".yml") || filePath.endsWith(".yaml")) return FileType.YAML_CONFIG;
        return FileType.UNKNOWN;
    }

    // ======================================================
    // 🧩 Helper Converters
    // ======================================================

    private List<SpringBootProjectGitHubService.GitHubFile> convertToGitHubFiles(List<ExtractedFile> extractedFiles) {
        List<SpringBootProjectGitHubService.GitHubFile> githubFiles = new ArrayList<>();
        for (ExtractedFile ef : extractedFiles) {
            githubFiles.add(new SpringBootProjectGitHubService.GitHubFile(ef.getFilePath(), ef.getContent()));
        }
        return githubFiles;
    }

    // ======================================================
    // 📊 Result & Exceptions
    // ======================================================

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

    public static class FileExtractionException extends Exception {
        public FileExtractionException(String message) { super(message); }
        public FileExtractionException(String message, Throwable cause) { super(message, cause); }
    }

    public static class GitHubPushException extends Exception {
        public GitHubPushException(String message) { super(message); }
        public GitHubPushException(String message, Throwable cause) { super(message, cause); }
    }
}
