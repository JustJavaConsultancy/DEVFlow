package com.justjava.devFlow.projects;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProjectsController {
    @GetMapping("/projects")
    public String getProjects(){
        return "projects/allProjects";
    }
    @GetMapping("/project-details")
    public String getProjectDetails(){
        return "projects/projectDetails";
    }
}
