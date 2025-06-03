package com.OSS.Health.service.community.vigor;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class LongTermContributorService_new {
	
	@Autowired
	private MysqlDataMapper mysqlDataMapper;
	
    private static final String MYSQL_ID = "2.3.2";	// 请替换为对应ID
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
    	if (REPO_OWNER == null || REPO_NAME == null || REPO_PATH == null) {
            throw new IllegalStateException("Repo info not initialized. Call init() first.");
        }
	    // 设置Git仓库路径
	    String repoPath = REPO_PATH + "/.git";
        
        // 初始化Git仓库
        Git git = Git.open(new File(repoPath));
        
        //清除表中的数据
        mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, MYSQL_ID);

        // 获取所有提交记录
        Iterable<RevCommit> commitsTmp = git.log().call();
        List<RevCommit> commits = new ArrayList<>();
        commitsTmp.forEach(commits::add); // 将 Iterable 转换为 List
        
        // 存储贡献者的提交时间记录
        Map<String, List<LocalDate>> contributorCommits = new HashMap<>();

        // 获取第一个提交的时间
        LocalDate firstCommitDate = getFirstCommitDate(commits);

        // 获取当前日期
        LocalDate currentDate = LocalDate.now();

        // 计算第一个时间节点
        LocalDate startDate = firstCommitDate.plusYears(3);
        if (startDate.getDayOfMonth() != 1) {
            startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
        }
        
        // 如果当前日期距离第一个提交超过三年，开始计算
        if (startDate.isBefore(currentDate)) {
            // 遍历提交记录，将提交时间按作者存储
            for (RevCommit commit : commits) {
                String author = commit.getAuthorIdent().getName();
                LocalDate commitDate = commit.getAuthorIdent().getWhen()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                contributorCommits
                        .computeIfAbsent(author, k -> new ArrayList<>())
                        .add(commitDate);
            }

            // 按作者的提交时间排序
            contributorCommits.values().forEach(Collections::sort);

            // 开始从第一个commit时间加3年后的时间节点开始逐月统计
            LocalDate dateToCheck = startDate;

            // 逐月统计
            while (!dateToCheck.isAfter(currentDate)) {
            	LocalDate dateToCheckTmp = dateToCheck;
            	
            	// 计算截至dateToCheck的总提交数量
                long totalCommitsBeforeDate = contributorCommits.values().stream()
                        .flatMap(List::stream)
                        .filter(commitDate -> commitDate.isBefore(dateToCheckTmp))
                        .count();
                
                // 统计长期贡献者数量
                // 1. 收集所有贡献者及其提交数量
                List<Map.Entry<String, Long>> validContributors = contributorCommits.entrySet().stream()
                        .map(entry -> Map.entry(
                                entry.getKey(),
                                entry.getValue().stream()
                                        .filter(commitDate -> commitDate.isBefore(dateToCheckTmp) || commitDate.isEqual(dateToCheckTmp))
                                        .count())) // 统计提交数量
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())) // 按提交数量从高到低排序
                        .toList();

                // 2. 计算总提交数的80%
                long commitThreshold = (long) (totalCommitsBeforeDate * 0.8);
                long cumulativeCommits = 0;
                int longTermContributors = 0;

                // 3. 累计贡献者直到提交数达到80%，同时筛选贡献时长超过三年的贡献者
                for (Map.Entry<String, Long> contributor : validContributors) {
                    // 获取贡献者的首次提交时间
                    LocalDate firstContributionDate = contributorCommits.get(contributor.getKey()).get(0);
                    
                    cumulativeCommits += contributor.getValue();
                    
                    // 如果累计提交数已达到总提交数的80%，退出循环
                    if (cumulativeCommits >= commitThreshold) {
                        break;
                    }

                    // 判断该贡献者是否贡献超过三年
                    if (firstContributionDate.isBefore(dateToCheckTmp.minusYears(3))) {
                        longTermContributors++;
                    }
                }

                // 输出每月的长期贡献者数量
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(dateToCheck);
                entity.setS1("");
                entity.setId(MYSQL_ID);
                entity.setNumber((double) longTermContributors);

                mysqlDataMapper.insertMysqlData_new(REPO_OWNER+'_'+REPO_NAME, entity);
                // System.out.println("As of " + dateToCheck + ": " + longTermContributors + " long-term contributors");

                // 更新日期，进入下个月
                dateToCheck = dateToCheck.plusMonths(1);
            }
        } else {
            System.out.println("The repository is not old enough to calculate LongTermContributors.");
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
}
