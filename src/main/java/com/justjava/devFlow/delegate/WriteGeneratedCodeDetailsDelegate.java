package com.justjava.devFlow.delegate;

import com.justjava.devFlow.util.CodeDetailsExtractor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate that extracts generated code components from process variable
 * "storyDevelopmentDetail" and pushes them to a GitHub repository.
 * The repository name and GitHub credentials are retrieved from process variables.
 */
@Component
public class WriteGeneratedCodeDetailsDelegate implements JavaDelegate {

    private final CodeDetailsExtractor codeDetailsExtractor;

    public WriteGeneratedCodeDetailsDelegate(CodeDetailsExtractor codeDetailsExtractor) {
        this.codeDetailsExtractor = codeDetailsExtractor;
    }

    @Override
    public void execute(DelegateExecution execution) {
        try {
            System.out.println("🚀 Starting code details extraction and GitHub push process...");

            // Retrieve process variables
            String storyDevelopmentDetail = (String) execution.getVariable("storyDevelopmentDetail");
            String repositoryName = (String) execution.getVariable("repositoryName");
            String githubUsername = (String) execution.getVariable("githubUsername");
            String githubToken = (String) execution.getVariable("githubToken");
            String repositoryDescription = (String) execution.getVariable("repositoryDescription");
            boolean isPrivateRepo = getBooleanVariable(execution, "isPrivateRepo", true);

            // Validate required process variables
            validateProcessVariables(storyDevelopmentDetail, repositoryName, githubUsername, githubToken);

            System.out.println("📦 Processing story development details for repository: " + repositoryName);
            System.out.println("👤 GitHub user: " + githubUsername);
            System.out.println("Story development details length: " +
                    (storyDevelopmentDetail != null ? storyDevelopmentDetail.length() : 0) + " characters");

            // Extract code files from story development details and push to GitHub
            CodeDetailsExtractor.GitHubPushResult result =
                    codeDetailsExtractor.extractAndPushToGitHub(
                            storyDevelopmentDetail,
                            repositoryName,
                            githubUsername,
                            githubToken,
                            repositoryDescription != null ? repositoryDescription : "Code components from story development",
                            isPrivateRepo
                    );

            // Log successful extraction with detailed breakdown
            System.out.println(" Successfully extracted " + result.getFilesExtracted() + " code files");
            System.out.println(" Successfully pushed " + result.getFilesPushed() + " files to GitHub");
            System.out.println(" Repository URL: " + result.getRepositoryUrl());
            System.out.println(" File type breakdown: " + result.getFileTypeBreakdown());

            // Set process variables with comprehensive results
            execution.setVariable("githubRepositoryUrl", result.getRepositoryUrl());
            execution.setVariable("githubRepositoryName", result.getRepositoryName());
            execution.setVariable("filesExtracted", result.getFilesExtracted());
            execution.setVariable("filesPushed", result.getFilesPushed());
            execution.setVariable("extractionStatus", "SUCCESS");
            execution.setVariable("codeDetailsPushComplete", true);

            // Set detailed file type breakdown
            execution.setVariable("javaMainFiles", result.getFileTypeBreakdown().javaMainFiles);
            execution.setVariable("javaTestFiles", result.getFileTypeBreakdown().javaTestFiles);
            execution.setVariable("htmlFiles", result.getFileTypeBreakdown().htmlFiles);
            execution.setVariable("sqlFiles", result.getFileTypeBreakdown().sqlFiles);
            execution.setVariable("yamlFiles", result.getFileTypeBreakdown().yamlFiles);
            execution.setVariable("otherFiles", result.getFileTypeBreakdown().otherFiles);

            System.out.println("🎉 Successfully processed code details and pushed to GitHub repository: " + result.getRepositoryUrl());

        } catch (CodeDetailsExtractor.CodeExtractionException e) {
            handleError(execution, "Failed to extract code files from story development details: " + e.getMessage(), e);
        } catch (CodeDetailsExtractor.GitHubPushException e) {
            handleError(execution, "Failed to push code files to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            handleError(execution, "Unexpected error during code details processing: " + e.getMessage(), e);
        }
    }

    /**
     * Validates required process variables
     */
    private void validateProcessVariables(String storyDevelopmentDetail, String repositoryName,
                                          String githubUsername, String githubToken) {
        if (storyDevelopmentDetail == null || storyDevelopmentDetail.trim().isEmpty()) {
            throw new IllegalArgumentException("Process variable 'storyDevelopmentDetail' is null or empty");
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
        execution.setVariable("codeDetailsPushComplete", false);
        execution.setVariable("githubRepositoryUrl", null);
        execution.setVariable("githubRepositoryName", null);
        execution.setVariable("filesExtracted", 0);
        execution.setVariable("filesPushed", 0);

        // Reset file type breakdown variables
        execution.setVariable("javaMainFiles", 0);
        execution.setVariable("javaTestFiles", 0);
        execution.setVariable("htmlFiles", 0);
        execution.setVariable("sqlFiles", 0);
        execution.setVariable("yamlFiles", 0);
        execution.setVariable("otherFiles", 0);

        throw new RuntimeException(errorMessage, e);
    }

    /**
     * Backward compatibility method - can be used if local filesystem write is needed
     * @deprecated Use GitHub integration instead
     */
    private void processWithLocalFilesystem(DelegateExecution execution) {
        try {
            System.out.println("⚠️ Using local filesystem fallback for code details (deprecated)...");

            String storyDevelopmentDetail = (String) execution.getVariable("storyDevelopmentDetail");
            String appPath = (String) execution.getVariable("appPath");

            if (storyDevelopmentDetail == null || storyDevelopmentDetail.trim().isEmpty()) {
                throw new IllegalArgumentException("Process variable 'storyDevelopmentDetail' is null or empty");
            }

            if (appPath == null || appPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Process variable 'appPath' is null or empty");
            }

            // Extract files using the existing method
            var extractedFiles = codeDetailsExtractor.extractAllCodeComponents(storyDevelopmentDetail);

            // Debug extracted files
            extractedFiles.forEach(file -> {
                System.out.println("📄 Extracted code file: " + file.getFilePath());
                System.out.println("📏 Content length: " + file.getContent().length() + " characters");
                System.out.println("🔧 File type: " + file.getFileType());
            });

            // Write to local filesystem (deprecated)
            codeDetailsExtractor.writeCodeFiles(appPath, extractedFiles);

            execution.setVariable("filesExtracted", extractedFiles.size());
            execution.setVariable("extractionStatus", "SUCCESS");
            execution.setVariable("appPath", appPath);

            System.out.println("✅ Successfully extracted " + extractedFiles.size() + " code files to " + appPath);

        } catch (Exception e) {
            handleError(execution, "Error during local filesystem processing of code details: " + e.getMessage(), e);
        }
    }
}