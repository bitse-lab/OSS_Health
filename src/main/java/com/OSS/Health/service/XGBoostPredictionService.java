package com.OSS.Health.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * XGBoost预测服务
 * 对应Python后端的 api_xgboost.py (端口: 8091)
 */
@Service
public class XGBoostPredictionService {

    // Python服务地址（写死）
    private final String pythonXGBoostUrl = "http://localhost:8091";
    
    // RestTemplate实例
    private final RestTemplate restTemplate;
    
    // 模拟特征名称列表（作为备用）
    private static final List<String> FEATURE_NAMES = Arrays.asList(
        "code_changes", "issue_count", "commit_count", "pr_count",
        "org_commits", "org_entropy", "volunteer_entropy", "volunteer_commits",
        "review_ratio", "pr_merged_ratio", "pr_linked_ratio",
        "contributor_count", "star_count", "fork_count",
        "long_term_contributors_active"
    );

    public XGBoostPredictionService() {
        this.restTemplate = new RestTemplate();
        // 设置超时时间
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10秒连接超时
        factory.setReadTimeout(60000);     // 60秒读取超时
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * 从文件进行预测
     * 对应Python的 /predict/csv 接口
     */
    public Map<String, Object> predictFromFile(MultipartFile file) throws Exception {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                throw new IllegalArgumentException("上传的文件为空");
            }
            
            // 验证文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                throw new IllegalArgumentException("只支持CSV文件格式");
            }
            
            // 调用Python服务
            Map<String, Object> pythonResult = callPythonPredictCsv(file);
            
