package com.justjava.devFlow.delegate;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.common.engine.api.FlowableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.regex.Pattern;

@Component
public class GenerateBPMNDiagram implements JavaDelegate {

    private static final String CORRECT_OMGDI_NAMESPACE = "http://www.omg.org/spec/DD/20100524/DI";
    private static final String INCORRECT_OMGDI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    private static final Pattern INCORRECT_OMGDI_PATTERN =
            Pattern.compile("xmlns:omgdi=\"http://www\\.w3\\.org/2001/XMLSchema-instance\"");
    private static final Pattern WAYPOINT_NAMESPACE_PATTERN =
            Pattern.compile("(<[^>]*?)(xmlns:[^=]*=\"[^\"]*\")?([^>]*?waypoint[^>]*>)");

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) {
        try {
            String bpmnXml = (String) runtimeService.getVariable(execution.getId(), "bpmnDefinition");
            System.out.println("Retrieved BPMN definition, length: " + (bpmnXml != null ? bpmnXml.length() : 0));

            if (bpmnXml == null || bpmnXml.isBlank()) {
                throw new IllegalArgumentException("BPMN XML cannot be null or empty");
            }

            // Clean and normalize XML
            String cleanedBpmnXml = cleanBpmnXml(bpmnXml);

            // Fix common namespace issues
            String fixedBpmnXml = fixNamespaceIssues(cleanedBpmnXml);

            BpmnModel bpmnModel = null;
            Exception conversionError = null;

            // First try with the original/fixed XML
            try {
                bpmnModel = convertXmlToBpmnModel(fixedBpmnXml);
            } catch (Exception e) {
                conversionError = e;
                System.err.println("First conversion attempt failed: " + e.getMessage());

                // If conversion fails, try with additional fixes
                try {
                    String fallbackBpmnXml = applyFallbackFixes(fixedBpmnXml);
                    bpmnModel = convertXmlToBpmnModel(fallbackBpmnXml);
                    System.out.println("Fallback conversion successful");
                } catch (Exception fallbackError) {
                    System.err.println("Fallback conversion also failed: " + fallbackError.getMessage());
                    throw conversionError; // Throw the original error
                }
            }

            if (bpmnModel == null) {
                throw new FlowableException("Failed to convert XML to BPMN model after all attempts");
            }

            // Generate diagram
            String base64Diagram = generateDiagram(bpmnModel);
            String encodedBpmn = Base64.getEncoder().encodeToString(fixedBpmnXml.getBytes(StandardCharsets.UTF_8));

            // Set variables
            execution.setVariable("bpmnDefinition", encodedBpmn);
            execution.setVariable("processDiagramBase64", base64Diagram);

            System.out.println("BPMN diagram generated successfully");

        } catch (Exception e) {
            System.err.println("Error generating BPMN diagram: " + e.getMessage());
            e.printStackTrace();
            throw new FlowableException("Failed to generate BPMN diagram", e);
        }
    }

    private String cleanBpmnXml(String bpmnXml) {
        String cleaned = bpmnXml.trim();
        // Remove BOM if present
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }

    private String fixNamespaceIssues(String bpmnXml) {
        String fixedXml = bpmnXml;

        // Fix incorrect omgdi namespace declaration
        if (fixedXml.contains("xmlns:omgdi=\"" + INCORRECT_OMGDI_NAMESPACE + "\"")) {
            fixedXml = fixedXml.replace(
                    "xmlns:omgdi=\"" + INCORRECT_OMGDI_NAMESPACE + "\"",
                    "xmlns:omgdi=\"" + CORRECT_OMGDI_NAMESPACE + "\""
            );
            System.out.println("Fixed incorrect omgdi namespace declaration");
        }

        // Ensure waypoint elements use correct namespace
        if (fixedXml.contains("<omgdi:waypoint") || fixedXml.contains("</omgdi:waypoint>")) {
            // This is correct, no need to fix
            System.out.println("Waypoint elements using correct omgdi namespace");
        } else if (fixedXml.contains("waypoint") && !fixedXml.contains("omgdi:waypoint")) {
            // If waypoint elements exist but without proper namespace, we need to handle this
            System.out.println("Warning: Waypoint elements may have incorrect namespace formatting");
        }

        return fixedXml;
    }

    private String applyFallbackFixes(String bpmnXml) {
        String fixedXml = bpmnXml;

        // Additional fallback fixes can be applied here if needed
        // For example, ensuring all waypoint elements are properly closed

        // Fix self-closing waypoint elements if they exist
        fixedXml = fixedXml.replaceAll("<omgdi:waypoint([^>/]*)/>", "<omgdi:waypoint$1></omgdi:waypoint>");

        return fixedXml;
    }

    private BpmnModel convertXmlToBpmnModel(String bpmnXml) {
        try {
            BpmnXMLConverter converter = new BpmnXMLConverter();
            byte[] xmlBytes = bpmnXml.getBytes(StandardCharsets.UTF_8);

            return converter.convertToBpmnModel(
                    () -> new ByteArrayInputStream(xmlBytes),
                    true,
                    false
            );
        } catch (Exception e) {
            System.err.println("Error converting XML to BPMN model: " + e.getMessage());
            // Log the first 500 chars of XML for debugging
            System.err.println("XML sample: " + (bpmnXml.length() > 500 ? bpmnXml.substring(0, 500) + "..." : bpmnXml));
            throw e;
        }
    }

    private String generateDiagram(BpmnModel bpmnModel) {
        ProcessDiagramGenerator diagramGenerator = org.flowable.engine.ProcessEngines
                .getDefaultProcessEngine()
                .getProcessEngineConfiguration()
                .getProcessDiagramGenerator();

        try (InputStream diagramStream = diagramGenerator.generateDiagram(
                bpmnModel,
                "png",
                Collections.emptyList(),
                Collections.emptyList(),
                "Arial",
                "Arial",
                "Arial",
                null,
                1.0,
                true
        )) {
            byte[] diagramBytes = diagramStream.readAllBytes();
            return Base64.getEncoder().encodeToString(diagramBytes);
        } catch (Exception e) {
            System.err.println("Error generating diagram: " + e.getMessage());
            throw new FlowableException("Failed to generate process diagram", e);
        }
    }
}