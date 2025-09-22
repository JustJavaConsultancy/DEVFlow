package com.justjava.devFlow.config;

import com.justjava.devFlow.aau.AuthenticationManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
    @Autowired
    AuthenticationManager authenticationManager;

    @ModelAttribute("currentPath")
    public String getCurrentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
    @ModelAttribute("userName")
    public String addUserName(HttpServletRequest request) {
            return (String) authenticationManager.get("name");
    }
}
