package com.justjava.devFlow.tasks;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
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
                .active()
                .orderByTaskCreateTime().desc()
                .list();
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(projectId)
                .includeProcessVariables()
                .finished()
                .orderByTaskCreateTime().desc()
                .list();
        historicTasks.forEach(task -> {
           /* System.out.println(" The task process variables here=== "+task.getProcessVariables() +
                    " The Task State ==== "+task.getState()+
                    " task ID==="+  task.getId()+
                    " the task kept variable==="+runtimeService.getVariables(task.getExecutionId(),List.of(task.getId())).get(task.getId())+
                    " This is the task name === "+task.getName()+ " project descriptiontask creation date" +
                            " == " + task.getCreateTime()
                    );*/
        });
        model.addAttribute("tasks", tasks);
        model.addAttribute("completedTasks",historicTasks);
        //model.addAttribute("userId", userId);
        return "tasks/projectTasks";
    }
    @GetMapping("/tasks/revert-confirm/{taskId}")
    public String getRevertConfirmation(@PathVariable String taskId, Model model) {
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().finished().taskId(taskId).singleResult();
        //System.out.println("The task " + task);


        model.addAttribute("task", task);
        return "/tasks/revert-confirm-modal :: revertConfirmModal";
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
    @GetMapping("/layoutDesign" )
    public String getLayoutDesign(Model model) {
        return "tasks/reviewLayoutCode";
    }
}
