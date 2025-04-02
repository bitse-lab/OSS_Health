package com.OSS.Health.service.community.organization;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;

import ch.qos.logback.core.joran.conditional.IfAction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class MonthOrgCommitsService{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "2.1.1";	// 请替换为对应ID
    private static final String REPO_PATH = "D:/Plateform/Git/repositories/core"; // 请替换为git仓库存储位置
    
    // 用于存储每个提交的信息
    private List<CommitInfo> commitDataList = new ArrayList<>();
    // 用于存储committer和其所有邮箱的映射
    private Map<String, List<String>> userEmail = new HashMap<>();    
    // 用于存储邮箱和对应committer的映射
    private Map<String, List<String>> emailUser = new HashMap<>();
    // 初始化 user 中存储属于组织的人，email中存储属于组织的邮件
    List<String> users = new ArrayList<>();
    List<String> emails = new ArrayList<>();

    // 获取公共邮箱列表
    Set<String> commonEmailDomains = loadCommonEmailDomains();
	
	public List<Map<String, Object>> getMysqlData() {
		return mysqlDataMapper.getMysqlDataModelNoS1(MYSQL_ID);
    }
	
	public void generateMonthlyReport() throws Exception {
	    // 设置Git仓库路径
	    String repoPath = REPO_PATH + "/.git";

	    try (Git git = Git.open(new File(repoPath))) {

	        // 清除表中的数据
	        mysqlDataMapper.clearMysqlDataById(MYSQL_ID);

	        // 获取所有提交记录
	        Iterable<RevCommit> commitsTmp = git.log().call();
	        List<RevCommit> commits = new ArrayList<>();
	        commitsTmp.forEach(commits::add); // 将 Iterable 转换为 List

	        // 获取第一个提交的时间
	        LocalDate firstCommitDate = getFirstCommitDate(commits);

	        // 计算第一个时间节点
	        LocalDate startDate = firstCommitDate.plusMonths(1);
	        if (startDate.getDayOfMonth() != 1) {
	            startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
	        }

	        // 初始化
	        for (RevCommit commit : commits) {
	            // 获取邮箱地址
	            String email = commit.getAuthorIdent().getEmailAddress();
	            if (email != null && email.contains("@")) {
	                // 添加邮箱到 userEmail 和 emailUser
	                String committerName = commit.getAuthorIdent().getName();

	                // 保存 committer 与邮箱的映射
	                userEmail
	                    .computeIfAbsent(committerName, k -> new ArrayList<>())
	                    .add(email.toLowerCase());

	                // 保存 邮箱 与 committer 的映射
	                emailUser
	                    .computeIfAbsent(email.toLowerCase(), k -> new ArrayList<>())
	                    .add(committerName);
	            }
	        }
	        
	        // 遍历 userEmail，找出所有拥有非公共邮箱的人员
	        for (Map.Entry<String, List<String>> entry : userEmail.entrySet()) {
	            String committer = entry.getKey();
	            List<String> emails = entry.getValue();

	            // 检查是否有非公共邮箱
	            boolean hasPrivateEmail = emails.stream()
	                    .anyMatch(emailAddress -> !isCommonEmail(emailAddress, commonEmailDomains));

	            if (hasPrivateEmail) {
	                // 如果该人员有非公共邮箱，加入到 user 数组
	                users.add(committer);
	            }
	        }
	        
	        // 遍历 emailUser，找出属于组织的邮箱并加入 email 数组
	        for (Map.Entry<String, List<String>> entry : emailUser.entrySet()) {
	            String emailAddress = entry.getKey();
	            List<String> committers = entry.getValue();

	            // 检查邮箱是否有属于组织的 committer
	            if (committers.stream().anyMatch(users::contains)) {
	                // 如果邮箱的某个用户在 user 数组中，加入到 email 数组
	                emails.add(emailAddress);
	            }
	        }
	        
	        // 根据users和emails添加commitDataList
	        for (RevCommit commit : commits) {
	        	// 获取提交信息
	            String commitId = commit.getId().name();
	            String committerName = commit.getAuthorIdent().getName();
	            String email = commit.getAuthorIdent().getEmailAddress();
	            LocalDate commitDate = commit.getAuthorIdent().getWhen()
	                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

	            // 判断是否属于组织（users 或 emails 中有匹配）
	            boolean isPrivateEmail = users.contains(committerName) || emails.contains(email.toLowerCase());

	            // 添加到 commitDataList
	            commitDataList.add(new CommitInfo(commitId, commitDate, isPrivateEmail));
	        }
	        
	        // 按月份统计 organization 数量
	        Map<YearMonth, Integer> monthlyPrivateEmailCount = new HashMap<>();
	        
	        for (CommitInfo commitInfo : commitDataList) {
	            if (commitInfo.isPrivateEmail()) {
	                // 获取 commit 对应的月份
	                YearMonth ym = YearMonth.from(commitInfo.getCommitDate());
	                
	                // 统计该月份的 isPrivateEmail == true 的 commit 数量
	                monthlyPrivateEmailCount.put(ym, monthlyPrivateEmailCount.getOrDefault(ym, 0) + 1);
	            }
	        }

	        // 每月的邮箱统计
	        for (Map.Entry<YearMonth, Integer> entry : monthlyPrivateEmailCount.entrySet()) {
	            YearMonth ym = entry.getKey();
	            Integer emailNum= entry.getValue(); 

	            MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(ym.atDay(1));
                entity.setS1("");
                entity.setId(MYSQL_ID);
                entity.setNumber((double) emailNum);

                mysqlDataMapper.insertMysqlData(entity);
	        }
	    }
	}
	
	// 获取第一个提交的时间
    private LocalDate getFirstCommitDate(List<RevCommit> commits) {
        return StreamSupport.stream(commits.spliterator(), false)
                .map(commit -> commit.getAuthorIdent().getWhen()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new RuntimeException("No commits found"));
    }
    
    // 返回去除常见公共邮箱后的独特邮箱域数量
    private long getUniqueDomains(YearMonth ym, Map<YearMonth, List<String>> monthlyEmailCount) {
    	// 定义常见的公共邮箱域
    	Set<String> commonEmailDomains = loadCommonEmailDomains();

        // 获取该月份的所有邮箱列表
        List<String> emails = monthlyEmailCount.getOrDefault(ym, Collections.emptyList());

        // 统计去除公共邮箱后的独特域名数量
        return (int) emails.stream()
            .map(email -> email.substring(email.indexOf("@") + 1)) // 获取域名
            .filter(domain -> !commonEmailDomains.contains(domain)) // 过滤掉公共邮箱
            .distinct() // 去重
            .peek(System.out::println)
            .count();
    }
    
    // 从文件加载公共邮箱域名
    private Set<String> loadCommonEmailDomains() {
        Set<String> commonEmailDomains = new HashSet<>();
        File file = new File("resources/free_email_provider_domains.txt"); // 读取上一级目录的文件

        if (!file.exists()) {
            System.err.println("Can't find domains file.");
            System.out.println("Now path:"+new File(".").getAbsolutePath());
            return commonEmailDomains; // 返回空集合，避免影响计算
        }

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String domain = scanner.nextLine().trim();
                if (!domain.isEmpty()) {
                    commonEmailDomains.add(domain);
                }
            }
        } catch (Exception e) {
            System.err.println("Read domains file error：" + e.getMessage());
        }

        return commonEmailDomains;
    }
    
    // 定义 CommitInfo 类
    private class CommitInfo {
        private String commitId;
        private LocalDate commitDate;
        private boolean isPrivateEmail; // 是否属于组织

        public CommitInfo(String commitId, LocalDate commitDate, boolean isPrivateEmail) {
            this.commitId = commitId;
            this.commitDate = commitDate;
            this.isPrivateEmail = isPrivateEmail;
        }

        public String getCommitId() { return commitId; }
        public LocalDate getCommitDate() { return commitDate; }
        public boolean isPrivateEmail() { return isPrivateEmail; }
        public void setPrivateEmail(boolean isPrivateEmail) { this.isPrivateEmail = isPrivateEmail; }
    }
    
    // 检查邮箱是否是公共邮箱
    private boolean isCommonEmail(String email, Set<String> commonEmailDomains) {
        String domain = email.substring(email.indexOf("@") + 1);
        return commonEmailDomains.contains(domain);
    }
}