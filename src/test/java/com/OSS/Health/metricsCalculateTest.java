package com.OSS.Health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.community.resilience.ReviewRatioService;

@SpringBootTest
public class metricsCalculateTest {

    @Autowired
    private ReviewRatioService test;

    @Test
    public void testService() throws Exception {
        // 调用服务方法
        test.generateMonthlyReport();
    }
}
