package com.OSS.Health;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.GetGithubApi;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class APITest {
	private static final String SAMPLE_REPO_JSON= "D:/Plateform/Git/repositories/OSS_Health/resources/sampleRep_deep-learning_1_new.json";
	private static final String CLONE_PATH= "E:/GithubRep";
	private static final String DEAFULT_FOLDER_NAME = "Github_Api_Message";

    @Test
    public void testService() throws Exception {
        // 解析 JSON
        ObjectMapper mapper = new ObjectMapper();
        List<RepoInfo> repos = mapper.readValue(new File(SAMPLE_REPO_JSON), new TypeReference<List<RepoInfo>>() {});

        for (RepoInfo repo : repos) {
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
//            File apiDir= new File(localPath+ File.separator+ DEAFULT_FOLDER_NAME);
//            if (apiDir.exists()) {
//                System.out.println("Repository api have been gotten, skipping: " + localPath);
//                continue;
//            }
            try {
                GetGithubApi test = new GetGithubApi(repoOwnerOnly, repoNameOnly, localPath);
                if(!test.storeGithubApi()) {
                    System.out.println(repo.name + " Test error.");
                } else {
                    System.out.println(repo.name + " Test successful.");
                }
            } catch (Exception e) {
                System.out.println("Failed to process " + repo.name + ": " + e.getMessage());
            }
        }
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
