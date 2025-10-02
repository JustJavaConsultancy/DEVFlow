package com.justjava.devFlow.delegate;

import com.justjava.devFlow.util.SpringBootProjectGitHubService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("downloadSpringProjectDelegate")
public class DownloadSpringProjectDelegate implements JavaDelegate {

    private final SpringBootProjectGitHubService projectGitHubService;

    public DownloadSpringProjectDelegate(SpringBootProjectGitHubService projectGitHubService) {
        this.projectGitHubService = projectGitHubService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        // ✅ Get project parameters from process variables
        String groupId = (String) execution.getVariable("groupId");
        String artifactId = (String) execution.getVariable("artifactId");
        String javaVersion = (String) execution.getVariable("javaVersion");
        String springBootVersion = (String) execution.getVariable("springBootVersion");
        String dependencies = (String) execution.getVariable("dependencies");

        // ✅ Get GitHub configuration from process variables
        String githubUsername = (String) execution.getVariable("githubUsername");
        String githubToken = (String) execution.getVariable("githubToken");
        String repositoryDescription = (String) execution.getVariable("repositoryDescription");
        boolean isPrivateRepo = getBooleanVariable(execution, "isPrivateRepo", true);

        try {
            // ✅ Use SpringBootProjectGitHubService to download and push to GitHub
            SpringBootProjectGitHubService.GitHubRepositoryResult result =
                    projectGitHubService.downloadAndPushToGitHub(
                            groupId,
                            artifactId,
                            javaVersion,
                            springBootVersion,
                            dependencies,
                            githubUsername,
                            githubToken,
                            repositoryDescription != null ? repositoryDescription : "Spring Boot project: " + artifactId,
                            isPrivateRepo
                    );

            // ✅ Set process variables with GitHub results
            execution.setVariable("githubRepositoryUrl", result.getRepositoryUrl());
            execution.setVariable("githubRepositoryName", result.getRepositoryName());
            execution.setVariable("filesExtracted", result.getFilesCount());
            execution.setVariable("downloadStatus", "SUCCESS");
            execution.setVariable("projectGenerated", true);

            System.out.println("✅ Spring Boot project successfully created and pushed to GitHub: " + result.getRepositoryUrl());
            System.out.println("✅ Repository: " + result.getRepositoryName());
            System.out.println("✅ Files processed: " + result.getFilesCount());

        } catch (SpringBootProjectGitHubService.ProjectDownloadException e) {
            handleError(execution, "Failed to download Spring Boot project: " + e.getMessage(), e);
        } catch (SpringBootProjectGitHubService.GitHubPushException e) {
            handleError(execution, "Failed to push project to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            handleError(execution, "Unexpected error during project generation: " + e.getMessage(), e);
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

        execution.setVariable("downloadStatus", "FAILED");
        execution.setVariable("errorMessage", errorMessage);
        execution.setVariable("projectGenerated", false);
        execution.setVariable("githubRepositoryUrl", null);
        execution.setVariable("githubRepositoryName", null);
        execution.setVariable("filesExtracted", 0);

        throw new RuntimeException(errorMessage, e);
    }
}