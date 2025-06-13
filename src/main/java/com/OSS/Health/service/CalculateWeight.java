package com.OSS.Health.service;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.mapper.MysqlWeightMapper;
import com.OSS.Health.model.MysqlWeightModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalculateWeight {

    private static final String SAMPLE_REPO_JSON = "D:/Plateform/Git/repositories/OSS_Health/resources/sampleRep_deep-learning_1_new.json";
    private static final String CLONE_PATH = "E:/GithubRep";

    @Autowired
    private MysqlDataMapper mysqlDataMapper;

    @Autowired
    private MysqlWeightMapper mysqlWeightMapper;

    private static final Set<String> POSITIVE_INDICATORS = Set.of(
        "1.2.4",
        "1.3.1", "1.3.2", "1.3.3", "1.3.4",
        "2.1.1", "2.1.2", "2.1.3", "2.1.4",
        "2.2.1", "2.2.2", "2.2.3",
        "2.3.1",
        "3.2.1", "3.2.2"
    );

    private static final Set<String> NEGATIVE_INDICATORS = Set.of(
        "1.1.1", "1.1.2", "1.1.3", "1.1.4",
        "1.2.1", "1.2.2", "1.2.3"
    );

    public boolean calculateWeight() throws Exception {
        Set<String> allIds = new HashSet<>();
        allIds.addAll(POSITIVE_INDICATORS);
        allIds.addAll(NEGATIVE_INDICATORS);

        Map<String, List<Double>> dataMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        List<RepoInfo> repos = mapper.readValue(new File(SAMPLE_REPO_JSON), new TypeReference<List<RepoInfo>>() {});

        for (String id : allIds) {
            List<Double> values = new ArrayList<>();
            
//            for(int i = 0; i < Math.min(50, repos.size()); i++) {
//            	RepoInfo repo = repos.get(i);
            for (RepoInfo repo : repos) {
                if (!repo.name.contains("/")) {
                    System.out.println("Invalid repository name: " + repo.name);
                    continue;
                }

                String repoNameOnly = repo.name.substring(repo.name.lastIndexOf("/") + 1);
                String repoOwnerOnly = repo.name.substring(0, repo.name.lastIndexOf("/"));
                String tableName = repoOwnerOnly + "_" + generateSafeTableName(repoOwnerOnly, repoNameOnly);

                // 获取本地git库路径
                String repoPath = CLONE_PATH + File.separator + repoNameOnly;

                // 用 JGit 获取第一个和最后一个 commit 时间（Date 类型）
                Date[] firstLast = getFirstAndLastCommitDate(repoPath);
                if (firstLast == null || firstLast[0] == null || firstLast[1] == null) {
                    System.out.println("Failed to get commit times for repo: " + repoNameOnly);
                    continue;
                }
                Date startDate = firstLast[0];
                Date endDate = firstLast[1];

                // 查询数据并过滤时间范围
                List<Map<String, Object>> records = mysqlDataMapper.getMysqlDataModelNoS1_new(tableName, id);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                ZoneId zone = ZoneId.systemDefault();

                List<Double> filteredValues = records.stream()
                    .filter(record -> {
                        Object timeObj = record.get("time");
                        if (timeObj == null) return false;
                        Date recordDate = null;
                        try {
                            if (timeObj instanceof Date) {
                                recordDate = (Date) timeObj;
                            } else if (timeObj instanceof String) {
                                LocalDate localDate = LocalDate.parse((String) timeObj, formatter);
                                recordDate = Date.from(localDate.atStartOfDay(zone).toInstant());
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            System.out.println("Date parse error: " + e.getMessage());
                            return false;
                        }
                        return !recordDate.before(startDate) && !recordDate.after(endDate);
                    })
                    .map(record -> ((Number) record.get("number")).doubleValue())
                    .collect(Collectors.toList());

                values.addAll(filteredValues);
            }

            dataMap.put(id, values);
        }

        // 标准化
        Map<String, List<Double>> normalizedData = new HashMap<>();
        for (String id : allIds) {
            List<Double> values = dataMap.get(id);
            if (values == null || values.size() <= 1) {
                normalizedData.put(id, List.of());
                continue;
            }

            double min = Collections.min(values);
            double max = Collections.max(values);

            if (Math.abs(max - min) < 1e-8) {
                normalizedData.put(id, List.of());
                continue;
            }

            List<Double> normList = values.stream().map(val -> {
                if (POSITIVE_INDICATORS.contains(id)) {
                    return (val - min) / (max - min);
                } else {
                    return (max - val) / (max - min);
                }
            }).collect(Collectors.toList());

            normalizedData.put(id, normList);
        }

        // 熵值计算
        Map<String, Double> entropyMap = new HashMap<>();
        for (String id : allIds) {
            List<Double> normVals = normalizedData.get(id);
            if (normVals == null || normVals.size() <= 1) {
                entropyMap.put(id, 1.0);
                continue;
            }

            int n = normVals.size();
            double k = 1.0 / Math.log(n);
            double sum = normVals.stream().mapToDouble(Double::doubleValue).sum();

            if (sum == 0.0) {
                entropyMap.put(id, 1.0);
                continue;
            }

            double entropy = 0.0;
            for (double val : normVals) {
                double pij = val / sum;
                if (pij > 0) {
                    entropy -= pij * Math.log(pij);
                }
            }
            entropy *= k;
            entropyMap.put(id, entropy);
        }

        // 权重计算
        Map<String, Double> weightMap = new HashMap<>();
        double totalDivers = 0.0;
        for (String id : allIds) {
            double d = 1.0 - entropyMap.getOrDefault(id, 1.0);
            weightMap.put(id, d);
            totalDivers += d;
        }

        Map<String, Double> normalizedWeight = new HashMap<>();
        for (String id : allIds) {
            double norm = (totalDivers == 0.0) ? 0.0 : weightMap.get(id) / totalDivers;
            normalizedWeight.put(id, norm);
        }

        // 存储到数据库
        mysqlWeightMapper.clearMysqlWeightAll();
        for (Map.Entry<String, Double> entry : normalizedWeight.entrySet()) {
            MysqlWeightModel model = new MysqlWeightModel();
            model.setId(entry.getKey());
            model.setWeight(entry.getValue());
            mysqlWeightMapper.insertMysqlWeight(model);
        }

        return true;
    }

    // JGit 获取第一个和最后一个 commit 的 Date 数组： [firstCommitDate, lastCommitDate]
    private Date[] getFirstAndLastCommitDate(String repoPath) {
        try (Git git = Git.open(new File(repoPath))) {
            Iterable<RevCommit> commits = git.log().call();

            Date firstCommitDate = null;
            Date lastCommitDate = null;

            for (RevCommit commit : commits) {
                Date commitDate = commit.getAuthorIdent().getWhen();
                if (lastCommitDate == null || commitDate.after(lastCommitDate)) {
                    lastCommitDate = commitDate;
                }
                if (firstCommitDate == null || commitDate.before(firstCommitDate)) {
                    firstCommitDate = commitDate;
                }
            }
            return new Date[] {firstCommitDate, lastCommitDate};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 生成安全表名，防止超长
    public static String generateSafeTableName(String owner, String name) {
        String prefix = owner + "_";
        int maxLength = 64;
        int allowedNameLength = maxLength - prefix.length();
        if (name.length() <= allowedNameLength) {
            return name;
        }
        return name.substring(0, allowedNameLength);
    }

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
