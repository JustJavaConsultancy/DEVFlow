package com.justjava.devFlow.tasks;

import jakarta.servlet.http.HttpServletRequest;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @PostMapping("/api/stories/save")
    @ResponseBody
    public ResponseEntity<String> saveStory(
            @RequestParam String storyId,
            @RequestParam String story,
            @RequestParam String taskId) {

        try {
            System.out.println("🔍 Checking task for ID: " + taskId);
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

            // ✅ Ensure task exists
            if (task == null) {
                System.out.println("❌ No task found for id: " + taskId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Task not found with id: " + taskId);
            }

            String processInstanceId = task.getProcessInstanceId();
            System.out.println("📦 Process instance: " + processInstanceId);

            // ✅ Fetch the process variable
            Object storiesObj = runtimeService.getVariable(processInstanceId, "stories");
            if (storiesObj == null) {
                System.out.println("❌ No 'stories' variable found in process instance");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No 'stories' variable found for this process");
            }

            // ✅ Validate that 'stories' is a Map
            if (!(storiesObj instanceof Map)) {
                System.out.println("❌ Invalid 'stories' type: " + storiesObj.getClass().getName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid 'stories' format — expected a Map");
            }

            Map<String, Object> stories = (Map<String, Object>) storiesObj;

            // ✅ Fetch 'user_stories' inside 'stories'
            Object userStoriesObj = stories.get("user_stories");
            if (userStoriesObj == null) {
                System.out.println("❌ No 'user_stories' found inside 'stories'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No 'user_stories' found inside 'stories'");
            }

            if (!(userStoriesObj instanceof List)) {
                System.out.println("❌ Invalid 'user_stories' type: " + userStoriesObj.getClass().getName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid 'user_stories' format — expected a List");
            }

            List<Map<String, Object>> userStories = (List<Map<String, Object>>) userStoriesObj;

            // ✅ Find and update story by ID
            boolean storyFound = false;
            for (Map<String, Object> userStory : userStories) {
                Object id = userStory.get("Id");
                if (id != null && storyId.equals(String.valueOf(id))) {
                    userStory.put("story", story);
                    storyFound = true;
                    break;
                }
            }

            if (!storyFound) {
                System.out.println("❌ Story not found with id: " + storyId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Story not found with id: " + storyId);
            }

            // ✅ Update and save the process variable
            stories.put("user_stories", userStories);
            runtimeService.setVariable(processInstanceId, "stories", stories);

            System.out.println("✅ Story updated successfully:");
            System.out.println("  - storyId: " + storyId);
            System.out.println("  - story content: " + story);
            System.out.println("  - taskId: " + taskId);

            return ResponseEntity.ok("Story saved successfully");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Error saving story: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving story: " + e.getMessage());
        }
    }
}
