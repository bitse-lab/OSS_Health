package com.OSS.Health;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.OSS.Health.service.GetGithubApi;

@SpringBootTest
public class simpleApiTest{
	private static final String repoName = "cann/cann-recipes-infer";
    @Test
    public void testService() throws Exception {    
        if (!repoName.contains("/")) {
    	    System.out.println("Invalid repository name: " + repoName);
    	    return;
    	}
    	String repoNameOnly = repoName.substring(repoName.lastIndexOf("/") + 1);
    	String repoOwnerOnly = repoName.substring(0, repoName.lastIndexOf("/"));
        try {
        	RestTemplate restTemplate = new RestTemplate();
        	String url = String.format(
                    "https://api.gitcode.com/api/v5/repos/%s/%s/commits",
                    repoOwnerOnly, repoNameOnly
                );
        	HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Authorization", "Bearer " + "tT1stzb1yuYsq8gtJtcFJAdX");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
                );
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Commits fetched successfully:");
                System.out.println(response.getHeaders());
                System.out.println(response.getBody());
            } else {
                System.out.println("Failed to fetch commits. HTTP status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Failed to process " + repoName + ": " + e.getMessage());
        }
    }
}