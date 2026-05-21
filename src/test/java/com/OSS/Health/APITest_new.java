package com.OSS.Health;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.GetGithubApi;
import com.OSS.Health.service.GetGithubApi_new;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class APITest_new {
	private static final String REPO_PATH= "F://github_repos";
	private static final String FILE_PATH = "F://github_api_repos";

    @Test
    public void testService() throws Exception {
    	GetGithubApi_new test = new GetGithubApi_new(REPO_PATH, FILE_PATH);
    	test.storeGithubApi();
    	}
}
