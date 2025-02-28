package com.OSS.Health.service.community.resilience;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
public class PRLinkedIssueService{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "2.2.3";	// 请替换为对应ID
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
	            	// 计算从dateToStartTmp到dateToCheckTmp 这90天内的PR有linked的比率
	            	int prCount = 0;
	            	int linkedPRCount = 0;
	            	for (PRData prData : prDatas) {
	            	    if (!prData.commitTime.isBefore(dateToStartTmp) && !prData.commitTime.isAfter(dateToCheckTmp)) {
	            	        prCount++;
	            	        if (prData.linked) {
	            	            linkedPRCount++;
	            	        }
	            	    }
	            	}
	                
	            	// 输出每月的代码贡献者	                             
	                if (prCount > 0) {
	                    entity.setNumber((double) linkedPRCount / prCount);
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
	
	// 从本地文件提取PR，并且判断它是否和 issue linked
	class PRData{
		String id;
		LocalDate commitTime;
		Boolean linked;
		
		public PRData(String id, LocalDate commitTime, Boolean linked) {
	        this.id = id;
	        this.commitTime = commitTime;
	        this.linked = linked;
	    }

	    @Override
	    public String toString() {
	        return "PRData{id='" + id + "', commitTime=" + commitTime + ", linked=" + linked + "}";
	    }
	}
	
	private List<PRData> getPRData() throws IOException {
		List<PRData> prDataList = new ArrayList<PRData>();
		String filePath= REPO_PATH+ "/Github_Api_Message/PRData.json";
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		File jsonFile = new File(filePath);
        JsonNode rootNode = objectMapper.readTree(jsonFile);
        
        for (JsonNode prNode : rootNode) {
            String prId = prNode.get("id").asText();

            LocalDate commitTime = LocalDate.parse(prNode.get("created_at").asText().substring(0, 10));

            // Check if the PR is linked to an Issue "https://docs.github.com/zh/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue"
            // Keywords for linking PRs to issues
            String[] keywords = {"close", "closes", "closed", "fix", "fixes", "fixed", "resolve", "resolves", "resolved"};
        	// Regular expression pattern to match keywords and issues in PR title and body
            String keywordPattern = "(?i)\\b(" + String.join("|", keywords) + ")\\b\\s*(\\S+)?(\\s*#\\d+)";
            // Combine the title and body for the search
            String combinedText = prNode.get("title").asText() + " " + prNode.get("body").asText();
            // Create a pattern matcher for detecting keywords with issue numbers
            Pattern pattern = Pattern.compile(keywordPattern);
            Matcher matcher = pattern.matcher(combinedText);
            
            Boolean isLinked= matcher.find();

            // Create a new PRData object and add it to the list
            PRData prData = new PRData(prId, commitTime, isLinked);
            prDataList.add(prData);
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