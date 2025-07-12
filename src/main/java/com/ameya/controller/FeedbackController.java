package com.ameya.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @PostMapping("/submit")
    public ResponseEntity<String> submitFeedback(@RequestParam String feedback) {
        // Vulnerable to XSS
        return ResponseEntity.ok("Thank you for your feedback: " + feedback);
    }
}
