package com.justjava.devFlow.tasks;

import jakarta.servlet.http.HttpServletRequest;
import org.flowable.bpmn.model.*;
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

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private org.flowable.engine.RepositoryService repositoryService;

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
    @GetMapping("/tasks/skip-confirm/{taskId}")
    public String getSkipConfirmation(@PathVariable String taskId, Model model) {
        try {
            Task currentTask = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();

            if (currentTask == null) {
                model.addAttribute("error", "Task not found with ID: " + taskId);
                return "/tasks/error-fragment :: error";
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(currentTask.getProcessDefinitionId());
            FlowElement currentElement = bpmnModel.getMainProcess()
                    .getFlowElement(currentTask.getTaskDefinitionKey());

            if (!(currentElement instanceof FlowNode)) {
                model.addAttribute("error", "Current element is not a flow node");
                return "/tasks/error-fragment :: error";
            }

            Set<String> visited = new HashSet<>();
            UserTask nextUserTask = findNextUserTask((FlowNode) currentElement, visited, currentElement.getId());

            if (nextUserTask != null) {
                System.out.println("✅ Next user task found: " + nextUserTask.getName() + " (ID: " + nextUserTask.getId() + ")");
                model.addAttribute("nextTaskId", nextUserTask.getId());
            } else {
                System.out.println("⚠️ No valid next user task found after: " + currentTask.getName());
                model.addAttribute("nextTaskId", "");
            }

            return "/tasks/skip-confirm-modal :: skip-confirm-modal";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to determine next task");
            return "/tasks/error-fragment :: error";
        }
    }

    /**
     * 🔁 Recursively find the next valid UserTask, avoiding loops and self-references.
     */
    private UserTask findNextUserTask(FlowNode currentNode, Set<String> visited, String startId) {
        if (!visited.add(currentNode.getId())) {
            return null; // Already visited, avoid loop
        }

        for (SequenceFlow flow : currentNode.getOutgoingFlows()) {
            FlowElement target = flow.getTargetFlowElement();

            // Skip if the target loops back to the start node
            if (target.getId().equals(startId)) {
                continue;
            }

            if (target instanceof UserTask) {
                return (UserTask) target;
            }

            if (target instanceof FlowNode) {
                UserTask next = findNextUserTask((FlowNode) target, visited, startId);
                if (next != null) return next;
            }
        }
        return null;
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
            @RequestParam String acceptanceCriteria,
            @RequestParam String taskId) {

        try {
            System.out.println("🔍 Checking task for ID: " + taskId);

            // Try to find an active task first
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            String processInstanceId = null;

            if (task != null) {
                processInstanceId = task.getProcessInstanceId();
                System.out.println("📦 Active process instance: " + processInstanceId);
            } else {
                // If not active, check history
                HistoricTaskInstance completedTask = historyService
                        .createHistoricTaskInstanceQuery()
                        .finished()
                        .taskId(taskId)
                        .singleResult();

                if (completedTask == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("No active or completed task found for ID: " + taskId);
                }

                processInstanceId = completedTask.getProcessInstanceId();
                System.out.println("📦 Completed process instance: " + processInstanceId);
            }

            // ✅ Fetch the 'stories' variable from runtime
            Object storiesObj = runtimeService.getVariable(processInstanceId, "stories");
            if (storiesObj == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No 'stories' variable found for this process");
            }

            if (!(storiesObj instanceof Map)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid 'stories' format — expected a Map");
            }

            Map<String, Object> stories = (Map<String, Object>) storiesObj;

            // ✅ Fetch 'user_stories'
            Object userStoriesObj = stories.get("user_stories");
            if (userStoriesObj == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No 'user_stories' found inside 'stories'");
            }

            if (!(userStoriesObj instanceof List)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid 'user_stories' format — expected a List");
            }

            List<Map<String, Object>> userStories = (List<Map<String, Object>>) userStoriesObj;

            // ✅ Find and update story
            boolean storyFound = false;
            for (Map<String, Object> userStory : userStories) {
                Object id = userStory.get("id");
                if (id != null && storyId.equals(String.valueOf(id))) {
                    userStory.put("story", story);
                    userStory.put("acceptance_criteria", parseAcceptanceCriteria(acceptanceCriteria));
                    storyFound = true;
                    break;
                }
            }

            if (!storyFound) {
                System.out.println("❌ Story not found with id: " + storyId);
                System.out.println("All stories: " + userStories);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Story not found with id: " + storyId);
            }

            // ✅ Update process variable
            stories.put("user_stories", userStories);
            runtimeService.setVariable(processInstanceId, "stories", stories);

            System.out.println("✅ Story updated successfully:");
            System.out.println("  - storyId: " + storyId);
            System.out.println("  - story content: " + story);
            System.out.println("  - acceptance criteria: " + acceptanceCriteria);
            System.out.println("  - taskId: " + taskId);

            return ResponseEntity.ok("Story saved successfully");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Error saving story: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving story: " + e.getMessage());
        }
    }

    /**
     * Helper method to parse acceptance criteria from string to List.
     */
    private List<String> parseAcceptanceCriteria(String acceptanceCriteria) {
        if (acceptanceCriteria == null || acceptanceCriteria.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(acceptanceCriteria.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

}
