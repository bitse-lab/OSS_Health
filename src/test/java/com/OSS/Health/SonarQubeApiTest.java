package com.OSS.Health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.SonarQubeApi;

@SpringBootTest
public class SonarQubeApiTest {
	@Autowired
    private SonarQubeApi test;

    @Test
    public void testService() throws Exception {
        // 调用服务方法
        if(!test.GetSonarQubeApi()) {
        	System.out.println("Test error.");
        }else {
        	System.out.println("Test successful.");
        }
    }
}
