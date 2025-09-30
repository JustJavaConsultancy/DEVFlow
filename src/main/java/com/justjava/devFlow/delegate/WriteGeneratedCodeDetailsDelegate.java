package com.justjava.devFlow.delegate;

import com.justjava.devFlow.util.ArtifactFileExtractor;
import com.justjava.devFlow.util.CodeDetailsExtractor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Delegate that extracts generated Thymeleaf and Controller artifacts
 * from process variable "artifact" and writes them into the correct
 * Spring Boot project folders. The project root path is retrieved
 * from process variable "appPath".
 */
@Component
public class WriteGeneratedCodeDetailsDelegate implements JavaDelegate {

    @Autowired
    CodeDetailsExtractor codeDetailsExtractor;
    @Override
    public void execute(DelegateExecution execution) {
        try {
            System.out.println("1 Starting file extraction process.............");

            // Retrieve process variables
            String storyDevelopmentDetail = (String) execution.getVariable("storyDevelopmentDetail");

            System.out.println("2 Starting file extraction process.............");
            String appPath = (String) execution.getVariable("appPath");

            if (storyDevelopmentDetail == null || storyDevelopmentDetail.trim().isEmpty()) {
                throw new IllegalArgumentException("Process variable 'artifact' is null or empty");
            }

            if (appPath == null || appPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Process variable 'appPath' is null or empty");
            }
            List<CodeDetailsExtractor.ExtractedCodeFile> extractedFiles = codeDetailsExtractor.extractAllCodeComponents(storyDevelopmentDetail);
            extractedFiles.forEach(file -> {
                System.out.println("The file path here==="+file.getFilePath());
                System.out.println("The file content here==="+file.getContent());
            });
            codeDetailsExtractor.writeCodeFiles(appPath,extractedFiles);


            System.out.println("Application root path: " + appPath);

            execution.setVariable("filesExtracted", extractedFiles.size());
            execution.setVariable("extractionStatus", "SUCCESS");

            System.out.println("Successfully extracted " + extractedFiles.size() + " files to " + appPath);

        } catch (Exception e) {
            System.out.println("Error during file extraction: " + e.getMessage());
            execution.setVariable("extractionStatus", "FAILED");
            execution.setVariable("errorMessage", e.getMessage());
            throw new RuntimeException("File extraction failed", e);
        }
    }
}