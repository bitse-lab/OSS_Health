package com.OSS.Health.service.software.productivity;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class MonthChangedCodesService{
	@Autowired
    private MysqlDataMapper mysqlDataMapper;

    private static final String MYSQL_ID = "1.3.1";	// 请替换为对应ID
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

	        // 统计每月的代码提交行数
            Map<YearMonth, Integer> monthlyLinesChanged = calculateMonthlyLinesChanged(git, commits);

            // 插入数据库
            if (startDate.isBefore(currentDate)) {
                LocalDate dateToCheck = startDate;

                while (!dateToCheck.isAfter(currentDate)) {
                    YearMonth currentMonth = YearMonth.from(dateToCheck);
                    int linesChanged = monthlyLinesChanged.getOrDefault(currentMonth, 0);

                    MysqlDataModel entity = new MysqlDataModel();
                    entity.setTime(dateToCheck);
                    entity.setS1("");
                    entity.setId(MYSQL_ID);
                    entity.setNumber((double) linesChanged);

                    mysqlDataMapper.insertMysqlData(entity);
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
    
    // 计算每个月的代码提交行数
    private Map<YearMonth, Integer> calculateMonthlyLinesChanged(Git git, List<RevCommit> commits) throws Exception {
        Map<YearMonth, Integer> monthlyLinesChanged = new HashMap<>();

        try (RevWalk revWalk = new RevWalk(git.getRepository())) {
            for (int i = 0; i < commits.size() - 1; i++) {
                RevCommit currentCommit = commits.get(i);
                RevCommit parentCommit = commits.get(i + 1); // 取上一个 commit 作为对比基准

                LocalDate commitDate = currentCommit.getAuthorIdent().getWhen().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                YearMonth yearMonth = YearMonth.from(commitDate);

                int linesChanged = getLinesChanged(git, revWalk, currentCommit, parentCommit);
                monthlyLinesChanged.put(yearMonth, monthlyLinesChanged.getOrDefault(yearMonth, 0) + linesChanged);
            }
        }
        return monthlyLinesChanged;
    }
    
    // 获取某次提交的增删代码行数
    private int getLinesChanged(Git git, RevWalk revWalk, RevCommit newCommit, RevCommit oldCommit) throws Exception {
        int totalChanges = 0;

        AbstractTreeIterator oldTreeParser = prepareTreeParser(git, revWalk, oldCommit);
        AbstractTreeIterator newTreeParser = prepareTreeParser(git, revWalk, newCommit);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter(out)) {
            df.setRepository(git.getRepository());
            List<DiffEntry> diffs = df.scan(oldTreeParser, newTreeParser);

            for (DiffEntry diff : diffs) {
                df.format(diff);
                String diffText = out.toString();
                int insertions = countOccurrences(diffText, "+");
                int deletions = countOccurrences(diffText, "-");

                totalChanges += (insertions + deletions);
                out.reset();
            }
        }
        return totalChanges;
    }

    // 解析 commit 的 Tree
    private AbstractTreeIterator prepareTreeParser(Git git, RevWalk revWalk, RevCommit commit) throws Exception {
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        try (var reader = git.getRepository().newObjectReader()) {
            treeParser.reset(reader, revWalk.parseTree(commit.getTree()));
        }
        return treeParser;
    }

    // 统计字符串中某个字符出现的次数
    private int countOccurrences(String text, String symbol) {
        return text.length() - text.replace(symbol, "").length();
    }
}