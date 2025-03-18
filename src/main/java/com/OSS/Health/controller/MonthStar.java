package com.OSS.Health.controller;

import com.OSS.Health.service.market.influence.MonthStarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8081")  // 允许来自 localhost:8081 的跨域请求
public class MonthStar {

	@Autowired
    private MonthStarService service;

    @GetMapping("/api/monthstar")
    public List<Map<String, Object>> getMonthStar() {
        return service.getMysqlData();
    }
}
