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
public class CodeContributorCountService{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private RestTemplate restTemplate= new RestTemplate();
	@Autowired
    private ObjectMapper objectMapper;
	
	private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String REPO_OWNER = "vuejs"; // 请替换为仓库的拥有者
    private static final String REPO_NAME = "core";   // 请替换为仓库名称
	
	public List<Map<String, Object>> getMysqlData() {
		return mysqlDataMapper.getMysqlDataModelNoS1("2.3.1");
    }
	
	public List<Map<String, Object>> getCodeCommitter() {
		return mysqlDataMapper.getMysqlDataModelNoS1("2.3.1.1");
    }
	
	public List<Map<String, Object>> getPRSubmitter() {
		return mysqlDataMapper.getMysqlDataModelNoS1("2.3.1.2");
    }
	
	public List<Map<String, Object>> getReviewer() {
		return mysqlDataMapper.getMysqlDataModelNoS1("2.3.1.3");
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
	        mysqlDataMapper.clearMysqlDataById("2.3.1");
	        mysqlDataMapper.clearMysqlDataById("2.3.1.1");
	        mysqlDataMapper.clearMysqlDataById("2.3.1.2");
	        mysqlDataMapper.clearMysqlDataById("2.3.1.3");
	        
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
//	                // 判断是否为PR提交者
//	                if (isPRCommit(commit)) {
//	                    prSubmitters.computeIfAbsent(author, k -> new ArrayList<>()).add(commitDate);
//	                }
//	                // 判断是否为代码审核者
//	                if (isReviewer(commit)) {
//	                    reviewers.computeIfAbsent(author, k -> new ArrayList<>()).add(commitDate);
//	                }
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
	                entity.setNumber(activeContributors.size());
	                
	                mysqlDataMapper.insertMysqlData(entity);
	            	// 输出每月的代码贡献者
	                entity.setId("2.3.1.1");                
	                entity.setNumber(activeCodeCommitters);
	                
	                mysqlDataMapper.insertMysqlData(entity);
	                // 输出每月的PR提交者
	                entity.setId("2.3.1.2");                
	                entity.setNumber(activePRSubmitters);
	                
	                mysqlDataMapper.insertMysqlData(entity);
	                // 输出每月的代码审核者
	                entity.setId("2.3.1.3");                
	                entity.setNumber(activeReviewers);
	                
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
	
	// 获取第一个提交的时间
    private LocalDate getFirstCommitDate(Iterable<RevCommit> commits) {
        return StreamSupport.stream(commits.spliterator(), false)
                .map(commit -> commit.getAuthorIdent().getWhen()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new RuntimeException("No commits found"));
    }
    
//    // 判断是否为 PR 提交者
//    private boolean isPRCommit(RevCommit commit) throws IOException {
//        // 1. 检查是否是合并提交（至少有两个父提交）
//        boolean isMergeCommit = commit.getParentCount() >= 2;
//
//        // 2. 检查提交信息中是否包含 PR 编号（如 #123）
//        boolean hasPRNumber = containsPRNumber(commit.getFullMessage());
//
//        // 3. 如果是合并提交且包含 PR 编号，则判定为 PR 提交
//        return isMergeCommit && hasPRNumber;
//    }
//
//    private boolean containsPRNumber(String message) {
//        // 匹配 GitHub/GitLab/Bitbucket 的 PR 编号格式（如 #123、!123、PR-123）
//        Pattern pattern = Pattern.compile("(#|!|PR-)\\d+");
//        return pattern.matcher(message).find();
//    }
//
//    // 判断是否为代码审核者
//    private boolean isReviewer(RevCommit commit) {
//        // 1. 解析提交信息中的 Reviewed-by Trailer
//        List<String> reviewers = parseTrailers(commit.getFullMessage(), "Reviewed-by");
//
//        // 2. 如果有 Reviewer 标记，则返回 true
//        return !reviewers.isEmpty();
//    }
//
//    private List<String> parseTrailers(String message, String trailerKey) {
//        List<String> reviewers = new ArrayList<>();
//        String[] lines = message.split("\n");
//        
//        // 匹配格式：Reviewed-by: Name <email>
//        Pattern pattern = Pattern.compile("^" + trailerKey + ":\\s*(.+)$", Pattern.CASE_INSENSITIVE);
//        
//        for (String line : lines) {
//            Matcher matcher = pattern.matcher(line.trim());
//            if (matcher.find()) {
//                reviewers.add(matcher.group(1));
//            }
//        }
//        return reviewers;
//    }
    
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
	
	// 获取 PR 提交者（Submitters）
	private Map<String, List<LocalDate>> getPRSubmitters() throws IOException {
	    Map<String, List<LocalDate>> prSubmitters = new HashMap<>();
	    int page = 1;
	    int perPage = 100; // 每页100个PR
	    String token = "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";  // 使用你自己的GitHub Personal Access Token

	    // 设置认证信息
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("Authorization", "Bearer " + token);  // 添加认证信息

	    // 创建一个包含认证信息的请求实体
	    HttpEntity<String> entity = new HttpEntity<>(headers);

	    // 构造 API 请求 URL
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

	        // 遍历当前页的PR
	        for (JsonNode pr : prArray) {
	            String author = pr.path("user").path("login").asText(); // 获取提交者的 GitHub 用户名
	            String createdAt = pr.path("created_at").asText();  // 获取 PR 创建时间
	            // 转换为 LocalDate
	            LocalDate prDate = LocalDate.parse(createdAt.substring(0, 10));

	            // 在 prSubmitters 中加入该提交者的信息
	            prSubmitters.computeIfAbsent(author, k -> new ArrayList<>()).add(prDate);
	        }

	        // 增加页码，继续请求下一页
	        page++;
	    }

	    return prSubmitters;
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