package com.OSS.Health.service.community.resilience;

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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class PRMergedRatioService_new{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;
	
    private static final String MYSQL_ID = "2.2.2";	// 请替换为对应ID
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
    	// 设置Git仓库路径
		String repoPath = REPO_PATH + "/.git";

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
	        LocalDate startDate = firstCommitDate.plusDays(90);
	        if (startDate.getDayOfMonth() != 1) {
	            startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
	        }
	        
	        // 存储自定义的PRData
	        List<PRData> prDatas= getPRData();
	        
	        // 如果当前日期距离第一个提交超过90d，开始计算
	        if (startDate.isBefore(currentDate)) {	        	            
	            // 开始从第一个commit时间加90d后的时间节点开始逐月统计
	            LocalDate dateToCheck = startDate;
	
	            // 逐月统计
	            while (!dateToCheck.isAfter(currentDate)) {
	            	LocalDate dateToCheckTmp = dateToCheck;
	            	LocalDate dateToStartTmp = dateToCheck.minusDays(90);	            	            	
	            	
	            	MysqlDataModel entity = new MysqlDataModel();
	            	entity.setTime(dateToCheck);
	            	entity.setS1("");
	            	entity.setId(MYSQL_ID);
	            	// 计算从dateToStartTmp到dateToCheckTmp 这90天内的PR有review的比率
	            	int prCount = 0;
	            	int mergedPRCount = 0;
	            	for (PRData prData : prDatas) {
	            	    if (!prData.commitTime.isBefore(dateToStartTmp) && !prData.commitTime.isAfter(dateToCheckTmp)) {
	            	        prCount++;
	            	        if (prData.merged) {
	            	            mergedPRCount++;
	            	        }
	            	    }
	            	}
	                
	            	// 输出每月的代码贡献者	                             
	                if (prCount > 0) {
	                    entity.setNumber((double) mergedPRCount / prCount);
	                } else {
	                    entity.setNumber(0.0);  // 或者根据需求设置为 null
	                }
	                
	                mysqlDataMapper.insertMysqlData_new(REPO_OWNER+'_'+REPO_NAME, entity);

	                // 更新日期，进入下个月
	                dateToCheck = dateToCheck.plusMonths(1);
	            }
	        }
	        else {
	            System.out.println("The repository is not old enough to calculate.");
	        }
        }
	}
	
	//创建一个保存对应PR的id,commit_time和是否有review的类
	class PRData{
		String id;
		LocalDate commitTime;
		Boolean merged;
		
		PRData(String id, LocalDate commitTime, boolean merged) {
            this.id = id;
            this.commitTime = commitTime;
            this.merged = merged;
        }
	}

	private List<PRData> getPRData() throws IOException {
	    List<PRData> result = new ArrayList<PRData>();
	    File file = new File(REPO_PATH + "/Github_Api_Message/PRData.json");
	    ObjectMapper mapper = new ObjectMapper();
	    JsonNode root = mapper.readTree(file);

	    for (JsonNode pr : root) {
	        String id = pr.get("id").asText();
	        LocalDate createdAt = LocalDate.parse(pr.get("created_at").asText().substring(0, 10));
	        boolean isMerged = pr.has("merged_at") && !pr.get("merged_at").isNull();

	        result.add(new PRData(id, createdAt, isMerged));
	    }

	    return result;
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