package com.OSS.Health.service;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.mapper.MysqlWeightMapper;
import com.OSS.Health.model.MysqlDataModel;
import com.OSS.Health.model.MysqlWeightModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalculateWeight {
	private static final String SAMPLE_REPO_JSON= "D:/Plateform/Git/repositories/OSS_Health/resources/sampleRep_deep-learning_1_new.json";

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

            for (RepoInfo repo : repos) {
                if (!repo.name.contains("/")) {
                    System.out.println("Invalid repository name: " + repo.name);
                    continue;
                }

                String repoNameOnly = repo.name.substring(repo.name.lastIndexOf("/") + 1);
                String repoOwnerOnly = repo.name.substring(0, repo.name.lastIndexOf("/"));
                String tableName = repoOwnerOnly + "_" + generateSafeTableName(repoOwnerOnly, repoNameOnly);

                List<Map<String, Object>> records = mysqlDataMapper.getMysqlDataModelNoS1_new(tableName, id);
                List<Double> recordValues = records.stream()
                        .map(record -> ((Number) record.get("number")).doubleValue())
                        .collect(Collectors.toList());

                values.addAll(recordValues);
            }

            dataMap.put(id, values);
        }

        // 标准化
        Map<String, List<Double>> normalizedData = new HashMap<>();
        for (String id : allIds) {
            List<Double> values = dataMap.get(id);
            if (values == null || values.size() <= 1) {
                normalizedData.put(id, List.of()); // 数据不足
                continue;
            }

            double min = Collections.min(values);
            double max = Collections.max(values);

            // 如果 max == min，标准化后将缺乏差异性，熵设最大
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
                entropyMap.put(id, 1.0); // 熵最大，权重最小
                continue;
            }

            int n = normVals.size();
            double k = 1.0 / Math.log(n);
            double sum = normVals.stream().mapToDouble(Double::doubleValue).sum();

            if (sum == 0.0) {
                entropyMap.put(id, 1.0); // 全 0 情况，信息缺失
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
    
    // 获得安全的表名，防止超出64字符
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
        
        // 添加无参构造函数
        public RepoInfo() {}

        public RepoInfo(String name, int stargazers, int rank) {
            this.name = name;
            this.stargazers = stargazers;
            this.rank = rank;
        }
    }
}

