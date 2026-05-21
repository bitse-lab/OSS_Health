package com.OSS.Health.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.message.Message;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GetGithubApi_new{
	private static final String GITHUB_API_URL = "https://api.github.com";
	private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";

    private static final String GITHUB_TOKEN = "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";  // 使用你自己的GitHub Personal Access Token
    private static final String GITHUB_TOKEN1 = "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"; //备用token
    
    private final String REPO_PATH;
    private final String FILE_PATH;
    private String IN_USE_TOKEN= GITHUB_TOKEN;
    
    private RestTemplate restTemplate= new RestTemplate();
    private ObjectMapper objectMapper= new ObjectMapper();
    
    // 日志文件路径
    private String logFilePath;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    public GetGithubApi_new(String repoPath, String filePath) {
        this.REPO_PATH = repoPath;
        this.FILE_PATH = filePath;
        this.logFilePath = filePath + "/github_api_"+ dateFormat1.format(new Date()) +".log";
    }
    
    /**
     * 写入日志到文件
     * @param message 日志消息
     * @param isError 是否为错误日志
     */
    private void writeLog(String message, boolean isError) {
        try (FileWriter fw = new FileWriter(logFilePath, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = dateFormat.format(new Date());
            String logLevel = isError ? "[ERROR]" : "[INFO]";
            pw.println(timestamp + " " + logLevel + " " + message);
            
        } catch (IOException e) {
            System.err.println("写入日志失败: " + e.getMessage());
        }
    }
    
    private void logSuccess(String owner, String repoName) {
        String message = "成功处理仓库: " + owner + "/" + repoName;
        writeLog(message, false);
        System.out.println(message);
    }
    
    private void logFailure(String owner, String repoName, String reason) {
        String message = "处理失败: " + owner + "/" + repoName + " - 原因: " + reason;
        writeLog(message, true);
        System.err.println(message);
    }
    
    private void logBatchStart(int totalCount) {
        String separator = "========================================";
        writeLog(separator, false);
        writeLog("开始批量处理 GitHub 仓库数据", false);
        writeLog("仓库路径: " + REPO_PATH, false);
        writeLog("输出路径: " + FILE_PATH, false);
        writeLog("待处理仓库数量: " + totalCount, false);
        writeLog(separator, false);
    }
    
    private void logBatchEnd(int total, int success, int failed, List<String> failedRepos) {
        String separator = "========================================";
        writeLog(separator, false);
        writeLog("批量处理完成", false);
        writeLog("总计处理: " + total + " 个仓库", false);
        writeLog("成功: " + success + " 个", false);
        writeLog("失败: " + failed + " 个", false);
        
        if (total > 0) {
            double successRate = (success * 100.0 / total);
            writeLog("成功率: " + String.format("%.2f%%", successRate), false);
        }
        
        if (!failedRepos.isEmpty()) {
            writeLog("失败的仓库列表:", true);
            for (String repo : failedRepos) {
                writeLog("  - " + repo, true);
            }
        }
        
        writeLog(separator, false);
        writeLog("", false); // 空行分隔
    }
    
    /**
     * 批量处理所有需要处理的仓库
     * @return 处理成功返回 true，否则返回 false
     */
    public boolean storeGithubApi() {
        System.out.println("=== 开始批量处理 GitHub 仓库数据 ===");
        System.out.println("仓库路径: " + REPO_PATH);
        System.out.println("输出路径: " + FILE_PATH);
        System.out.println();
        
        // 获取需要处理的仓库列表
        List<RepoInfo> reposToProcess = getNeedProcessedRepos();
        
        if (reposToProcess.isEmpty()) {
            System.out.println("没有需要处理的仓库");
            return true;
        }
        Collections.shuffle(reposToProcess);
        
        logBatchStart(reposToProcess.size());
        
        System.out.println("\n=== 开始处理仓库 ===\n");
        
        int successCount = 0;
        int failCount = 0;
        List<String> failedRepos = new ArrayList<>();
        
        // 遍历处理每个仓库
        for (int i = 0; i < reposToProcess.size(); i++) {
            RepoInfo repo = reposToProcess.get(i);
            
            try {
                // 调用单个仓库处理函数
                boolean success = storeSingleGithubApi(repo.getOwner(), repo.getRepoName());
                
                if (success) {
                    successCount++;
                    logSuccess(repo.getOwner(), repo.getRepoName());
                } else {
                    failCount++;
                    failedRepos.add(repo.getFullName());
                    logFailure(repo.getOwner(), repo.getRepoName(), "处理过程返回失败");
                }
                
            } catch (Exception e) {
                failCount++;
                failedRepos.add(repo.getFullName());
                String errorMsg = "异常: " + e.getMessage();
                logFailure(repo.getOwner(), repo.getRepoName(), errorMsg);
                e.printStackTrace();
            }
            
            System.out.println();
        }
        
        // 输出处理结果统计
        System.out.println("\n========================================");
        System.out.println("=== 批量处理完成 ===");
        System.out.println("========================================");
        System.out.println("总计处理: " + reposToProcess.size() + " 个仓库");
        System.out.println("成功: " + successCount + " 个");
        System.out.println("失败: " + failCount + " 个");
        System.out.println("成功率: " + String.format("%.2f%%", (successCount * 100.0 / reposToProcess.size())));        
        System.out.println("========================================\n");
        
        logBatchEnd(reposToProcess.size(), successCount, failCount, failedRepos);
        
        // 如果所有仓库都处理成功，返回 true
        return failCount == 0;
    }

    
    public boolean storeSingleGithubApi(String owner, String repoName) {
    	if(!initStoreGithubApi(owner, repoName)) {
    		return false;
    	}
    	
    	if(!storeIssueData(owner, repoName)) {
    		return false;
    	}
    	
    	if(!storeStarData(owner, repoName)) {
    		return false;
    	}
    	
    	if(!storeForkData(owner, repoName)) {
    		return false;
    	}
    	
    	if(!storePRData(owner, repoName)) {
    		return false;
    	}
    	
    	if(!storePRReviewData(owner, repoName)) {
    		return false;
    	}
    	
    	return true;
    }
    
    private void saveToJsonFile(JsonNode jsonData, String repoName, String fileName) throws IOException {
        // 将 PR 数据保存为 JSON 文件
        String filePath = FILE_PATH + "/" + repoName + "/"+ fileName+ ".json";
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }else {
        	return;
        }

        // 将 PR 数据写入到文件
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, jsonData);
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
    }
    
    private ResponseEntity<String> fetchResponse(
            RestTemplate restTemplate, String url) throws InterruptedException{

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = null;
        
        try {
        	response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch(Exception e) {
        	response = null;
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
        if(response== null) {
        	// 重新请求原始 URL
            headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
            entity = new HttpEntity<>(headers);
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            } catch(Exception e) {
                System.err.println("重试请求失败: " + e.getMessage());
                throw e;
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
        	response = null;
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
        if(response== null) {
        	try {
            	response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            } catch(Exception e) {
            	System.err.println("重试请求失败: " + e.getMessage());
            	throw e;
            }
        }

        return response;
    }
    
    private boolean storePRData(String owner, String repoName) {
    	String fileName = FILE_PATH + "/" + repoName + "/PRData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
    	int page = 1;
	    int perPage = 100; // 每页100个评论
	    // 获取所有 PR
	    String urlTemplate = GITHUB_API_URL + "/repos/" + owner + "/" + repoName + "/pulls?state=all&page=%d&per_page=%d";
	    // 创建一个 ArrayNode 来收集所有 PR 数据
        ArrayNode allPRs = objectMapper.createArrayNode();
	    while (true) {
	        // 请求当前页的PR数据
	        String url = String.format(urlTemplate, page, perPage);
			// ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
	        ResponseEntity<String> response = null;
	        try {
	        	response = fetchResponse(restTemplate, url);
	        } catch (Exception e) {
				System.out.println(e.getMessage());
				return false;
			}
	        if(response== null) break;

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
            saveToJsonFile(allPRs, repoName, "PRData");
        } catch (IOException e) {
            System.err.println("Error saving PR data to file: " + e.getMessage());
            return false;
        }
	    
	    // System.out.println("prPages: " + (page-1));
	    
	    return true;
    }
    
    private boolean storePRReviewData(String owner, String repoName) {
    	
    	String fileName = FILE_PATH + "/" + repoName + "/PRReviewData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
        
        fileName = FILE_PATH + "/" + repoName + "/PRData.json";
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
	    String urlTemplate = GITHUB_API_URL + "/repos/" + owner + "/" + repoName + "/pulls/%d/reviews?page=%d&per_page=%d";
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
        	        } catch (Exception e) {
        				System.out.println(e.getMessage());
        				return false;
        			}
                	if(response== null) {
                		break;
                	}
                    // 每读取 200 个, 显示一次进度
                    ++getedApiNum;
//                    if(getedApiNum% 200 == 0) {
//                    	System.out.println("Get api num: "+ getedApiNum);
//                    }
                    
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
            saveToJsonFile(allPRReviews, repoName, "PRReviewData");
        } catch (IOException e) {
            System.err.println("Error saving PRReview data to file: " + e.getMessage());
            return false;
        }
  	
    	return true;
    }
    
    private boolean storeIssueData(String owner, String repoName) {
    	String fileName = FILE_PATH + "/" + repoName + "/IssueData.json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
    	int page = 1;
	    int perPage = 100; // 每页100个
	    // 获取所有 Issue
	    String urlTemplate = GITHUB_API_URL + "/repos/" + owner + "/" + repoName + "/issues?state=all&per_page=" + perPage + "&page=%d";
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
    	        } catch (Exception e) {
    				System.out.println(e.getMessage());
    				return false;
    			}
                if (response== null) {
                	break;
                }
                // 每读取 200 个, 显示一次进度
                ++getedApiNum;
//                if(getedApiNum% 200 == 0) {
//                	System.out.println("Get api num: "+ getedApiNum);
//                }
                
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
            saveToJsonFile(allIssues, repoName, "IssueData");
        } catch (IOException e) {
            System.err.println("Error saving Issue data to file: " + e.getMessage());
            return false;
        }
	    
