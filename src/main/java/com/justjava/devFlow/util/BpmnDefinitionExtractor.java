package com.justjava.devFlow.util;

import org.springframework.stereotype.Component;

/**
 * Utility for extracting BPMN definitions from generated artifacts
 * and pushing them to GitHub.
 */
@Component
public class BpmnDefinitionExtractor {

    /** Represents the result of a GitHub push operation */
    public static class GitHubPushResult {
        private final String repositoryUrl;
        private final String repositoryName;
        private final int definitionsExtracted;

        public GitHubPushResult(String repositoryUrl, String repositoryName, int definitionsExtracted) {
            this.repositoryUrl = repositoryUrl;
            this.repositoryName = repositoryName;
            this.definitionsExtracted = definitionsExtracted;
        }

        public String getRepositoryUrl() { return repositoryUrl; }
        public String getRepositoryName() { return repositoryName; }
        public int getDefinitionsExtracted() { return definitionsExtracted; }
    }

    /** Exception for file extraction failures */
    public static class FileExtractionException extends Exception {
        public FileExtractionException(String message) { super(message); }
        public FileExtractionException(String message, Throwable cause) { super(message, cause); }
    }

    /** Exception for GitHub push failures */
    public static class GitHubPushException extends Exception {
        public GitHubPushException(String message) { super(message); }
        public GitHubPushException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * Extracts BPMN definitions and pushes them to GitHub.
     *
     * @param bpmnDefinitions the BPMN XML content or compressed artifact
     * @return a GitHubPushResult containing metadata of the push
     */
    public GitHubPushResult extractAndPushToGitHub(
            String bpmnDefinitions,
            String repositoryName,
            String githubUsername,
            String githubToken,
            String repositoryDescription,
            boolean isPrivateRepo
    ) throws FileExtractionException, GitHubPushException {

        if (bpmnDefinitions == null || bpmnDefinitions.trim().isEmpty()) {
            throw new FileExtractionException("BPMN definitions input is empty or null");
        }

        // 🧩 Simulated extraction and GitHub push
        System.out.println("📄 Extracting BPMN definitions...");
        int extractedCount = 1; // in a real impl, this would be the parsed BPMN file count

        // Simulate GitHub push
        System.out.println("📤 Pushing extracted definitions to GitHub repository: " + repositoryName);
        String repoUrl = "https://github.com/" + githubUsername + "/" + repositoryName;

        // return simulated result
        return new GitHubPushResult(repoUrl, repositoryName, extractedCount);
    }
}
