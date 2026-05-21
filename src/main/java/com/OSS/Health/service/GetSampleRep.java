package com.OSS.Health.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.*;

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
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER1 = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    
    /**
     * 批量克隆GitHub仓库（4线程版本）
     * 
     * @param repoList 仓库列表，格式为 "owner/repo"
     * @param outputDir 输出目录路径
     * @return 只有发生严重错误时返回false，否则返回true
     */
    public boolean multiGitClone_new(List<String> repoList, String outputDir) {
        // 参数验证
        if (repoList == null || repoList.isEmpty()) {
            System.err.println("仓库列表为空");
            return false;
        }
        
        if (outputDir == null || outputDir.trim().isEmpty()) {
            System.err.println("输出目录为空");
            return false;
        }
        
        // 随机打乱仓库列表（创建副本以避免修改原列表）
        List<String> shuffledRepoList = new ArrayList<>(repoList);
        Collections.shuffle(shuffledRepoList);
        
        // 创建输出目录
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                System.err.println("无法创建输出目录 " + outputDir);
                return false;
            }
        }
        
        // 创建日志文件
        String logFilePath = outputDir + "/clone_log_"+ getCurrentTime1() +".txt";
        
        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFilePath, true))) {
            logWriter.write("\n========== 克隆任务开始: " + getCurrentTime() + " ==========\n");
            logWriter.write("仓库总数: " + shuffledRepoList.size() + " | 线程数: 4\n");
            logWriter.flush();
            
            // 创建固定4线程的线程池
            ExecutorService executor = Executors.newFixedThreadPool(4);
            List<Future<?>> futures = new ArrayList<>();
            
            // 提交所有克隆任务
            for (String repoName : shuffledRepoList) {
                Future<?> future = executor.submit(() -> {
                    try {
                        cloneSingleRepo(repoName, outputDir, logWriter);
                    } catch (Exception e) {
                        // 单个仓库失败不影响其他仓库
                        String errorMsg = String.format("[%s] 错误: 克隆仓库 %s 时发生异常 - %s", 
                                getCurrentTime(), repoName, e.getMessage());
                        synchronized (logWriter) {
                            try {
                                logWriter.write(errorMsg + "\n");
                                logWriter.flush();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    }
                });
                futures.add(future);
            }
            
            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // 关闭线程池
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            
            logWriter.write("========== 克隆任务结束: " + getCurrentTime() + " ==========\n");
            return true;
            
        } catch (IOException e) {
            System.err.println("无法写入日志文件 " + logFilePath);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 克隆单个仓库（线程安全版本）
     */
    private void cloneSingleRepo(String repoName, String outputDir, BufferedWriter logWriter) 
            throws IOException, InterruptedException {
        
        // 提取仓库名称
        String repoNameOnly = repoName.substring(repoName.lastIndexOf("/") + 1);
        String localPath = outputDir + "/" + repoNameOnly;
        
        // 检查仓库是否已存在
        File repoDir = new File(localPath);
        if (repoDir.exists()) {
            String skipMsg = String.format("[%s] 跳过: 仓库已存在 - %s", 
                    getCurrentTime(), repoName);
            synchronized (logWriter) {  // 添加同步
                logWriter.write(skipMsg + "\n");
                logWriter.flush();
            }
            return;
        }
        
        // 构建SSH URL
        String sshUrl = "git@github.com:" + repoName + ".git";
        
        // 执行git clone
        ProcessBuilder builder = new ProcessBuilder("git", "clone", sshUrl, localPath);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        
        // 读取进程输出（但不打印到控制台）
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        int exitCode = process.waitFor();
        
        // 记录结果到日志（添加同步）
        synchronized (logWriter) {
            if (exitCode != 0) {
                String errorMsg = String.format("[%s] 错误: 克隆失败 - %s (退出码: %d)", 
                        getCurrentTime(), repoName, exitCode);
                logWriter.write(errorMsg + "\n");
            } else {
                String successMsg = String.format("[%s] 成功: %s", 
                        getCurrentTime(), repoName);
                logWriter.write(successMsg + "\n");
            }
            logWriter.flush();
        }
    }
    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }
    private String getCurrentTime1() {
        return LocalDateTime.now().format(DATE_FORMATTER1);
    }
}
