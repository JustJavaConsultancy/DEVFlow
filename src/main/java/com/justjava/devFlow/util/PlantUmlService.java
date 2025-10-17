package com.justjava.devFlow.util;

import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.core.DiagramDescription;
import net.sourceforge.plantuml.error.PSystemError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlantUmlService {

    private static final Logger logger = LoggerFactory.getLogger(PlantUmlService.class);

    // Patterns to detect common syntax errors
    private static final Pattern INVALID_COMPONENT_SYNTAX = Pattern.compile("\\[([^]]+)\\]\\s*(-->?|\\\\.\\\\.>)\\s*\\[([^]]+)\\]", Pattern.MULTILINE);
    private static final Pattern COMPONENT_NAME_EXTRACTION = Pattern.compile("\\[([^]]+)\\]", Pattern.MULTILINE);
    private static final Pattern ARROW_LINES = Pattern.compile(".*(-->?|\\\\.\\\\.>).*", Pattern.MULTILINE);
    private static final Pattern VALID_DIAGRAM_TYPES = Pattern.compile(
            "@startuml\\s*(start|class|component|usecase|sequence|activity|state|object|deployment)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Generate PNG image from PlantUML source
     *
     * @param plantUmlSource The PlantUML diagram source code
     * @return PNG image as byte array
     * @throws IOException              If diagram generation fails
     * @throws IllegalArgumentException If source is invalid
     */
    public byte[] generatePng(String plantUmlSource) throws IOException {
        // Validate input
        if (plantUmlSource == null || plantUmlSource.trim().isEmpty()) {
            throw new IllegalArgumentException("PlantUML source cannot be null or empty");
        }

        // Ensure @startuml and @enduml tags are present
        String processedSource = processPlantUmlSource(plantUmlSource);

        logger.debug("Generating PNG from PlantUML source: {} characters", processedSource.length());

        SourceStringReader reader = new SourceStringReader(processedSource);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Generate the PNG image and get the result description
            DiagramDescription result = reader.outputImage(outputStream);

            byte[] pngData = outputStream.toByteArray();

            if (pngData.length == 0) {
                String errorInfo = "Unknown error - no PNG data generated";
                if (result != null) {
                    errorInfo = result.getDescription();
                }
                throw new IOException("Generated PNG is empty - diagram generation may have failed. Result: " + errorInfo);
            }

            // Check if the result indicates an error
            if (result != null && (result.getDescription().contains("ERROR") || result.getDescription().contains("(Error)"))) {
                throw new IOException("PlantUML generation resulted in error: " + result.getDescription());
            }

            logger.info("Successfully generated PNG: {}, Size: {} bytes",
                    result != null ? result.getDescription() : "Unknown",
                    pngData.length);
            return pngData;

        } catch (IOException e) {
            logger.error("Failed to generate PNG from PlantUML: {}", e.getMessage());
            throw new IOException("PlantUML diagram generation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during PNG generation: {}", e.getMessage());
            throw new IOException("Unexpected error during diagram generation: " + e.getMessage(), e);
        }
    }

    /**
     * Auto-correct common PlantUML syntax issues and return valid PlantUML
     * This method can fix issues like the problematic example provided
     */
    public String autoCorrectPlantUml(String plantUmlSource) {
        if (plantUmlSource == null || plantUmlSource.trim().isEmpty()) {
            return "@startuml\n@enduml";
        }

        logger.info("Attempting to auto-correct PlantUML syntax");

        // Process basic structure first
        String processed = processPlantUmlSource(plantUmlSource);

        // Extract content between @startuml and @enduml
        String content = extractContent(processed);

        // Detect diagram type and apply appropriate corrections
        String diagramType = detectDiagramType(content);

        String correctedContent = applyCorrections(content, diagramType);

        // Rebuild the complete PlantUML
        String result = "@startuml\n" + correctedContent + "\n@enduml";

        // Validate the corrected result
        if (validateSyntax(result)) {
            logger.info("Successfully auto-corrected PlantUML syntax");
            return result;
        } else {
            logger.warn("Auto-correction attempted but result may still contain issues");
            return result; // Return the best attempt
        }
    }

    /**
     * Extract content between @startuml and @enduml
     */
    private String extractContent(String processedSource) {
        return processedSource.replaceAll("(?s).*@startuml\\s*", "")
                .replaceAll("(?s)@enduml.*", "")
                .trim();
    }

    /**
     * Detect the type of diagram based on content
     */
    private String detectDiagramType(String content) {
        if (content.contains("->") || content.contains("-->")) {
            return "sequence";
        } else if (content.contains("class") || content.matches(".*class\\s+\\w+.*")) {
            return "class";
        } else if (content.contains("component") || content.matches(".*\\[.*\\].*")) {
            return "component";
        } else if (content.contains("usecase") || content.contains("actor")) {
            return "usecase";
        } else if (content.contains("start") || content.contains(":") && content.contains("if")) {
            return "activity";
        } else {
            return "component"; // Default to component diagram
        }
    }

    /**
     * Apply appropriate corrections based on diagram type
     */
    private String applyCorrections(String content, String diagramType) {
        switch (diagramType) {
            case "component":
                return correctComponentDiagram(content);
            case "sequence":
                return correctSequenceDiagram(content);
            case "class":
                return correctClassDiagram(content);
            default:
                return correctGenericDiagram(content);
        }
    }

    /**
     * Correct component diagram syntax issues
     */
    private String correctComponentDiagram(String content) {
        StringBuilder corrected = new StringBuilder();
        Set<String> declaredComponents = new LinkedHashSet<>();
        List<String> relationships = new ArrayList<>();

        // First pass: extract all components and relationships
        String[] lines = content.split("\n");

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            // Check for [Component] --> [Another] pattern
            Matcher invalidMatcher = INVALID_COMPONENT_SYNTAX.matcher(trimmedLine);
            if (invalidMatcher.find()) {
                String fromComponent = invalidMatcher.group(1).trim();
                String arrow = invalidMatcher.group(2);
                String toComponent = invalidMatcher.group(3).trim();

                // Add components to declared set
                declaredComponents.add(fromComponent);
                declaredComponents.add(toComponent);

                // Store relationship
                relationships.add(getComponentAlias(fromComponent) + " " + arrow + " " + getComponentAlias(toComponent));
            } else {
                // Check if it's a standalone component reference
                Matcher componentMatcher = COMPONENT_NAME_EXTRACTION.matcher(trimmedLine);
                while (componentMatcher.find()) {
                    declaredComponents.add(componentMatcher.group(1).trim());
                }

                // Check for arrow lines without proper components
                if (ARROW_LINES.matcher(trimmedLine).find() && !invalidMatcher.find()) {
                    relationships.add(trimmedLine);
                } else if (!trimmedLine.contains("-->") && !trimmedLine.contains("..>")) {
                    // Assume it's a component declaration or other valid line
                    corrected.append(trimmedLine).append("\n");
                }
            }
        }

        // Add component declarations
        for (String component : declaredComponents) {
            if (!component.isEmpty()) {
                corrected.append("component \"").append(component).append("\" as ").append(getComponentAlias(component)).append("\n");
            }
        }

        corrected.append("\n");

        // Add relationships
        for (String relationship : relationships) {
            corrected.append(relationship).append("\n");
        }

        // Add any remaining lines that weren't processed
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty() &&
                    !INVALID_COMPONENT_SYNTAX.matcher(trimmedLine).find() &&
                    !COMPONENT_NAME_EXTRACTION.matcher(trimmedLine).find() &&
                    !ARROW_LINES.matcher(trimmedLine).find()) {
                // This line wasn't processed yet, add it
                if (!corrected.toString().contains(trimmedLine)) {
                    corrected.append(trimmedLine).append("\n");
                }
            }
        }

        return corrected.toString().trim();
    }

    /**
     * Generate a safe alias for a component name
     */
    private String getComponentAlias(String componentName) {
        // Remove special characters and spaces, convert to camelCase
        return componentName.replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase();
    }

    /**
     * Correct sequence diagram syntax issues
     */
    private String correctSequenceDiagram(String content) {
        // Basic sequence diagram corrections
        String corrected = content.replaceAll("\\[([^]]+)\\]\\s*-->?\\s*\\[([^]]+)\\]",
                "participant \"$1\" as $1\nparticipant \"$2\" as $2\n$1 -> $2: message");

        return corrected;
    }

    /**
     * Correct class diagram syntax issues
     */
    private String correctClassDiagram(String content) {
        // Ensure proper class declarations
        return content;
    }

    /**
     * Correct generic diagram syntax issues
     */
    private String correctGenericDiagram(String content) {
        // Apply general corrections
        StringBuilder corrected = new StringBuilder();
        String[] lines = content.split("\n");

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            // Fix common arrow syntax issues
            String fixedLine = trimmedLine
                    .replaceAll("-->", " --> ")
                    .replaceAll("->", " -> ")
                    .replaceAll("\\.\\.>", " ..> ")
                    .replaceAll("\\s+", " ") // Normalize spaces
                    .trim();

            corrected.append(fixedLine).append("\n");
        }

        return corrected.toString().trim();
    }

    /**
     * Process PlantUML source to ensure it has proper @startuml and @enduml tags
     */
    private String processPlantUmlSource(String source) {
        if (source == null) {
            return "@startuml\n@enduml";
        }

        String trimmed = source.trim();

        if (trimmed.isEmpty()) {
            return "@startuml\n@enduml";
        }

        // If already properly formatted, return as-is
        if (trimmed.startsWith("@startuml") && trimmed.contains("@enduml")) {
            return trimmed;
        }

        // Add @startuml and @enduml if missing
        StringBuilder processed = new StringBuilder();
        if (!trimmed.startsWith("@startuml")) {
            processed.append("@startuml\n");
        }

        processed.append(trimmed);

        if (!trimmed.contains("@enduml")) {
            processed.append("\n@enduml");
        }

        return processed.toString();
    }

    /**
     * Enhanced PlantUML syntax validation
     * Returns false for invalid syntax like the provided example
     */
    public boolean validateSyntax(String plantUmlSource) {
        if (plantUmlSource == null || plantUmlSource.trim().isEmpty()) {
            logger.warn("Validation failed: PlantUML source is null or empty");
            return false;
        }

        // Try to parse with PlantUML engine
        return validateWithPlantUmlEngine(plantUmlSource);
    }

    /**
     * Validate using PlantUML's internal parsing
     */
    private boolean validateWithPlantUmlEngine(String plantUmlSource) {
        try {
            String processedSource = processPlantUmlSource(plantUmlSource);
            SourceStringReader reader = new SourceStringReader(processedSource);

            // Try to generate but discard result - just check for syntax errors
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                DiagramDescription result = reader.outputImage(os);

                // Check if generation resulted in an error or null result
                if (result == null || result.getDescription().contains("ERROR") ||
                        result.getDescription().contains("(Error)") ||
                        result.getDescription().contains("No diagram found")) {
                    logger.warn("PlantUML generation error: {}", result != null ? result.getDescription() : "Null result");
                    return false;
                }

                return true;
            }

        } catch (Exception e) {
            logger.warn("PlantUML validation failed with exception: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Quick pre-validation check before attempting generation
     */
    private boolean isPotentiallyValidPlantUml(String plantUmlSource) {
        if (plantUmlSource == null || plantUmlSource.trim().isEmpty()) {
            return false;
        }

        // Must contain @startuml (case insensitive)
        if (!plantUmlSource.toLowerCase().contains("@startuml")) {
            return false;
        }

        // Should contain some PlantUML keywords or patterns
        return plantUmlSource.matches("(?s).*(->|-->|class |component |actor |participant |usecase |:).*");
    }

    /**
     * Get detailed validation message
     */
    public String getValidationMessage(String plantUmlSource) {
        if (validateSyntax(plantUmlSource)) {
            return "PlantUML syntax is valid";
        } else {
            return "PlantUML syntax contains errors - check component declarations and arrow syntax";
        }
    }

    /**
     * Safe generation method that attempts auto-correction if initial generation fails
     */
    public byte[] generatePngWithFallback(String plantUmlSource) throws IOException {
        try {
            // First try direct generation
            return generatePng(plantUmlSource);
        } catch (Exception e) {
            logger.warn("Direct generation failed, attempting auto-correction: {}", e.getMessage());

            // Try auto-correction
            String corrected = autoCorrectPlantUml(plantUmlSource);
            return generatePng(corrected);
        }
    }

    /**
     * Simple test method to verify basic PlantUML functionality
     */
    public boolean testPlantUmlGeneration() {
        try {
            String testPlantUml = "@startuml\nAlice -> Bob: Hello\nBob --> Alice: Hi!\n@enduml";
            byte[] result = generatePng(testPlantUml);
            return result != null && result.length > 0;
        } catch (Exception e) {
            logger.error("PlantUML test failed: {}", e.getMessage());
            return false;
        }
    }
}