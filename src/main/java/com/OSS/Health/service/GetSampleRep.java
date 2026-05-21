package com.OSS.Health.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class GetSampleRep {
	private static final String IN_FILE= "resources/topic_deep-learning_1.json";
	private static final String OUT_FILE= "resources/sampleRep_deep-learning_1.json";
	private static final String SAMPLE_REPO_JSON= "D:/Plateform/Git/repositories/OSS_Health_gitcode/resources/openharmony_repos.json";
	private static final String CLONE_PATH= "E:/GithubRep2";

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
    
    
    public boolean getNextRandomRep(int starNum) {
        ObjectMapper mapper = new ObjectMapper();
        File inputFile = new File(IN_FILE);

        try {
            JsonNode rootNode = mapper.readTree(inputFile);
            JsonNode itemsNode = rootNode.get("items");

            if (itemsNode == null || !itemsNode.isArray()) {
                System.err.println("Invalid format: 'items' not found or not array.");
                return false;
            }

            List<JsonNode> projects = new ArrayList<>();
            itemsNode.forEach(projects::add);

            // 按 stargazers 降序排序并添加排名
            projects.sort((a, b) -> Integer.compare(
                    b.path("stargazers").asInt(),
                    a.path("stargazers").asInt())
            );

            List<RepoInfo> rankedList = new ArrayList<>();
            for (int i = 0; i < projects.size(); i++) {
                JsonNode node = projects.get(i);
                String name = node.path("name").asText();
                int stars = node.path("stargazers").asInt();
                rankedList.add(new RepoInfo(name, stars, i + 1));
            }

            // 根据 starNum 查找对应 repo 的排名
            Optional<RepoInfo> match = rankedList.stream()
                    .filter(repo -> repo.stargazers == starNum)
                    .findFirst();

            if (match.isEmpty()) {
                System.err.println("No repository with stargazers == " + starNum);
                return false;
            }

            int rank = match.get().rank;
            int total = rankedList.size();
            int layerSize = total / 100;
            int layerIndex = (rank - 1) / layerSize;

            int start = layerIndex * layerSize;
            int end = (layerIndex == 99) ? total : start + layerSize;
            if (start >= end) {
                System.err.println("Invalid layer boundaries.");
                return false;
            }

            List<RepoInfo> layer = rankedList.subList(start, end);
            Random random = new Random();
            RepoInfo newSample = layer.get(random.nextInt(layer.size()));

            // 输出为 JSON 格式
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", newSample.name);
            result.put("stargazers", newSample.stargazers);
            result.put("rank", newSample.rank);

            String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            System.out.println(jsonOutput);
            
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean storeSampleRep() {
        ObjectMapper mapper = new ObjectMapper();
        File inputFile = new File(IN_FILE);
        File outputFile = new File(OUT_FILE);

        try {
            // 读取并提取 items 列表
            JsonNode rootNode = mapper.readTree(inputFile);
            JsonNode itemsNode = rootNode.get("items");

            if (itemsNode == null || !itemsNode.isArray()) {
                System.err.println("Invalid format: 'items' not found or not array.");
                return false;
            }

            List<JsonNode> projects = new ArrayList<>();
            itemsNode.forEach(projects::add);

            // 按 stargazers 降序排序并加排名
            projects.sort((a, b) -> Integer.compare(
                    b.path("stargazers").asInt(),
                    a.path("stargazers").asInt())
            );

            List<RepoInfo> rankedList = new ArrayList<>();
            for (int i = 0; i < projects.size(); i++) {
                JsonNode node = projects.get(i);
                String name = node.path("name").asText();
                int stars = node.path("stargazers").asInt();
                rankedList.add(new RepoInfo(name, stars, i + 1));
            }

            // 分层采样：每 1% 层中随机抽一个
            int total = rankedList.size();
            int layerSize = total / 100;
            List<RepoInfo> sampled = new ArrayList<>();
            Random random = new Random();

            for (int i = 0; i < 100; i++) {
                int start = i * layerSize;
                int end = (i == 99) ? total : start + layerSize;
                if (start >= end) continue;
                List<RepoInfo> layer = rankedList.subList(start, end);
                RepoInfo randomPick = layer.get(random.nextInt(layer.size()));
                sampled.add(randomPick);
            }

            // 写出结果
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, sampled);
            System.out.println("Sampled data saved to " + outputFile.getPath());
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean multiGitClone_gitcode() {
        int successCount = 0;
        int failCount = 0;
        List<String> failedRepos = new ArrayList<>();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<RepoInfo> repos = mapper.readValue(
                new File(SAMPLE_REPO_JSON), 
                new TypeReference<List<RepoInfo>>() {}
            );

            for (RepoInfo repo : repos) {
                String repoNameOnly = repo.name.substring(repo.name.lastIndexOf("/") + 1);
                
                // 关键修改：将SSH URL改为HTTPS URL
                // 原SSH格式：git@gitcode.com:OpenHarmony/xxx.git
                // 新HTTPS格式：https://gitcode.com/OpenHarmony/xxx.git
                String httpsUrl = "https://gitcode.com/" + repo.name + ".git";
                String localPath = CLONE_PATH + "/" + repoNameOnly;

                File repoDir = new File(localPath);
                if (repoDir.exists()) {
                    System.out.println("Repository already exists, skipping: " + localPath);
                    successCount++; // 已存在的仓库算作成功
                    continue;
                }

                System.out.println("Cloning: " + httpsUrl + " into " + localPath);
                
                // 使用HTTPS克隆，无需任何SSH配置
                ProcessBuilder builder = new ProcessBuilder("git", "clone", httpsUrl, localPath);
                builder.redirectErrorStream(true);
                
                try {
                    Process process = builder.start();
                    
                    // 读取输出
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }
                    }

                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        System.out.println("Git clone failed for " + repo.name + " with exit code " + exitCode);
                        failCount++;
                        failedRepos.add(repo.name);
                        continue; // 继续下一个仓库，不中断循环
                    } else {
                        System.out.println("Successfully cloned: " + repo.name);
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    System.err.println("Exception while cloning " + repo.name + ": " + e.getMessage());
                    failCount++;
                    failedRepos.add(repo.name);
                    continue; // 继续下一个仓库
                }
            }
            
            // 打印最终统计信息
            System.out.println("\n========== 克隆任务完成统计 ==========");
            System.out.println("总计处理仓库: " + (successCount + failCount));
            System.out.println("成功克隆: " + successCount);
            System.out.println("失败克隆: " + failCount);
            
            if (!failedRepos.isEmpty()) {
                System.out.println("\n失败的仓库列表:");
                for (String failedRepo : failedRepos) {
                    System.out.println("  - " + failedRepo);
                }
            }
            System.out.println("=======================================");
            
            return failCount == 0; // 如果没有失败，返回true，否则返回false
            
        } catch (Exception e) {
            System.err.println("Fatal error in multiGitClone_gitcode: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean multiGitClone() {
        try {
            // 解析 JSON
            ObjectMapper mapper = new ObjectMapper();
            List<RepoInfo> repos = mapper.readValue(new File(SAMPLE_REPO_JSON), new TypeReference<List<RepoInfo>>() {});

            for (RepoInfo repo : repos) {
            	String repoNameOnly = repo.name.substring(repo.name.lastIndexOf("/") + 1);
            	
                String sshUrl = "git@gitcode.com:" + repo.name + ".git";
                String localPath = CLONE_PATH + "/" + repoNameOnly;

                File repoDir = new File(localPath);
                if (repoDir.exists()) {
                    System.out.println("Repository already exists, skipping: " + localPath);
                    continue;
                }

                System.out.println("Cloning: " + sshUrl + " into " + localPath);
                ProcessBuilder builder = new ProcessBuilder("git", "clone", sshUrl, localPath);
                
                // 设置环境变量以跳过主机密钥检查
                builder.environment().put("GIT_SSH_COMMAND", 
                    "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no");
                
                builder.redirectErrorStream(true);
                Process process = builder.start();
                
                // 捕获并输出进程的输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.out.println("Git clone failed for " + repo.name + " with exit code " + exitCode);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