            response.put("status", "success");
            response.put("message", "CSV预测成功");
            response.put("filename", filename);
            response.putAll(pythonResult);
            
        } catch (Exception e) {
            System.err.println("调用Python XGBoost服务失败，使用本地模拟: " + e.getMessage());
            // 降级到本地处理
            return predictFromFileLocal(file);
        }
        
        return response;
    }

    /**
     * 从数据进行预测（单个样本）
     * 对应Python的 /predict 接口
     */
    public Map<String, Object> predictFromData(Map<String, Double> features) throws Exception {
        try {
            // 调用Python服务
            return callPythonPredict(features);
            
        } catch (Exception e) {
            System.err.println("调用Python XGBoost服务失败，使用本地模拟: " + e.getMessage());
            // 降级到本地处理
            return predictFromDataLocal(features);
        }
    }

    /**
     * 批量预测
     * 对应Python的 /predict/batch 接口
     */
    public Map<String, Object> predictBatch(List<Map<String, Double>> dataList) throws Exception {
        try {
            // 调用Python服务
            return callPythonPredictBatch(dataList);
            
        } catch (Exception e) {
            System.err.println("调用Python XGBoost服务失败，使用本地模拟: " + e.getMessage());
            // 降级到本地处理
            return predictBatchLocal(dataList);
        }
    }

    /**
     * 获取模型信息
     * 对应Python的 /model/info 接口
     */
    public Map<String, Object> getModelInfo() {
        try {
            // 调用Python服务
            String url = pythonXGBoostUrl + "/model/info";
            ResponseEntity<Map> pythonResponse = restTemplate.getForEntity(url, Map.class);
            
            if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
                return pythonResponse.getBody();
            }
        } catch (Exception e) {
            System.err.println("获取模型信息失败: " + e.getMessage());
        }
        
        // 返回模拟数据
        return getModelInfoLocal();
    }

    /**
     * 获取特征列表
     */
    public Map<String, Object> getFeatures() {
        try {
            // 调用Python服务
            String url = pythonXGBoostUrl + "/model/features";
            ResponseEntity<Map> pythonResponse = restTemplate.getForEntity(url, Map.class);
            
            if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
                return pythonResponse.getBody();
            }
        } catch (Exception e) {
            System.err.println("获取特征列表失败: " + e.getMessage());
        }
        
        // 返回本地特征列表
        Map<String, Object> result = new HashMap<>();
        result.put("features", FEATURE_NAMES);
        result.put("count", FEATURE_NAMES.size());
        return result;
    }

    /**
     * 检查模型是否已加载
     */
    public boolean isModelLoaded() {
        try {
            String url = pythonXGBoostUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("model_loaded"));
            }
        } catch (Exception e) {
            System.err.println("检查模型状态失败: " + e.getMessage());
        }
        return false;
    }

    // ==================== Python服务调用方法 ====================

    /**
     * 调用Python的 /predict/csv 接口
     */
    private Map<String, Object> callPythonPredictCsv(MultipartFile file) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        body.add("file", fileResource);
        
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        String url = pythonXGBoostUrl + "/predict/csv";
        
        ResponseEntity<Map> pythonResponse = restTemplate.postForEntity(url, request, Map.class);
        
        if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
            return pythonResponse.getBody();
        } else {
            throw new Exception("Python服务返回错误状态: " + pythonResponse.getStatusCode());
        }
    }

    /**
     * 调用Python的 /predict 接口
     */
    private Map<String, Object> callPythonPredict(Map<String, Double> features) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("features", features);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url = pythonXGBoostUrl + "/predict";
        
        ResponseEntity<Map> pythonResponse = restTemplate.postForEntity(url, request, Map.class);
        
        if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
            return pythonResponse.getBody();
        } else {
            throw new Exception("Python服务返回错误状态: " + pythonResponse.getStatusCode());
        }
    }

    /**
     * 调用Python的 /predict/batch 接口
     */
    private Map<String, Object> callPythonPredictBatch(List<Map<String, Double>> dataList) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", dataList);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url = pythonXGBoostUrl + "/predict/batch";
        
        ResponseEntity<Map> pythonResponse = restTemplate.postForEntity(url, request, Map.class);
        
        if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
            return pythonResponse.getBody();
        } else {
            throw new Exception("Python服务返回错误状态: " + pythonResponse.getStatusCode());
        }
    }

    // ==================== 本地降级处理方法（保留原有逻辑）====================

    /**
     * 本地CSV预测（降级方案）
     */
    private Map<String, Object> predictFromFileLocal(MultipartFile file) throws Exception {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Map<String, Double>> dataList = parseCsvFile(file);
            
            if (dataList.isEmpty()) {
                throw new IllegalArgumentException("CSV文件中没有有效数据");
            }
            
            List<Map<String, Object>> predictions = new ArrayList<>();
            List<Map<String, Object>> samplesWithMissingFeatures = new ArrayList<>();
            
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Double> features = dataList.get(i);
                ValidationResult validation = validateAndFillFeatures(features);
                Map<String, Object> prediction = makePrediction(validation.features);
                prediction.put("row_index", i);
                
                if (!validation.missingFeatures.isEmpty()) {
                    prediction.put("missing_features", validation.missingFeatures);
                    Map<String, Object> missingInfo = new HashMap<>();
                    missingInfo.put("row_index", i);
                    missingInfo.put("missing_features", validation.missingFeatures);
                    samplesWithMissingFeatures.add(missingInfo);
                }
                
                predictions.add(prediction);
            }
            
            response.put("predictions", predictions);
            response.put("total_count", predictions.size());
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("note", "使用本地模拟预测");
            
            if (!samplesWithMissingFeatures.isEmpty()) {
                response.put("warning", "Some rows had missing features filled with 0");
                response.put("samples_with_missing_features", samplesWithMissingFeatures);
            }
            
            response.put("status", "success");
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "CSV预测失败: " + e.getMessage());
            throw e;
        }
        
        return response;
    }

    /**
     * 本地单样本预测（降级方案）
     */
    private Map<String, Object> predictFromDataLocal(Map<String, Double> features) throws Exception {
        try {
            ValidationResult validation = validateAndFillFeatures(features);
            Map<String, Object> result = makePrediction(validation.features);
            result.put("note", "使用本地模拟预测");
            
            if (!validation.missingFeatures.isEmpty()) {
                result.put("warning", "Missing features filled with 0: " + validation.missingFeatures);
                result.put("missing_features", validation.missingFeatures);
            }
            
            return result;
            
        } catch (Exception e) {
            throw new Exception("预测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 本地批量预测（降级方案）
     */
    private Map<String, Object> predictBatchLocal(List<Map<String, Double>> dataList) throws Exception {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Map<String, Object>> predictions = new ArrayList<>();
            List<Map<String, Object>> samplesWithMissingFeatures = new ArrayList<>();
            
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Double> features = dataList.get(i);
                ValidationResult validation = validateAndFillFeatures(features);
                Map<String, Object> prediction = makePrediction(validation.features);
                prediction.put("sample_index", i);
                
                if (!validation.missingFeatures.isEmpty()) {
                    prediction.put("missing_features", validation.missingFeatures);
                    Map<String, Object> missingInfo = new HashMap<>();
                    missingInfo.put("sample_index", i);
                    missingInfo.put("missing_features", validation.missingFeatures);
                    samplesWithMissingFeatures.add(missingInfo);
                }
                
                predictions.add(prediction);
            }
            
            response.put("predictions", predictions);
            response.put("total_count", predictions.size());
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("note", "使用本地模拟预测");
            
            if (!samplesWithMissingFeatures.isEmpty()) {
                response.put("warning", "Some samples had missing features filled with 0");
                response.put("samples_with_missing_features", samplesWithMissingFeatures);
            }
            
            response.put("status", "success");
            
        } catch (Exception e) {
            throw new Exception("批量预测失败: " + e.getMessage(), e);
        }
        
        return response;
    }

    /**
     * 本地模型信息（降级方案）
     */
    private Map<String, Object> getModelInfoLocal() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("model_type", "XGBoost (Local Mock)");
        info.put("timestamp", LocalDateTime.now().toString());
        info.put("n_features", FEATURE_NAMES.size());
        info.put("feature_names", FEATURE_NAMES);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("accuracy", 0.85);
        metrics.put("precision", 0.83);
        metrics.put("recall", 0.87);
        metrics.put("f1_score", 0.85);
        info.put("metrics", metrics);
        
        Map<String, Object> hyperparameters = new HashMap<>();
        hyperparameters.put("max_depth", 6);
        hyperparameters.put("learning_rate", 0.1);
        hyperparameters.put("n_estimators", 100);
        info.put("hyperparameters", hyperparameters);
        
        return info;
    }

    // ==================== 私有辅助方法 ====================

    private ValidationResult validateAndFillFeatures(Map<String, Double> features) {
        Map<String, Double> filledFeatures = new HashMap<>();
        List<String> missingFeatures = new ArrayList<>();
        
        Set<String> extraFeatures = new HashSet<>(features.keySet());
        extraFeatures.removeAll(FEATURE_NAMES);
        if (!extraFeatures.isEmpty()) {
            throw new IllegalArgumentException("Unknown features: " + extraFeatures);
        }
        
        for (String featureName : FEATURE_NAMES) {
            if (features.containsKey(featureName)) {
                filledFeatures.put(featureName, features.get(featureName));
            } else {
                filledFeatures.put(featureName, 0.0);
                missingFeatures.add(featureName);
            }
        }
        
        return new ValidationResult(filledFeatures, missingFeatures);
    }

    private Map<String, Object> makePrediction(Map<String, Double> features) {
        double score = calculateHealthScore(features);
        int prediction = score > 0.5 ? 1 : 0;
        double probability = score;
        double confidence = Math.abs(probability - 0.5) * 2;
        String label = prediction == 1 ? "Survived" : "Not Survived";
        
        Map<String, Object> result = new HashMap<>();
        result.put("prediction", prediction);
        result.put("probability", Math.round(probability * 10000.0) / 10000.0);
        result.put("confidence", Math.round(confidence * 10000.0) / 10000.0);
        result.put("label", label);
        
        return result;
    }

    private double calculateHealthScore(Map<String, Double> features) {
        double score = 0.0;
        
        score += Math.min(features.getOrDefault("star_count", 0.0) / 100.0, 0.2);
        score += Math.min(features.getOrDefault("fork_count", 0.0) / 50.0, 0.15);
        score += Math.min(features.getOrDefault("commit_count", 0.0) / 200.0, 0.2);
        score += Math.min(features.getOrDefault("contributor_count", 0.0) / 20.0, 0.15);
        score += Math.min(features.getOrDefault("pr_merged_ratio", 0.0), 0.15);
        score += Math.min(features.getOrDefault("review_ratio", 0.0), 0.15);
        
        return Math.min(score, 1.0);
    }

    private List<Map<String, Double>> parseCsvFile(MultipartFile file) throws Exception {
        List<Map<String, Double>> dataList = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV文件为空");
            }
            
            String[] headers = headerLine.split(",");
            List<String> headerList = Arrays.stream(headers)
                    .map(String::trim)
                    .collect(Collectors.toList());
            
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                String[] values = line.split(",");
                if (values.length != headers.length) {
                    System.err.println("Warning: Row " + rowNum + " has " + values.length + 
                            " values but expected " + headers.length);
                    continue;
                }
                
                Map<String, Double> rowData = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String header = headerList.get(i);
                    String value = values[i].trim();
                    
                    try {
                        if (!value.isEmpty()) {
                            rowData.put(header, Double.parseDouble(value));
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid number at row " + rowNum + 
                                ", column '" + header + "': " + value);
                    }
                }
                
                if (!rowData.isEmpty()) {
                    dataList.add(rowData);
                }
            }
        }
        
        return dataList;
    }

    private static class ValidationResult {
        Map<String, Double> features;
        List<String> missingFeatures;
        
        ValidationResult(Map<String, Double> features, List<String> missingFeatures) {
            this.features = features;
            this.missingFeatures = missingFeatures;
        }
    }
}
