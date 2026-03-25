package com.cafeteriamanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiDocumentationController {

    @GetMapping({"/api", "/api/"})
    public String redirectToSwaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
