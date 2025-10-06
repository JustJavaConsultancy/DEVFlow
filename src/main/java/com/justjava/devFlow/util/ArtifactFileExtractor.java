package com.justjava.devFlow.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    // ==============================================================
    // ✅ NEW LOGIC STARTS HERE — Replaces old multi-pattern chaos
    // ==============================================================

    /**
     * Extracts files from artifact and pushes them to GitHub repository
     */
    public GitHubPushResult extractAndPushToGitHub(String artifact, String repositoryName,
                                                   String githubUsername, String githubToken,
                                                   String repositoryDescription, boolean isPrivateRepo)
            throws FileExtractionException, GitHubPushException {

        try {
            System.out.println("Starting file extraction from artifact and pushing to GitHub...");

            // ✅ Unified extraction logic (new)
            List<ExtractedFile> extractedFiles = extractFilesFromUnifiedFormat(artifact);

            if (extractedFiles.isEmpty()) {
                throw new FileExtractionException("No files were extracted from the artifact");
            }

            System.out.println("Extracted " + extractedFiles.size() + " files, now pushing to GitHub...");

            // Convert ExtractedFile to GitHubFile
            List<SpringBootProjectGitHubService.GitHubFile> githubFiles = convertToGitHubFiles(extractedFiles);

            // Push to GitHub via service
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
     * ✅ Simplified, unified extraction logic based on **File Path:** patterns
     * Works with markdown / AI-generated content.
     */
    public List<ExtractedFile> extractFilesFromUnifiedFormat(String artifact) {
        List<ExtractedFile> extractedFiles = new ArrayList<>();

        System.out.println("Starting unified extraction of files using explicit file path markers...");

        Pattern filePathPattern = Pattern.compile("\\*\\*File Path:\\*\\*\\s*`([^`]+)`", Pattern.MULTILINE);
        Matcher matcher = filePathPattern.matcher(artifact);

        List<Integer> pathIndices = new ArrayList<>();
        List<String> paths = new ArrayList<>();

        while (matcher.find()) {
            paths.add(matcher.group(1).trim());
            pathIndices.add(matcher.start());
        }

        for (int i = 0; i < paths.size(); i++) {
            String currentPath = paths.get(i);
            int startIndex = pathIndices.get(i);
            int endIndex = (i + 1 < pathIndices.size()) ? pathIndices.get(i + 1) : artifact.length();
            String fileSection = artifact.substring(startIndex, endIndex);

            String content = extractFileContent(fileSection);
            FileType fileType = determineFileType(currentPath);

            if (content != null && !content.isEmpty()) {
                extractedFiles.add(new ExtractedFile(currentPath, content, fileType));
                System.out.println("✅ Extracted: " + currentPath + " (" + fileType + ")");
            }
        }

        System.out.println("Total extracted files: " + extractedFiles.size());
        return extractedFiles;
    }

    /**
     * Extracts the actual file content between code fences or text blocks.
     */
    private String extractFileContent(String section) {
        // Common code fences like ```java, ```html, ```yaml
        Pattern codeBlockPattern = Pattern.compile("(?s)```[a-zA-Z]*\\s*([\\s\\S]+?)```");
        Matcher blockMatcher = codeBlockPattern.matcher(section);

        if (blockMatcher.find()) {
            return blockMatcher.group(1).trim();
        }

        // Fallback: try everything after “File Path” if no fences found
        int start = section.indexOf("**File Path:**");
        if (start != -1) {
            return section.substring(start).replaceAll("(?s)^.*?```[a-zA-Z]*\\s*", "")
                    .replaceAll("```$", "").trim();
        }

        return null;
    }

    /**
     * Determine file type by extension
     */
    private FileType determineFileType(String filePath) {
        if (filePath.endsWith(".java")) return FileType.JAVA_CLASS;
        if (filePath.endsWith(".html")) return FileType.HTML_TEMPLATE;
        if (filePath.endsWith(".yml") || filePath.endsWith(".yaml")) return FileType.YAML_CONFIG;
        return FileType.UNKNOWN;
    }

    // ==============================================================
    // ✅ Everything below remains intact from your original bean
    // ==============================================================

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
