package com.justjava.devFlow.tasks;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private FormService formService;
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;
    @GetMapping("/tasks/{projectId}")
    public String getTasks(@PathVariable  String projectId, Model model){

        ProcessInstance project=runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(projectId)
                .includeProcessVariables()
                .singleResult();

        List<Task> tasks = taskService
                .createTaskQuery()
                .processInstanceId(projectId)
                .includeProcessVariables()
                .orderByTaskCreateTime().desc()
                .list();

        tasks.forEach(task -> {
            System.out.println(" The task process variables here==="+
                    " The Task State ==== "+task.getState()
                    +task.getProcessVariables());
        });
        model.addAttribute("tasks", tasks);
        //model.addAttribute("userId", userId);
        return "tasks/projectTasks";
    }
    @GetMapping("/reviewSRS")
    public String reviewSRS(){
        return "tasks/reviewSrs";
    }
    @GetMapping("/reviewUserStories")
    public String reviewUserStories(){
        return "tasks/reviewUserStories";
    }
    @GetMapping("/reviewSolutionArchitecture")
    public String reviewSolutionArchitecture(){
        return "tasks/reviewSolutionArchitecture";
    }
    @GetMapping("/UAT")
    public String getUAT(){
        return "tasks/UAT";
    }
    @GetMapping("/codeReview")
    public String getCodeReview(){
        return "tasks/codeReview";
    }
    @GetMapping("/requirement")
    public String getRequirement(){
        return "tasks/requirement";
    }
}
