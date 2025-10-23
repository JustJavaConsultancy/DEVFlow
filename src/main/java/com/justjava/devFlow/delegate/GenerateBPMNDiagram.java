package com.justjava.devFlow.delegate;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
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
        String processDefinitionId = execution.getProcessDefinitionId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

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
            execution.setVariable("processDiagramBase64", base64);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
