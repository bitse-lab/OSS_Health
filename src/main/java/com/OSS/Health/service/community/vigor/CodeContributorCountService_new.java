package com.OSS.Health.service.community.vigor;

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
public class CodeContributorCountService_new{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private RestTemplate restTemplate= new RestTemplate();
	@Autowired
    private ObjectMapper objectMapper;

	private String REPO_OWNER;
    private String REPO_NAME;
    private String REPO_PATH;
    
    public void init(String repoOwner, String repoName, String repoPath) {
    	this.REPO_OWNER = repoOwner;
        this.REPO_NAME = repoName;
        this.REPO_PATH = repoPath;
    }
	
	public List<Map<String, Object>> getMysqlData() {
		return mysqlDataMapper.getMysqlDataModelNoS1_new(REPO_OWNER+'_'+REPO_NAME, "2.3.1");
    }
	
	public List<Map<String, Object>> getCodeCommitter() {
		return mysqlDataMapper.getMysqlDataModelNoS1_new(REPO_OWNER+'_'+REPO_NAME, "2.3.1.1");
    }
	
	public List<Map<String, Object>> getPRSubmitter() {
		return mysqlDataMapper.getMysqlDataModelNoS1_new(REPO_OWNER+'_'+REPO_NAME, "2.3.1.2");
    }
	
	public List<Map<String, Object>> getReviewer() {
		return mysqlDataMapper.getMysqlDataModelNoS1_new(REPO_OWNER+'_'+REPO_NAME, "2.3.1.3");
    }
	
