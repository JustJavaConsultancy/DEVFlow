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
            System.out.println(" The task process variables here=== "+task.getProcessVariables() +
                    " The Task State ==== "+task.getState()+
                    " task ID==="+  task.getId()+
                    " the task kept variable==="+runtimeService.getVariables(task.getExecutionId(),List.of(task.getId())).get(task.getId())+
                    " This is the task name === "+task.getName()+ " project descriptiontask creation date" +
                            " == " + task.getCreateTime()
                    );
        });
        model.addAttribute("tasks", tasks);
        model.addAttribute("completedTasks",historicTasks);
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
    @GetMapping("/layoutDesign" )
    public String getLayoutDesign(Model model) {
        model.addAttribute("artifacts","<!-- src/main/resources/templates/layout.html -->\n" +
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\" >\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Document Management System</title>\n" +
                "\n" +
                "    <!-- Bootstrap 5 CSS CDN -->\n" +
                "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\" />\n" +
                "\n" +
                "    <!-- Bootstrap Icons -->\n" +
                "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css\" rel=\"stylesheet\" />\n" +
                "\n" +
                "    <style>\n" +
                "        body {\n" +
                "            min-height: 100vh;\n" +
                "            overflow-x: hidden;\n" +
                "        }\n" +
                "        #sidebar-wrapper {\n" +
                "            min-height: 100vh;\n" +
                "            width: 260px;\n" +
                "            transition: all 0.3s;\n" +
                "            background-color: #343a40;\n" +
                "        }\n" +
                "        #sidebar-wrapper.collapsed {\n" +
                "            width: 70px;\n" +
                "        }\n" +
                "        #sidebar-wrapper .nav-link {\n" +
                "            color: #adb5bd;\n" +
                "        }\n" +
                "        #sidebar-wrapper .nav-link:hover, \n" +
                "        #sidebar-wrapper .nav-link.active {\n" +
                "            color: #fff;\n" +
                "            background-color: #495057;\n" +
                "        }\n" +
                "        #sidebar-wrapper .nav-link .bi {\n" +
                "            font-size: 1.2rem;\n" +
                "        }\n" +
                "        #sidebarWrapperCollapsed .nav-link span {\n" +
                "            display: none;\n" +
                "        }\n" +
                "        #sidebar-wrapper.collapsed .nav-link span {\n" +
                "            display: none;\n" +
                "        }\n" +
                "        #sidebar-wrapper.collapsed .nav-link {\n" +
                "            text-align: center;\n" +
                "            padding-left: 0.75rem;\n" +
                "            padding-right: 0.75rem;\n" +
                "        }\n" +
                "        #sidebar-wrapper .nav-link span {\n" +
                "            margin-left: 10px;\n" +
                "        }\n" +
                "        #page-content-wrapper {\n" +
                "            width: 100%;\n" +
                "        }\n" +
                "        .navbar-brand {\n" +
                "            font-weight: 600;\n" +
                "            font-size: 1.25rem;\n" +
                "        }\n" +
                "        /* Sidebar toggle button */\n" +
                "        #sidebar-toggle-btn {\n" +
                "            cursor: pointer;\n" +
                "            font-size: 1.5rem;\n" +
                "            color: #fff;\n" +
                "            user-select: none;\n" +
                "        }\n" +
                "        /* Prevent body scroll when sidebar open on small screens */\n" +
                "        @media (max-width: 991.98px) {\n" +
                "            #sidebar-wrapper {\n" +
                "                position: fixed;\n" +
                "                z-index: 1035;\n" +
                "                height: 100vh;\n" +
                "                left: -260px;\n" +
                "            }\n" +
                "            #sidebar-wrapper.active {\n" +
                "                left: 0;\n" +
                "            }\n" +
                "            #page-content-wrapper {\n" +
                "                padding-left: 0;\n" +
                "            }\n" +
                "            #overlay {\n" +
                "                position: fixed;\n" +
                "                display:none;\n" +
                "                width: 100vw;\n" +
                "                height: 100vh;\n" +
                "                top: 0; left: 0;\n" +
                "                background: rgba(0,0,0,0.5);\n" +
                "                z-index: 1030;\n" +
                "            }\n" +
                "            #overlay.active {\n" +
                "                display: block;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "\n" +
                "    <script th:inline=\"javascript\">\n" +
                "        /*<![CDATA[*/\n" +
                "        function toggleSidebar() {\n" +
                "            const sidebar = document.getElementById('sidebar-wrapper');\n" +
                "            const overlay = document.getElementById('overlay');\n" +
                "            if (window.innerWidth < 992) {\n" +
                "                if(sidebar.classList.contains('active')){\n" +
                "                    sidebar.classList.remove('active');\n" +
                "                    overlay.classList.remove('active');\n" +
                "                } else {\n" +
                "                    sidebar.classList.add('active');\n" +
                "                    overlay.classList.add('active');\n" +
                "                }\n" +
                "            } else {\n" +
                "                sidebar.classList.toggle('collapsed');\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        document.addEventListener('DOMContentLoaded', function() {\n" +
                "            const overlay = document.getElementById('overlay');\n" +
                "            if(overlay){\n" +
                "                overlay.addEventListener('click', function() {\n" +
                "                    const sidebar = document.getElementById('sidebar-wrapper');\n" +
                "                    sidebar.classList.remove('active');\n" +
                "                    overlay.classList.remove('active');\n" +
                "                });\n" +
                "            }\n" +
                "        });\n" +
                "        /*]]>*/\n" +
                "    </script>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "    <div id=\"overlay\"></div>\n" +
                "\n" +
                "    <div class=\"d-flex\" id=\"wrapper\">\n" +
                "\n" +
                "        <!-- Sidebar -->\n" +
                "        <nav id=\"sidebar-wrapper\" class=\"bg-dark\">\n" +
                "            <div class=\"sidebar-heading text-white px-3 py-4 fs-5 d-flex align-items-center justify-content-between\">\n" +
                "                <span>Modules</span>\n" +
                "                <i id=\"sidebar-toggle-btn\" class=\"bi bi-list\" onclick=\"toggleSidebar()\" title=\"Toggle sidebar\"></i>\n" +
                "            </div>\n" +
                "            <div class=\"list-group list-group-flush\">\n" +
                "\n" +
                "                <!-- Management Group -->\n" +
                "                <div class=\"list-group-item list-group-item-action bg-dark text-white px-3 pt-3 pb-1 fw-bold small text-uppercase\">\n" +
                "                    <i class=\"bi bi-gear-fill me-2\"></i>Management\n" +
                "                </div>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-pencil-square\"></i><span>Document Editing</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-person-fill\"></i><span>User Management</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-shield-lock-fill\"></i><span>Digital Signature</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-clock-history\"></i><span>Versioning</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-file-earmark-richtext\"></i><span>Scanning &amp; AI Processing</span>\n" +
                "                </a>\n" +
                "\n" +
                "                <!-- Monitoring & Audit Group -->\n" +
                "                <div class=\"list-group-item list-group-item-action bg-dark text-white px-3 pt-3 pb-1 fw-bold small text-uppercase\">\n" +
                "                    <i class=\"bi bi-card-list me-2\"></i>Monitoring &amp; Audit\n" +
                "                </div>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-journal-text\"></i><span>Audit Logging</span>\n" +
                "                </a>\n" +
                "\n" +
                "                <!-- Architecture Views -->\n" +
                "                <div class=\"list-group-item list-group-item-action bg-dark text-white px-3 pt-3 pb-1 fw-bold small text-uppercase\">\n" +
                "                    <i class=\"bi bi-layout-text-sidebar-reverse me-2\"></i>Architecture Views\n" +
                "                </div>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-diagram-3-fill\"></i><span>Logical View</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-code-slash\"></i><span>Development View</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-arrow-repeat\"></i><span>Process View</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-hdd-network-fill\"></i><span>Deployment View</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-collection-play-fill\"></i><span>Scenarios &amp; Use Cases</span>\n" +
                "                </a>\n" +
                "\n" +
                "                <!-- Other Groups -->\n" +
                "                <div class=\"list-group-item list-group-item-action bg-dark text-white px-3 pt-3 pb-1 fw-bold small text-uppercase\">\n" +
                "                    <i class=\"bi bi-info-circle-fill me-2\"></i>Others\n" +
                "                </div>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-list-check\"></i><span>Architecture Decisions</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-box-check\"></i><span>Functional Requirements</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-bar-chart-line-fill\"></i><span>Non-Functional Requirements</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-stack\"></i><span>Technology Stack</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-plug-fill\"></i><span>External Interfaces</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-clipboard-data\"></i><span>Traceability Matrix</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-shield-exclamation\"></i><span>Risks &amp; Mitigation</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-check-circle-fill\"></i><span>Standards Compliance</span>\n" +
                "                </a>\n" +
                "                <a href=\"#\" class=\"list-group-item list-group-item-action bg-dark text-white px-3 d-flex align-items-center\">\n" +
                "                    <i class=\"bi bi-calendar-check-fill\"></i><span>Evolution &amp; Roadmap</span>\n" +
                "                </a>\n" +
                "\n" +
                "            </div>\n" +
                "        </nav>\n" +
                "        <!-- /#sidebar-wrapper -->\n" +
                "\n" +
                "        <!-- Page Content -->\n" +
                "        <div id=\"page-content-wrapper\" class=\"flex-grow-1\">\n" +
                "\n" +
                "            <nav class=\"navbar navbar-expand-lg navbar-dark bg-primary border-bottom\">\n" +
                "                <div class=\"container-fluid\">\n" +
                "                    <button class=\"btn btn-primary d-lg-none\" type=\"button\" id=\"mobileSidebarToggle\" onclick=\"toggleSidebar()\">\n" +
                "                        <i class=\"bi bi-list\"></i>\n" +
                "                    </button>\n" +
                "                    <a class=\"navbar-brand ms-2\" href=\"#\">Document Management System</a>\n" +
                "\n" +
                "                    <div class=\"collapse navbar-collapse justify-content-end\" id=\"navbarSupportedContent\">\n" +
                "                        <ul class=\"navbar-nav align-items-center\">\n" +
                "                            <li class=\"nav-item me-3\">\n" +
                "                                <a class=\"nav-link position-relative\" href=\"#\" title=\"Notifications\">\n" +
                "                                    <i class=\"bi bi-bell-fill fs-5 text-white\"></i>\n" +
                "                                    <span class=\"position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger\">\n" +
                "                                        3\n" +
                "                                        <span class=\"visually-hidden\">unread notifications</span>\n" +
                "                                    </span>\n" +
                "                                </a>\n" +
                "                            </li>\n" +
                "                            <li class=\"nav-item dropdown\">\n" +
                "                                <a class=\"nav-link dropdown-toggle d-flex align-items-center\" href=\"#\" role=\"button\" data-bs-toggle=\"dropdown\" aria-expanded=\"false\">\n" +
                "                                    <img src=\"https://ui-avatars.com/api/?name=JD\" alt=\"User\" class=\"rounded-circle me-2\" width=\"32\" height=\"32\">\n" +
                "                                    <span class=\"text-white\">johndoe</span>\n" +
                "                                </a>\n" +
                "                                <ul class=\"dropdown-menu dropdown-menu-end\">\n" +
                "                                    <li><a class=\"dropdown-item\" href=\"#\"><i class=\"bi bi-person-circle me-2\"></i>Profile</a></li>\n" +
                "                                    <li><a class=\"dropdown-item\" href=\"#\"><i class=\"bi bi-gear-fill me-2\"></i>Settings</a></li>\n" +
                "                                    <li><hr class=\"dropdown-divider\"></li>\n" +
                "                                    <li><a class=\"dropdown-item\" href=\"#\"><i class=\"bi bi-box-arrow-right me-2\"></i>Logout</a></li>\n" +
                "                                </ul>\n" +
                "                            </li>\n" +
                "                        </ul>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </nav>\n" +
                "\n" +
                "            <main class=\"container-fluid px-4 py-4\">\n" +
                "                <div th:fragment=\"content\"></div>\n" +
                "            </main>\n" +
                "\n" +
                "        </div>\n" +
                "        <!-- /#page-content-wrapper -->\n" +
                "\n" +
                "    </div>\n" +
                "    <!-- /#wrapper -->\n" +
                "\n" +
                "    <!-- Bootstrap 5 Bundle with Popper -->\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>\n" +
                "</body>\n" +
                "</html>\n" +
                "\n" +
                "\n" +
                "---\n" +
                "\n" +
                "\n" +
                "<!-- src/main/resources/templates/home.html -->\n" +
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\" >\n" +
                "<head>\n" +
                "    <title>Dashboard - Document Management System</title>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div th:replace=\"layout :: content\">\n" +
                "        <div class=\"container\">\n" +
                "            <h1 class=\"mb-4\">Welcome to the Document Management System</h1>\n" +
                "\n" +
                "            <div class=\"row gx-4 gy-4\">\n" +
                "                <!-- Management Modules -->\n" +
                "                <div class=\"col-12 col-md-6 col-xl-4\">\n" +
                "                    <div class=\"card border-primary h-100\">\n" +
                "                        <div class=\"card-body d-flex flex-column\">\n" +
                "                            <div class=\"d-flex align-items-center mb-3\">\n" +
                "                                <i class=\"bi bi-pencil-square fs-2 text-primary me-3\"></i>\n" +
                "                                <h5 class=\"card-title mb-0\">Document Editing</h5>\n" +
                "                            </div>\n" +
                "                            <p class=\"card-text flex-grow-1\">\n" +
                "                                Collaboratively create and edit documents in real-time, with conflict resolution and version history.\n" +
                "                            </p>\n" +
                "                            <a href=\"#\" class=\"btn btn-primary mt-auto\">Go to Editing</a>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "\n" +
                "                <div class=\"col-12 col-md-6 col-xl-4\">\n" +
                "                    <div class=\"card border-success h-100\">\n" +
                "                        <div class=\"card-body d-flex flex-column\">\n" +
                "                            <div class=\"d-flex align-items-center mb-3\">\n" +
                "                                <i class=\"bi bi-person-fill fs-2 text-success me-3\"></i>\n" +
                "                                <h5 class=\"card-title mb-0\">User Management</h5>\n" +
                "                            </div>\n" +
                "                            <p class=\"card-text flex-grow-1\">\n" +
                "                                Manage users, roles, authentication, and authorization integrated with enterprise identity providers.\n" +
                "                            </p>\n" +
                "                            <a href=\"#\" class=\"btn btn-success mt-auto\">Manage Users</a>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "\n" +
                "                <div class=\"col-12 col-md-6 col-xl-4\">\n" +
                "                    <div class=\"card border-warning h-100\">\n" +
                "                        <div class=\"card-body d-flex flex-column\">\n" +
                "                            <div class=\"d-flex align-items-center mb-3\">\n" +
                "                                <i class=\"bi bi-shield-lock-fill fs-2 text-warning me-3\"></i>\n" +
                "                                <h5 class=\"card-title mb-0\">Digital Signature</h5>\n" +
                "                            </div>\n" +
                "                            <p class=\"card-text flex-grow-1\">\n" +
                "                                Apply and verify document digital signatures adhering to PKI and compliance standards.\n" +
                "                            </p>\n" +
                "                            <a href=\"#\" class=\"btn btn-warning mt-auto\">Signatures</a>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "\n" +
                "                <!-- Versioning -->\n" +
                "                <div class=\"col-12 col-md-6 col-xl-4\">\n" +
                "                    <div class=\"card border-info h-100\">\n" +
                "                        <div class=\"card-body d-flex flex-column\">\n" +
                "                            <div class=\"d-flex align-items-center mb-3\">\n" +
                "                                <i class=\"bi bi-clock-history fs-2 text-info me-3\"></i>\n" +
                "                                <h5 class=\"card-title mb-0\">Versioning</h5>\n" +
                "                            </div>\n" +
                "                            <p class=\"card-text flex-grow-1\">\n" +
                "                                Browse and restore document versions with immutable history for compliance and auditability.\n" +
                "                            </p>\n" +
                "                            <a href=\"#\" class=\"btn btn-info mt-auto\">View Versions</a>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "\n" +
                "                <!-- Scanning & AI Processing -->\n" +
                "                <div class=\"col-12 col-md-6 col-xl-4\">\n" +
                "                    <div class=\"card border-secondary h-100\">\n" +
                "                        <div class=\"card-body d-flex flex-column\">\n" +
                "                            <div class=\"d-flex align-items-center mb-3\">\n" +
                "                                <i class=\"bi bi-file-earmark-richtext fs-2 text-secondary me-3\"></i>\n" +
                "                                <h5 class=\"card-title mb-0\">Scanning & AI Processing</h5>\n" +
                "                            </div>\n" +
                "                            <p class=\"card-text flex-grow-1\">\n" +
                "                                Upload scanned documents, extract metadata using AI, index, and securely manage digital assets.\n" +
                "                            </p>\n" +
                "                            <a href=\"#\" class=\"btn btn-secondary mt-auto\">Scan Documents</a>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "\n" +
                "                <!-- Audit Logging -->\n" +
                "                <div class=\"col-12 col-md-6 col-xl-4\">\n" +
                "                    <div class=\"card border-dark h-100\">\n" +
                "                        <div class=\"card-body d-flex flex-column\">\n" +
                "                            <div class=\"d-flex align-items-center mb-3\">\n" +
                "                                <i class=\"bi bi-journal-text fs-2 text-dark me-3\"></i>\n" +
                "                                <h5 class=\"card-title mb-0\">Audit Logging</h5>\n" +
                "                            </div>\n" +
                "                            <p class=\"card-text flex-grow-1\">\n" +
                "                                Monitor and review audit trails of document edits, signature applications, and system events.\n" +
                "                            </p>\n" +
                "                            <a href=\"#\" class=\"btn btn-dark mt-auto\">View Audit Logs</a>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Bootstrap 5 JS Bundle -->\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>\n" +
                "</body>\n" +
                "</html>\n" +
                "\n" +
                "\n" +
                "---\n" +
                "\n" +
                "java\n" +
                "// src/main/java/tech/justjava/controller/HomeController.java\n" +
                "package tech.justjava.controller;\n" +
                "\n" +
                "import org.springframework.stereotype.Controller;\n" +
                "import org.springframework.web.bind.annotation.GetMapping;\n" +
                "\n" +
                "@Controller\n" +
                "public class HomeController {\n" +
                "\n" +
                "    @GetMapping({\"/\", \"/index\"})\n" +
                "    public String index() {\n" +
                "        return \"home\"; // resolves to home.html\n" +
                "    }\n" +
                "}");
        return "tasks/reviewLayoutCode";
    }
}