//	    System.out.println("issuePages: " + (page-1));
	    
	    return true;
    }
    
    private boolean storeForkData(String owner, String repoName) {
        String fileName = FILE_PATH + "/" + repoName + "/ForkData.json";
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
            String query = String.format(queryTemplate, owner, repoName, endCursor.equals("null") ? "null" : ("\\\"" + endCursor + "\\\""));

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(query, headers);
            // ResponseEntity<String> response = restTemplate.exchange(GITHUB_GRAPHQL_URL, HttpMethod.POST, entity, String.class);
            ResponseEntity<String> response = null;
            try {
	        	response = fetchResponse_GraphQL(restTemplate, GITHUB_GRAPHQL_URL, entity);
	        } catch (Exception e) {
				System.out.println(e.getMessage());
				return false;
			}
            if(response== null) {
        		break;
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
            saveToJsonFile(allForks, repoName, "ForkData");
        } catch (IOException e) {
            System.err.println("Error saving Fork data to file: " + e.getMessage());
            return false;
        }

        return true;
    }
    
    private boolean storeStarData(String owner, String repoName) {
        String fileName = FILE_PATH + "/" + repoName + "/StarData.json";
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
        	String query = String.format(queryTemplate, owner, repoName, endCursor.equals("null") ? "null" : ("\\\"" + endCursor + "\\\""));

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + IN_USE_TOKEN);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(query, headers);
            // ResponseEntity<String> response = restTemplate.exchange(GITHUB_GRAPHQL_URL, HttpMethod.POST, entity, String.class);
            ResponseEntity<String> response = null;
            try {
	        	response = fetchResponse_GraphQL(restTemplate, GITHUB_GRAPHQL_URL, entity);
	        } catch (Exception e) {
				System.out.println(e.getMessage());
				return false;
			}
            if(response== null) {
        		break;
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
            saveToJsonFile(allStars, repoName, "StarData");
        } catch (IOException e) {
            System.err.println("Error saving Star data to file: " + e.getMessage());
            return false;
        }

        return true;
    }
    
    private boolean initStoreGithubApi(String owner, String repoName)  {
    	try {
            // 参数校验
            if (repoName == null || repoName.trim().isEmpty()) {
                return false;
            }
            
            if (FILE_PATH == null || FILE_PATH.trim().isEmpty()) {
                System.err.println("错误: FILE_PATH 未配置");
                return false;
            }
            
            // 创建仓库专属文件夹路径
            File repoFolder = new File(FILE_PATH, repoName);
            
        	if (!repoFolder.exists()) {
                boolean created = repoFolder.mkdirs();
                if (created) {
                    return true;
                } else {
                    return false;
                }
            } else {
            	return true;
            }
            
        } catch (SecurityException e) {
            System.err.println("✗ 权限不足，无法创建文件夹: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("✗ 创建文件夹时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取所有需要处理的仓库（排除已存在于 FILE_PATH 的仓库）
     * @return 过滤后的仓库信息列表
     */
    public List<RepoInfo> getNeedProcessedRepos() {
        // 获取所有仓库
        List<RepoInfo> allRepos = getInitialRepos();
        
        // 获取 FILE_PATH 目录下的所有文件夹名
        Set<String> existingFolders = getExistingFolderNames();
        
        // 过滤掉已存在的仓库
        List<RepoInfo> filteredRepos = new ArrayList<>();
        for (RepoInfo repo : allRepos) {
            if (!existingFolders.contains(repo.getRepoName())) {
                filteredRepos.add(repo);
            }
        }
        
        System.out.println("\n总共发现 " + allRepos.size() + " 个仓库");
        System.out.println("需要处理 " + filteredRepos.size() + " 个仓库");
        System.out.println("跳过 " + (allRepos.size() - filteredRepos.size()) + " 个已存在的仓库");
        
        return filteredRepos;
    }

    /**
     * 获取 FILE_PATH 目录下的所有文件夹名
     * @return 文件夹名集合
     */
    private Set<String> getExistingFolderNames() {
        Set<String> folderNames = new HashSet<>();
        File filePathDir = new File(FILE_PATH);
        
        if (!filePathDir.exists() || !filePathDir.isDirectory()) {
            System.err.println("FILE_PATH 不存在或不是目录: " + FILE_PATH);
            return folderNames;
        }
        
        File[] files = filePathDir.listFiles();
        if (files == null) {
            return folderNames;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                folderNames.add(file.getName());
            }
        }
        
        System.out.println("FILE_PATH 目录下已存在 " + folderNames.size() + " 个文件夹");
        
        return folderNames;
    }
    
    /**
     * 使用 JGit 从 REPO_PATH 目录下获取所有 git 库的 owner 和 reponame
     * @return 包含所有仓库信息的列表
     */
    public List<RepoInfo> getInitialRepos() {
        List<RepoInfo> repos = new ArrayList<>();
        File baseDir = new File(REPO_PATH);
        
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            System.err.println("路径不存在或不是目录: " + REPO_PATH);
            return repos;
        }
        
        File[] files = baseDir.listFiles();
        if (files == null) {
            return repos;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                File gitDir = new File(file, ".git");
                if (gitDir.exists() && gitDir.isDirectory()) {
                    RepoInfo repoInfo = extractRepoInfoUsingJGit(file);
                    if (repoInfo != null) {
                        repos.add(repoInfo);
//                        int commitCount = countCommits(file);
//                        if (commitCount < 50) {
//                            repos.add(repoInfo);
//                        }
                    }
                }
            }
        }
        
        return repos;
    }
    
    //统计commit数量，最多50
    private int countCommits(File repoDir) {
        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
             RevWalk walk = new RevWalk(repository)) {

            ObjectId head = repository.resolve("HEAD");
            if (head == null) {
                return 0;
            }

            walk.markStart(walk.parseCommit(head));

            int count = 0;
            for (RevCommit commit : walk) {
                count++;
                if (count >= 50) {
                    break;   // 超过阈值直接停止
                }
            }
            return count;

        } catch (Exception e) {
            System.err.println("统计 commit 失败: " + repoDir.getName());
            return Integer.MAX_VALUE; // 异常仓库直接跳过
        }
    }
    
    /**
     * 使用 JGit 从 Git 仓库中提取 owner 和 repo 信息
     * @param repoDir Git 仓库目录
     * @return 仓库信息对象
     */
    private RepoInfo extractRepoInfoUsingJGit(File repoDir) {
        Repository repository = null;
        try {
            // 打开 Git 仓库
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repository = builder.setGitDir(new File(repoDir, ".git"))
                               .readEnvironment()
                               .findGitDir()
                               .build();
            
            // 获取配置
            StoredConfig config = repository.getConfig();
            
            // 获取远程仓库配置（通常是 origin）
            Set<String> remotes = config.getSubsections("remote");
            
            for (String remoteName : remotes) {
                String url = config.getString("remote", remoteName, "url");
                if (url != null && url.contains("github.com")) {
                    String ownerAndRepo = parseGitHubUrl(url);
                    if (ownerAndRepo != null && ownerAndRepo.contains("/")) {
                        String[] parts = ownerAndRepo.split("/");
                        if (parts.length == 2) {
                            return new RepoInfo(parts[0], parts[1], repoDir.getName());
                        }
                    }
                }
            }
            
            // 如果上面的方法没找到，尝试使用 RemoteConfig
            List<RemoteConfig> remoteConfigs = RemoteConfig.getAllRemoteConfigs(config);
            for (RemoteConfig remoteConfig : remoteConfigs) {
                List<URIish> uris = remoteConfig.getURIs();
                for (URIish uri : uris) {
                    String url = uri.toString();
                    if (url.contains("github.com")) {
                        String ownerAndRepo = parseGitHubUrl(url);
                        if (ownerAndRepo != null && ownerAndRepo.contains("/")) {
                            String[] parts = ownerAndRepo.split("/");
                            if (parts.length == 2) {
                                return new RepoInfo(parts[0], parts[1], repoDir.getName());
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("读取 Git 仓库失败: " + repoDir.getAbsolutePath());
            e.printStackTrace();
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
        
        return null;
    }
    
    /**
     * 从 Git URL 中提取 owner/repo
     * @param url Git 远程仓库 URL
     * @return owner/repo 字符串
     */
    private String parseGitHubUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // 移除 .git 后缀
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        
        // 处理 HTTPS URL: https://github.com/owner/repo
        if (url.contains("github.com/")) {
            int index = url.indexOf("github.com/");
            return url.substring(index + "github.com/".length());
        }
        
        // 处理 SSH URL: git@github.com:owner/repo
        if (url.contains("github.com:")) {
            int index = url.indexOf("github.com:");
            return url.substring(index + "github.com:".length());
        }
        
        return null;
    }
    
    /**
     * 仓库信息类
     */
    public static class RepoInfo {
        private String owner;
        private String repoName;
        private String localDirName;
        
        public RepoInfo(String owner, String repoName, String localDirName) {
            this.owner = owner;
            this.repoName = repoName;
            this.localDirName = localDirName;
        }
        
        public String getOwner() {
            return owner;
        }
        
        public String getRepoName() {
            return repoName;
        }
        
        public String getLocalDirName() {
            return localDirName;
        }
        
        public String getFullName() {
            return owner + "/" + repoName;
        }
        
        @Override
        public String toString() {
            return "RepoInfo{" +
                    "owner='" + owner + '\'' +
                    ", repoName='" + repoName + '\'' +
                    ", localDirName='" + localDirName + '\'' +
                    '}';
        }
    }

}