package com.ameya.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/server")
public class ServerController {

    @GetMapping("/execute")
    public ResponseEntity<String> executeCommand(@RequestParam String command) throws IOException {
        // Vulnerable to Command Injection
        String result = executeSystemCommand(command);
        return ResponseEntity.ok(result);
    }

    private String executeSystemCommand(String command) throws IOException {
        return new String(Runtime.getRuntime().exec("echo 'Blocked unsafe command'").getInputStream().readAllBytes());
    }
}
