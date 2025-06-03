package com.OSS.Health.service.software.productivity;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class MonthIssueService_new{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "1.3.2";	// 请替换为对应ID
    private String REPO_OWNER;
    private String REPO_NAME;
    private String REPO_PATH;
	
    public void init(String repoOwner, String repoName, String repoPath) {
    	this.REPO_OWNER = repoOwner;
        this.REPO_NAME = repoName;
        this.REPO_PATH = repoPath;
    }
	
	public List<Map<String, Object>> getMysqlData() {
		return mysqlDataMapper.getMysqlDataModelNoS1_new(REPO_OWNER+'_'+REPO_NAME, MYSQL_ID);
    }
	
	public void generateMonthlyReport() throws Exception {
		if (REPO_OWNER == null || REPO_NAME == null || REPO_PATH == null) {
            throw new IllegalStateException("Repo info not initialized. Call init() first.");
        }
        String repoPath = REPO_PATH+ "/.git";

        try(Git git = Git.open(new File(repoPath))){
	        
	        //清除表中的数据
	        mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, MYSQL_ID);
	        
	        // 获取所有提交记录
	        Iterable<RevCommit> commitsTmp = git.log().call();
	        List<RevCommit> commits = new ArrayList<>();
	        commitsTmp.forEach(commits::add); // 将 Iterable 转换为 List
	        
	        // 获取第一个提交的时间
	        LocalDate firstCommitDate = getFirstCommitDate(commits);
	
	        // 获取当前日期
	        LocalDate currentDate = LocalDate.now();
	
	        // 计算第一个时间节点
	        LocalDate startDate = firstCommitDate.with(TemporalAdjusters.firstDayOfNextMonth());
	        // 预处理：按月份统计 issue 数量
	        Map<YearMonth, Integer> monthIssueCount = getIssueData();
	        
	        // 开始逐月写入数据库
	        if (startDate.isBefore(currentDate)) {
	            LocalDate dateToCheck = startDate;

	            while (!dateToCheck.isAfter(currentDate)) {
	            	YearMonth currentMonth = YearMonth.from(dateToCheck); // 统计的是这个月

	                int issueCount = monthIssueCount.getOrDefault(currentMonth, 0);

	                MysqlDataModel entity = new MysqlDataModel();
	                entity.setTime(dateToCheck);
	                entity.setS1("");
	                entity.setId(MYSQL_ID);
	                entity.setNumber((double) issueCount);

	                mysqlDataMapper.insertMysqlData_new(REPO_OWNER+'_'+REPO_NAME, entity);
	                // 下个月
	                dateToCheck = dateToCheck.plusMonths(1);
	            }
	        } else {
	            System.out.println("The repository is not old enough to calculate.");
	        }
        }
	}
	
	// 从本地文件提取Issue，并且将每月的issue数量存入map		
	private Map<YearMonth, Integer> getIssueData() throws IOException {
		Map<YearMonth, Integer> monthIssueCount = new HashMap<>();
		String filePath= REPO_PATH+ "/Github_Api_Message/IssueData.json";
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		File jsonFile = new File(filePath);
	    JsonNode rootNode = objectMapper.readTree(jsonFile);
	    
	    for (JsonNode issueNode : rootNode) {
	    	// 提取 created_at 字段
	    	JsonNode createdAtNode = issueNode.get("created_at");
	    	if (createdAtNode == null) {
	    	    createdAtNode = issueNode.get("createdAt");
	    	}
	    	if (createdAtNode == null || createdAtNode.isNull()) continue;
	    	String createdAtStr = createdAtNode.asText();

	        // 解析为 LocalDateTime
	        OffsetDateTime createdAt = OffsetDateTime.parse(createdAtStr);

	        // 提取 YearMonth
	        YearMonth yearMonth = YearMonth.from(createdAt);

	        // 累加每月 Issue 数量
	        monthIssueCount.put(yearMonth, monthIssueCount.getOrDefault(yearMonth, 0) + 1);
	    }	
	
	    return monthIssueCount;
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