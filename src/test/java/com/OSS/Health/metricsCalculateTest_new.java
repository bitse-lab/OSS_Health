package com.OSS.Health;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.CalculateAllMetrics;
import com.OSS.Health.service.community.organization.*;
import com.OSS.Health.service.community.resilience.*;
import com.OSS.Health.service.community.vigor.*;
import com.OSS.Health.service.market.influence.*;
import com.OSS.Health.service.software.productivity.*;

@SpringBootTest
public class metricsCalculateTest_new {
	private static final String CLONE_PATH= "D:/Plateform/Git/repositories";
	private static final String repoName = "vuejs/core";
	
	@Autowired
    private CalculateAllMetrics test;

	@Test
	public void testService() throws Exception {
	    if (!repoName.contains("/")) {
	        System.out.println("Invalid repository name: " + repoName);
	        return;
	    }

	    String repoNameOnly = repoName.substring(repoName.lastIndexOf("/") + 1);
	    String repoOwnerOnly = repoName.substring(0, repoName.lastIndexOf("/"));
	    String localPath = CLONE_PATH + File.separator + repoNameOnly;

	    // 检查仓库路径是否存在
	    File repoDir = new File(localPath);
	    if (!repoDir.exists()) {
	        System.err.println("Repo directory does not exist: " + localPath);
	        return;
	    }

	    // 实例化你的服务类（不更改原类）
	    test.init(repoOwnerOnly, repoNameOnly, localPath);

	    try {
	        System.out.println("开始执行 generateMonthlyReport...");
	        test.calculateAllMetrics();
	        System.out.println("执行完毕。");

	        // 可以在这里添加额外断言或验证
	        // e.g. 读取数据库验证结果、输出日志等
	    } catch (Exception e) {
	        System.err.println("测试中发生异常: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

}
