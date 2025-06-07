package com.ameya.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SonarQubeService {

    @Value("${sonarqube.url}")
    private String sonarUrl;

    @Value("${sonarqube.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchMetrics(String projectKey) {
        String endpoint = sonarUrl + "/api/measures/component?component=" + projectKey
                + "&metricKeys=coverage,bugs,vulnerabilities,code_smells";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(token, ""); // Username = token, password empty
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                endpoint, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }
}

