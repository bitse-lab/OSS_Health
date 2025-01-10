package com.OSS.Health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.community.vigor.LongTermContributorService;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class LongTermContributorTest {

    @Autowired
    private LongTermContributorService longTermContributorService;

    @Test
    public void testService() throws Exception {
        // 调用服务方法
        longTermContributorService.generateMonthlyReport();
    }
}
