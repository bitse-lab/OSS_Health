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
	private static final String SAMPLE_REPO_JSON= "D:/Plateform/Git/repositories/OSS_Health/resources/sampleRep_deep-learning_1_new.json";
	private static final String CLONE_PATH= "E:/GithubRep";

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
    
    public boolean multiGitClone() {
        try {
            // 解析 JSON
            ObjectMapper mapper = new ObjectMapper();
            List<RepoInfo> repos = mapper.readValue(new File(SAMPLE_REPO_JSON), new TypeReference<List<RepoInfo>>() {});

            for (RepoInfo repo : repos) {
            	String repoNameOnly = repo.name.substring(repo.name.lastIndexOf("/") + 1);
            	
                String sshUrl = "git@github.com:" + repo.name + ".git";
                String localPath = CLONE_PATH + "/" + repoNameOnly;

                File repoDir = new File(localPath);
                if (repoDir.exists()) {
                    System.out.println("Repository already exists, skipping: " + localPath);
                    continue;
                }

                System.out.println("Cloning: " + sshUrl + " into " + localPath);
                ProcessBuilder builder = new ProcessBuilder("git", "clone", sshUrl, localPath);
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
