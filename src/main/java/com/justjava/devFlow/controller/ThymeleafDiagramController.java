package com.justjava.devFlow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.justjava.devFlow.util.PlantUmlService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/diagrams")
public class ThymeleafDiagramController {

    private final PlantUmlService plantUmlService;

    public ThymeleafDiagramController(PlantUmlService plantUmlService) {
        this.plantUmlService = plantUmlService;
    }

    /**
     * Serve the main Thymeleaf template
     */
    @GetMapping("/load-page")
    public String index(Model model) {
        // Set default PlantUML example
        model.addAttribute("defaultPlantUml", """
            @startuml
            Alice -> Bob: Authentication Request
            Bob --> Alice: Authentication Response
            Alice -> Bob: Another authentication Request
            Alice <-- Bob: Another authentication Response
            @enduml
            """);
        return "/diagram-generator";
    }

    /**
     * HTMX endpoint to generate diagram and return img tag
     * Uses fallback generation with auto-correction
     */
    @PostMapping("/generate-diagram-htmx")
    @ResponseBody
    public String generateDiagramHtmx(@RequestParam("plantUmlSource") String plantUmlSource) {
        try {
            //System.out.println("The plantUmlSource==" + plantUmlSource);

            // Use the enhanced fallback method that auto-corrects if needed
            byte[] pngData = plantUmlService.generatePngWithFallback(plantUmlSource);
            String base64Image = java.util.Base64.getEncoder().encodeToString(pngData);

            return "<img src='data:image/png;base64," + base64Image +
                    "' alt='Generated Diagram' class='img-fluid border rounded' " +
                    "style='max-width: 100%; height: auto;'>";

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            System.err.println("Error generating diagram: " + errorMessage);

            // Try auto-correction as a last resort
            return attemptAutoCorrectionFallback(plantUmlSource, errorMessage);
        }
    }

    /**
     * Enhanced validation with detailed feedback
     */
    @PostMapping("/validate-syntax-htmx")
    @ResponseBody
    public String validateSyntaxHtmx(@RequestParam("plantUmlSource") String plantUmlSource) {
        boolean isValid = plantUmlService.validateSyntax(plantUmlSource);
        String validationMessage = plantUmlService.getValidationMessage(plantUmlSource);

        if (isValid) {
            return "<div class='alert alert-success'>" +
                    "<strong>✓ PlantUML syntax is valid</strong><br>" +
                    "<small>" + validationMessage + "</small>" +
                    "</div>";
        } else {
            return "<div class='alert alert-warning'>" +
                    "<strong>⚠ PlantUML syntax issues detected</strong><br>" +
                    "<small>" + validationMessage + "</small><br>" +
                    "<button class='btn btn-sm btn-outline-primary mt-2' " +
                    "hx-post='/diagrams/auto-correct-htmx' " +
                    "hx-target='#plantUmlSource' " +
                    "hx-include='[name=\"plantUmlSource\"]' " +
                    "hx-indicator='#validation-indicator'>" +
                    "🔄 Auto-Correct Syntax" +
                    "</button>" +
                    "</div>";
        }
    }

    /**
     * Auto-correct endpoint for HTMX
     */
    @PostMapping("/auto-correct-htmx")
    @ResponseBody
    public String autoCorrectPlantUmlHtmx(@RequestParam("plantUmlSource") String plantUmlSource) {
        try {
            String corrected = plantUmlService.autoCorrectPlantUml(plantUmlSource);

            return "<script>" +
                    "document.getElementById('plantUmlSource').value = " +
                    escapeJavaScriptString(corrected) + ";" +
                    "htmx.trigger('#plantUmlSource', 'change');" +
                    "showTempMessage('PlantUML syntax auto-corrected!', 'success');" +
                    "</script>";

        } catch (Exception e) {
            return "<div class='alert alert-danger'>Auto-correction failed: " +
                    e.getMessage() + "</div>";
        }
    }

