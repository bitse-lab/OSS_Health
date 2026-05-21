package com.OSS.Health;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class ReposToCommunityTest {
    
    private static final String SAMPLE_REPO_JSON = "D:/Plateform/Git/repositories/OSS_Health_gitcode/resources/kernel_liteos.json";
    private static final String CLONE_PATH = "E:/GithubRep2";
    
    @Autowired
    private MysqlDataMapper mysqlDataMapper;

    @Test
    public void testMergedRepositoryCalculation() throws Exception {
        // 解析 JSON 获取仓库列表
        ObjectMapper mapper = new ObjectMapper();
        List<RepoInfo> repos = mapper.readValue(new File(SAMPLE_REPO_JSON), new TypeReference<List<RepoInfo>>() {});
        
        // 过滤有效的仓库
        List<RepoInfo> validRepos = repos.stream()
            .filter(repo -> repo.name.contains("/"))
            .filter(repo -> {
                String repoNameOnly = repo.name.substring(repo.name.lastIndexOf("/") + 1);
                String localPath = CLONE_PATH + File.separator + repoNameOnly;
                return new File(localPath).exists();
            })
            .collect(Collectors.toList());
        
        if (validRepos.isEmpty()) {
            System.out.println("No valid repositories found!");
            return;
        }
        
        System.out.println("Found " + validRepos.size() + " valid repositories for merging");
        
        // 创建合并后的表名
        String mergedTableName = "kernel_liteos";
        
        // 合并计算所有指标
        MergedRepoCalculator calculator = new MergedRepoCalculator(mysqlDataMapper);
        calculator.calculateMergedMetrics(validRepos, mergedTableName, CLONE_PATH);
        
        System.out.println("Merged calculation completed for table: " + mergedTableName);
    }
    
    /**
     * 多仓库合并计算器
     */
    public static class MergedRepoCalculator {
        
        private final MysqlDataMapper mysqlDataMapper;
        
        public MergedRepoCalculator(MysqlDataMapper mysqlDataMapper) {
            this.mysqlDataMapper = mysqlDataMapper;
        }
        
        public void calculateMergedMetrics(List<RepoInfo> repos, String tableName, String clonePath) throws Exception {
            // 创建合并表
            mysqlDataMapper.createTable(tableName);
            
            // 合并所有仓库的Git数据
            MergedGitData mergedData = mergeGitData(repos, clonePath);
            
            // 基于合并数据计算各项指标
            calculateMonthOrgCommits(mergedData, tableName);
            calculateMonthOrgEntropy(mergedData, tableName);
            calculateMonthVolunteerCommits(mergedData, tableName);
            calculateMonthVolunteerEntropy(mergedData, tableName);
            calculatePRLinkedIssue(mergedData, tableName);
            calculatePRMergedRatio(mergedData, tableName);
            calculateReviewRatio(mergedData, tableName);
            calculateCodeContributorCount(mergedData, tableName);
            calculateLongTermContributor(mergedData, tableName);
            System.out.println(1);
            calculateMonthFork(mergedData, tableName);
            System.out.println(2);
            calculateMonthStar(mergedData, tableName);
            System.out.println(3);
            calculateMonthChangedCodes(mergedData, tableName);
            System.out.println(4);
            calculateMonthCommit(mergedData, tableName);
            System.out.println(5);
            calculateMonthIssue(mergedData, tableName);
            System.out.println(6);
            calculateMonthPR(mergedData, tableName);
            System.out.println(7);
        }
        
        /**
         * 合并多个仓库的Git数据和API数据
         */
        private MergedGitData mergeGitData(List<RepoInfo> repos, String clonePath) throws Exception {
            MergedGitData mergedData = new MergedGitData();
            
            for (RepoInfo repo : repos) {
                String repoNameOnly = repo.name.substring(repo.name.lastIndexOf("/") + 1);
                String localPath = clonePath + File.separator + repoNameOnly;
                
                System.out.println("Merging data from: " + repo.name);
                
                // 合并Git提交数据
                mergeCommitData(mergedData, localPath);
                
                // 合并API数据
                mergeApiData(mergedData, localPath);
            }
            
            // 排序合并后的数据
            mergedData.commits.sort(Comparator.comparing(CommitData::getCommitDate));
            mergedData.prData.sort(Comparator.comparing(PRData::getCreatedAt));
            mergedData.issueData.sort(Comparator.comparing(IssueData::getCreatedAt));
            mergedData.starData.sort(Comparator.comparing(d -> d));
            mergedData.forkData.sort(Comparator.comparing(d -> d));
            
            return mergedData;
        }
        
        /**
         * 合并单个仓库的提交数据
         */
        private void mergeCommitData(MergedGitData mergedData, String repoPath) throws Exception {
            String gitPath = repoPath + "/.git";
            try (Git git = Git.open(new File(gitPath))) {
                Iterable<RevCommit> commits = git.log().call();
                
                for (RevCommit commit : commits) {
                    CommitData commitData = new CommitData();
                    commitData.commitId = commit.getId().name();
                    commitData.authorName = commit.getAuthorIdent().getName();
                    commitData.authorEmail = commit.getAuthorIdent().getEmailAddress();
                    commitData.commitDate = commit.getAuthorIdent().getWhen()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    commitData.message = commit.getFullMessage();
                    commitData.repoPath = repoPath; // 记录来源仓库
                    
                    mergedData.commits.add(commitData);
                }
            }
        }
        
        /**
         * 合并单个仓库的API数据
         */
        private void mergeApiData(MergedGitData mergedData, String repoPath) throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            
            // 合并PR数据
            File prFile = new File(repoPath + "/Github_Api_Message/PRData.json");
            if (prFile.exists()) {
                JsonNode prArray = mapper.readTree(prFile);
                for (JsonNode prNode : prArray) {
                    PRData prData = new PRData();
                    prData.number = prNode.get("number").asText();
                    prData.createdAt = LocalDate.parse(prNode.get("created_at").asText().substring(0, 10));
                    prData.merged = prNode.has("merged_at") && !prNode.get("merged_at").isNull() && !prNode.get("merged_at").asText().isEmpty();
                    prData.repoPath = repoPath;
                    
                    // 检查是否linked to issue
                    prData.linkedToIssue = checkPRLinkedToIssue(prNode);
                    
                    mergedData.prData.add(prData);
                }
            }
            
            // 合并Issue数据
            File issueFile = new File(repoPath + "/Github_Api_Message/IssueData.json");
            if (issueFile.exists()) {
                JsonNode issueArray = mapper.readTree(issueFile);
                for (JsonNode issueNode : issueArray) {
                    IssueData issueData = new IssueData();
                    issueData.number = issueNode.get("number").asText();
                    issueData.createdAt = LocalDate.parse(issueNode.get("created_at").asText().substring(0, 10));
                    issueData.repoPath = repoPath;
                    
                    mergedData.issueData.add(issueData);
                }
            }
            
            // 合并Star数据
            File starFile = new File(repoPath + "/Github_Api_Message/StarData.json");
            if (starFile.exists()) {
                JsonNode starArray = mapper.readTree(starFile);
                for (JsonNode starNode : starArray) {
                    String starredAtStr = starNode.get("starred_at").asText();
                    LocalDate starredAt = LocalDate.parse(starredAtStr.substring(0, 10));
                    mergedData.starData.add(starredAt);
                }
            }
            
            // 合并Fork数据
            File forkFile = new File(repoPath + "/Github_Api_Message/ForkData.json");
            if (forkFile.exists()) {
                JsonNode forkArray = mapper.readTree(forkFile);
                for (JsonNode forkNode : forkArray) {
                    String createdAtStr = forkNode.get("created_at").asText();
                    LocalDate createdAt = LocalDate.parse(createdAtStr.substring(0, 10));
                    mergedData.forkData.add(createdAt);
                }
            }
            
            // 合并Review数据
            File reviewFile = new File(repoPath + "/Github_Api_Message/PRReviewData.json");
            if (reviewFile.exists()) {
                JsonNode reviewMap = mapper.readTree(reviewFile);
                Iterator<Map.Entry<String, JsonNode>> fields = reviewMap.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String prNumber = entry.getKey();
                    JsonNode reviewList = entry.getValue();
                    
                    // 找到对应的PR并设置review数量
                    mergedData.prData.stream()
                        .filter(pr -> pr.number.equals(prNumber) && pr.repoPath.equals(repoPath))
                        .findFirst()
                        .ifPresent(pr -> pr.reviewCount = reviewList.size());
                }
            }
        }
        
        /**
         * 检查PR是否链接到Issue
         */
        private boolean checkPRLinkedToIssue(JsonNode prNode) {
            // 检查close_related_issue字段
            if (prNode.has("close_related_issue") && prNode.get("close_related_issue").asInt() > 0) {
                return true;
            }
            
            // 检查标题和描述中的关键词
            String[] keywords = {"close", "closes", "closed", "fix", "fixes", "fixed", "resolve", "resolves", "resolved"};
            String title = prNode.has("title") ? prNode.get("title").asText() : "";
            String body = prNode.has("body") ? prNode.get("body").asText() : "";
            String combinedText = (title + " " + body).toLowerCase();
            
            for (String keyword : keywords) {
                if (combinedText.contains(keyword + " #") || combinedText.contains(keyword + "s #")) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * 基于合并数据计算组织提交数
         */
        private void calculateMonthOrgCommits(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.1.1");
            
            // 识别组织用户
            Set<String> orgUsers = identifyOrgUsers(mergedData.commits);
            
            // 按月统计组织提交
            Map<YearMonth, Integer> monthlyOrgCommits = mergedData.commits.stream()
                .filter(commit -> orgUsers.contains(commit.authorName))
                .collect(Collectors.groupingBy(
                    commit -> YearMonth.from(commit.commitDate),
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
            
            // 插入数据库
            for (Map.Entry<YearMonth, Integer> entry : monthlyOrgCommits.entrySet()) {
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(entry.getKey().atDay(1));
                entity.setS1("");
                entity.setId("2.1.1");
                entity.setNumber(entry.getValue().doubleValue());
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
            }
        }
        
        /**
         * 基于合并数据计算组织信息熵
         */
        private void calculateMonthOrgEntropy(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.1.2");
            
            // 识别组织用户和组织域名
            Map<String, String> userOrgMapping = identifyUserOrgMapping(mergedData.commits);
            
            // 按月统计各组织提交数
            Map<YearMonth, Map<String, Integer>> monthlyOrgCommits = new HashMap<>();
            
            for (CommitData commit : mergedData.commits) {
                String org = userOrgMapping.get(commit.authorName);
                if (org != null && !org.equals("unknown")) {
                    YearMonth ym = YearMonth.from(commit.commitDate);
                    monthlyOrgCommits.computeIfAbsent(ym, k -> new HashMap<>())
                        .merge(org, 1, Integer::sum);
                }
            }
            
            // 计算每月的组织信息熵
            for (Map.Entry<YearMonth, Map<String, Integer>> entry : monthlyOrgCommits.entrySet()) {
                YearMonth ym = entry.getKey();
                Map<String, Integer> orgCounts = entry.getValue();
                
                int totalCommits = orgCounts.values().stream().mapToInt(Integer::intValue).sum();
                int orgCount = orgCounts.size();
                
                if (totalCommits > 0 && orgCount > 1) {
                    double entropy = orgCounts.values().stream()
                        .mapToDouble(count -> {
                            double p = (double) count / totalCommits;
                            return p * Math.log(p) / Math.log(2);
                        })
                        .sum();
                    
                    entropy = -entropy;
                    double maxEntropy = Math.log(orgCount) / Math.log(2);
                    double normalizedEntropy = entropy / maxEntropy;
                    
                    MysqlDataModel entity = new MysqlDataModel();
                    entity.setTime(ym.atDay(1));
                    entity.setS1("");
                    entity.setId("2.1.2");
                    entity.setNumber(normalizedEntropy);
                    mysqlDataMapper.insertMysqlData_new(tableName, entity);
                }
            }
        }
        
        /**
         * 基于合并数据计算志愿者提交数
         */
        private void calculateMonthVolunteerCommits(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.1.4");
            
            // 识别组织用户
            Set<String> orgUsers = identifyOrgUsers(mergedData.commits);
            
            // 按月统计志愿者提交
            Map<YearMonth, Integer> monthlyVolunteerCommits = mergedData.commits.stream()
                .filter(commit -> !orgUsers.contains(commit.authorName))
                .collect(Collectors.groupingBy(
                    commit -> YearMonth.from(commit.commitDate),
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
            
            // 插入数据库
            for (Map.Entry<YearMonth, Integer> entry : monthlyVolunteerCommits.entrySet()) {
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(entry.getKey().atDay(1));
                entity.setS1("");
                entity.setId("2.1.4");
                entity.setNumber(entry.getValue().doubleValue());
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
            }
        }
        
        /**
         * 基于合并数据计算志愿者信息熵
         */
        private void calculateMonthVolunteerEntropy(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.1.3");
            
            // 识别组织用户
            Set<String> orgUsers = identifyOrgUsers(mergedData.commits);
            
            // 按月统计各志愿者提交数
            Map<YearMonth, Map<String, Integer>> monthlyVolunteerCommits = new HashMap<>();
            
            for (CommitData commit : mergedData.commits) {
                if (!orgUsers.contains(commit.authorName)) {
                    YearMonth ym = YearMonth.from(commit.commitDate);
                    monthlyVolunteerCommits.computeIfAbsent(ym, k -> new HashMap<>())
                        .merge(commit.authorName, 1, Integer::sum);
                }
            }
            
            // 计算每月的志愿者信息熵
            for (Map.Entry<YearMonth, Map<String, Integer>> entry : monthlyVolunteerCommits.entrySet()) {
                YearMonth ym = entry.getKey();
                Map<String, Integer> volunteerCounts = entry.getValue();
                
                int totalCommits = volunteerCounts.values().stream().mapToInt(Integer::intValue).sum();
                
                if (totalCommits > 0) {
                    double entropy = volunteerCounts.values().stream()
                        .mapToDouble(count -> {
                            double p = (double) count / totalCommits;
                            return p * Math.log(p) / Math.log(2);
                        })
                        .sum();
                    
                    entropy = -entropy;
                    
                    MysqlDataModel entity = new MysqlDataModel();
                    entity.setTime(ym.atDay(1));
                    entity.setS1("");
                    entity.setId("2.1.3");
                    entity.setNumber(entropy);
                    mysqlDataMapper.insertMysqlData_new(tableName, entity);
                }
            }
        }
        
        /**
         * 基于合并数据计算PR链接Issue比率
         */
        private void calculatePRLinkedIssue(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.2.3");
            
            if (mergedData.prData.isEmpty()) return;
            
            LocalDate firstPRDate = mergedData.prData.get(0).createdAt;
            LocalDate startDate = firstPRDate.plusDays(90);
            if (startDate.getDayOfMonth() != 1) {
                startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
            }
            LocalDate currentDate = LocalDate.now();
            
            LocalDate dateToCheck = startDate;
            while (!dateToCheck.isAfter(currentDate)) {
                LocalDate dateStart = dateToCheck.minusDays(90);
                LocalDate dateEnd = dateToCheck;
                
                List<PRData> periodPRs = mergedData.prData.stream()
                    .filter(pr -> !pr.createdAt.isBefore(dateStart) && !pr.createdAt.isAfter(dateEnd))
                    .collect(Collectors.toList());
                
                if (!periodPRs.isEmpty()) {
                    long linkedCount = periodPRs.stream().mapToLong(pr -> pr.linkedToIssue ? 1 : 0).sum();
                    double ratio = (double) linkedCount / periodPRs.size();
                    
                    MysqlDataModel entity = new MysqlDataModel();
                    entity.setTime(dateToCheck);
                    entity.setS1("");
                    entity.setId("2.2.3");
                    entity.setNumber(ratio);
                    mysqlDataMapper.insertMysqlData_new(tableName, entity);
                }
                
                dateToCheck = dateToCheck.plusMonths(1);
            }
        }
        
        /**
         * 基于合并数据计算PR合并比率
         */
        private void calculatePRMergedRatio(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.2.2");
            
            if (mergedData.prData.isEmpty()) return;
            
            LocalDate firstPRDate = mergedData.prData.get(0).createdAt;
            LocalDate startDate = firstPRDate.plusDays(90);
            if (startDate.getDayOfMonth() != 1) {
                startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
            }
            LocalDate currentDate = LocalDate.now();
            
            // 如果当前日期距离第一个PR超过90d，开始计算
            if (startDate.isBefore(currentDate)) {
                // 开始从第一个PR时间加90d后的时间节点开始逐月统计
                LocalDate dateToCheck = startDate;
                
                // 逐月统计
                while (!dateToCheck.isAfter(currentDate)) {
                    LocalDate dateToStart = dateToCheck.minusDays(90);
                    
                    // 计算从dateToStart到dateToCheck这90天内的PR合并比率
                    int prCount = 0;
                    int mergedPRCount = 0;
                    
                    for (PRData prData : mergedData.prData) {
                        if (!prData.createdAt.isBefore(dateToStart) && !prData.createdAt.isAfter(dateToCheck)) {
                            prCount++;
                            if (prData.merged) {
                                mergedPRCount++;
                            }
                        }
                    }
                    
                    MysqlDataModel entity = new MysqlDataModel();
                    entity.setTime(dateToCheck);
                    entity.setS1("");
                    entity.setId("2.2.2");
                    
                    // 计算合并比率
                    if (prCount > 0) {
                        entity.setNumber((double) mergedPRCount / prCount);
                    } else {
                        entity.setNumber(0.0);
                    }
                    
                    mysqlDataMapper.insertMysqlData_new(tableName, entity);
                    
                    // 更新日期，进入下个月
                    dateToCheck = dateToCheck.plusMonths(1);
                }
            } else {
                System.out.println("The merged repository data is not old enough to calculate PR merged ratio.");
            }
        }
        
        /**
         * 基于合并数据计算Review比率
         */
        private void calculateReviewRatio(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.2.1");
            
            if (mergedData.prData.isEmpty()) return;
            
            LocalDate firstPRDate = mergedData.prData.get(0).createdAt;
            LocalDate startDate = firstPRDate.plusDays(90);
            if (startDate.getDayOfMonth() != 1) {
                startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
            }
            LocalDate currentDate = LocalDate.now();
            
            LocalDate dateToCheck = startDate;
            while (!dateToCheck.isAfter(currentDate)) {
                LocalDate dateStart = dateToCheck.minusDays(90);
                LocalDate dateEnd = dateToCheck;
                
                List<PRData> periodPRs = mergedData.prData.stream()
                    .filter(pr -> !pr.createdAt.isBefore(dateStart) && !pr.createdAt.isAfter(dateEnd))
                    .collect(Collectors.toList());
                
                if (!periodPRs.isEmpty()) {
                    long reviewedCount = periodPRs.stream().mapToLong(pr -> pr.reviewCount > 0 ? 1 : 0).sum();
                    double ratio = (double) reviewedCount / periodPRs.size();
                    
                    MysqlDataModel entity = new MysqlDataModel();
                    entity.setTime(dateToCheck);
                    entity.setS1("");
                    entity.setId("2.2.1");
                    entity.setNumber(ratio);
                    mysqlDataMapper.insertMysqlData_new(tableName, entity);
                }
                
                dateToCheck = dateToCheck.plusMonths(1);
            }
        }
        
        /**
         * 基于合并数据计算代码贡献者数量
         */
        private void calculateCodeContributorCount(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.3.1");
            
            if (mergedData.commits.isEmpty()) return;
            
            LocalDate firstCommitDate = mergedData.commits.get(0).commitDate;
            LocalDate startDate = firstCommitDate.plusDays(90);
            if (startDate.getDayOfMonth() != 1) {
                startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
            }
            LocalDate currentDate = LocalDate.now();
            
            LocalDate dateToCheck = startDate;
            while (!dateToCheck.isAfter(currentDate)) {
                LocalDate dateStart = dateToCheck.minusDays(90);
                LocalDate dateEnd = dateToCheck;
                
                Set<String> activeContributors = mergedData.commits.stream()
                    .filter(commit -> !commit.commitDate.isBefore(dateStart) && !commit.commitDate.isAfter(dateEnd))
                    .map(commit -> commit.authorName)
                    .collect(Collectors.toSet());
                
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(dateToCheck);
                entity.setS1("");
                entity.setId("2.3.1");
                entity.setNumber((double) activeContributors.size());
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
                
                dateToCheck = dateToCheck.plusMonths(1);
            }
        }
        
        /**
         * 基于合并数据计算长期贡献者数量
         */
        private void calculateLongTermContributor(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "2.3.2");
            
            if (mergedData.commits.isEmpty()) return;
            
            LocalDate firstCommitDate = mergedData.commits.get(0).commitDate;
            LocalDate startDate = firstCommitDate.plusYears(3);
            if (startDate.getDayOfMonth() != 1) {
                startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
            }
            LocalDate currentDate = LocalDate.now();
            
            if (startDate.isAfter(currentDate)) {
                System.out.println("Repository not old enough for long-term contributor calculation");
                return;
            }
            
            // 按贡献者统计提交
            Map<String, List<LocalDate>> contributorCommits = mergedData.commits.stream()
                .collect(Collectors.groupingBy(
                    commit -> commit.authorName,
                    Collectors.mapping(commit -> commit.commitDate, Collectors.toList())
                ));
            
            LocalDate dateToCheck = startDate;
            while (!dateToCheck.isAfter(currentDate)) {
                // 创建final变量供lambda使用
                final LocalDate currentCheckDate = dateToCheck;
                
                // 计算长期贡献者
                long longTermContributors = contributorCommits.entrySet().stream()
                    .filter(entry -> {
                        LocalDate firstContribution = entry.getValue().stream().min(LocalDate::compareTo).orElse(currentCheckDate);
                        return firstContribution.isBefore(currentCheckDate.minusYears(3));
                    })
                    .count();
                
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(dateToCheck);
                entity.setS1("");
                entity.setId("2.3.2");
                entity.setNumber((double) longTermContributors);
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
                
                dateToCheck = dateToCheck.plusMonths(1);
            }
        }
        
        /**
         * 基于合并数据计算每月Fork数
         */
        private void calculateMonthFork(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "3.2.2");
            
            if (mergedData.forkData.isEmpty() || mergedData.commits.isEmpty()) {
                System.out.println("No fork data or commit data available for calculation");
                return;
            }
            
            LocalDate firstCommitDate = mergedData.commits.get(0).commitDate;
            LocalDate startDate = firstCommitDate.plusDays(30);
            if (startDate.getDayOfMonth() != 1) {
                startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
            }
            LocalDate currentDate = LocalDate.now();
            
            if (startDate.isAfter(currentDate)) {
                System.out.println("The merged repository data is not old enough to calculate fork data.");
                return;
            }
            
            LocalDate dateToCheck = startDate;
            while (!dateToCheck.isAfter(currentDate)) {
                LocalDate dateToStartTmp = dateToCheck.minusMonths(1);
                
                // 修复：使用正确的时间范围
                final LocalDate startRange = dateToStartTmp;
                final LocalDate endRange = dateToCheck;
                
                // 计算该月新增fork数量
                long forkCount = mergedData.forkData.stream()
                    .filter(forkDate -> !forkDate.isBefore(startRange) && !forkDate.isAfter(endRange))
                    .count();
                
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(dateToStartTmp);
                entity.setS1("");
                entity.setId("3.2.2");
                entity.setNumber((double) forkCount);
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
                
                dateToCheck = dateToCheck.plusMonths(1);
            }
        }
        
        /**
         * 基于合并数据计算每月Star数
         */
        private void calculateMonthStar(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "3.2.1");
            
            if (mergedData.starData.isEmpty() || mergedData.commits.isEmpty()) {
                System.out.println("No star data or commit data available for calculation");
                return;
            }
            
            LocalDate firstCommitDate = mergedData.commits.get(0).commitDate;
            LocalDate startDate = firstCommitDate.plusDays(30);
            if (startDate.getDayOfMonth() != 1) {
                startDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth());
            }
            LocalDate currentDate = LocalDate.now();
            
            if (startDate.isAfter(currentDate)) {
                System.out.println("The merged repository data is not old enough to calculate star data.");
                return;
            }
            
            LocalDate dateToCheck = startDate;
            while (!dateToCheck.isAfter(currentDate)) {
                LocalDate dateToStartTmp = dateToCheck.minusMonths(1);
                
                // 修复：使用正确的时间范围，与单库算法保持一致
                final LocalDate startRange = dateToStartTmp;
                final LocalDate endRange = dateToCheck;
                
                // 计算该月新增star数量
                long starCount = mergedData.starData.stream()
                    .filter(starDate -> !starDate.isBefore(startRange) && !starDate.isAfter(endRange))
                    .count();
                
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(dateToStartTmp);  // 保存的是当月第一天
                entity.setS1("");
                entity.setId("3.2.1");
                entity.setNumber((double) starCount);
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
                
                dateToCheck = dateToCheck.plusMonths(1);
            }
        }
        
        /**
         * 基于合并数据计算每月代码变更行数（完整版）
         */
        private void calculateMonthChangedCodes(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "1.3.1");
            
            Map<YearMonth, Integer> monthlyChangedLines = new HashMap<>();
            
            // 遍历所有提交，计算实际的代码变更行数
            for (CommitData commit : mergedData.commits) {
                YearMonth yearMonth = YearMonth.from(commit.commitDate);
                int changedLines = calculateCommitChangedLines(commit);
                monthlyChangedLines.merge(yearMonth, changedLines, Integer::sum);
            }
            
            for (Map.Entry<YearMonth, Integer> entry : monthlyChangedLines.entrySet()) {
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(entry.getKey().atDay(1));
                entity.setS1("");
                entity.setId("1.3.1");
                entity.setNumber(entry.getValue().doubleValue());
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
            }
        }

        /**
         * 计算单个提交的代码变更行数
         */
        private int calculateCommitChangedLines(CommitData commit) {
            try {
                // 使用git命令获取提交的统计信息
                ProcessBuilder pb = new ProcessBuilder("git", "show", "--stat", "--format=", commit.commitId);
                pb.directory(new File(commit.repoPath));
                Process process = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                int totalChanges = 0;
                
                while ((line = reader.readLine()) != null) {
                    // 解析git show --stat输出，格式如：filename | 10 +++++-----
                    if (line.contains("|") && (line.contains("+") || line.contains("-"))) {
                        String[] parts = line.split("\\|");
                        if (parts.length > 1) {
                            String statPart = parts[1].trim();
                            // 提取数字部分
                            String[] statTokens = statPart.split("\\s+");
                            if (statTokens.length > 0) {
                                try {
                                    totalChanges += Integer.parseInt(statTokens[0]);
                                } catch (NumberFormatException e) {
                                    // 如果解析失败，尝试计算+和-的数量
                                    totalChanges += countPlusMinusChars(statPart);
                                }
                            }
                        }
                    }
                }
                
                process.waitFor();
                return totalChanges;
                
            } catch (Exception e) {
                System.err.println("Error calculating changed lines for commit " + commit.commitId + ": " + e.getMessage());
                // 如果git命令失败，使用备用方法
                return calculateChangedLinesFromDiff(commit);
            }
        }

        /**
         * 从diff输出计算变更行数的备用方法
         */
        private int calculateChangedLinesFromDiff(CommitData commit) {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "diff", "--numstat", commit.commitId + "^", commit.commitId);
                pb.directory(new File(commit.repoPath));
                Process process = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                int totalChanges = 0;
                
                while ((line = reader.readLine()) != null) {
                    // git diff --numstat输出格式：added deleted filename
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int added = "-".equals(parts[0]) ? 0 : Integer.parseInt(parts[0]);
                            int deleted = "-".equals(parts[1]) ? 0 : Integer.parseInt(parts[1]);
                            totalChanges += added + deleted;
                        } catch (NumberFormatException e) {
                            // 忽略二进制文件等无法解析的行
                        }
                    }
                }
                
                process.waitFor();
                return totalChanges;
                
            } catch (Exception e) {
                System.err.println("Error in backup method for commit " + commit.commitId + ": " + e.getMessage());
                return 0;
            }
        }

        /**
         * 计算字符串中+和-字符的数量
         */
        private int countPlusMinusChars(String str) {
            int count = 0;
            for (char c : str.toCharArray()) {
                if (c == '+' || c == '-') {
                    count++;
                }
            }
            return count;
        }
        
        /**
         * 基于合并数据计算每月代码变更行数（带批量处理和降级策略）- 完整修复版
         */
        private void calculateMonthChangedCodes_new(MergedGitData mergedData, String tableName) {
            try {
                mysqlDataMapper.clearMysqlDataById_new(tableName, "1.3.1");
                
                Map<YearMonth, Integer> monthlyChangedLines = new HashMap<>();
                
                // 1. 尝试批量获取所有commit的变更数据
                Map<String, Integer> commitChangesMap = batchGetCommitChanges_new(mergedData.commits);
                
                // 2. 在内存中计算月度统计
                for (CommitData commit : mergedData.commits) {
                    YearMonth yearMonth = YearMonth.from(commit.commitDate);
                    int changedLines = commitChangesMap.getOrDefault(commit.commitId, 0);
                    monthlyChangedLines.merge(yearMonth, changedLines, Integer::sum);
                }
                
                // 3. 插入数据库
                for (Map.Entry<YearMonth, Integer> entry : monthlyChangedLines.entrySet()) {
                    MysqlDataModel entity = new MysqlDataModel();
                    entity.setTime(entry.getKey().atDay(1));
                    entity.setS1("");
                    entity.setId("1.3.1");
                    entity.setNumber(entry.getValue().doubleValue());
                    mysqlDataMapper.insertMysqlData_new(tableName, entity);
                }
                
                System.out.println("✅ 月度代码变更统计完成: " + monthlyChangedLines.size() + " 个月");
                
            } catch (Exception e) {
                System.err.println("❌ 处理库时出错，已跳过: " + 
                    (mergedData != null && mergedData.commits != null && !mergedData.commits.isEmpty() ? 
                     mergedData.commits.get(0).repoPath : "未知库") + 
                    ", 错误: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * 批量获取多个commit的变更行数 - 智能降级策略（完整修复版）
         */
        private Map<String, Integer> batchGetCommitChanges_new(List<CommitData> commits) {
            Map<String, Integer> result = new HashMap<>();
            if (commits.isEmpty()) return result;
            
            String repoPath = commits.get(0).repoPath;
            
            // 🔍 检查命令长度
            int estimatedLength = calculateCommandLength(commits);
            int maxLength = isWindows() ? 7000 : 100000;
            
            System.out.println("📊 批量处理信息:");
            System.out.println("   仓库: " + new File(repoPath).getName());
            System.out.println("   Commits数量: " + commits.size());
            System.out.println("   预估命令长度: " + estimatedLength);
            
            // 如果命令太长，直接分批
            if (estimatedLength > maxLength) {
                System.out.println("⚠️ 命令行过长 (" + estimatedLength + " > " + maxLength + ")，直接使用分批模式");
                return batchProcessInChunks(commits, repoPath);
            }
            
            // 策略1: 尝试一次性批量获取
            try {
                return batchGetCommitChangesInternal_new(commits, repoPath);
            } catch (IOException e) {
                System.err.println("⚠️ 批量获取失败: " + e.getMessage());
                System.err.println("   切换到分批处理模式...");
                return batchProcessInChunks(commits, repoPath);
            } catch (Exception e) {
                System.err.println("❌ 批量获取异常: " + e.getMessage());
                e.printStackTrace();
                return batchProcessInChunks(commits, repoPath);
            }
        }

        /**
         * 分批处理commits
         */
        private Map<String, Integer> batchProcessInChunks(List<CommitData> commits, String repoPath) {
            Map<String, Integer> result = new HashMap<>();
            int batchSize = 50; // 减小批次大小以提高成功率
            
            System.out.println("🔄 开始分批处理，批次大小: " + batchSize);
            
            for (int i = 0; i < commits.size(); i += batchSize) {
                int end = Math.min(i + batchSize, commits.size());
                List<CommitData> batch = commits.subList(i, end);
                int batchNum = i / batchSize + 1;
                
                try {
                    Map<String, Integer> batchResult = batchGetCommitChangesInternal_new(batch, repoPath);
                    result.putAll(batchResult);
                    System.out.println("✅ 批次 " + batchNum + " 成功 (" + batch.size() + " commits)");
                } catch (Exception batchError) {
                    System.err.println("❌ 批次 " + batchNum + " 失败: " + batchError.getMessage());
                    System.err.println("   降级为逐个处理...");
                    fallbackGetCommitChanges_new(batch, result);
                }
            }
            
            System.out.println("✅ 分批处理完成，共处理 " + result.size() + " 个commits");
            return result;
        }

        /**
         * 内部批量获取方法（使用git show --numstat）- 完全修复版
         */
        private Map<String, Integer> batchGetCommitChangesInternal_new(
                List<CommitData> commits, String repoPath) throws IOException, InterruptedException {
            
            Map<String, Integer> result = new HashMap<>();
            
            // 构建命令
            List<String> command = new ArrayList<>();
            command.add("git");
            command.add("show");
            command.add("--numstat");
            command.add("--format=commit %H");  // 输出commit标记
            command.add("--no-renames");        // 禁用重命名检测
            
            // 添加所有commit hash
            for (CommitData commit : commits) {
                command.add(commit.commitId);
            }
            
            // 🔍 调试信息
            System.out.println("🔍 执行命令: git show --numstat [" + commits.size() + " commits]");
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(repoPath));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 解析输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String currentCommit = null;
                int currentChanges = 0;
                boolean inNumstatSection = false;  // 🎯 关键：标记是否在numstat区域
                String line;
                
                while ((line = reader.readLine()) != null) {
                    // 识别commit行
                    if (line.startsWith("commit ")) {
                        // 保存上一个commit的结果
                        if (currentCommit != null) {
                            result.put(currentCommit, currentChanges);
                        }
                        // 开始新的commit
                        currentCommit = line.substring(7).trim();
                        currentChanges = 0;
                        inNumstatSection = true;  // 🎯 commit后面就是numstat区域
                        
                    } else if (inNumstatSection) {
                        // 🎯 在numstat区域内
                        if (line.isEmpty()) {
                            // 空行表示numstat区域结束
                            inNumstatSection = false;
                        } else if (line.contains("\t")) {
                            // 解析 numstat 行：added\tdeleted\tfilename
                            String[] parts = line.split("\t");
                            if (parts.length >= 2) {
                                try {
                                    int added = parts[0].equals("-") ? 0 : Integer.parseInt(parts[0]);
                                    int deleted = parts[1].equals("-") ? 0 : Integer.parseInt(parts[1]);
                                    currentChanges += added + deleted;
                                } catch (NumberFormatException e) {
                                    // 跳过二进制文件（显示为 - ）
                                }
                            }
                        }
                    }
                    // 其他行（diff内容等）直接忽略
                }
                
                // 保存最后一个commit
                if (currentCommit != null) {
                    result.put(currentCommit, currentChanges);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Git命令执行失败，退出码: " + exitCode);
            }
            
            System.out.println("✅ 批量处理成功: " + result.size() + " commits");
            return result;
        }

        /**
         * 降级方案：逐个获取commit变更（单个失败不影响其他）
         */
        private void fallbackGetCommitChanges_new(List<CommitData> commits, Map<String, Integer> result) {
            int successCount = 0;
            int failCount = 0;
            
            System.out.println("🔄 开始逐个处理 " + commits.size() + " 个commits...");
            
            for (CommitData commit : commits) {
                try {
                    int changes = calculateCommitChangedLines_new(commit);
                    result.put(commit.commitId, changes);
                    successCount++;
                } catch (Exception e) {
                    // 单个commit失败，只记录日志，继续处理下一个
                    System.err.println("⚠️ 跳过commit " + commit.commitId.substring(0, 8) + 
                                     ": " + e.getMessage());
                    result.put(commit.commitId, 0); // 失败的commit记为0变更
                    failCount++;
                }
            }
            
            System.out.println("📊 逐个处理完成: 成功 " + successCount + ", 失败 " + failCount);
        }

        /**
         * 计算单个提交的代码变更行数
         */
        private int calculateCommitChangedLines_new(CommitData commit) {
            try {
                // 使用git diff --numstat获取精确的变更行数
                ProcessBuilder pb = new ProcessBuilder("git", "diff", "--numstat", 
                                                      commit.commitId + "^", commit.commitId);
                pb.directory(new File(commit.repoPath));
                Process process = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                int totalChanges = 0;
                
                while ((line = reader.readLine()) != null) {
                    // git diff --numstat输出格式：added deleted filename
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int added = parts[0].equals("-") ? 0 : Integer.parseInt(parts[0]);
                            int deleted = parts[1].equals("-") ? 0 : Integer.parseInt(parts[1]);
                            totalChanges += added + deleted;
                        } catch (NumberFormatException e) {
                            // 忽略二进制文件等无法解析的行
                        }
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new IOException("Git命令执行失败，退出码: " + exitCode);
                }
                
                return totalChanges;
                
            } catch (Exception e) {
                System.err.println("Error calculating changed lines for commit " + 
                                 commit.commitId.substring(0, 8) + ": " + e.getMessage());
                return 0;
            }
        }

        /**
         * 计算命令长度
         */
        private int calculateCommandLength(List<CommitData> commits) {
            int baseLength = "git show --numstat --format=commit %H --no-renames ".length();
            int commitsLength = commits.size() * 41; // 每个commit hash 40字符 + 空格
            return baseLength + commitsLength;
        }

        /**
         * 判断是否Windows系统
         */
        private boolean isWindows() {
            return System.getProperty("os.name").toLowerCase().contains("win");
        }



        /**
         * 基于合并数据计算每月提交数
         */
        private void calculateMonthCommit(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "1.3.3");
            
            Map<YearMonth, Long> monthlyCommits = mergedData.commits.stream()
                .collect(Collectors.groupingBy(
                    commit -> YearMonth.from(commit.commitDate),
                    Collectors.counting()
                ));
            
            for (Map.Entry<YearMonth, Long> entry : monthlyCommits.entrySet()) {
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(entry.getKey().atDay(1));
                entity.setS1("");
                entity.setId("1.3.3");
                entity.setNumber(entry.getValue().doubleValue());
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
            }
        }

        /**
         * 基于合并数据计算每月Issue数
         */
        private void calculateMonthIssue(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "1.3.2");
            
            Map<YearMonth, Long> monthlyIssues = mergedData.issueData.stream()
                .collect(Collectors.groupingBy(
                    issue -> YearMonth.from(issue.createdAt),
                    Collectors.counting()
                ));
            
            for (Map.Entry<YearMonth, Long> entry : monthlyIssues.entrySet()) {
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(entry.getKey().atDay(1));
                entity.setS1("");
                entity.setId("1.3.2");
                entity.setNumber(entry.getValue().doubleValue());
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
            }
        }

        /**
         * 基于合并数据计算每月PR数
         */
        private void calculateMonthPR(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "1.3.4");
            
            Map<YearMonth, Long> monthlyPRs = mergedData.prData.stream()
                .collect(Collectors.groupingBy(
                    pr -> YearMonth.from(pr.createdAt),
                    Collectors.counting()
                ));
            
            for (Map.Entry<YearMonth, Long> entry : monthlyPRs.entrySet()) {
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(entry.getKey().atDay(1));
                entity.setS1("");
                entity.setId("1.3.4");
                entity.setNumber(entry.getValue().doubleValue());
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
            }
        }

        /**
         * 基于合并数据计算每月活跃开发者数（完整版）
         */
        private void calculateMonthActiveDeveloper(MergedGitData mergedData, String tableName) throws Exception {
            mysqlDataMapper.clearMysqlDataById_new(tableName, "1.1.2");
            
            Map<YearMonth, Set<String>> monthlyDevelopers = new HashMap<>();
            
            // 收集每月的活跃开发者（基于提交）
            for (CommitData commit : mergedData.commits) {
                YearMonth yearMonth = YearMonth.from(commit.commitDate);
                monthlyDevelopers.computeIfAbsent(yearMonth, k -> new HashSet<>())
                    .add(normalizeAuthorName(commit.authorName, commit.authorEmail));
            }
            
            // 收集每月的活跃开发者（基于PR）
            for (PRData pr : mergedData.prData) {
                YearMonth yearMonth = YearMonth.from(pr.createdAt);
                if (pr.authorName != null) {
                    monthlyDevelopers.computeIfAbsent(yearMonth, k -> new HashSet<>())
                        .add(normalizeAuthorName(pr.authorName, pr.authorEmail));
                }
            }
            
            // 收集每月的活跃开发者（基于Issue）
            for (IssueData issue : mergedData.issueData) {
                YearMonth yearMonth = YearMonth.from(issue.createdAt);
                if (issue.authorName != null) {
                    monthlyDevelopers.computeIfAbsent(yearMonth, k -> new HashSet<>())
                        .add(normalizeAuthorName(issue.authorName, issue.authorEmail));
                }
            }
            
            for (Map.Entry<YearMonth, Set<String>> entry : monthlyDevelopers.entrySet()) {
                MysqlDataModel entity = new MysqlDataModel();
                entity.setTime(entry.getKey().atDay(1));
                entity.setS1("");
                entity.setId("1.1.2");
                entity.setNumber((double) entry.getValue().size());
                mysqlDataMapper.insertMysqlData_new(tableName, entity);
            }
        }

        /**
         * 标准化作者名称，处理同一人使用不同名称的情况
         */
        private String normalizeAuthorName(String name, String email) {
            if (email != null && email.contains("@")) {
                // 使用邮箱作为唯一标识符
                return email.toLowerCase().trim();
            }
            if (name != null) {
                // 标准化名称：去除空格，转小写
                return name.toLowerCase().trim().replaceAll("\\s+", " ");
            }
            return "unknown";
        }

        /**
         * 识别组织用户（完整版）
         */
        private Set<String> identifyOrgUsers(List<CommitData> commits) {
            Set<String> commonEmailDomains = loadCommonEmailDomains();
            Map<String, UserInfo> userInfoMap = new HashMap<>();
            
            // 收集每个用户的详细信息
            for (CommitData commit : commits) {
                if (commit.authorEmail != null && commit.authorEmail.contains("@")) {
                    String normalizedUser = normalizeAuthorName(commit.authorName, commit.authorEmail);
                    UserInfo userInfo = userInfoMap.computeIfAbsent(normalizedUser, k -> new UserInfo());
                    
                    userInfo.names.add(commit.authorName);
                    userInfo.emails.add(commit.authorEmail.toLowerCase());
                    userInfo.commitCount++;
                    
                    // 记录最早和最晚的提交时间
                    if (userInfo.firstCommit == null || commit.commitDate.isBefore(userInfo.firstCommit)) {
                        userInfo.firstCommit = commit.commitDate;
                    }
                    if (userInfo.lastCommit == null || commit.commitDate.isAfter(userInfo.lastCommit)) {
                        userInfo.lastCommit = commit.commitDate;
                    }
                }
            }
            
            // 基于多个维度识别组织用户
            Set<String> orgUsers = new HashSet<>();
            for (Map.Entry<String, UserInfo> entry : userInfoMap.entrySet()) {
                String user = entry.getKey();
                UserInfo info = entry.getValue();
                
                boolean isOrgUser = false;
                
                // 1. 有非公共邮箱域名
                boolean hasPrivateEmail = info.emails.stream()
                    .anyMatch(email -> !isCommonEmail(email, commonEmailDomains));
                
                // 2. 提交数量较多（超过阈值）
                boolean isActiveContributor = info.commitCount >= 5;
                
                // 3. 贡献时间跨度较长（超过30天）
                boolean isLongTermContributor = info.firstCommit != null && info.lastCommit != null &&
                    ChronoUnit.DAYS.between(info.firstCommit, info.lastCommit) > 30;
                
                // 4. 使用多个邮箱地址
                boolean hasMultipleEmails = info.emails.size() > 1;
                
                if (hasPrivateEmail || (isActiveContributor && isLongTermContributor) || hasMultipleEmails) {
                    isOrgUser = true;
                }
                
                if (isOrgUser) {
                    orgUsers.add(user);
                }
            }
            
            return orgUsers;
        }

        /**
         * 用户信息类
         */
        private static class UserInfo {
            Set<String> names = new HashSet<>();
            Set<String> emails = new HashSet<>();
            int commitCount = 0;
            LocalDate firstCommit;
            LocalDate lastCommit;
        }

        /**
         * 识别用户组织映射（完整版）
         */
        private Map<String, String> identifyUserOrgMapping(List<CommitData> commits) {
            Set<String> commonEmailDomains = loadCommonEmailDomains();
            Map<String, String> userOrgMapping = new HashMap<>();
            Map<String, UserInfo> userInfoMap = new HashMap<>();
            
            // 收集用户信息
            for (CommitData commit : commits) {
                if (commit.authorEmail != null && commit.authorEmail.contains("@")) {
                    String normalizedUser = normalizeAuthorName(commit.authorName, commit.authorEmail);
                    UserInfo userInfo = userInfoMap.computeIfAbsent(normalizedUser, k -> new UserInfo());
                    userInfo.emails.add(commit.authorEmail.toLowerCase());
                }
            }
            
            // 为每个用户确定组织
            for (Map.Entry<String, UserInfo> entry : userInfoMap.entrySet()) {
                String user = entry.getKey();
                UserInfo info = entry.getValue();
                
                // 优先选择非公共邮箱域名
                String selectedOrg = null;
                Map<String, Integer> domainCount = new HashMap<>();
                
                for (String email : info.emails) {
                    if (!isCommonEmail(email, commonEmailDomains)) {
                        String domain = email.substring(email.indexOf("@") + 1);
                        domainCount.merge(domain, 1, Integer::sum);
                    }
                }
                
                // 选择出现次数最多的域名作为组织
                if (!domainCount.isEmpty()) {
                    selectedOrg = domainCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("unknown");
                } else {
                    selectedOrg = "independent"; // 只有公共邮箱的用户标记为独立开发者
                }
                
                userOrgMapping.put(user, selectedOrg);
            }
            
            return userOrgMapping;
        }

        /**
         * 从文件加载公共邮箱域名
         */
        private Set<String> loadCommonEmailDomains() {
            Set<String> commonEmailDomains = new HashSet<>();
            File file = new File("resources/free_email_provider_domains.txt");

            if (!file.exists()) {
                System.err.println("Can't find domains file.");
                System.out.println("Now path:" + new File(".").getAbsolutePath());
                return commonEmailDomains;
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

        /**
         * 检查邮箱是否是公共邮箱
         */
        private boolean isCommonEmail(String email, Set<String> commonEmailDomains) {
            String domain = email.substring(email.indexOf("@") + 1);
            return commonEmailDomains.contains(domain);
        }

        /**
         * 合并后的Git数据结构
         */
        public static class MergedGitData {
            public List<CommitData> commits = new ArrayList<>();
            public List<PRData> prData = new ArrayList<>();
            public List<IssueData> issueData = new ArrayList<>();
            public List<LocalDate> starData = new ArrayList<>();
            public List<LocalDate> forkData = new ArrayList<>();
        }

        /**
         * 更新CommitData类以包含更多信息
         */
        public static class CommitData {
            public String commitId;
            public String authorName;
            public String authorEmail;
            public LocalDate commitDate;
            public String message;
            public String repoPath;
            public int addedLines;
            public int deletedLines;
            public List<String> modifiedFiles = new ArrayList<>();
            
            public LocalDate getCommitDate() {
                return commitDate;
            }
        }

        /**
         * 更新PRData类以包含更多信息
         */
        public static class PRData {
            public String number;
            public LocalDate createdAt;
            public boolean merged;
            public boolean linkedToIssue;
            public int reviewCount;
            public String repoPath;
            public String authorName;
            public String authorEmail;
            public int changedFiles;
            public int addedLines;
            public int deletedLines;
            
            public LocalDate getCreatedAt() {
                return createdAt;
            }
        }

        /**
         * 更新IssueData类以包含更多信息
         */
        public static class IssueData {
            public String number;
            public LocalDate createdAt;
            public String repoPath;
            public String authorName;
            public String authorEmail;
            public String state;
            public List<String> labels = new ArrayList<>();
            
            public LocalDate getCreatedAt() {
                return createdAt;
            }
        }

    }
    /**
     * 仓库信息类
     */
    private static class RepoInfo {
        public String name;
        public int stargazers;
        public int rank;

        public RepoInfo() {}

        public RepoInfo(String name, int stargazers, int rank) {
            this.name = name;
            this.stargazers = stargazers;
            this.rank = rank;
        }
    }
}
        