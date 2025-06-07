package com.ameya.controller;

import com.ameya.service.SonarQubeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class QualityReportController {

    private final SonarQubeService sonarService;

    public QualityReportController(SonarQubeService sonarService) {
        this.sonarService = sonarService;
    }

    @GetMapping("/quality-report/{projectKey}")
    public String getReport(@PathVariable String projectKey) {
        return sonarService.fetchMetrics(projectKey);
    }
}
