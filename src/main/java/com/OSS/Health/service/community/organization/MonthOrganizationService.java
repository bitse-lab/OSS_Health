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
public class MonthOrganizationService{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "2.1.1";	// 请替换为对应ID
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

	        // 计算第一个时间节点
	        LocalDate startDate = firstCommitDate.plusMonths(1);
	        if (startDate.getDayOfMonth() != 1) {
	            startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
	        }

	        // 预处理：按月份统计 organization 数量
	        Map<YearMonth, List<String>> monthlyEmailCount = new HashMap<>();

	        for (RevCommit commit : commits) {
	            // 获取提交时间并转换为 YearMonth
	            LocalDate commitDate = commit.getAuthorIdent().getWhen()
	                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	            YearMonth ym = YearMonth.from(commitDate);

	            // 获取邮箱地址
	            String email = commit.getAuthorIdent().getEmailAddress();
	            if (email != null && email.contains("@")) {
	                // 添加邮箱到对应月份的列表中
	                monthlyEmailCount
	                    .computeIfAbsent(ym, k -> new ArrayList<>())
	                    .add(email.toLowerCase());  // 小写以避免重复判断问题
	            }
	        }

	        // 每月的邮箱统计
	        for (Map.Entry<YearMonth, List<String>> entry : monthlyEmailCount.entrySet()) {
	            YearMonth ym = entry.getKey();

	            // 统计种类数：
	            long uniqueDomains = getUniqueDomains(ym, monthlyEmailCount);

	            MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(ym.atDay(1));
                entity.setS1("");
                entity.setId(MYSQL_ID);
                entity.setNumber((double) uniqueDomains);

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
    	Set<String> commonEmailDomains = Set.of(
		    "gmail.com", "googlemail.com", "yahoo.com", "yahoo.co.jp", "hotmail.com", "hotmail.de",
		    "outlook.com", "live.com", "live.cn", "live.com.au", "msn.com", "icloud.com", "me.com", "mac.com",
		    "qq.com", "vip.qq.com", "126.com", "163.com", "sina.com", "sohu.com", "yeah.net", "foxmail.com",
		    "aliyun.com", "tencent.com", "users.noreply.github.com",

		    "fastmail.com", "protonmail.com", "pm.me", "tutanota.com", "hushmail.com", "mailbox.org",
		    "gmx.com", "gmx.de", "web.de", "mail.com", "inbox.lv", "dismail.de",

		    "yandex.ru", "rediffmail.com", "nate.com", "daum.net", "hanmail.net", "naver.com",

		    "laposte.net", "seznam.cz", "t-online.de", "bigpond.com",

		    "bellsouth.net", "att.net", "verizon.net", "rogers.com", "shaw.ca",

		    "btinternet.com", "blueyonder.co.uk", "ntlworld.com", "talktalk.net", "virginmedia.com", "sky.com"
		);




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
}