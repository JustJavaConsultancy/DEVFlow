package com.justjava.devFlow.projects;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class ProjectsController {
    @Autowired
    RuntimeService runtimeService;

    @Autowired
    HistoryService  historyService;
    @GetMapping("/projects")
    public String getProjects(Model model){
        List<ProcessInstance> projects = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("softwareEngineeringProcess")
                .includeProcessVariables()
                .active()
                .list();
        projects.forEach(project -> {
            System.out.println(" The Process Instance" +
                    " Here=ID=="+project.getProcessInstanceId()
                    +" the start time ==="+project.getStartTime()
                    +" the variables==="+project.getProcessVariables());
        });
        List<HistoricProcessInstance> completedProcess =historyService
                .createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();
        System.out.println(" The Completed Process Here==="+completedProcess.size());
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
        model.addAttribute("project",project);
        return "projects/projectDetails";
    }
    @PostMapping("/project/start")
    public String startProject(@RequestParam Map<String,Object>  startVariables,Model model){


        System.out.println(" The Sent Parameter Here==="+startVariables);

        ProcessInstance processInstance=runtimeService
                .startProcessInstanceByKey("softwareEngineeringProcess",startVariables);
        List<ProcessInstance> projects = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("softwareEngineeringProcess")
                .includeProcessVariables()
                .active()
                .list();
        projects.forEach(project -> {
            System.out.println(" The Process Instance" +
                    " Here=ID=="+project.getProcessInstanceId()
                    +" the start time ==="+project.getStartTime()
                    +" the variables==="+project.getProcessVariables());
        });
        List<HistoricProcessInstance> completedProcess =historyService
                .createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();
        System.out.println(" The Completed Process Here==="+completedProcess.size());
        model.addAttribute("projects",projects);
        model.addAttribute("completedProject",completedProcess.size());
        model.addAttribute("activeProject",projects.size());
        System.out.println(" The Process Instance Here==="+processInstance.getProcessVariables());
        return "/projects/allProjects";
    }

}
