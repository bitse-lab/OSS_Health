package com.OSS.Health;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.GetGithubApi;

@SpringBootTest
public class APITest_SpecialFunction {
	private static final String CLONE_PATH= "E:/GithubRep";
	private static final String DEAFULT_FOLDER_NAME = "Github_Api_Message";
	private static final String repoName = "paddlepaddle/paddle-lite";
	
	

    @Test
    public void testService() throws Exception {    
        if (!repoName.contains("/")) {
    	    System.out.println("Invalid repository name: " + repoName);
    	    return;
    	}
    	String repoNameOnly = repoName.substring(repoName.lastIndexOf("/") + 1);
    	String repoOwnerOnly = repoName.substring(0, repoName.lastIndexOf("/"));

        String localPath = CLONE_PATH + File.separator + repoNameOnly;

        File repoDir = new File(localPath);
        if (!repoDir.exists()) {
            System.out.println("Repository doesn't exist, skipping: " + localPath);
            return;
        }
        try {
            GetGithubApi test = new GetGithubApi(repoOwnerOnly, repoNameOnly, localPath);
            // 在此行选择需要的方法
            if(!test.storeIssueData_GraphQL()) {
                System.out.println(repoName + " Test error.");
            } else {
                System.out.println(repoName + " Test successful.");
            }
        } catch (Exception e) {
            System.out.println("Failed to process " + repoName + ": " + e.getMessage());
        }
    }
}
