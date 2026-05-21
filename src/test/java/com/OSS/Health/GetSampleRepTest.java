package com.OSS.Health;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OSS.Health.service.GetSampleRep;

@SpringBootTest
public class GetSampleRepTest {
	@Autowired
    private GetSampleRep test;
	
    @Test
    public void testMultiGitClone() {
    	// 从CSV文件读取仓库列表
        String csvFilePath = "F://df_intersection.csv";
        List<String> repoList = readRepoListFromCSV(csvFilePath, "name_with_owner");
        
        if (repoList.isEmpty()) {
            System.err.println("错误: 未能从CSV文件读取到任何仓库");
            return;
        }
        
        System.out.println("成功读取 " + repoList.size() + " 个仓库");
        
        String outputDir = "F://github_repos";
        
        // 执行克隆
        boolean result = test.multiGitClone_new(repoList, outputDir);
        
        if (result) {
            System.out.println("测试成功完成");
        } else {
            System.out.println("测试失败：发生严重错误");
        }
    }
    
    private List<String> readRepoListFromCSV(String csvFilePath, String columnName) {
        List<String> repoList = new ArrayList<>();
        
        try (FileReader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, 
                 CSVFormat.Builder.create()
                     .setHeader()  // 自动从第一行读取header
                     .setSkipHeaderRecord(true)  // 跳过header行
                     .setTrim(true)  // 自动trim空格
                     .build())) {
            
            for (CSVRecord record : csvParser) {
                String value = record.get(columnName);
                if (value != null && !value.isEmpty()) {
                    repoList.add(value);
                }
            }
            
            System.out.println("从CSV文件读取了 " + repoList.size() + " 个仓库");
            
        } catch (IOException e) {
            System.err.println("读取CSV文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("错误: 未找到列 '" + columnName + "'");
            e.printStackTrace();
        }
        
        return repoList;
    }
}
