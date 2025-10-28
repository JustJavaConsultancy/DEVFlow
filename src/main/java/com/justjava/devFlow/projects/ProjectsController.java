package com.justjava.devFlow.projects;

import com.justjava.devFlow.aau.AuthenticationManager;
import com.justjava.devFlow.keycloak.KeycloakService;
import com.justjava.devFlow.util.EmailUtil;
import com.justjava.devFlow.util.SendGridService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class ProjectsController {
    @Autowired
    private TaskService taskService;

    @Autowired
    KeycloakService keycloakService;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    HistoryService  historyService;

    @Autowired
    SendGridService sendGridService;

    @Value("${app.base-url}")
    String baseUrl;

    @Value("${flowable.mail.server.username}")
    String fromEmail;

    @GetMapping("/projects")
    public String getProjects(Model model){

        System.out.println(" Calling a projects controller");
        List<ProcessInstance> projects = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("softwareEngineeringProcess")
                .processInstanceBusinessKey(String.valueOf(authenticationManager.get("sub")))
                .orderByStartTime()
                .desc()
                .includeProcessVariables()
                .active()
                .list();
        projects.forEach(project -> {
            System.out.println(" The Process Instance" +
                    " Here=ID=="+project.getProcessInstanceId()
                    //+" the start time ==="+project.getStartTime()
                    //+" the springInitializrResponse==="+project.getProcessVariables().get("springInitializrResponse")
                    + " the artifact===" +project.getProcessVariables().get("artifact"));
        });
        List<HistoricProcessInstance> completedProcess =historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(String.valueOf(authenticationManager.get("sub")))
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

    @GetMapping("/admin/projects")
    public String getAllProjects(Model model){
        List<ProcessInstance> projects = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey("softwareEngineeringProcess")
                .includeProcessVariables()
                .active()
                .list();
/*        projects.forEach(project -> {
            System.out.println(" The Process Instance" +
                    " Here=ID=="+project.getProcessInstanceId()
                    //+" the start time ==="+project.getStartTime()
                    //+" the springInitializrResponse==="+project.getProcessVariables().get("springInitializrResponse")
                    + "\n\n\n\n\n\n\n\n\n\n\n\n\n the architecture===" +
                    project.getProcessVariables().get("architecture")
            +"\n\n\n\n\n\n\n\n\n\n\n\n\n");
        });*/
        List<HistoricProcessInstance> completedProcess =historyService
                .createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();
        List<HistoricTaskInstance> historicTasks = historyService
                .createHistoricTaskInstanceQuery()
                .finished()
                .orderByTaskCreateTime().desc()
                .list();
        List<Task> tasks = taskService
                .createTaskQuery()
                .includeProcessVariables()
                .orderByTaskCreateTime().desc()
                .list();
        model.addAttribute("projects",projects);
        model.addAttribute("completedProject",completedProcess.size());
        model.addAttribute("activeProject",projects.size());
        return "projects/adminAllProjects";
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

        model.addAttribute("projectId", projectId);
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
        int percentage = (int) Math.round((completedTasks.size() / 7.0) * 100);
        Object emails = runtimeService.getVariable(projectId, "invitedEmails");
        List<String> invitedEmails = new ArrayList<>();

        if (emails instanceof List<?>) {
            invitedEmails = (List<String>) emails;
        } else if (emails instanceof String) {
            invitedEmails.add((String) emails);
        }

        model.addAttribute("invitedEmails", invitedEmails);
        model.addAttribute("completedTasks",completedTasks);
        model.addAttribute("project",project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("completedTaskPercentage",percentage);
        model.addAttribute("completedTaskCount",completedTasks.size());
        return "client/dashboard";
    }

    @PostMapping("/project/start")
    public String startProject(@RequestParam Map<String,Object>  startVariables,Model model){
        //System.out.println(" The Sent Parameter Here==="+startVariables);

        String businessKey= String.valueOf(authenticationManager.get("sub")) ;

        startVariables.put("process",false);
        //System.out.println(" The login user here==="+businessKey);
        startVariables.put("progress",0);
        ProcessInstance processInstance=runtimeService
                .startProcessInstanceByKey("softwareEngineeringProcess",
                        businessKey,startVariables);
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
    @PostMapping("/richtext")
    public ResponseEntity<String> receiveRichText(
            @RequestParam("taskId") String taskId,
            @RequestParam("requirement") String requirement) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            HistoricTaskInstance completedTask = historyService
                    .createHistoricTaskInstanceQuery()
                    .finished()
                    .taskId(taskId)
                    .singleResult();
            runtimeService.setVariable(completedTask.getProcessInstanceId(), "aiRequirementAnalysis", requirement);
        }else {
            runtimeService.setVariable(task.getProcessInstanceId(), "aiRequirementAnalysis", requirement);
        }

        return ResponseEntity.ok("saved");
    }
    @PostMapping("/richtext/architecture")
    public ResponseEntity<String> receiveRichTextArchitecture(
            @RequestParam("taskId") String taskId,
            @RequestParam("requirement") String requirement) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            HistoricTaskInstance completedTask = historyService
                    .createHistoricTaskInstanceQuery()
                    .finished()
                    .taskId(taskId)
                    .singleResult();
            runtimeService.setVariable(completedTask.getProcessInstanceId(), "architecture", requirement);
        }else {
            runtimeService.setVariable(task.getProcessInstanceId(), "architecture", requirement);
        }

        return ResponseEntity.ok("saved");
    }
    @PostMapping("/richtext/plan")
    public ResponseEntity<String> receiveRichTextplan(
            @RequestParam("taskId") String taskId,
            @RequestParam("requirement") String plan) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            HistoricTaskInstance completedTask = historyService
                    .createHistoricTaskInstanceQuery()
                    .finished()
                    .taskId(taskId)
                    .singleResult();
            runtimeService.setVariable(completedTask.getProcessInstanceId(), "plans", plan);
        }else {
            runtimeService.setVariable(task.getProcessInstanceId(), "plans", plan);
        }

        return ResponseEntity.ok("saved");
    }

    @PostMapping("/project/save-url")
    public String saveProjectURL(@RequestParam Map<String,Object> urlDetails, HttpServletRequest request){
        String processInstanceId = (String) urlDetails.get("projectId");
        String repoUrl = (String) urlDetails.get("projectUrl");
     //   System.out.println(" The URL Details Here === " + urlDetails);
        runtimeService.setVariable(processInstanceId, "projectURL", repoUrl);

        // Get the page the request came from
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @PostMapping("/project/invite")
    @ResponseBody
    public String inviteTeamMember(@RequestParam Map<String,Object> inviteDetails,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {

        String businessKey = String.valueOf(authenticationManager.get("sub"));
        String email = (String) inviteDetails.get("email");
        String password = "1234";
        String projectId = (String) inviteDetails.get("projectId");
        String webUrl = baseUrl + "/project-progress/" + projectId;

        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("username", email);
        params.put("status", true);

        // Email Structure
        String subject = "";

        try {
            List<Map<String, Object>> userByEmail = keycloakService.getUsersByEmail(email);
            if (userByEmail.isEmpty()){
                keycloakService.createUser(params);
                subject = "Invite Message from JustJava";
            } else {
                // User exists but hasn't been invited to this project yet
                subject = "Project Invitation from JustJava";
            }

            // Get existing invited emails from process variable
            List<String> invitedEmails = new ArrayList<>();
            Object existingEmails = runtimeService.getVariable(projectId, "invitedEmails");

            if (existingEmails instanceof List) {
                // Cast and add all existing emails
                invitedEmails = new ArrayList<>((List<String>) existingEmails);
            } else if (existingEmails instanceof String) {
                // Handle case where it might be stored as a single string
                invitedEmails.add((String) existingEmails);
            }

            // Check if email has already been invited
            if (invitedEmails.contains(email)) {
                sendGridService.sendTemplateEmail(email, subject, password, webUrl);
                return "<div class='success-message'>" +
                        "<i class='fas fa-check-circle mr-2'></i>" +
                        "Invitation sent successfully to " + email + " (already invited)" +
                        "</div>";
            } else {
                // Add the new email to the list
                invitedEmails.add(email);

                // Store the updated list in process variable
                runtimeService.setVariable(projectId, "invitedEmails", invitedEmails);
                sendGridService.sendTemplateEmail(email, subject, password, webUrl);

                return "<div class='success-message'>" +
                        "<i class='fas fa-check-circle mr-2'></i>" +
                        "Invitation sent successfully to " + email +
                        "</div>";
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "<div class='error-message'>" +
                    "<i class='fas fa-exclamation-circle mr-2'></i>" +
                    "Failed to send invitation: " + e.getMessage() +
                    "</div>";
        }
    }
    @PostMapping("/client/project/invite")
    public String inviteTeamMember(@RequestParam Map<String,Object> inviteDetails, HttpServletRequest request){
        String referer = request.getHeader("Referer");
        String businessKey = String.valueOf(authenticationManager.get("sub"));
        String email = (String) inviteDetails.get("email");
        String password = "1234";
        String projectId = (String) inviteDetails.get("projectId");
        String webUrl = baseUrl + "/project-progress/" + projectId;

        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("username", email);
        params.put("status", true);

        // Email Structure
        String subject = "";

        List<Map<String, Object>> userByEmail = keycloakService.getUsersByEmail(email);
        if (userByEmail.isEmpty()){
            keycloakService.createUser(params);
            subject = "Invite Message from JustJava";
        } else {
            // User exists but hasn't been invited to this project yet
            subject = "Project Invitation from JustJava";
        }
        // Get existing invited emails from process variable
        List<String> invitedEmails = new ArrayList<>();
        Object existingEmails = runtimeService.getVariable(projectId, "invitedEmails");

        if (existingEmails instanceof List) {
            // Cast and add all existing emails
            invitedEmails = new ArrayList<>((List<String>) existingEmails);
        } else if (existingEmails instanceof String) {
            // Handle case where it might be stored as a single string
            invitedEmails.add((String) existingEmails);
        }

        // Check if email has already been invited
        if (invitedEmails.contains(email)) {
            sendGridService.sendTemplateEmail(email, subject, password, webUrl);
        }else {


            // Add the new email to the list
            invitedEmails.add(email);

            // Store the updated list in process variable
            runtimeService.setVariable(projectId, "invitedEmails", invitedEmails);
            sendGridService.sendTemplateEmail(email, subject, password, webUrl);
        }
        return "redirect:" + (referer != null ? referer : "/");
    }

}
