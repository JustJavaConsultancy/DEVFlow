package com.justjava.devFlow.projects;

import com.justjava.devFlow.aau.AuthenticationManager;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class ProjectsController {
    @Autowired
    private TaskService taskService;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    HistoryService  historyService;
    @GetMapping("/projects")
    public String getProjects(Model model){

        List<ProcessInstance> projects = runtimeService
                .createProcessInstanceQuery()
                //.processInstanceBusinessKey(String.valueOf(authenticationManager.get("sub")))
                .processDefinitionKey("softwareEngineeringProcess")
                .orderByStartTime().desc()
                .includeProcessVariables()
                .active()
                .list();
        projects.forEach(project -> {
/*            System.out.println(" The Process Instance" +
                    " Here=ID=="+project.getProcessInstanceId()
                    +" the start time ==="+project.getStartTime()
                    +" the variables==="+project.getProcessVariables().get("b0e80426-962f-11f0-a585-00155dd09231"));*/
        });
        List<HistoricProcessInstance> completedProcess =historyService
                .createHistoricProcessInstanceQuery()
                //.processInstanceBusinessKey(String.valueOf(authenticationManager.get("sub")))
                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();
        //System.out.println(" The Completed Process Here==="+completedProcess.size());
        model.addAttribute("projects",projects);
        model.addAttribute("completedProject",completedProcess.size());
        model.addAttribute("activeProject",projects.size());

        return "projects/allProjects";
    }
    @GetMapping("/project-details/{projectId}")
    public String getProjectDetails(@PathVariable String projectId,  Model model){
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
        model.addAttribute("project",project);
        model.addAttribute("tasks", tasks);
        return "projects/projectDetails";
    }
    @GetMapping("/project-progress/{projectId}")
    public String getProjectProgress(@PathVariable String projectId,  Model model){
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
        List <HistoricTaskInstance> completedTasks= historyService
                .createHistoricTaskInstanceQuery()
                .processInstanceId(projectId)
                .includeProcessVariables()
                .finished()
                .orderByTaskCreateTime().desc()
                .list();
        double percentage = ((double) completedTasks.size() / 7) * 100;
        model.addAttribute("completedTasks",completedTasks);
        model.addAttribute("project",project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("completedTaskPercentage",percentage);
        model.addAttribute("completedTaskCount",completedTasks.size());
        return "client/dashboard";
    }

    @PostMapping("/project/start")
    public String startProject(@RequestParam Map<String,Object>  startVariables,Model model){


        System.out.println(" The Sent Parameter Here==="+startVariables);

        String businessKey= String.valueOf(authenticationManager.get("sub")) ;
        startVariables.put("progress",0);
        ProcessInstance processInstance=runtimeService
                .startProcessInstanceByKey("softwareEngineeringProcess",businessKey,startVariables);
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
        //System.out.println(" The Completed Process Here==="+completedProcess.size());
        model.addAttribute("projects",projects);
        model.addAttribute("completedProject",completedProcess.size());
        model.addAttribute("activeProject",projects.size());
        //System.out.println(" The Process Instance Here==="+processInstance.getProcessVariables());
        return "redirect:/projects";
    }

}
