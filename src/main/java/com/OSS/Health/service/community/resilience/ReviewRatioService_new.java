package com.OSS.Health.service.community.resilience;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;
import com.OSS.Health.service.community.resilience.PRMergedRatioService_new.PRData;
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
public class ReviewRatioService_new{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "2.2.1";	// 请替换为对应ID
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
	            	entity.setId("2.2.1");
	            	// 计算从dateToStartTmp到dateToCheckTmp 这90天内的PR有review的比率
	            	int prCount = 0;
	            	int reviewedPRCount = 0;
	            	for (PRData prData : prDatas) {
	            	    if (!prData.commitTime.isBefore(dateToStartTmp) && !prData.commitTime.isAfter(dateToCheckTmp)) {
	            	        prCount++;
	            	        if (prData.reviewNum > 0) {
	            	            reviewedPRCount++;
	            	        }
	            	    }
	            	}
	                
	            	// 输出每月的代码贡献者	                             
	                if (prCount > 0) {
	                    entity.setNumber((double) reviewedPRCount / prCount);
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
		int reviewNum;
		
		PRData(String id, LocalDate commitTime, int reviewNum) {
            this.id = id;
            this.commitTime = commitTime;
            this.reviewNum = reviewNum;
        }
	}
	
	// 获取 PR 以及其是否被 review
	private List<PRData> getPRData() throws IOException {
	    List<PRData> prDataList = new ArrayList<>();
	    ObjectMapper mapper = new ObjectMapper();

	    // 读取 PR 基本信息（创建时间）
	    File prFile = new File(REPO_PATH + "/Github_Api_Message/PRData.json");
	    JsonNode prArray = mapper.readTree(prFile);

	    // 读取 PR 的 review 数量信息（key 为 PR 编号，value 为 review 列表）
	    File reviewFile = new File(REPO_PATH + "/Github_Api_Message/PRReviewData.json");
	    JsonNode reviewMapNode = mapper.readTree(reviewFile);

	    // 构建 Map<String, Integer>：key 是 PR 编号，value 是该 PR 的 review 数量
	    Map<String, Integer> reviewCountMap = new HashMap<>();
	    Iterator<Map.Entry<String, JsonNode>> fields = reviewMapNode.fields();
	    while (fields.hasNext()) {
	        Map.Entry<String, JsonNode> entry = fields.next();
	        String prNumber = entry.getKey();  // "155", "2", ...
	        int reviewNum = entry.getValue().size(); // 数组长度就是 review 数量
	        reviewCountMap.put(prNumber, reviewNum);
	    }

	    // 构建 PRData 列表
	    for (JsonNode prNode : prArray) {
	        String number = prNode.get("number").asText(); // 与上面 key 对应
	        LocalDate commitTime = LocalDate.parse(prNode.get("created_at").asText().substring(0, 10));
	        int reviewNum = reviewCountMap.getOrDefault(number, 0);
	        prDataList.add(new PRData(number, commitTime, reviewNum));
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