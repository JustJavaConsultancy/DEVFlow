package com.justjava.devFlow.model;

import org.flowable.engine.runtime.ProcessInstance;

import java.util.Date;
import java.util.Map;

public class ProcessInstanceWithVariables implements ProcessInstance {
    private final ProcessInstance processInstance;
    private final Map<String, Object> processVariables;

    public ProcessInstanceWithVariables(ProcessInstance processInstance, Map<String, Object> processVariables) {
        this.processInstance = processInstance;
        this.processVariables = processVariables;
    }

    @Override
    public String getId() {
        return processInstance.getId();
    }


    @Override
    public String getProcessDefinitionId() {
        return processInstance.getProcessDefinitionId();
    }

    @Override
    public String getBusinessKey() {
        return processInstance.getBusinessKey();
    }

    @Override
    public String getBusinessStatus() {
        return null;
    }

    @Override
    public String getProcessDefinitionKey() {
        return processInstance.getProcessDefinitionKey();
    }

    @Override
    public String getProcessDefinitionName() {
        return processInstance.getProcessDefinitionName();
    }

    @Override
    public Integer getProcessDefinitionVersion() {
        return processInstance.getProcessDefinitionVersion();
    }

    @Override
    public String getProcessDefinitionCategory() {
        return null;
    }

    @Override
    public String getDeploymentId() {
        return processInstance.getDeploymentId();
    }

    @Override
    public Date getStartTime() {
        return processInstance.getStartTime();
    }



    @Override
    public boolean isSuspended() {
        return processInstance.isSuspended();
    }

    @Override
    public boolean isEnded() {
        return false;
    }

    @Override
    public String getActivityId() {
        return null;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstance.getProcessInstanceId();
    }

    @Override
    public String getParentId() {
        return processInstance.getParentId();
    }

    @Override
    public String getSuperExecutionId() {
        return processInstance.getSuperExecutionId();
    }

    @Override
    public String getRootProcessInstanceId() {
        return processInstance.getRootProcessInstanceId();
    }



    @Override
    public String getTenantId() {
        return processInstance.getTenantId();
    }

    @Override
    public String getName() {
        return processInstance.getName();
    }

    @Override
    public String getDescription() {
        return processInstance.getDescription();
    }

    @Override
    public String getReferenceId() {
        return processInstance.getReferenceId();
    }

    @Override
    public String getReferenceType() {
        return processInstance.getReferenceType();
    }

    @Override
    public String getPropagatedStageInstanceId() {
        return processInstance.getPropagatedStageInstanceId();
    }

    @Override
    public String getLocalizedName() {
        return processInstance.getLocalizedName();
    }

    @Override
    public String getLocalizedDescription() {
        return processInstance.getLocalizedDescription();
    }

    @Override
    public String getStartUserId() {
        return processInstance.getStartUserId();
    }

    @Override
    public String getCallbackId() {
        return processInstance.getCallbackId();
    }

    @Override
    public String getCallbackType() {
        return processInstance.getCallbackType();
    }


    @Override
    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

}