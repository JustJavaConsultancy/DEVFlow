package com.justjava.devFlow.tasks;

import org.flowable.engine.TaskService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/tasks")
public class FlowableTaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private FormService formService;
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    // Get all tasks for a user
    @GetMapping("/user/{userId}")
    public String getUserTasks(@PathVariable String userId, Model model) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc()
                .list();

        model.addAttribute("tasks", tasks);
        model.addAttribute("userId", userId);
        return "task/task-list";
    }
    @GetMapping
    public String getAllTasks( Model model) {
        List<Task> tasks = taskService
                .createTaskQuery()
                .includeProcessVariables()
                .orderByTaskCreateTime().desc()
                .list();

        tasks.forEach(task -> {
            //System.out.println(" The task process variables here==="+task.getProcessVariables());
        });
        model.addAttribute("tasks", tasks);
        //model.addAttribute("userId", userId);
        return "task/task-list";
    }

    // Get all candidate tasks for a user
    @GetMapping("/candidate/{userId}")
    public String getCandidateTasks(@PathVariable String userId, Model model) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateUser(userId)
                .orderByTaskCreateTime().desc()
                .list();

        model.addAttribute("tasks", tasks);
        model.addAttribute("userId", userId);
        return "task/candidate-task-list";
    }

    // View task form
    @GetMapping("/view/{taskId}")
    public String viewTaskForm(@PathVariable String taskId, Model model) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found with id: " + taskId);
        }

        String currentTask;
        switch (task.getTaskDefinitionKey().toLowerCase()) {
            case "requirement_elicitation":
                currentTask = "tasks/requirement";
                break;
            case "formtask_12":
                currentTask = "tasks/reviewSrs";
                break;
            case "formtask_43":
                currentTask = "tasks/reviewUserStories";
                break;
            case "formtask_11":
                currentTask = "tasks/reviewSolutionArchitecture";
                break;
            case "deployment":
                currentTask = "tasks/deployment";
                break;
            case "formtask_4":
                currentTask = "tasks/UAT";
                break;
            default:
                currentTask = "tasks/default";
                break;
        }

        // Load task variables
        Map<String, Object> taskVariables = taskService.getVariables(taskId);

        // Load process variables
        Map<String, Object> processVariables = runtimeService.getVariables(task.getProcessInstanceId(),List.of(taskId));

