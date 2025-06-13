package com.OSS.Health.controller;

import com.OSS.Health.mapper.MysqlDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8081")  // 允许来自 localhost:8081 的跨域请求
public class AllMetrics {
	@Autowired
    private MysqlDataMapper mysqlDataMapper;
	
	@GetMapping("/api/repo/metric")
	public List<Map<String, Object>> getMysqlData( @RequestParam String repoOwner, @RequestParam String repoName, @RequestParam String id) {
	    return mysqlDataMapper.getMysqlDataModelNoS1_new(repoOwner + "_" + generateSafeTableName(repoOwner, repoName), id);
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
}