    /**
     * Enhanced download endpoint with fallback generation
     */
    @PostMapping("/download-diagram-htmx")
    public ResponseEntity<byte[]> downloadDiagramHtmx(@RequestParam("plantUmlSource") String plantUmlSource,
                                                      @RequestParam(value = "filename", required = false) String filename) {
        try {
            System.out.println("Download request - plantUmlSource length: " + plantUmlSource.length());

            // Use fallback generation for downloads too
            byte[] pngData = plantUmlService.generatePngWithFallback(plantUmlSource);

            // Generate filename if not provided
            String actualFilename = generateFilename(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + actualFilename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .header("X-Diagram-Size", String.valueOf(pngData.length))
                    .body(pngData);

        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Download failed: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Enhanced download info with auto-correction option
     */
    @PostMapping("/download-diagram-info")
    @ResponseBody
    public String downloadDiagramInfo(@RequestParam("plantUmlSource") String plantUmlSource) {
        try {
            // Validate first
            boolean isValid = plantUmlService.validateSyntax(plantUmlSource);

            if (!isValid) {
                return "<div class='alert alert-warning'>" +
                        "<strong>⚠ Syntax issues detected</strong><br>" +
                        "<small>Diagram may not generate correctly.</small><br>" +
                        "<div class='mt-2'>" +
                        "<button class='btn btn-sm btn-outline-primary me-2' " +
                        "hx-post='/diagrams/auto-correct-htmx' " +
                        "hx-target='#plantUmlSource' " +
                        "hx-include='[name=\"plantUmlSource\"]'>" +
                        "🔄 Auto-Correct" +
                        "</button>" +
                        "<button class='btn btn-sm btn-success' " +
                        "onclick='downloadDiagram()'>" +
                        "💾 Download Anyway" +
                        "</button>" +
                        "</div>" +
                        "</div>";
            }

            byte[] pngData = plantUmlService.generatePng(plantUmlSource);
            String base64Image = java.util.Base64.getEncoder().encodeToString(pngData);

            return "<div class='alert alert-success'>" +
                    "<strong>✓ Diagram ready for download</strong><br>" +
                    "<small>Size: " + pngData.length + " bytes • Syntax: Valid</small><br>" +
                    "<button class='btn btn-sm btn-outline-primary mt-2' onclick='triggerDownload(\"" +
                    base64Image + "\")'>Click to Download PNG</button>" +
                    "</div>";

        } catch (Exception e) {
            return "<div class='alert alert-danger'>" +
                    "<strong>Download preparation failed</strong><br>" +
                    "<small>" + e.getMessage() + "</small><br>" +
                    "<button class='btn btn-sm btn-outline-warning mt-2' " +
                    "hx-post='/diagrams/auto-correct-htmx' " +
                    "hx-target='#plantUmlSource' " +
                    "hx-include='[name=\"plantUmlSource\"]'>" +
                    "🔄 Try Auto-Correct" +
                    "</button>" +
                    "</div>";
        }
    }

    /**
     * Service health check endpoint
     */
    @GetMapping("/service-health")
    @ResponseBody
    public String serviceHealth() {
        boolean isHealthy = plantUmlService.testPlantUmlGeneration();

        if (isHealthy) {
            return "<div class='alert alert-success'>" +
                    "<strong>✓ PlantUML Service is Healthy</strong><br>" +
                    "<small>Diagram generation is working properly</small>" +
                    "</div>";
        } else {
            return "<div class='alert alert-danger'>" +
                    "<strong>❌ PlantUML Service Issues</strong><br>" +
                    "<small>Diagram generation may not work correctly</small>" +
                    "</div>";
        }
    }

    /**
     * Handle file upload for PlantUML files
     */
    @PostMapping("/upload-plantuml-file")
    @ResponseBody
    public String uploadPlantUmlFile(@RequestParam("plantUmlFile") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return "<div class='alert alert-warning'>Please select a file to upload</div>";
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Validate the uploaded content
            boolean isValid = plantUmlService.validateSyntax(content);
            String validationStatus = isValid ?
                    "<small class='text-success'>✓ Valid syntax</small>" :
                    "<small class='text-warning'>⚠ May need auto-correction</small>";

            // Return the content to be placed in the textarea
            return "<script>" +
                    "document.getElementById('plantUmlSource').value = " +
                    escapeJavaScriptString(content) + ";" +
                    "htmx.trigger('#plantUmlSource', 'change');" +
                    "showTempMessage('File uploaded successfully! " + validationStatus + "', 'success');" +
                    "</script>";

        } catch (Exception e) {
            return "<div class='alert alert-danger'>Error reading file: " +
                    e.getMessage() + "</div>";
        }
    }

    /**
     * Attempt auto-correction as fallback when generation fails
     */
    private String attemptAutoCorrectionFallback(String plantUmlSource, String originalError) {
        try {
            System.out.println("Attempting auto-correction as fallback...");

            String corrected = plantUmlService.autoCorrectPlantUml(plantUmlSource);
            byte[] pngData = plantUmlService.generatePng(corrected);
            String base64Image = java.util.Base64.getEncoder().encodeToString(pngData);

            return "<div class='alert alert-warning'>" +
                    "<strong>⚠ Diagram generated with auto-correction</strong><br>" +
                    "<small>Original error: " + originalError + "</small>" +
                    "</div>" +
                    "<img src='data:image/png;base64," + base64Image +
                    "' alt='Auto-corrected Diagram' class='img-fluid border rounded mt-2' " +
                    "style='max-width: 100%; height: auto;'>" +
                    "<div class='mt-2'>" +
                    "<button class='btn btn-sm btn-outline-secondary' " +
                    "onclick='applyAutoCorrectedCode(" + escapeJavaScriptString(corrected) + ")'>" +
                    "📝 Use Corrected Code" +
                    "</button>" +
                    "</div>";

        } catch (Exception correctionError) {
            return "<div class='alert alert-danger'>" +
                    "<strong>❌ Generation failed even with auto-correction</strong><br>" +
                    "<small>Original error: " + originalError + "</small><br>" +
                    "<small>Correction attempt: " + correctionError.getMessage() + "</small><br>" +
                    "<button class='btn btn-sm btn-outline-warning mt-2' " +
                    "hx-post='/diagrams/auto-correct-htmx' " +
                    "hx-target='#plantUmlSource' " +
                    "hx-include='[name=\"plantUmlSource\"]'>" +
                    "🔄 Try Manual Auto-Correction" +
                    "</button>" +
                    "</div>";
        }
    }

    /**
     * Generate proper filename
     */
    private String generateFilename(String requestedFilename) {
        String filename = requestedFilename != null ? requestedFilename : "diagram";

        // Remove invalid characters
        filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");

        // Ensure .png extension
        if (!filename.toLowerCase().endsWith(".png")) {
            filename += ".png";
        }

        return filename;
    }

    private String escapeJavaScriptString(String input) {
        if (input == null) {
            return "''";
        }
        return "'" + input.replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\"", "\\\"") + "'";
    }
}