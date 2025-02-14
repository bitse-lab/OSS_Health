package com.OSS.Health.controller;

import com.OSS.Health.service.community.vigor.CodeContributorCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8081")  // 允许来自 localhost:8081 的跨域请求
public class CodeContributorCount {

	@Autowired
    private CodeContributorCountService service;

    @GetMapping("/api/codecontributorcount/total")
    public List<Map<String, Object>> getCodeContributorCount() {
        return service.getMysqlData();
    }
    
    @GetMapping("/api/codecontributorcount/codecommiter")
    public List<Map<String, Object>> getCodeCommitter() {
        return service.getCodeCommitter();
    }
    
    @GetMapping("/api/codecontributorcount/prsubmitter")
    public List<Map<String, Object>> getPRSubmitter() {
        return service.getPRSubmitter();
    }
    
    @GetMapping("/api/codecontributorcount/reviewer")
    public List<Map<String, Object>> getReviewer() {
        return service.getReviewer();
    }
}
