package com.OSS.Health;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.CalculateAllMetrics;
import com.OSS.Health.service.software.SonarQubeApi_new;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class CalculateSampleRepTest_SonarQubeApi {
	private static final String SAMPLE_REPO_JSON= "D:/Plateform/Git/repositories/OSS_Health/resources/sampleRep_deep-learning_1_new.json";
	private static final String CLONE_PATH= "E:/GithubRep";
	
	@Autowired
	private ApplicationContext context;

    @Test
    public void testService() throws Exception {
        // 解析 JSON
        ObjectMapper mapper = new ObjectMapper();
        List<RepoInfo> repos = mapper.readValue(new File(SAMPLE_REPO_JSON), new TypeReference<List<RepoInfo>>() {});

        for (RepoInfo repo : repos) {
        	System.out.println("Start to analyze: " + repo.name);
        	if (!repo.name.contains("/")) {
        	    System.out.println("Invalid repository name: " + repo.name);
        	    continue;
        	}
        	String repoNameOnly = repo.name.substring(repo.name.lastIndexOf("/") + 1);
        	String repoOwnerOnly = repo.name.substring(0, repo.name.lastIndexOf("/"));

            String localPath = CLONE_PATH + File.separator + repoNameOnly;

            File repoDir = new File(localPath);
            if (!repoDir.exists()) {
                System.out.println("Repository doesn't exist, skipping: " + localPath);
                continue;
            }
            try {
            	SonarQubeApi_new test = context.getBean(SonarQubeApi_new.class);
                test.init(repoOwnerOnly, generateSafeTableName(repoOwnerOnly, repoNameOnly), localPath);
                test.analyzeProjectByMonth();
            } catch (Exception e) {
                System.out.println("Failed to process " + repo.name + ": " + e.getMessage());
            }
            // break;
        }
    }
    
    // 获得安全的表名，防止超出64字符
    public static String generateSafeTableName(String owner, String name) {
        String prefix = owner + "_";
        int maxLength = 64;

        int allowedNameLength = maxLength - prefix.length();
        if (name.length() <= allowedNameLength) {
            return name;
        }

        return name.substring(0, allowedNameLength);
    }
    
    private static class RepoInfo {
        public String name;
        public int stargazers;
        public int rank;
        
        // 添加无参构造函数
        public RepoInfo() {}

        public RepoInfo(String name, int stargazers, int rank) {
            this.name = name;
            this.stargazers = stargazers;
            this.rank = rank;
        }
    }
}
