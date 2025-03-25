package com.OSS.Health.service.software.productivity;

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
public class MonthCommitService{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "1.3.3";	// 请替换为对应ID
    private static final String REPO_PATH = "D:/Plateform/Git/repositories/core"; // 请替换为git仓库存储位置
	
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

	        // 获取当前日期
	        LocalDate currentDate = LocalDate.now();

	        // 计算第一个时间节点
	        LocalDate startDate = firstCommitDate.plusMonths(1);
	        if (startDate.getDayOfMonth() != 1) {
	            startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
	        }

	        // 预处理：按月份统计 commit 数量
	        Map<YearMonth, Integer> monthlyCommitCount = new HashMap<>();
	        for (RevCommit commit : commits) {
	            LocalDate commitDate = commit.getAuthorIdent().getWhen().toInstant()
	                    .atZone(ZoneId.systemDefault()).toLocalDate();
	            YearMonth yearMonth = YearMonth.from(commitDate);
	            monthlyCommitCount.put(yearMonth, monthlyCommitCount.getOrDefault(yearMonth, 0) + 1);
	        }

	        // 开始逐月写入数据库
	        if (startDate.isBefore(currentDate)) {
	            LocalDate dateToCheck = startDate;

	            while (!dateToCheck.isAfter(currentDate)) {
	                LocalDate dateToStartTmp = dateToCheck.minusMonths(1);
	                YearMonth currentMonth = YearMonth.from(dateToStartTmp.plusMonths(1)); // 统计的是这个月

	                int commitCount = monthlyCommitCount.getOrDefault(currentMonth, 0);

	                MysqlDataModel entity = new MysqlDataModel();
	                entity.setTime(dateToCheck);
	                entity.setS1("");
	                entity.setId(MYSQL_ID);
	                entity.setNumber((double) commitCount);

	                mysqlDataMapper.insertMysqlData(entity);

	                // 下个月
	                dateToCheck = dateToCheck.plusMonths(1);
	            }
	        } else {
	            System.out.println("The repository is not old enough to calculate.");
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
}