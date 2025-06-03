package com.OSS.Health.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.logging.log4j.message.Message;
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
	private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";
//    private static final String REPO_OWNER = "mozilla"; // 请替换为仓库的拥有者
//    private static final String REPO_NAME = "DeepSpeech";   // 请替换为仓库名称
//    private static final String REPO_PATH = "E:/GithubRep/DeepSpeech"; // 请替换为git仓库存储位置
    private static final String DEAFULT_FOLDER_NAME = "Github_Api_Message"; //在对应git仓库位置下存储保存的api信息
    private static final String GITHUB_TOKEN = "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";  // 使用你自己的GitHub Personal Access Token
    private static final String GITHUB_TOKEN1 = "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"; //备用token
    
    private final String REPO_OWNER;
    private final String REPO_NAME;
    private final String REPO_PATH;
    private String IN_USE_TOKEN= GITHUB_TOKEN;
    
    private RestTemplate restTemplate= new RestTemplate();
    private ObjectMapper objectMapper= new ObjectMapper();
    
    public GetGithubApi(String repoOwner, String repoName, String repoPath) {
        this.REPO_OWNER = repoOwner;
        this.REPO_NAME = repoName;
        this.REPO_PATH = repoPath;
    }
    
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
    	
    	if(!storeStarData()) {
    		System.out.println("Star get error.");
    		return false;
    	}
    	
    	if(!storeForkData()) {
    		System.out.println("Fork get error.");
    		return false;
    	}
    	
    	if(!storeIssueData()) {
    		System.out.println("Issue get error.");
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
	    // 获取所有 PR
	    String urlTemplate = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/pulls?state=all&page=%d&per_page=%d";
	    // 创建一个 ArrayNode 来收集所有 PR 数据
        ArrayNode allPRs = objectMapper.createArrayNode();
	    while (true) {
	        // 请求当前页的PR数据
	        String url = String.format(urlTemplate, page, perPage);
			// ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
	        ResponseEntity<String> response = null;
	        try {
	        	response = fetchResponse(restTemplate, url);
	        } catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
	        if(response== null) return false;

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

                try {
                	ResponseEntity<String> response = null;
                	try {
        	        	response = fetchResponse(restTemplate, url);
        	        } catch (InterruptedException e) {
        				System.out.println(e.getMessage());
        			}
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
    
    private boolean storeStarData() {
        System.out.println("Start storeStarData.");
        String fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/StarData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }

        // GraphQL 查询模板
        String queryTemplate = "{ \"query\": \"query { repository(owner: \\\"%s\\\", name: \\\"%s\\\") { stargazers(first: 100, after: %s) { edges { starredAt node { login } } pageInfo { endCursor hasNextPage } } } }\" }";

        String endCursor = "null"; // 初始时无游标
        boolean hasNextPage = true;
        
        ArrayNode allStars = objectMapper.createArrayNode();

        while (hasNextPage) {
            // 构造 GraphQL 查询
        	String query = String.format(queryTemplate, REPO_OWNER, REPO_NAME, endCursor.equals("null") ? "null" : ("\\\"" + endCursor + "\\\""));

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(query, headers);
            // ResponseEntity<String> response = restTemplate.exchange(GITHUB_GRAPHQL_URL, HttpMethod.POST, entity, String.class);
            ResponseEntity<String> response = null;
            try {
	        	response = fetchResponse_GraphQL(restTemplate, GITHUB_GRAPHQL_URL, entity);
	        } catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
            
            JsonNode jsonResponse;
            try {
                jsonResponse = objectMapper.readTree(response.getBody());
            } catch (IOException e) {
                System.err.println("Error parsing the JSON response: " + e.getMessage());
                return false;
            }

            // 解析数据
            JsonNode edges = jsonResponse.at("/data/repository/stargazers/edges");
            if (edges.isArray()) {
                for (JsonNode edge : edges) {
                    allStars.add(edge);
                }
            }

            // 获取分页信息
            hasNextPage = jsonResponse.at("/data/repository/stargazers/pageInfo/hasNextPage").asBoolean();
            endCursor = jsonResponse.at("/data/repository/stargazers/pageInfo/endCursor").asText();
        }

        // 保存 JSON 文件
        try {
            saveToJsonFile(allStars, "StarData");
        } catch (IOException e) {
            System.err.println("Error saving Star data to file: " + e.getMessage());
            return false;
        }

        return true;
    }

    
    private boolean storeForkData() {
        System.out.println("Start storeForkData.");
        String fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/ForkData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }

        // GraphQL 查询模板
        String queryTemplate = "{ \"query\": \"query { repository(owner: \\\"%s\\\", name: \\\"%s\\\") { forks(first: 100, after: %s) { edges { node { nameWithOwner createdAt } } pageInfo { endCursor hasNextPage } } } }\" }";

        String endCursor = "null"; // 初始时无游标
        boolean hasNextPage = true;

        ArrayNode allForks = objectMapper.createArrayNode();

        while (hasNextPage) {
            // 构造 GraphQL 查询
            String query = String.format(queryTemplate, REPO_OWNER, REPO_NAME, endCursor.equals("null") ? "null" : ("\\\"" + endCursor + "\\\""));

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(query, headers);
            // ResponseEntity<String> response = restTemplate.exchange(GITHUB_GRAPHQL_URL, HttpMethod.POST, entity, String.class);
            ResponseEntity<String> response = null;
            try {
	        	response = fetchResponse_GraphQL(restTemplate, GITHUB_GRAPHQL_URL, entity);
	        } catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}

            JsonNode jsonResponse;
            try {
                jsonResponse = objectMapper.readTree(response.getBody());
            } catch (IOException e) {
                System.err.println("Error parsing the JSON response: " + e.getMessage());
                return false;
            }

            // 解析 Fork 数据
            JsonNode edges = jsonResponse.at("/data/repository/forks/edges");
            if (edges.isArray()) {
                for (JsonNode edge : edges) {
                    allForks.add(edge);
                }
            	}

            // 获取分页信息
            hasNextPage = jsonResponse.at("/data/repository/forks/pageInfo/hasNextPage").asBoolean();
            endCursor = jsonResponse.at("/data/repository/forks/pageInfo/endCursor").asText();
        }

        // 保存 JSON 文件
        try {
            saveToJsonFile(allForks, "ForkData");
        } catch (IOException e) {
            System.err.println("Error saving Fork data to file: " + e.getMessage());
            return false;
        }

        return true;
    }
    
    private boolean storeIssueData() {
    	System.out.println("Start storeIssueData.");
    	String fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/IssueData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
    	int page = 1;
	    int perPage = 100; // 每页100个
	    // 获取所有 Issue
	    String urlTemplate = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/issues?state=all&per_page=" + perPage + "&page=%d";
	    // 创建一个 ArrayNode 来收集所有 Issue 数据
        ArrayNode allIssues = objectMapper.createArrayNode();
        int getedApiNum= 0;
	    while (true) {
	    	// 构建请求 URL
	    	String url = String.format(urlTemplate, page);

            try {
            	ResponseEntity<String> response = null;
                try {
    	        	response = fetchResponse(restTemplate, url);
    	        } catch (InterruptedException e) {
    				System.out.println(e.getMessage());
    			}
                // 每读取 200 个, 显示一次进度
                ++getedApiNum;
                if(getedApiNum% 200 == 0) {
                	System.out.println("Get api num: "+ getedApiNum);
                }
                
                JsonNode issueArray = objectMapper.readTree(response.getBody());
                if (issueArray.isEmpty()) {
                    break;
                }
                for (JsonNode issue : issueArray) {
    	            // 过滤掉 PR（Issues API 会返回 PR 和 Issue）
    	            if (issue.get("pull_request") == null) {
    	                allIssues.add(issue);
    	            }
    	        }
                ++page;
            } catch (IOException e) {
                System.err.println("Error reading issue data from response: " + e.getMessage());
                return false;
            } 
	    }
	    
	    try {
            saveToJsonFile(allIssues, "IssueData");
        } catch (IOException e) {
            System.err.println("Error saving Issue data to file: " + e.getMessage());
            return false;
        }
	    
	    System.out.println("issuePages: " + (page-1));
	    
	    return true;
    }
    
    public boolean storeIssueData_GraphQL() {
        System.out.println("Start storeIssueData.");
        String fileName = REPO_PATH + "/" + DEAFULT_FOLDER_NAME + "/IssueData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }

        String queryTemplate = "{ \"query\": \"query { repository(owner: \\\"%s\\\", name: \\\"%s\\\") { issues(first: 100, after: %s, states: [OPEN, CLOSED]) { edges { node { number title body state createdAt closedAt author { login } comments { totalCount } reactions { totalCount } } } pageInfo { endCursor hasNextPage } } } }\" }";

        String endCursor = "null";
        boolean hasNextPage = true;

        ArrayNode allIssues = objectMapper.createArrayNode();

        while (hasNextPage) {
            String query = String.format(queryTemplate, REPO_OWNER, REPO_NAME, endCursor.equals("null") ? "null" : ("\\\"" + endCursor + "\\\""));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(query, headers);
            ResponseEntity<String> response = null;
            try {
                response = fetchResponse_GraphQL(restTemplate, GITHUB_GRAPHQL_URL, entity);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }

            JsonNode jsonResponse;
            try {
                jsonResponse = objectMapper.readTree(response.getBody());
            } catch (IOException e) {
                System.err.println("Error parsing the JSON response: " + e.getMessage());
                return false;
            }

            JsonNode edges = jsonResponse.at("/data/repository/issues/edges");
            if (edges.isArray()) {
                for (JsonNode edge : edges) {
                	JsonNode node = edge.get("node");
                    if (node != null) {
                        allIssues.add(node);
                    }
                }
            }

            hasNextPage = jsonResponse.at("/data/repository/issues/pageInfo/hasNextPage").asBoolean();
            endCursor = jsonResponse.at("/data/repository/issues/pageInfo/endCursor").asText();
        }

        try {
            saveToJsonFile(allIssues, "IssueData");
        } catch (IOException e) {
            System.err.println("Error saving Issue data to file: " + e.getMessage());
            return false;
        }

        return true;
    }
    
    private void checkRateLimit(ResponseEntity<String> response) throws InterruptedException {
        // 获取剩余的请求次数
        String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");
        // 获取重置时间
        String reset = response.getHeaders().getFirst("X-RateLimit-Reset");

        if (remaining != null && Integer.parseInt(remaining) <= 20) {
            // 如果剩余请求次数小于 20，计算需要等待的时间
            long resetTime = Long.parseLong(reset);
            long currentTime = System.currentTimeMillis() / 1000;
            long waitTime = resetTime - currentTime + 1;  // 等待直到 rate limit 重置

            Date currentDate = new Date(currentTime * 1000);
            System.out.println("Rate limit exceeded. Sleeping for " + waitTime + " seconds."+ "Now time: "+ currentDate);
            Thread.sleep(waitTime * 1000);  // 休眠，直到重置时间
            System.out.println("Rate limit refresh.");
        }
    } 
    
    // 如果速率被限制了就切换token，返回false，需要切换；yes不用切换
    private boolean checkRateLimit_new(ResponseEntity<String> response) throws InterruptedException {
        // 获取剩余的请求次数
        String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");

        if (remaining != null && Integer.parseInt(remaining) <= 20) {
            return false;
        }
        return true;
    }  
    
    private void switchToken() {
        if (IN_USE_TOKEN.equals(GITHUB_TOKEN)) {
            IN_USE_TOKEN = GITHUB_TOKEN1;
        } else {
            IN_USE_TOKEN = GITHUB_TOKEN;
        }
        System.out.println("Switch Token");
    }
    
    // 将 checkRateLimit_new 和 checkRateLimit 合并封装，返回response
    private ResponseEntity<String> fetchResponse(
            RestTemplate restTemplate, String url) throws InterruptedException{

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = null;
        
        try {
        	response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch(Exception e) {
        	System.err.println(e.getMessage());
        }

        if (response == null || !checkRateLimit_new(response)) {
        	switchToken();
        	// 如果依旧被限速就进行等待
        	url = "https://api.github.com/rate_limit";

            headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
            entity = new HttpEntity<>(headers);
            ResponseEntity<String> responseTmp= null;

            try {
                responseTmp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            } catch (Exception e) {
                System.err.println("Error fetching rate limit info: " + e.getMessage());
            }

            if (responseTmp == null || !checkRateLimit_new(responseTmp)) {
            	checkRateLimit(responseTmp);
            }
        }

        return response;
    }
    
    private ResponseEntity<String> fetchResponse_GraphQL(
            RestTemplate restTemplate, String url, HttpEntity<String> entity) throws InterruptedException {

        ResponseEntity<String> response = null;
        try {
        	response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch(Exception e) {
        	System.err.println(e.getMessage());
        }

        if (response == null || !checkRateLimit_new(response)) {
        	switchToken();
        	// 请求 /rate_limit，检测备用 token 是否有效
            String rateLimitUrl = "https://api.github.com/rate_limit";
            HttpHeaders newHeaders = new HttpHeaders();
            newHeaders.set("Authorization", "Bearer " + IN_USE_TOKEN);
            HttpEntity<String> newEntity = new HttpEntity<>(newHeaders);
            ResponseEntity<String> responseTmp= null;
        	try {
        	    responseTmp = restTemplate.exchange(rateLimitUrl, HttpMethod.GET, newEntity, String.class);
        	} catch (Exception e) {
        	    System.err.println(e.getMessage());
        	}

            if (responseTmp == null || !checkRateLimit_new(responseTmp)) {
                checkRateLimit(responseTmp);
            }
        }

        return response;
    }

}