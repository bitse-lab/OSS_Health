package com.OSS.Health.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.OSS.Health.service.ReportGenerationService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 报告生成API控制器
 * 对应Python后端的 api_report.py
 */
@RestController
@CrossOrigin(origins = "http://localhost:8081")
@RequestMapping("/api/report")
public class ReportGenerate {

    @Autowired
    private ReportGenerationService reportService;

    @Value("${test.data.file.path:src/main/resources/input.txt}")
    private String testDataFilePath;

    /**
     * 测试1: 从本地文件生成报告
     * 对应Python的 /generate-report 接口
     * 
     * @param file 上传的文件（如 cann_total.txt）
     * @return 生成的报告内容
     */
    @PostMapping("/generate-from-file")
    public ResponseEntity<Map<String, Object>> generateReportFromFile(
            @RequestParam("file") MultipartFile file) {
        
        try {
            // 调用服务层生成报告
            Map<String, Object> result = reportService.generateReportFromFile(file);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "报告生成失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 测试2: 使用本地 input.txt 文件生成报告
     * 直接读取文件内容并发送给 Python 后端
     * 
     * @return 从本地文件读取的测试数据报告结果
     */
    @GetMapping("/test-data")
    public ResponseEntity<Map<String, Object>> getTestDataReport() {
        
        try {
            // 1. 读取本地 input.txt 文件
            Path filePath = Paths.get("resources/input.txt");
            File file = filePath.toFile();
            
            if (!file.exists()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "本地测试文件 input.txt 不存在");
                errorResponse.put("file_path", filePath.toAbsolutePath().toString());
                errorResponse.put("hint", "请将 input.txt 文件放在 resources/ 目录下");
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorResponse);
            }
            
            // 2. 读取文件内容（不解析，直接作为文本）
            String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
            
            // 3. 直接调用服务层，将文本内容发送给 Python 后端
            // Map<String, Object> result = reportService.generateReportFromText(fileContent);
            
            Path outputFilePath = Paths.get("resources/output.txt");
            File outputFile = outputFilePath.toFile();
            String outputContent = "";
            
            if (outputFile.exists()) {
                outputContent = Files.readString(outputFilePath, StandardCharsets.UTF_8);
            } else {
                outputContent = "output.txt 文件不存在";
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "报告生成成功");
            response.put("output_content", outputContent);
            response.put("output_file_info", Map.of(
                "file_name", "output.txt",
                "file_path", outputFilePath.toAbsolutePath().toString(),
                "file_exists", outputFile.exists(),
                "file_size_bytes", outputFile.exists() ? outputFile.length() : 0
            ));
            
            // 6. 添加测试标识和元数据（可选）
            response.put("test_mode", true);
            response.put("data_source", "input.txt");
            response.put("input_file_info", Map.of(
                "file_name", "input.txt",
                "file_size_bytes", fileContent.length(),
                "file_lines", fileContent.split("\n").length
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "测试数据报告生成失败: " + e.getMessage());
            errorResponse.put("error_type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 健康检查
     * 
     * @return 服务健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean serviceAvailable = reportService != null;
            
            // 检查本地测试文件是否存在
            Path filePath = Paths.get("resources/input.txt");
            File file = filePath.toFile();
            boolean testFileExists = file.exists();
            
            health.put("status", (serviceAvailable && testFileExists) ? "healthy" : "warning");
            health.put("service_available", serviceAvailable);
            health.put("test_file_exists", testFileExists);
            health.put("test_file_path", filePath.toAbsolutePath().toString());
            health.put("timestamp", java.time.LocalDateTime.now().toString());
            
            if (!testFileExists) {
                health.put("warning", "测试文件 input.txt 不存在，/test-data 接口将无法使用");
            }
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "error");
            health.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(health);
        }
    }
}
