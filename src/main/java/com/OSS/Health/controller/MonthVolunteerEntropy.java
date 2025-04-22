package com.OSS.Health.controller;

import com.OSS.Health.service.community.organization.MonthVolunteerEntropyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8081")  // 允许来自 localhost:8081 的跨域请求
public class MonthVolunteerEntropy {

	@Autowired
    private MonthVolunteerEntropyService service;

    @GetMapping("/api/monthvolunteerentropy")
    public List<Map<String, Object>> getMonthOrgEntropy() {
        return service.getMysqlData();
    }
}
