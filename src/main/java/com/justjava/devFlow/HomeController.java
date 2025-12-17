package com.justjava.devFlow;

import com.justjava.devFlow.aau.AuthenticationManager;
import com.justjava.devFlow.model.ProcessInstanceWithVariables;
import jakarta.servlet.http.HttpServletRequest;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        // Fetch active process instances without variables initially
        List<ProcessInstance> processInstances = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("softwareEngineeringProcess")
                .active()
                .list();

        // List of required variable names
        List<String> requiredVariableNames = List.of("projectName", "projectDescription", "clientName", "progress", "dueDate");

        // Create a list to hold the process instances with their variables
        List<ProcessInstanceWithVariables> projects = new ArrayList<>();
        for (ProcessInstance processInstance : processInstances) {
            Map<String, Object> variables = new HashMap<>();
            for (String variableName : requiredVariableNames) {
                Object variableValue = runtimeService.getVariable(processInstance.getId(), variableName);
                if (variableValue != null) {
                    variables.put(variableName, variableValue);
                }
            }
            projects.add(new ProcessInstanceWithVariables(processInstance, variables));
        }

        // Fetch only finished historic process instances (no variables needed)
        long completedProjectCount = historyService
                .createHistoricProcessInstanceQuery()
                .finished()
                .count();
        // Fetch only finished historic tasks (no variables needed)
        long historicTasksCount = historyService
                .createHistoricTaskInstanceQuery()
                .finished()
                .count();

        // Fetch only active tasks (no variables needed)
        long tasksCount = taskService
                .createTaskQuery()
                        .count();

        // Add attributes to the model
        model.addAttribute("projects", projects);
        model.addAttribute("activeTask", tasksCount);
        model.addAttribute("completedTask", historicTasksCount);
        model.addAttribute("completedProject", completedProjectCount);
        model.addAttribute("activeProject", projects.size());

        return "dashboard";
    }
}