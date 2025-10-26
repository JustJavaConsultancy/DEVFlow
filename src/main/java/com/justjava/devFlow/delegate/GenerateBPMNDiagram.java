package com.justjava.devFlow.delegate;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

@Component
public class GenerateBPMNDiagram implements JavaDelegate {
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ProcessEngine processEngine;

    @Override
    public void execute(DelegateExecution execution){
//        String processDefinitionId = execution.getProcessDefinitionId();
        System.out.println("These are the process variables" + runtimeService.getVariables(execution.getId()));
//        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        String bpmnXml = (String) runtimeService.getVariable(execution.getId(), "bpmnDefinition");
        System.out.println("This is the definition of bpmn" + bpmnXml);

        if (bpmnXml == null || bpmnXml.isBlank()) {
            throw new IllegalArgumentException("BPMN XML cannot be null or empty");
        }

        // Clean and normalize XML text
        bpmnXml = bpmnXml.trim();
        if (bpmnXml.startsWith("\uFEFF")) {
            bpmnXml = bpmnXml.substring(1); // remove BOM if present
        }

        // Convert string → InputStream
        byte[] xmlBytes = bpmnXml.getBytes(StandardCharsets.UTF_8);
        InputStreamProvider provider = () -> new ByteArrayInputStream(xmlBytes);

        BpmnXMLConverter converter = new BpmnXMLConverter();
        BpmnModel bpmnModel = converter.convertToBpmnModel(provider, true, false);

        ProcessDiagramGenerator diagramGenerator = processEngine
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
            String base64 = Base64.getEncoder().encodeToString(diagramStream.readAllBytes());
//            System.out.println("This is the base64" + base64);
            String encodeBpmn = Base64.getEncoder().encodeToString(bpmnXml.getBytes(StandardCharsets.UTF_8));
            execution.setVariable("bpmnDefinition", encodeBpmn);
            execution.setVariable("processDiagramBase64", base64);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
