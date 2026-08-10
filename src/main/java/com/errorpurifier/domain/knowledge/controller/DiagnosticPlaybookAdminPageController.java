package com.errorpurifier.domain.knowledge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DiagnosticPlaybookAdminPageController {

    @GetMapping("/admin")
    public String redirectToAdminPage() {
        return "redirect:/admin/";
    }

    @GetMapping("/admin/")
    public String adminPage() {
        return "forward:/admin/index.html";
    }
}
