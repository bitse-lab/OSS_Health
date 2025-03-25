package com.OSS.Health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.community.organization.*;
import com.OSS.Health.service.market.influence.*;
import com.OSS.Health.service.software.productivity.*;

@SpringBootTest
public class metricsCalculateTest {

    @Autowired
    private MonthPRService test;

    @Test
    public void testService() throws Exception {
        // 调用服务方法
        test.generateMonthlyReport();
    }
}
