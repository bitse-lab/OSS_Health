package com.OSS.Health.controller;

import com.OSS.Health.model.LongTermContributor;
import com.OSS.Health.service.community.vigor.LongTermContributorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:8081")  // 允许来自 localhost:8081 的跨域请求
public class LongTermContributorController {

	@Autowired
    private LongTermContributorService longTermContributorService;

    @GetMapping("/api/longtermcontributors")
    public List<LongTermContributor> getLongTermContributor() {
        return longTermContributorService.getLongTermContributor();
    }
}
