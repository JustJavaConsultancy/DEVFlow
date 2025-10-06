package com.justjava.devFlow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PrepareProjectConfigurationDelegate implements JavaDelegate {

    @Value("${app.github.token}")
    private String githubToken;

    @Value("${app.github.username}")
    private String githubUsername;

    @Override
    public void execute(DelegateExecution execution) {
        // Get project configuration from process variables or set defaults

        System.out.println(" Welcome to this delegate (prepareProjectConfigurationDelegate) ");
        execution.setVariable("groupId","tech.justjava");
        execution.setVariable("artifactId",String.valueOf(execution.getVariable("projectName")));
        //execution.setVariable("packageName","tech.justjava"+".JDocMan".toLowerCase());
        execution.setVariable("javaVersion","21");
        execution.setVariable("springBootVersion","3.5.5");
        execution.setVariable("outputDir","../workspace");


        // Add dependencies (Spring Web as example)
        String  dependencies = "web,data-jpa,thymeleaf,oauth2-client,lombok,devtools,validation,security";//buildDependenciesList();

        System.out.println(" dependencies==="+dependencies);
        execution.setVariable("dependencies",dependencies);



        // ✅ Get GitHub configuration from process variables
        execution.setVariable("githubUsername", githubUsername);
        execution.setVariable("githubToken",githubToken);
        execution.setVariable("repositoryDescription", "Spring Boot project: " + execution.getVariable("projectDescription"));
        execution.setVariable("isPrivateRepo", true);



        // Prepare headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        //headers.put("User-Agent", "Flowable-Process/1.0");



    }
    private List<Map<String, String>> buildDependenciesList() {
        List<Map<String, String>> dependencies = new ArrayList<>();
        dependencies.add(createDependency("web", "Spring Web"));
        dependencies.add(createDependency("postgresql", "PostgreSQL Driver"));
        dependencies.add(createDependency("data-jpa", "Spring Data JPA"));
        dependencies.add(createDependency("thymeleaf", "Thymeleaf"));
        dependencies.add(createDependency("oauth2-client", "OAuth2 Client"));
        dependencies.add(createDependency("lombok", "Lombok"));
        dependencies.add(createDependency("devtools", "Spring Boot DevTools"));
        dependencies.add(createDependency("validation", "Validation"));
        dependencies.add(createDependency("security", "Spring Security"));
        return dependencies;
    }
    private Map<String, String> createDependency(String id, String name) {
        Map<String, String> dependency = new HashMap<>();
        dependency.put("id", id);
        dependency.put("name", name);
        return dependency;
    }
}