/*        System.out.println(" The Task Id==="+task.getId()+"\n\n\n\n\n\n\n\n " + " The whole process variables===="+
                runtimeService.getVariables(task.getExecutionId()));
        System.out.println("This is the process variable== " + processVariables);*/

        model.addAttribute("task", task);
        model.addAttribute("taskId",taskId);
        model.addAttribute("taskVariables", taskVariables);
        model.addAttribute("processVariables", runtimeService.getVariables(task.getExecutionId()));

        return currentTask;
    }
    @GetMapping("/viewCompleted/{taskId}")
    public String viewCompletedTaskForm(@PathVariable String taskId, Model model) {
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found with id: " + taskId);
        }

        String currentTask;
        switch (task.getTaskDefinitionKey().toLowerCase()) {
            case "requirement_elicitation":
                currentTask = "tasks/requirement";
                break;
            case "formtask_12":
                currentTask = "tasks/reviewSrs";
                break;
            case "formtask_43":
                currentTask = "tasks/reviewUserStories";
                break;
            case "formtask_11":
                currentTask = "tasks/reviewSolutionArchitecture";
                break;
            case "deployment":
                currentTask = "tasks/deployment";
                break;
            case "review":
                currentTask = "tasks/review";
                break;
            case "closure":
                currentTask = "tasks/closure";
                break;
            default:
                currentTask = "tasks/default";
                break;
        }

        // Load task variables
        //Map<String, Object> taskVariables = taskService.getVariables(taskId);

        // Load process variables
        Map<String, Object> processVariables = runtimeService.getVariables(task.getExecutionId(),
                List.of(task.getId()));
        //System.out.println(" The task id==="+taskId+"This is the process variable== " + processVariables);
        processVariables.putAll(runtimeService.getVariables(task.getExecutionId()));
        model.addAttribute("task", task);
        model.addAttribute("taskId",taskId);
        //model.addAttribute("taskVariables", taskVariables);
        model.addAttribute("processVariables",processVariables );

        return currentTask;
    }

    // Edit task form - generic form handler
    @GetMapping("/edit/{taskId}")
    public String editTaskForm(@PathVariable String taskId, Model model) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        System.out.println(task);
        if (task == null) {
            model.addAttribute("error", "Task not found");
            return "error/404";
        }

        // Load task variables for form data
        Map<String, Object> taskVariables = taskService.getVariables(taskId);

        model.addAttribute("task", task);
        model.addAttribute("taskVariables", taskVariables);
        model.addAttribute("formData", new HashMap<String, Object>());

        return "task/edit-task-form";
    }

    // Save task form data (without completing the task)
    @PostMapping("/save/{taskId}")
    public String saveTaskForm(@PathVariable String taskId,
                               @RequestParam Map<String, String> formParams,
                               RedirectAttributes redirectAttributes) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

            if (task == null) {
                redirectAttributes.addFlashAttribute("error", "Task not found");
                return "redirect:/tasks/user/" + task.getAssignee();
            }

            // Convert form parameters to proper types
            Map<String, Object> variables = convertFormParamsToVariables(formParams);

            // Save form data as task variables
            taskService.setVariables(taskId, variables);

            redirectAttributes.addFlashAttribute("success", "Form data saved successfully");
            return "redirect:/tasks/view/" + taskId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save form data: " + e.getMessage());
            return "redirect:/tasks/edit/" + taskId;
        }
    }

    // Complete task with form data
    @PostMapping("/complete/{taskId}")
    public String completeTask(@PathVariable String taskId,
                               @RequestParam Map<String, String> formParams,
                               RedirectAttributes redirectAttributes) {
        System.out.println("This is what is submitted === " + formParams);
        try {

            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

            if (task == null) {
                redirectAttributes.addFlashAttribute("error", "Task not found");
                return "redirect:/tasks/user/" + (task != null ? task.getAssignee() : "unknown");
            }

            // Convert form parameters to proper types
            Map<String, Object> variables = convertFormParamsToVariables(formParams);
            // Complete the task with form data
            runtimeService.setVariable(task.getExecutionId(),task.getId(),variables);

/*
            System.out.println(" During the complete cycle The task id here==="+task.getId()+"\n\n\n\n\n\n\n\n "
                            + " The whole process variables===="+ runtimeService.getVariables(task.getExecutionId()));
*/
            taskService.complete(taskId, variables);


            redirectAttributes.addFlashAttribute("success", "Task completed successfully");
            return "redirect:/tasks/" + task.getProcessInstanceId();

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to complete task: " + e.getMessage());
            return "redirect:/tasks/view/" + taskId;
        }
    }

    // Claim a task
    @PostMapping("/claim/{taskId}")
    public String claimTask(@PathVariable String taskId,
                            @RequestParam String userId,
                            RedirectAttributes redirectAttributes) {
        try {
            taskService.claim(taskId, userId);
            redirectAttributes.addFlashAttribute("success", "Task claimed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to claim task: " + e.getMessage());
        }
        return "redirect:/tasks/view/" + taskId;
    }

    // Unclaim a task
    @PostMapping("/unclaim/{taskId}")
    public String unclaimTask(@PathVariable String taskId,
                              RedirectAttributes redirectAttributes) {
        try {
            taskService.unclaim(taskId);
            redirectAttributes.addFlashAttribute("success", "Task unclaimed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to unclaim task: " + e.getMessage());
        }
        return "redirect:/tasks/view/" + taskId;
    }

    // Get task history for a process instance
    @GetMapping("/history/{processInstanceId}")
    public String getTaskHistory(@PathVariable String processInstanceId, Model model) {
        List<HistoricTaskInstance> historicTasks = historyService
                .createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        model.addAttribute("historicTasks", historicTasks);
        model.addAttribute("processInstanceId", processInstanceId);
        return "task/task-history";
    }

    // Utility method to convert form parameters to proper variable types
    private Map<String, Object> convertFormParamsToVariables(Map<String, String> formParams) {
        Map<String, Object> variables = new HashMap<>();

        for (Map.Entry<String, String> entry : formParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Skip Spring-specific parameters
            if (key.startsWith("_") || key.equals("submit")) {
                continue;
            }

            // Convert to appropriate types based on content
            if (value.equals("true") || value.equals("false")) {
                variables.put(key, Boolean.parseBoolean(value));
            } else if (value.matches("\\d+")) {
                variables.put(key, Long.parseLong(value));
            } else if (value.matches("\\d+\\.\\d+")) {
                variables.put(key, Double.parseDouble(value));
            } else {
                variables.put(key, value);
            }
        }

        return variables;
    }
}
