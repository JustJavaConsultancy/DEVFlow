package com.justjava.devFlow.delegate;

import com.justjava.devFlow.util.ArtifactFileExtractor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate that extracts generated Thymeleaf and Controller artifacts
 * from process variable "artifact" and pushes them to a GitHub repository.
 * The repository name and GitHub credentials are retrieved from process variables.
 */
@Component
public class WriteGeneratedArtifactsDelegate implements JavaDelegate {

    private final ArtifactFileExtractor artifactFileExtractor;

    public WriteGeneratedArtifactsDelegate(ArtifactFileExtractor artifactFileExtractor) {
        this.artifactFileExtractor = artifactFileExtractor;
    }

    @Override
    public void execute(DelegateExecution execution) {
        try {
            System.out.println("🚀 Starting artifact extraction and GitHub push process...");


            // Retrieve process variables
            String artifact = (String) execution.getVariable("artifact");
            String repositoryName = (String) execution.getVariable("repositoryName");

            if(repositoryName==null){
                repositoryName = String.valueOf(execution.getVariable("projectName"));
                execution.setVariable("repositoryName", repositoryName);
            }
            String githubUsername = (String) execution.getVariable("githubUsername");
            String githubToken = (String) execution.getVariable("githubToken");
            String repositoryDescription = (String) execution.getVariable("repositoryDescription");
            boolean isPrivateRepo = getBooleanVariable(execution, "isPrivateRepo", true);

            // Validate required process variables
            validateProcessVariables(artifact, repositoryName, githubUsername, githubToken);

            System.out.println("📦 Processing artifact for repository: " + repositoryName);
            System.out.println("👤 GitHub user: " + githubUsername);
            System.out.println("📝 Artifact length: " + (artifact != null ? artifact.length() : 0) + " characters");

            // Extract files from artifact and push to GitHub
            ArtifactFileExtractor.GitHubPushResult result =
                    artifactFileExtractor.extractAndPushToGitHub(
                            artifact,
                            repositoryName,
                            githubUsername,
                            githubToken,
                            repositoryDescription != null ? repositoryDescription : "Generated from Flowable artifacts",
                            isPrivateRepo
                    );

            // Debug: Log successful extraction
            System.out.println("✅ Successfully extracted " + result.getFilesExtracted() + " files");
            System.out.println("✅ Successfully pushed " + result.getFilesPushed() + " files to GitHub");
            System.out.println("🌐 Repository URL: " + result.getRepositoryUrl());

            // Set process variables with results
            execution.setVariable("githubRepositoryUrl", result.getRepositoryUrl());
            execution.setVariable("githubRepositoryName", result.getRepositoryName());
            execution.setVariable("filesExtracted", result.getFilesExtracted());
            execution.setVariable("filesPushed", result.getFilesPushed());
            execution.setVariable("extractionStatus", "SUCCESS");
            execution.setVariable("artifactPushComplete", true);

            System.out.println("🎉 Successfully processed artifacts and pushed to GitHub repository: " + result.getRepositoryUrl());

        } catch (ArtifactFileExtractor.FileExtractionException e) {
            handleError(execution, "Failed to extract files from artifact: " + e.getMessage(), e);
        } catch (ArtifactFileExtractor.GitHubPushException e) {
            handleError(execution, "Failed to push files to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            handleError(execution, "Unexpected error during artifact processing: " + e.getMessage(), e);
        }
    }

    /**
     * Validates required process variables
     */
    private void validateProcessVariables(String artifact, String repositoryName,
                                          String githubUsername, String githubToken) {
        if (artifact == null || artifact.trim().isEmpty()) {
            throw new IllegalArgumentException("Process variable 'artifact' is null or empty");
        }

        if (repositoryName == null || repositoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Process variable 'repositoryName' is null or empty");
        }

        if (githubUsername == null || githubUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("Process variable 'githubUsername' is null or empty");
        }

        if (githubToken == null || githubToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Process variable 'githubToken' is null or empty");
        }
    }

    /**
     * Helper method to safely get boolean variables from process execution
     */
    private boolean getBooleanVariable(DelegateExecution execution, String variableName, boolean defaultValue) {
        try {
            Object value = execution.getVariable(variableName);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
            return defaultValue;
        } catch (Exception e) {
            System.out.println("⚠️ Could not read boolean variable '" + variableName + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Helper method to handle errors consistently
     */
    private void handleError(DelegateExecution execution, String errorMessage, Exception e) {
        System.err.println("❌ " + errorMessage);
        if (e != null) {
            e.printStackTrace();
        }

        // Set error state in process variables
        execution.setVariable("extractionStatus", "FAILED");
        execution.setVariable("errorMessage", errorMessage);
        execution.setVariable("artifactPushComplete", false);
        execution.setVariable("githubRepositoryUrl", null);
        execution.setVariable("githubRepositoryName", null);
        execution.setVariable("filesExtracted", 0);
        execution.setVariable("filesPushed", 0);

        throw new RuntimeException(errorMessage, e);
    }

    /**
     * Backward compatibility method - can be used if local filesystem write is needed
     * @deprecated Use GitHub integration instead
     */
    private void processWithLocalFilesystem(DelegateExecution execution) {
        try {
            System.out.println("⚠️ Using local filesystem fallback (deprecated)...");

            String artifact = (String) execution.getVariable("artifact");
            String appPath = (String) execution.getVariable("appPath");

            if (artifact == null || artifact.trim().isEmpty()) {
                throw new IllegalArgumentException("Process variable 'artifact' is null or empty");
            }

            if (appPath == null || appPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Process variable 'appPath' is null or empty");
            }

            // Extract files using the existing method
            var extractedFiles = artifactFileExtractor.extractFiles(artifact);

            // Debug extracted files
            extractedFiles.forEach(file -> {
                System.out.println("📄 Extracted file: " + file.getFilePath());
                System.out.println("📏 Content length: " + file.getContent().length() + " characters");
            });

            // Write to local filesystem (deprecated)
            artifactFileExtractor.writeFiles(appPath, extractedFiles);

            execution.setVariable("filesExtracted", extractedFiles.size());
            execution.setVariable("extractionStatus", "SUCCESS");
            execution.setVariable("appPath", appPath);

            System.out.println("✅ Successfully extracted " + extractedFiles.size() + " files to " + appPath);

        } catch (Exception e) {
            handleError(execution, "Error during local filesystem processing: " + e.getMessage(), e);
        }
    }
}