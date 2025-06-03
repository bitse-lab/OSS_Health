package com.OSS.Health.service.community.organization;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;

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
public class MonthVolunteerEntropyService_new{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;                                                                                                                                                                                                                       

    private static final String MYSQL_ID = "2.1.3";	// 请替换为对应ID
    private String REPO_OWNER;
    private String REPO_NAME;
    private String REPO_PATH;
    
    // 用于存储每个提交的信息
    private List<CommitInfo> commitDataList = new ArrayList<>();
    // 用于存储committer和其所有邮箱的映射
    private Map<String, List<String>> userEmail = new HashMap<>();    
    // 用于存储组织和对应committer的映射
    private Map<String, List<String>> domainUser = new HashMap<>();
    // 初始化 user 中存储属于组织的人
    Set<String> users = new HashSet<>();
    // 初始化包含的组织
    Set<String> domains = new HashSet<>();

    // 获取公共邮箱列表
    Set<String> commonEmailDomains = loadCommonEmailDomains();
	
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

	    try (Git git = Git.open(new File(repoPath))) {

	        // 清除表中的数据
	        mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, MYSQL_ID);

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
	        	if (commit.getAuthorIdent() == null || commit.getAuthorIdent().getEmailAddress() == null) continue;
	            String email = commit.getAuthorIdent().getEmailAddress();
	            String committerName = commit.getAuthorIdent().getName();
	            LocalDate commitDate = commit.getAuthorIdent().getWhen()
	                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	            // 添加到 commitDataList，暂时组织设为 "unknown"
	            CommitInfo info = new CommitInfo( commit.getName(), email, committerName, commitDate, "unknown" );
	            commitDataList.add(info);
	            if (email != null && email.contains("@")) {
	                // 保存 committer 与邮箱的映射
	                userEmail
	                    .computeIfAbsent(committerName, k -> new ArrayList<>())
	                    .add(email.toLowerCase());
	            }
	        }
	        
	        // 按照 commitDate 升序排序
	        commitDataList.sort(Comparator.comparing(CommitInfo::getCommitDate));
	        
	        // 遍历 userEmail，找出所有拥有非公共邮箱的人员，并记录非公共邮箱的域名与人员名
	        for (Map.Entry<String, List<String>> entry : userEmail.entrySet()) {
	            String committer = entry.getKey();
	            List<String> emails = entry.getValue();

	            // 查找是否有非公共邮箱
	            boolean hasPrivateEmail = false;

	            for (String email : emails) {
	                String domain = email.substring(email.indexOf("@") + 1); // 提取域名

	                if (!isCommonEmail(email, commonEmailDomains)) {
	                	domains.add(domain);
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
	        
	        // 删除组织用户
	        Iterator<CommitInfo> iterator = commitDataList.iterator();
	        while (iterator.hasNext()) {
	            CommitInfo commit = iterator.next();
	            String committer = commit.getCommitter();

	            if (users.contains(committer)) {
	                iterator.remove();  // 删除组织用户的提交
	            }
	        }
	        
	        // 统计每个月的 volunteer 提交数
	        Map<YearMonth, Map<String, Integer>> monthlyVolunteerCommits = new HashMap<>();

	        for (CommitInfo commit : commitDataList) {
	            YearMonth yearMonth = YearMonth.from(commit.getCommitDate());
	            String committer = commit.getCommitter();

	            monthlyVolunteerCommits
	                .computeIfAbsent(yearMonth, k -> new HashMap<>())  // 初始化该月的组织提交映射
	                .merge(committer, 1, Integer::sum);  // 统计该组织的提交数
	        }

	        // 计算每个月的 Volunteer 信息熵
	        Map<YearMonth, Double> monthlyEntropy = new HashMap<>();

	        for (Map.Entry<YearMonth, Map<String, Integer>> entry : monthlyVolunteerCommits.entrySet()) {
	            YearMonth yearMonth = entry.getKey();
	            Map<String, Integer> volunteerCommitCounts = entry.getValue();

	            int totalCommits = volunteerCommitCounts.values().stream().mapToInt(Integer::intValue).sum(); // 该月总提交数

	            double entropy = volunteerCommitCounts.values().stream()
	                .mapToDouble(count -> {
	                    double p = (double) count / totalCommits;  // 计算该 Organization 的占比
	                    return p * Math.log(p) / Math.log(2);  // 计算 p_i * log_2(p_i)
	                })
	                .sum();

	            monthlyEntropy.put(yearMonth, -entropy); // 取负号得到最终信息熵
	        }

	        // 每月的 Volunteer 信息熵统计
	        for (Map.Entry<YearMonth, Double> entry : monthlyEntropy.entrySet()) {
	            YearMonth ym = entry.getKey();
	            Double entropyNum= entry.getValue(); 

	            MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(ym.atDay(1));
                entity.setS1("");
                entity.setId(MYSQL_ID);
                entity.setNumber(entropyNum);

                mysqlDataMapper.insertMysqlData_new(REPO_OWNER+'_'+REPO_NAME, entity);
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
        private String email; // 邮箱全名
        private String committer; // 贡献者

        public CommitInfo(String commitId, String email, String committer, LocalDate commitDate, String organization) {
            this.commitId = commitId;
            this.commitDate = commitDate;
            this.organization= organization;
            this.email= email;
            this.committer= committer;
        }

        public String getCommitId() { return commitId; }
        public LocalDate getCommitDate() { return commitDate; }
        public String getOrganization() { return organization; }
        public String getEmail() { return email; }
        public String getCommitter() { return committer; }
        public void setOrganization(String organization) { this.organization = organization; }
    }
    
    // 检查邮箱是否是公共邮箱
    private boolean isCommonEmail(String email, Set<String> commonEmailDomains) {
        String domain = email.substring(email.indexOf("@") + 1);
        return commonEmailDomains.contains(domain);
    }
}