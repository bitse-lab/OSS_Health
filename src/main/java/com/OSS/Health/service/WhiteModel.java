package com.OSS.Health.service;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class WhiteModel{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private RestTemplate restTemplate= new RestTemplate();
	@Autowired
    private ObjectMapper objectMapper;
	
	private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String REPO_OWNER = "vuejs"; // 请替换为仓库的拥有者
    private static final String REPO_NAME = "core";   // 请替换为仓库名称
    private static final String MYSQL_ID = "2.2.2";	// 请替换为对应ID
	
	public List<Map<String, Object>> getMysqlData() {
		return mysqlDataMapper.getMysqlDataModelNoS1(MYSQL_ID);
    }
	
	public void generateMonthlyReport() throws Exception {
    	// 设置Git仓库路径
        String repoPath = "D:/Plateform/Git/repositories/core/.git";
        
        // 初始化Git仓库
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoPath))
                                      .readEnvironment()
                                      .findGitDir()
                                      .build();
        try(Git git = Git.open(new File(repoPath))){
	        
	        //清除表中的数据
	        mysqlDataMapper.clearMysqlDataById(MYSQL_ID);
	        
	        // 获取所有提交记录
	        Iterable<RevCommit> commitsTmp = git.log().call();
	        List<RevCommit> commits = new ArrayList<>();
	        commitsTmp.forEach(commits::add); // 将 Iterable 转换为 List
	        
	        // 获取第一个提交的时间
	        LocalDate firstCommitDate = getFirstCommitDate(commits);
	
	        // 获取当前日期
	        LocalDate currentDate = LocalDate.now();
	
	        // 计算第一个时间节点
	        LocalDate startDate = firstCommitDate.plusDays(90);
	        if (startDate.getDayOfMonth() != 1) {
	            startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
	        }
	        
	        // 存储贡献者的提交时间记录
	        Map<String, List<LocalDate>> contributorCommits = new HashMap<>();
	        
	        // 如果当前日期距离第一个提交超过90d，开始计算
	        if (startDate.isBefore(currentDate)) {
	        	// 遍历提交记录，将提交时间按作者存储
	            for (RevCommit commit : commits) {
	                String author = commit.getAuthorIdent().getName();
	                LocalDate commitDate = commit.getAuthorIdent().getWhen()
	                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	            }
	            // 按作者的提交时间排序
	            contributorCommits.values().forEach(Collections::sort);
	            
	            // 开始从第一个commit时间加90d后的时间节点开始逐月统计
	            LocalDate dateToCheck = startDate;
	
	            // 逐月统计
	            while (!dateToCheck.isAfter(currentDate)) {
	            	LocalDate dateToCheckTmp = dateToCheck;
	            	LocalDate dateToStartTmp = dateToCheck.minusDays(90);	            	            	
	            	
	            	MysqlDataModel entity = new MysqlDataModel();
	            	entity.setTime(dateToCheck);
	            	entity.setS1("");
	                
	            	// 输出每月的代码贡献者
	                entity.setId(MYSQL_ID);                
	                entity.setNumber(1);

	                // 更新日期，进入下个月
	                dateToCheck = dateToCheck.plusMonths(1);
	            }
	        }
	        else {
	            System.out.println("The repository is not old enough to calculate.");
	        }
        }
	}
	
	// 获取代码审核者（Reviewers）
	private Map<String, List<LocalDate>> getReviewers() throws IOException {
	    Map<String, List<LocalDate>> reviewers = new HashMap<>();
	    int page = 1;
	    int perPage = 100; // 每页100个评论
	    String token = "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";  // 使用你自己的GitHub Personal Access Token

	    // 设置认证信息
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("Authorization", "Bearer " + token);  // 添加认证信息

	    // 创建一个包含认证信息的请求实体
	    HttpEntity<String> entity = new HttpEntity<>(headers);

	    // 获取所有 PR
	    String urlTemplate = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/pulls?page=%d&per_page=%d";
	    while (true) {
	        // 请求当前页的PR数据
	        String url = String.format(urlTemplate, page, perPage);
	        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

	        // 使用 Jackson 的 ObjectMapper 将返回的 JSON 字符串转为 JsonNode
	        JsonNode prArray = objectMapper.readTree(response.getBody());

	        // 如果当前页没有数据，跳出循环
	        if (prArray.isEmpty()) {
	            break;
	        }

	        // 获取每个 PR 的评论（审核者）
	        for (JsonNode pr : prArray) {
	            int prNumber = pr.path("number").asInt();
	            String reviewUrlTemplate = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/pulls/%d/reviews?page=%d&per_page=%d";

	            int reviewPage = 1;
	            while (true) {
	                // 请求当前页的评论数据
	                String reviewUrl = String.format(reviewUrlTemplate, prNumber, reviewPage, perPage);
	                ResponseEntity<String> reviewsResponse = restTemplate.exchange(reviewUrl, HttpMethod.GET, entity, String.class);

	                JsonNode reviewArray = objectMapper.readTree(reviewsResponse.getBody());

	                // 如果当前页没有数据，跳出循环
	                if (reviewArray.isEmpty()) {
	                    break;
	                }

	                // 遍历所有评论
	                for (JsonNode review : reviewArray) {
	                    String reviewer = review.path("user").path("login").asText(); // 获取审查者的 GitHub 用户名
	                    String reviewedAt = review.path("submitted_at").asText();  // 获取提交审核时间
	                    // 转换为 LocalDate
	                    LocalDate reviewDate = LocalDate.parse(reviewedAt.substring(0, 10));
	                    // 在 reviewers 中加入该审核者的信息
	                    reviewers.computeIfAbsent(reviewer, k -> new ArrayList<>()).add(reviewDate);
	                }
	                // 增加页码，继续请求下一页评论
	                reviewPage++;
	            }
	        }
	        // 增加页码，继续请求下一页PR
	        page++;
	    }

	    return reviewers;
	}
	
	// 获取第一个提交的时间
    private LocalDate getFirstCommitDate(List<RevCommit> commits) {
        return StreamSupport.stream(commits.spliterator(), false)
                .map(commit -> commit.getAuthorIdent().getWhen()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new RuntimeException("No commits found"));
    }
}