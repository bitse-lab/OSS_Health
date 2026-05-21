package com.OSS.Health.controller;

import com.OSS.Health.service.XGBoostPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * XGBoost预测API控制器
 * 对应Python后端的 api_xgboost.py
 */
@RestController
@CrossOrigin(origins = "http://localhost:8081")
@RequestMapping("/api/xgboost")
public class PredictXgboost {

    @Autowired
    private XGBoostPredictionService predictionService;

    /**
     * 测试1: 从本地文件进行预测
     * 对应Python的 /predict/csv 接口
     * 
     * @param file 上传的CSV文件
     * @return 预测结果
     */
    @PostMapping("/predict-from-file")
    public ResponseEntity<Map<String, Object>> predictFromFile(
            @RequestParam("file") MultipartFile file) {
        
        try {
            // 调用服务层进行预测
            Map<String, Object> result = predictionService.predictFromFile(file);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "预测失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 测试2: 使用固定测试数据进行预测
     * 对应Python的 /test 接口
     * 
     * @return 固定测试数据的预测结果
     */
    @GetMapping("/test-prediction")
    public ResponseEntity<Map<String, Object>> getTestPrediction() {
        
        try {
            // 构建测试数据
            Map<String, Object> testPayload = new HashMap<>();
            Map<String, Double> features = new HashMap<>();
            
            features.put("code_changes", 131571.0);
            features.put("issue_count", 7.0);
            features.put("commit_count", 153.0);
            features.put("pr_count", 16.0);
            features.put("org_commits", 52.0);
            features.put("org_entropy", 0.139232999);
            features.put("volunteer_entropy", 2.67517743);
            features.put("volunteer_commits", 101.0);
            features.put("review_ratio", 0.75);
            features.put("pr_merged_ratio", 0.625);
            features.put("pr_linked_ratio", 0.0);
            features.put("contributor_count", 12.0);
            features.put("star_count", 32.0);
            features.put("fork_count", 14.0);
            features.put("long_term_contributors_active", 0.0);
            
            testPayload.put("features", features);
            
            // 调用服务层进行预测
            Map<String, Object> predictionResult = predictionService.predictFromData(features);
            
            // 构建完整响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test endpoint - Fixed test data");
            response.put("test_data", testPayload);
            response.put("prediction_result", predictionResult);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // 添加模型信息
            Map<String, Object> modelInfo = new HashMap<>();
            modelInfo.put("model_loaded", true);
            modelInfo.put("feature_count", features.size());
            response.put("model_info", modelInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "测试预测失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 单个样本预测
     * 对应Python的 /predict 接口
     * 
     * @param requestBody 包含特征数据的请求体
     * @return 预测结果
     */
    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predict(
            @RequestBody Map<String, Object> requestBody) {
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Double> features = (Map<String, Double>) requestBody.get("features");
            
            if (features == null || features.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "特征数据不能为空");
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(errorResponse);
            }
            
            // 调用服务层进行预测
            Map<String, Object> result = predictionService.predictFromData(features);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "预测失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 获取模型信息
     * 对应Python的 /model/info 接口
     * 
     * @return 模型信息
     */
    @GetMapping("/model-info")
    public ResponseEntity<Map<String, Object>> getModelInfo() {
        
        try {
            Map<String, Object> modelInfo = predictionService.getModelInfo();
            return ResponseEntity.ok(modelInfo);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "获取模型信息失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 健康检查
     * 对应Python的 /health 接口
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean modelLoaded = predictionService.isModelLoaded();
            
            health.put("status", modelLoaded ? "healthy" : "unhealthy");
            health.put("model_loaded", modelLoaded);
            health.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "error");
            health.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(health);
        }
    }
}
