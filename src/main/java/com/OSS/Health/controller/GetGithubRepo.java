package com.OSS.Health.controller;

import com.OSS.Health.service.GetGithubApi_new;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8081")
@RequestMapping("/api")
public class GetGithubRepo {

    @Value("${github.repo.path:./repos}")
    private String repoPath;

    @Value("${github.file.path:./data}")
    private String filePath;

    /**
     * 获取指定 GitHub 仓库的数据
     * @param owner 仓库所有者
     * @param repoName 仓库名称
     * @return 处理结果
     */
    @GetMapping("/github/repo")
    public ResponseEntity<Map<String, Object>> getGithubRepoData(
            @RequestParam("owner") String owner,
            @RequestParam("repoName") String repoName) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 参数校验
            if (owner == null || owner.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "owner 参数不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (repoName == null || repoName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "repoName 参数不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 创建 GetGithubApi_new 实例并获取数据
            GetGithubApi_new githubApi = new GetGithubApi_new(repoPath, filePath);
            boolean success = githubApi.storeSingleGithubApi(owner, repoName);
            
            if (success) {
                response.put("success", true);
                response.put("message", "成功获取仓库数据");
                response.put("owner", owner);
                response.put("repoName", repoName);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "获取仓库数据失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "处理请求时发生异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
