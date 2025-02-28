package com.OSS.Health.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GetGithubApi{
	private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String REPO_OWNER = "vuejs"; // 请替换为仓库的拥有者
    private static final String REPO_NAME = "core";   // 请替换为仓库名称
    private static final String REPO_PATH = "D:/Plateform/Git/repositories/core"; // 请替换为git仓库存储位置
    private static final String DEAFULT_FOLDER_NAME = "Github_Api_Message"; //在对应git仓库位置下存储保存的api信息
    private static final String GITHUB_TOKEN = "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";  // 使用你自己的GitHub Personal Access Token
    private RestTemplate restTemplate= new RestTemplate();
    private ObjectMapper objectMapper= new ObjectMapper();
    
    public boolean storeGithubApi() {
    	if(!initStoreGithubApi()) {
    		System.out.println("Init error, Please check the git repo file.");
    		return false;
    	}
    	
    	if(!storePRData()) {
    		System.out.println("PR get error.");
    		return false;
    	}
    	
    	if(!storePRReviewData()) {
    		System.out.println("PRReview get error.");
    		return false;
    	}
    	
    	return true;
    }
    
    private boolean initStoreGithubApi() {
    	File folder = new File(REPO_PATH, DEAFULT_FOLDER_NAME);
    	if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                return true;
            } else {
                return false;
            }
        } else {
        	return true;
        }
    }
    
    private boolean storePRData() {
    	System.out.println("Start storePRData.");
    	String fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/PRData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
    	int page = 1;
	    int perPage = 100; // 每页100个评论
	    String token = GITHUB_TOKEN;	    
	    // 设置认证信息
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("Authorization", "Bearer " + token);  // 添加认证信息
	    // 创建一个包含认证信息的请求实体
	    HttpEntity<String> entity = new HttpEntity<>(headers);
	    // 获取所有 PR
	    String urlTemplate = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/pulls?state=all&page=%d&per_page=%d";
	    // 创建一个 ArrayNode 来收集所有 PR 数据
        ArrayNode allPRs = objectMapper.createArrayNode();
	    while (true) {
	        // 请求当前页的PR数据
	        String url = String.format(urlTemplate, page, perPage);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
			JsonNode prArray = null;
            try {
                prArray = objectMapper.readTree(response.getBody());
            } catch (IOException e) {
                System.err.println("Error parsing the JSON response: " + e.getMessage());
                return false;
            }

	        // 如果当前页没有数据，跳出循环
	        if (prArray.isEmpty()) {
	            break;
	        }

	        // 将PR信息保存为json文件
	        for (JsonNode pr : prArray) {
	        	allPRs.add(pr);
	        }
	        // 增加页码，继续请求下一页PR
	        page++;
	    }
	    
	    try {
            saveToJsonFile(allPRs, "PRData");
        } catch (IOException e) {
            System.err.println("Error saving PR data to file: " + e.getMessage());
            return false;
        }
	    
	    System.out.println("prPages: " + (page-1));
	    
	    return true;
    }
    
    private void saveToJsonFile(JsonNode jsonData, String fileName) throws IOException {
        // 将 PR 数据保存为 JSON 文件
        String filePath = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/"+ fileName+ ".json";
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }else {
        	System.out.println("file already exist");
        	return;
        }

        // 将 PR 数据写入到文件
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, jsonData);
        System.out.println("Saved all data to: " + fileName);
    }
    
    private boolean storePRReviewData() {
    	System.out.println("Start storePRReviewData.");
    	
    	String fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/PRReviewData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
        
        fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/PRData.json";
        File prDataFile= new File(fileName);
        if (!prDataFile.exists()) {
            return false;
        }
        //读取PRData
        ArrayNode prArray;
        try {
            prArray = (ArrayNode) objectMapper.readTree(prDataFile);
        } catch (IOException e) {
            System.err.println("Error reading PR data from file: " + e.getMessage());
            return false;
        }
        
        int page = 1;
	    int perPage = 100; // 每页100个评论
	    String token = GITHUB_TOKEN;	    
	    // 设置认证信息
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("Authorization", "Bearer " + token);  // 添加认证信息
	    // 创建一个包含认证信息的请求实体
	    HttpEntity<String> entity = new HttpEntity<>(headers);
	    // 获取所有 PRReview
	    String urlTemplate = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/pulls/%d/reviews?page=%d&per_page=%d";
	    // 创建一个 ArrayNode 来收集所有 PRReview 数据
	    ObjectNode allPRReviews = objectMapper.createObjectNode();
	    // 每读取 200 个, 显示一次进度
	    int getedApiNum=0;
	    // 获取review数据并且保存到 allPRReviews 中
        for (JsonNode pr : prArray) {
            int prNumber = pr.get("number").asInt();  // 获取当前 PR 的 number

            // 获取当前 PR 的 review 数据
            page= 1;
            boolean hasReviews = true;
            ArrayNode prReviews = objectMapper.createArrayNode();
            
            while (hasReviews) {
                // 构建请求 URL
                String url = String.format(urlTemplate, prNumber, page, perPage);
                ResponseEntity<String> response = null;

                try {
                	response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);                    
                    // 检查 Rate Limit
                    checkRateLimit(response);
                    // 每读取 200 个, 显示一次进度
                    ++getedApiNum;
                    if(getedApiNum% 200 == 0) {
                    	System.out.println("Get api num: "+ getedApiNum);
                    }
                    
                    JsonNode reviews = objectMapper.readTree(response.getBody());
                    if (reviews.isEmpty()) {
                        hasReviews = false;  // 如果没有更多 reviews，退出循环
                    } else {
                        // 将当前页的 reviews 添加到 prReviews 中
                        prReviews.addAll((ArrayNode) reviews);
                        page++;  // 增加页码，继续请求下一页
                    }
                } catch (IOException e) {
                    System.err.println("Error reading review data from response: " + e.getMessage());
                    return false;
                } catch (InterruptedException e) {
                    System.err.println("Rate limit exceeded. Retrying after sleep.");
                    return false;
                }
            }

            // 将获取到的 review 数据放入对应的 PR Number 下
            if (prReviews.size() > 0) {
                allPRReviews.set(String.valueOf(prNumber), prReviews);
            }
        }
        
	    try {
            saveToJsonFile(allPRReviews, "PRReviewData");
        } catch (IOException e) {
            System.err.println("Error saving PRReview data to file: " + e.getMessage());
            return false;
        }
  	
    	return true;
    }
    
    private void checkRateLimit(ResponseEntity<String> response) throws InterruptedException {
        // 获取剩余的请求次数
        String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");
        // 获取重置时间
        String reset = response.getHeaders().getFirst("X-RateLimit-Reset");

        if (remaining != null && Integer.parseInt(remaining) <= 0) {
            // 如果剩余请求次数为 0，计算需要等待的时间
            long resetTime = Long.parseLong(reset);
            long currentTime = System.currentTimeMillis() / 1000;
            long waitTime = resetTime - currentTime + 1;  // 等待直到 rate limit 重置

            Date currentDate = new Date(currentTime * 1000);
            System.out.println("Rate limit exceeded. Sleeping for " + waitTime + " seconds."+ "Now time: "+ currentDate);
            Thread.sleep(waitTime * 1000);  // 休眠，直到重置时间
            System.out.println("Rate limit refresh.");
        }
    }
    
    private boolean storePRRelatedIssueData() {
    	System.out.println("Start storePRRelatedIssueData.");
    	
    	String fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/PRRelatedIssueData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
        
        fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/PRData.json";
        File prDataFile= new File(fileName);
        if (!prDataFile.exists()) {
            return false;
        }
        //读取PRData
        ArrayNode prArray;
        try {
            prArray = (ArrayNode) objectMapper.readTree(prDataFile);
        } catch (IOException e) {
            System.err.println("Error reading PR data from file: " + e.getMessage());
            return false;
        }
        
        int page = 1;
	    int perPage = 100; // 每页100个评论
	    String token = GITHUB_TOKEN;	    
	    // 设置认证信息
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("Authorization", "Bearer " + token);  // 添加认证信息
	    // 创建一个包含认证信息的请求实体
	    HttpEntity<String> entity = new HttpEntity<>(headers);
	    // 获取所有 PRReview
	    String urlTemplate = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/pulls/%d/reviews?page=%d&per_page=%d";
	    // 创建一个 ArrayNode 来收集所有 PRRelatedIssue 数据
	    ObjectNode allPRRelatedIssues = objectMapper.createObjectNode();
	    // 每读取 200 个, 显示一次进度
	    int getedApiNum=0;
	    // 获取RelatedIssue数据并且保存到 allPRRelatedIssues 中
        for (JsonNode pr : prArray) {
        	
        }
        
	    try {
            saveToJsonFile(allPRRelatedIssues, "PRRelatedIssueData");
        } catch (IOException e) {
            System.err.println("Error saving PRRelatedIssue data to file: " + e.getMessage());
            return false;
        }
  	
    	return true;
    }
}