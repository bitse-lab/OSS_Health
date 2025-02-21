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
public class PRMergedRatioService{
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
	                
	                mysqlDataMapper.insertMysqlData(entity);

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
	}
	
	// 获取 PR 以及其是否被合并
	private List<PRData> getPRData() throws IOException {
		List<PRData> prDataList = new ArrayList<PRData>();
        
	    int page = 1;
	    int perPage = 100; // 每页100个评论
	    String token = "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";  // 使用你自己的GitHub Personal Access Token

	    // 设置认证信息
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("Authorization", "Bearer " + token);  // 添加认证信息

	    // 创建一个包含认证信息的请求实体
	    HttpEntity<String> entity = new HttpEntity<>(headers);

	    // 获取所有 PR
	    String urlTemplate = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/pulls?state=all&page=%d&per_page=%d";
	    while (true) {
	        // 请求当前页的PR数据
	        String url = String.format(urlTemplate, page, perPage);
	        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
	        JsonNode prArray = objectMapper.readTree(response.getBody());

	        // 如果当前页没有数据，跳出循环
	        if (prArray.isEmpty()) {
	            break;
	        }

	        // 获取每个 PR 的评论（审核者）
	        for (JsonNode pr : prArray) {
	        	//创建当前PR的prdata
	            PRData prDataTemp = new PRData();
	        	//写入当前PR的id和commitTime
		        prDataTemp.id=pr.get("id").asText();
		        prDataTemp.commitTime=LocalDate.parse(pr.get("created_at").asText().substring(0, 10));
		        prDataTemp.merged= false;
		        // 检查当前PR是否已合并，则设置 merged 为 true
		        if (pr.get("state").asText().equals("closed") && pr.has("merged_at") && pr.get("merged_at")!= null) {
		        	prDataTemp.merged = true;
		        }
		        prDataList.add(prDataTemp);
	        }
	        // 增加页码，继续请求下一页PR
	        page++;
	    }

	    return prDataList;
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