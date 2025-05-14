package com.OSS.Health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.GetSampleRep;

@SpringBootTest
public class GetSampleRepTest {
	@Autowired
    private GetSampleRep test;

    @Test
    public void testService() throws Exception {
        // 调用服务方法
        if(!test.multiGitClone()) {
        	System.out.println("Test error.");
        }else {
        	System.out.println("Test successful.");
        }
    }
}
