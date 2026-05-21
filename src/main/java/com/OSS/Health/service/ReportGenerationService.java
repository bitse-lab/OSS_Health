package com.OSS.Health.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportGenerationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String pythonReportUrl = "http://localhost:8092";

    /**
     * 从文件生成报告（文件上传方式）
     */
    public Map<String, Object> generateReportFromFile(MultipartFile file) throws Exception {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("上传的文件为空");
            }
            
            // 读取文件内容预览
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            // 调用 Python 服务（文件上传方式）
            Map<String, Object> pythonReport = callPythonServiceWithFile(file);
            
            response.put("status", "success");
            response.put("message", "报告生成成功");
            response.put("filename", file.getOriginalFilename());
            response.put("file_size", file.getSize());
            response.put("content_preview", content.substring(0, Math.min(200, content.length())));
            response.put("report", pythonReport);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "文件处理失败: " + e.getMessage());
            throw e;
        }
        
        return response;
    }

    /**
     * 从文本内容生成报告（直接发送文本）
     * 用于 /test-data 接口
     */
    public Map<String, Object> generateReportFromText(String textContent) throws Exception {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (textContent == null || textContent.trim().isEmpty()) {
                throw new IllegalArgumentException("文本内容为空");
            }
            
            // 调用 Python 服务（文本方式）
            Map<String, Object> pythonReport = callPythonServiceWithText(textContent);
            
            response.put("status", "success");
            response.put("message", "报告生成成功");
            response.put("content_length", textContent.length());
            response.put("report", pythonReport);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "文本处理失败: " + e.getMessage());
            throw e;
        }
        
        return response;
    }
    
    /**
     * 调用 Python 服务 - 文件上传方式
     */
    private Map<String, Object> callPythonServiceWithFile(MultipartFile file) throws Exception {
        try {
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
            String url = pythonReportUrl + "/generate-report";
            
            ResponseEntity<Map> pythonResponse = restTemplate.postForEntity(url, request, Map.class);
            
            if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
                return pythonResponse.getBody();
            } else {
                throw new Exception("Python 服务返回错误");
            }
        } catch (Exception e) {
            System.err.println("调用 Python 服务失败: " + e.getMessage());
            return generateMockReport();
        }
    }
    
    /**
     * 调用 Python 服务 - 文本方式
     */
    private Map<String, Object> callPythonServiceWithText(String textContent) throws Exception {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", textContent);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            String url = pythonReportUrl + "/generate-report-text";
            
            ResponseEntity<Map> pythonResponse = restTemplate.postForEntity(url, request, Map.class);
            
            if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
                return pythonResponse.getBody();
            } else {
                throw new Exception("Python 服务返回错误");
            }
        } catch (Exception e) {
            System.err.println("调用 Python 服务失败: " + e.getMessage());
            return generateMockReport();
        }
    }
    
    /**
     * 生成模拟报告（当 Python 服务不可用时）
     */
    private Map<String, Object> generateMockReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("health_score", 85.5);
        report.put("activity_level", "高");
        report.put("note", "使用模拟数据（Python 服务不可用）");
        return report;
    }
}
