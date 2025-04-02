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
public class MonthOrgEntropyService{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "2.1.2";	// 请替换为对应ID
    private static final String REPO_PATH = "D:/Plateform/Git/repositories/core"; // 请替换为git仓库存储位置
    
    // 用于存储每个提交的信息
    private List<CommitInfo> commitDataList = new ArrayList<>();
    // 用于存储committer和其所有邮箱的映射
    private Map<String, List<String>> userEmail = new HashMap<>();    
    // 用于存储组织和对应committer的映射
    private Map<String, List<String>> domainUser = new HashMap<>();
    // 初始化 user 中存储属于组织的人
    List<String> users = new ArrayList<>();

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
	            }
	        }
	        
	        // 遍历 userEmail，找出所有拥有非公共邮箱的人员，并记录非公共邮箱的域名与人员名
	        for (Map.Entry<String, List<String>> entry : userEmail.entrySet()) {
	            String committer = entry.getKey();
	            List<String> emails = entry.getValue();

	            // 查找是否有非公共邮箱
	            boolean hasPrivateEmail = false;

	            for (String email : emails) {
	                String domain = email.substring(email.indexOf("@") + 1); // 提取域名

	                if (!isCommonEmail(email, commonEmailDomains)) {
	                    hasPrivateEmail = true;
	                    // 记录非公共邮箱的域名与 committer
	                    domainUser.computeIfAbsent(domain, k -> new ArrayList<>()).add(committer);
	                }
	            }

	            if (hasPrivateEmail) {
	                // 如果该人员有非公共邮箱，加入到 users 列表
	                users.add(committer);
	            }
	        }
	        
	        // 用于存储 committer 和其第一个非公共邮箱域名的映射
	        Map<String, String> committerDomainMap = new HashMap<>();
	        // 预处理 committer -> 第一个 domain
	        for (Map.Entry<String, List<String>> entry : domainUser.entrySet()) {
	            String domain = entry.getKey();
	            for (String committer : entry.getValue()) {
	                committerDomainMap.putIfAbsent(committer, domain); // 仅存入第一个 domain
	            }
	        }
	        
	        // 根据users和domainUser添加commitDataList
	        for (RevCommit commit : commits) {
	            String committer = commit.getAuthorIdent().getName();
	            String email = commit.getAuthorIdent().getEmailAddress();
	            LocalDate commitDate = commit.getAuthorIdent().getWhen()
	                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

	            if (email == null || !email.contains("@")) {
	                continue; // 跳过无效邮箱
	            }

	            // 只有 committer 在 users 里才处理
	            if (users.contains(committer)) {
	            	String organization = committerDomainMap.getOrDefault(committer, "Unknown");
	            	if(organization== "Unknown") {
	            		continue;
	            	}

	                // 添加到 commitDataList
	                commitDataList.add(new CommitInfo(commit.getName(), commitDate, organization));
	            }
	        }
	        
	        // 统计每个月的组织提交数
	        Map<YearMonth, Map<String, Integer>> monthlyOrgCommits = new HashMap<>();

	        for (CommitInfo commit : commitDataList) {
	            YearMonth yearMonth = YearMonth.from(commit.getCommitDate());
	            String organization = commit.getOrganization();

	            monthlyOrgCommits
	                .computeIfAbsent(yearMonth, k -> new HashMap<>())  // 初始化该月的组织提交映射
	                .merge(organization, 1, Integer::sum);  // 统计该组织的提交数
	        }

	        // 计算每个月的 Organization 信息熵
	        Map<YearMonth, Double> monthlyEntropy = new HashMap<>();

	        for (Map.Entry<YearMonth, Map<String, Integer>> entry : monthlyOrgCommits.entrySet()) {
	            YearMonth yearMonth = entry.getKey();
	            Map<String, Integer> orgCommitCounts = entry.getValue();
	            if (orgCommitCounts.size() == 12) {
	                for (Map.Entry<String, Integer> a : orgCommitCounts.entrySet()) {
	                    System.out.println(a.getKey());  // 打印每个组织的名称
	                }
	            }

	            int totalCommits = orgCommitCounts.values().stream().mapToInt(Integer::intValue).sum(); // 该月总提交数

	            double entropy = orgCommitCounts.values().stream()
	                .mapToDouble(count -> {
	                    double p = (double) count / totalCommits;  // 计算该 Organization 的占比
	                    return p * Math.log(p) / Math.log(2);  // 计算 p_i * log_2(p_i)
	                })
	                .sum();

	            monthlyEntropy.put(yearMonth, -entropy); // 取负号得到最终信息熵
	        }

	        // 每月的组织信息熵统计
	        for (Map.Entry<YearMonth, Double> entry : monthlyEntropy.entrySet()) {
	            YearMonth ym = entry.getKey();
	            Double entropyNum= entry.getValue(); 

	            MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(ym.atDay(1));
                entity.setS1("");
                entity.setId(MYSQL_ID);
                entity.setNumber(entropyNum);

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
        private String organization; // 组织名字

        public CommitInfo(String commitId, LocalDate commitDate, String organization) {
            this.commitId = commitId;
            this.commitDate = commitDate;
            this.organization= organization;
        }

        public String getCommitId() { return commitId; }
        public LocalDate getCommitDate() { return commitDate; }
        public String getOrganization() { return organization; }
        public void setOrganizaiton(String organization) { this.organization = organization; }
    }
    
    // 检查邮箱是否是公共邮箱
    private boolean isCommonEmail(String email, Set<String> commonEmailDomains) {
        String domain = email.substring(email.indexOf("@") + 1);
        return commonEmailDomains.contains(domain);
    }
}