	public void generateMonthlyReport() throws Exception {
		if (REPO_OWNER == null || REPO_NAME == null || REPO_PATH == null) {
            throw new IllegalStateException("Repo info not initialized. Call init() first.");
        }
	    // 设置Git仓库路径
	    String repoPath = REPO_PATH + "/.git";
        
        // 初始化Git仓库
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoPath))
                                      .readEnvironment()
                                      .findGitDir()
                                      .build();
        try(Git git = Git.open(new File(repoPath))){
	        
	        //清除表中的数据
	        mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "2.3.1");
	        mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "2.3.1.1");
	        mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "2.3.1.2");
	        mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "2.3.1.3");
	        
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
	        Map<String, List<LocalDate>> prSubmitters = getPRSubmitters();
	        Map<String, List<LocalDate>> reviewers = getReviewers();
	        
	        // 如果当前日期距离第一个提交超过90d，开始计算
	        if (startDate.isBefore(currentDate)) {
	        	// 遍历提交记录，将提交时间按作者存储
	            for (RevCommit commit : commits) {
	                String author = commit.getAuthorIdent().getName();
	                LocalDate commitDate = commit.getAuthorIdent().getWhen()
	                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	                // 判断是否为代码提交者
	                if (isCodeCommit(repository, commit)) {
	                	contributorCommits.computeIfAbsent(author, k -> new ArrayList<>()).add(commitDate);
	                }
	            }
	            // 按作者的提交时间排序
	            contributorCommits.values().forEach(Collections::sort);
	            prSubmitters.values().forEach(Collections::sort);
	            reviewers.values().forEach(Collections::sort);
	            
	            // 开始从第一个commit时间加90d后的时间节点开始逐月统计
	            LocalDate dateToCheck = startDate;
	
	            // 逐月统计
	            while (!dateToCheck.isAfter(currentDate)) {
	            	LocalDate dateToCheckTmp = dateToCheck;
	            	LocalDate dateToStartTmp = dateToCheck.minusDays(90);
	            	
	            	// 计算从dateToStartTmp到dateToCheckTmp 这90天内的三种提交者数量
	            	int activeCodeCommitters = (int) contributorCommits.entrySet().stream()
	            	        .filter(entry -> entry.getValue().stream().anyMatch(date -> !date.isBefore(dateToStartTmp) && !date.isAfter(dateToCheckTmp))) // 活跃提交者
	            	        .count();
	            	int activePRSubmitters = (int) prSubmitters.entrySet().stream()
	            	        .filter(entry -> entry.getValue().stream().anyMatch(date -> !date.isBefore(dateToStartTmp) && !date.isAfter(dateToCheckTmp))) // 活跃PR提交者
	            	        .count();
	            	int activeReviewers = (int) reviewers.entrySet().stream()
	            	        .filter(entry -> entry.getValue().stream().anyMatch(date -> !date.isBefore(dateToStartTmp) && !date.isAfter(dateToCheckTmp))) // 活跃审核者
	            	        .count();
	            	
	            	MysqlDataModel entity = new MysqlDataModel();
	            	entity.setTime(dateToCheck);
	            	entity.setS1("");
	            	// 输出总的数量
	            	Set<String> activeContributors = new HashSet<>(); // 用于存储所有活跃提交者的用户名
	            	contributorCommits.entrySet().stream()
		                .filter(entry -> entry.getValue().stream().anyMatch(date -> !date.isBefore(dateToStartTmp) && !date.isAfter(dateToCheckTmp)))
		                .forEach(entry -> activeContributors.add(entry.getKey()));
	            	prSubmitters.entrySet().stream()
		                .filter(entry -> entry.getValue().stream().anyMatch(date -> !date.isBefore(dateToStartTmp) && !date.isAfter(dateToCheckTmp)))
		                .forEach(entry -> activeContributors.add(entry.getKey()));
	            	reviewers.entrySet().stream()
		                .filter(entry -> entry.getValue().stream().anyMatch(date -> !date.isBefore(dateToStartTmp) && !date.isAfter(dateToCheckTmp)))
		                .forEach(entry -> activeContributors.add(entry.getKey()));
	            	entity.setId("2.3.1");                
	                entity.setNumber((double)activeContributors.size());
	                
	                mysqlDataMapper.insertMysqlData_new(REPO_OWNER+'_'+REPO_NAME, entity);
	            	// 输出每月的代码贡献者
	                entity.setId("2.3.1.1");                
	                entity.setNumber((double)activeCodeCommitters);
	                
	                mysqlDataMapper.insertMysqlData_new(REPO_OWNER+'_'+REPO_NAME, entity);
	                // 输出每月的PR提交者
	                entity.setId("2.3.1.2");                
	                entity.setNumber((double)activePRSubmitters);
	                
	                mysqlDataMapper.insertMysqlData_new(REPO_OWNER+'_'+REPO_NAME, entity);
	                // 输出每月的代码审核者
	                entity.setId("2.3.1.3");                
	                entity.setNumber((double)activeReviewers);
	                
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
	
	// 获取第一个提交的时间
    private LocalDate getFirstCommitDate(Iterable<RevCommit> commits) {
        return StreamSupport.stream(commits.spliterator(), false)
                .map(commit -> commit.getAuthorIdent().getWhen()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new RuntimeException("No commits found"));
    }
    
    // 判断是否为代码提交者
    private boolean isCodeCommit(Repository repository, RevCommit commit) throws IOException, GitAPIException {
    	 // 检查该提交是否有父提交
        if (commit.getParentCount() == 0) {
            // 如果没有父提交，认为它是一个新提交，直接返回是否提交了代码
            List<DiffEntry> diffs = getCommitDiffsForInitialCommit(repository, commit);
            // 如果有代码更改，认为是提交了代码
            for (DiffEntry diff : diffs) {
                if (diff.getChangeType() != DiffEntry.ChangeType.DELETE && isCodeFile(diff)) {
                    return true;
                }
            }
            return false;
        }
        // 获取提交的父提交（如果有）
        RevCommit parentCommit = commit.getParent(0);

        // 获取当前提交和父提交之间的差异
        List<DiffEntry> diffs = getCommitDiffs(repository , parentCommit, commit);
        
        // 如果有代码更改，认为是提交了代码
        for (DiffEntry diff : diffs) {
            // 只关心代码文件的差异
            if (diff.getChangeType() != DiffEntry.ChangeType.DELETE && isCodeFile(diff)) {
                return true;
            }
        }
        return false;
    }
    
    private static List<DiffEntry> getCommitDiffsForInitialCommit(Repository repository, RevCommit commit) throws IOException, GitAPIException {
        // 对于初始提交（没有父提交），直接查看提交的树
        CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
        newTreeParser.reset(repository.newObjectReader(), commit.getTree());

        // 获取当前提交的差异（与空树对比）
        try (Git git = new Git(repository)) {  // Git 对象也应当在使用完后关闭
            DiffCommand diffCommand = git.diff();
            diffCommand.setOldTree(new CanonicalTreeParser()).setNewTree(newTreeParser);
            return diffCommand.call();
        }
    }

	private static List<DiffEntry> getCommitDiffs(Repository repository, RevCommit oldCommit, RevCommit newCommit) throws IOException, GitAPIException {
    	// 创建CanonicalTreeParser来解析树
        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(oldCommit.getTree());
            treeWalk.setRecursive(true);
            oldTreeParser.reset(repository.newObjectReader(), oldCommit.getTree());
        }

        CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(newCommit.getTree());
            treeWalk.setRecursive(true);
            newTreeParser.reset(repository.newObjectReader(), newCommit.getTree());
        }

        // 获取当前提交和父提交之间的差异
        try (Git git = new Git(repository)) {  // Git 对象也应当在使用完后关闭
            DiffCommand diffCommand = git.diff();
            diffCommand.setOldTree(oldTreeParser).setNewTree(newTreeParser);
            return diffCommand.call();
        }
    }
	
	// 获取 PR 提交者（Submitters）- 适配GitCode格式
	private Map<String, List<LocalDate>> getPRSubmitters() throws IOException {
	    Map<String, List<LocalDate>> prSubmitters = new HashMap<>();
	    File file = new File(REPO_PATH + "/Github_Api_Message/PRData.json");  // 修改路径为GitCode
	    if (!file.exists()) {
	        System.out.println("PRData.json file not found at: " + file.getAbsolutePath());
	        return prSubmitters;
	    }
	    
	    JsonNode root = objectMapper.readTree(file);
	    // GitCode的PR数据是数组格式
	    if (root.isArray()) {
	        for (JsonNode prNode : root) {
	            if (prNode.has("user") && prNode.get("user").has("login") && prNode.has("created_at")) {
	                String author = prNode.get("user").get("login").asText();
	                String dateStr = prNode.get("created_at").asText();
	                // GitCode的时间格式包含时区信息，需要截取前10位
	                LocalDate date = LocalDate.parse(dateStr.substring(0, 10));
	                prSubmitters.computeIfAbsent(author, k -> new ArrayList<>()).add(date);
	            }
	        }
	    }
	    return prSubmitters;
	}

	// 获取代码审核者（Reviewers）- 适配GitCode格式
	private Map<String, List<LocalDate>> getReviewers() throws IOException {
	    Map<String, List<LocalDate>> reviewers = new HashMap<>();
	    File file = new File(REPO_PATH + "/Github_Api_Message/PRReviewData.json");
	    if (!file.exists()) {
	        System.out.println("PRReviewData.json file not found at: " + file.getAbsolutePath());
	        return reviewers;
	    }
	    
	    JsonNode root = objectMapper.readTree(file);

	    // 遍历每个 PR 编号（如 "89", "88"）
	    Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
	    while (fields.hasNext()) {
	        Map.Entry<String, JsonNode> entry = fields.next();
	        JsonNode reviewList = entry.getValue(); // 是一个数组

	        for (JsonNode reviewNode : reviewList) {
	            JsonNode userNode = reviewNode.get("user");
	            // GitCode的评论数据结构，需要检查是否为审核类型的评论
	            if (userNode != null && userNode.has("login") && reviewNode.has("created_at")) {
	                String reviewer = userNode.get("login").asText();
	                String dateStr = reviewNode.get("created_at").asText();
	                // GitCode的时间格式包含时区信息，需要截取前10位
	                LocalDate date = LocalDate.parse(dateStr.substring(0, 10));
	                
	                // 可以根据comment_type或body内容来判断是否为审核评论
	                // 这里简化处理，将所有评论者都视为潜在的审核者
	                String body = reviewNode.has("body") ? reviewNode.get("body").asText() : "";
	                // 可以根据具体需求过滤审核类型的评论，比如包含"/lgtm"、"/approve"等
	                if (isReviewComment(body)) {
	                    reviewers.computeIfAbsent(reviewer, k -> new ArrayList<>()).add(date);
	                }
	            }
	        }
	    }

	    return reviewers;
	}
	
	// 判断是否为审核评论的辅助方法
	private boolean isReviewComment(String body) {
	    if (body == null || body.trim().isEmpty()) {
	        return false;
	    }
	    
	    String lowerBody = body.toLowerCase().trim();
	    // 根据GitCode平台的审核命令来判断
	    return lowerBody.startsWith("/lgtm") || 
	           lowerBody.startsWith("/approve") || 
	           lowerBody.startsWith("/reject") ||
	           lowerBody.contains("lgtm") ||
	           lowerBody.contains("approved") ||
	           lowerBody.contains("looks good");
	}

    private static boolean isCodeFile(DiffEntry diff) {
        // 判断文件类型是否是代码文件，可以扩展这个检查来匹配不同类型的代码文件
    	String fileName = diff.getNewPath();
    	return fileName.endsWith(".java")    // Java
    	    || fileName.endsWith(".py")      // Python
    	    || fileName.endsWith(".cpp")     // C++
    	    || fileName.endsWith(".h")       // C Header
    	    || fileName.endsWith(".hpp")     // C++ Header
    	    || fileName.endsWith(".c")       // C
    	    || fileName.endsWith(".js")      // JavaScript
    	    || fileName.endsWith(".ts")      // TypeScript
    	    || fileName.endsWith(".html")    // HTML
    	    || fileName.endsWith(".css")     // CSS
    	    || fileName.endsWith(".scss")    // SCSS
    	    || fileName.endsWith(".go")      // Go
    	    || fileName.endsWith(".rb")      // Ruby
    	    || fileName.endsWith(".php")     // PHP
    	    || fileName.endsWith(".swift")   // Swift
    	    || fileName.endsWith(".kt")      // Kotlin
    	    || fileName.endsWith(".scala")   // Scala
    	    || fileName.endsWith(".rust")    // Rust
    	    || fileName.endsWith(".sh")      // Shell Script
    	    || fileName.endsWith(".bat")     // Batch Script
    	    || fileName.endsWith(".sql")     // SQL
    	    || fileName.endsWith(".r")       // R
    	    || fileName.endsWith(".lua")     // Lua
    	    || fileName.endsWith(".clojure") // Clojure
    	    || fileName.endsWith(".jl")      // Julia
    	    || fileName.endsWith(".v")       // Verilog
    	    || fileName.endsWith(".vhdl");   // VHDL
    }
}
