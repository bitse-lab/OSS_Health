package com.OSS.Health.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.OSS.Health.service.software.SonarQubeApi;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8081")  // 允许来自 localhost:8081 的跨域请求
public class Software {

	@Autowired
    private SonarQubeApi service;

    @GetMapping("/api/technicaldebt")
    public List<Map<String, Object>> getTechnicalDebt() {
        return service.getMysqlData("1.1.1");
    }
    @GetMapping("/api/bugs")
    public List<Map<String, Object>> getBugs() {
        return service.getMysqlData("1.1.2");
    }
    @GetMapping("/api/codesmells")
    public List<Map<String, Object>> getCodeSmells() {
        return service.getMysqlData("1.1.3");
    }
    @GetMapping("/api/duplicatedlinesdensity")
    public List<Map<String, Object>> getDuplicatedLinesDensity() {
        return service.getMysqlData("1.1.4");
    }
    @GetMapping("/api/complexity")
    public List<Map<String, Object>> getComplexity() {
        return service.getMysqlData("1.2.1");
    }
    @GetMapping("/api/cognitivecomplexity")
    public List<Map<String, Object>> getCognitiveComplexity() {
        return service.getMysqlData("1.2.2");
    }
    @GetMapping("/api/vulnerabilities")
    public List<Map<String, Object>> getVulnerabilities() {
        return service.getMysqlData("1.2.3");
    }
    @GetMapping("/api/commentlinesdensity")
    public List<Map<String, Object>> getCommentLinesDensity() {
        return service.getMysqlData("1.2.4");
    }
    @GetMapping("/api/lines")
    public List<Map<String, Object>> getLines() {
        return service.getMysqlData("1.3.1");
    }
    @GetMapping("/api/ncloc")
    public List<Map<String, Object>> getNcloc() {
        return service.getMysqlData("1.3.2");
    }
}
