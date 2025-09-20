package com.justjava.devFlow;

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

import java.util.List;

@Controller
public class HomeController {
    @Autowired
    private TaskService taskService;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    HistoryService historyService;
    @GetMapping("/")
    public String home(Model model) {
        List<ProcessInstance> projects = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("softwareEngineeringProcess")
                .includeProcessVariables()
                .active()
                .list();
        projects.forEach(project -> {
/*            System.out.println(" The Process Instance" +
                    " Here=ID=="+project.getProcessInstanceId()
                    +" the start time ==="+project.getStartTime()
                    +" the variables==="+project.getProcessVariables());*/
        });
        List<HistoricProcessInstance> completedProcess =historyService
                .createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .orderByTaskCreateTime().desc()
                .list();
        List<Task> tasks = taskService
                .createTaskQuery()
                .includeProcessVariables()
                .orderByTaskCreateTime().desc()
                .list();
        //System.out.println(" The Completed Process Here==="+completedProcess.size());
        model.addAttribute("projects",projects);
        model.addAttribute("activeTask",tasks.size());
        model.addAttribute("completedTask",historicTasks.size());
        model.addAttribute("completedProject",completedProcess.size());
        model.addAttribute("activeProject",projects.size());

        return "dashboard";
    }
}
