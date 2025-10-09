package com.justjava.devFlow.delegate;

import com.justjava.devFlow.util.BpmnDefinitionExtractor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate that extracts generated BPMN definitions
 * from process variable "bpmnDefinitions" and pushes them to a GitHub repository.
 */
@Component
public class WriteGeneratedBpmnDefinitionsDelegate implements JavaDelegate {

    private final BpmnDefinitionExtractor bpmnDefinitionExtractor;

    public WriteGeneratedBpmnDefinitionsDelegate(BpmnDefinitionExtractor bpmnDefinitionExtractor) {
        this.bpmnDefinitionExtractor = bpmnDefinitionExtractor;
    }
    @Override
    public void execute(DelegateExecution execution) {
        try {
            System.out.println("🚀 Starting BPMN definition extraction and GitHub push process...");

            // Retrieve process variables
            String bpmnDefinitions = (String) execution.getVariable("artifact");
            String repositoryName = (String) execution.getVariable("repositoryName");
            if (repositoryName == null) {
                repositoryName = String.valueOf(execution.getVariable("projectName"));
                execution.setVariable("repositoryName", repositoryName);
            }

            String githubUsername = (String) execution.getVariable("githubUsername");
            String githubToken = (String) execution.getVariable("githubToken");
            String repositoryDescription = (String) execution.getVariable("repositoryDescription");
            boolean isPrivateRepo = getBooleanVariable(execution, "isPrivateRepo", true);

            // Validate required variables
            validateProcessVariables(bpmnDefinitions, repositoryName, githubUsername, githubToken);

            System.out.println("📦 Processing BPMN definitions for repository: " + repositoryName);
            System.out.println("👤 GitHub user: " + githubUsername);
            //System.out.println("📝 BPMN definition length: " + (bpmnDefinitions != null ? bpmnDefinitions.length() : 0) + " characters");

            // Extract BPMN definitions and push to GitHub
            BpmnDefinitionExtractor.GitHubPushResult result =
                    bpmnDefinitionExtractor.extractAndPushToGitHub(
                            bpmnDefinitions,
                            repositoryName,
                            githubUsername,
                            githubToken,
                            repositoryDescription != null ? repositoryDescription : "Generated BPMN definitions from Flowable",
                            isPrivateRepo
                    );

            System.out.println("✅ Successfully extracted " + result.getDefinitionsExtracted() + " BPMN definitions");
            System.out.println("🌐 Repository URL: " + result.getRepositoryUrl());

            // Update process variables
            execution.setVariable("githubRepositoryUrl", result.getRepositoryUrl());
            execution.setVariable("githubRepositoryName", result.getRepositoryName());
            execution.setVariable("definitionsExtracted", result.getDefinitionsExtracted());
            execution.setVariable("bpmnPushStatus", "SUCCESS");
            execution.setVariable("bpmnPushComplete", true);

            System.out.println("🎉 BPMN definitions successfully pushed to GitHub: " + result.getRepositoryUrl());

        } catch (BpmnDefinitionExtractor.FileExtractionException e) {
            handleError(execution, "Failed to extract BPMN definitions: " + e.getMessage(), e);
        } catch (BpmnDefinitionExtractor.GitHubPushException e) {
            handleError(execution, "Failed to push BPMN definitions to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            handleError(execution, "Unexpected error during BPMN definition processing: " + e.getMessage(), e);
        }
    }

    private void validateProcessVariables(String bpmnDefinitions, String repositoryName,
                                          String githubUsername, String githubToken) {
        if (bpmnDefinitions == null || bpmnDefinitions.trim().isEmpty())
            throw new IllegalArgumentException("Process variable 'bpmnDefinitions' is null or empty");

        if (repositoryName == null || repositoryName.trim().isEmpty())
            throw new IllegalArgumentException("Process variable 'repositoryName' is null or empty");

        if (githubUsername == null || githubUsername.trim().isEmpty())
            throw new IllegalArgumentException("Process variable 'githubUsername' is null or empty");

        if (githubToken == null || githubToken.trim().isEmpty())
            throw new IllegalArgumentException("Process variable 'githubToken' is null or empty");
    }

    private boolean getBooleanVariable(DelegateExecution execution, String variableName, boolean defaultValue) {
        try {
            Object value = execution.getVariable(variableName);
            if (value instanceof Boolean) return (Boolean) value;
            if (value instanceof String) return Boolean.parseBoolean((String) value);
            return defaultValue;
        } catch (Exception e) {
            System.out.println("⚠️ Could not read boolean variable '" + variableName + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    private void handleError(DelegateExecution execution, String errorMessage, Exception e) {
        System.err.println("❌ " + errorMessage);
        if (e != null) e.printStackTrace();

        execution.setVariable("bpmnPushStatus", "FAILED");
        execution.setVariable("bpmnPushComplete", false);
        execution.setVariable("githubRepositoryUrl", null);
        execution.setVariable("githubRepositoryName", null);
        execution.setVariable("definitionsExtracted", 0);
        execution.setVariable("errorMessage", errorMessage);

        throw new RuntimeException(errorMessage, e);
    }
}
