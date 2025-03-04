package com.OSS.Health.service.market.influence;

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
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class MonthStarService{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "3.2.1";	// 请替换为对应ID
    private static final String REPO_PATH = "D:/Plateform/Git/repositories/core"; // 请替换为git仓库存储位置
	
	public List<Map<String, Object>> getMysqlData() {
		return mysqlDataMapper.getMysqlDataModelNoS1(MYSQL_ID);
    }
	
	public void generateMonthlyReport() throws Exception {
    	// 设置Git仓库路径
        String repoPath = REPO_PATH+ "/.git";

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
	        LocalDate startDate = firstCommitDate.plusDays(30);
	        if (startDate.getDayOfMonth() != 1) {
	            startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
	        }
	        
	        // 存储starData
	        List<LocalDate> starDatas= getStarData();
	        
	        // 如果当前日期距离第一个提交超过一个月，开始计算
	        if (startDate.isBefore(currentDate)) {	        	            
	            // 开始从第一个commit时间加一个月后的时间节点开始逐月统计
	            LocalDate dateToCheck = startDate;
	
	            // 逐月统计
	            while (!dateToCheck.isAfter(currentDate)) {
	            	LocalDate dateToCheckTmp = dateToCheck;
	            	LocalDate dateToStartTmp = dateToCheck.minusMonths(1);	            	            	
	            	
	            	MysqlDataModel entity = new MysqlDataModel();
	            	entity.setTime(dateToStartTmp); //保留的实际上是当月的第一天到下个月第一天的计数
	            	entity.setS1("");
	            	entity.setId(MYSQL_ID);
	            	// 计算从dateToStartTmp到dateToCheckTmp 这30天内的新增star数量
	            	int starCount = 0;
	            	for (LocalDate starData : starDatas) {
	            	    if (!starData.isBefore(dateToStartTmp) && !starData.isAfter(dateToCheckTmp)) {
	            	        starCount++;
	            	    }
	            	}
	                
	            	// 输出每月的star数	                             
	                entity.setNumber(starCount);
	                
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
	
	private List<LocalDate> getStarData() throws IOException {
		List<LocalDate> starDataList = new ArrayList<LocalDate>();
		String filePath= REPO_PATH+ "/Github_Api_Message/StarData.json";
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		File jsonFile = new File(filePath);
	    if (!jsonFile.exists()) {
	        System.err.println("JSON file not found: " + filePath);
	        return starDataList; // 返回空列表
	    }
        JsonNode rootNode = objectMapper.readTree(jsonFile);
        
        for (JsonNode starNode : rootNode) {
        	JsonNode starredAtNode = starNode.get("starredAt");
            if (starredAtNode != null) {
                String starredAtStr = starredAtNode.asText(); // 获取日期字符串
                LocalDate starredDate = LocalDate.parse(starredAtStr.substring(0, 10)); // 提取 YYYY-MM-DD
                starDataList.add(starredDate);
            }
        }	

	    return starDataList;
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