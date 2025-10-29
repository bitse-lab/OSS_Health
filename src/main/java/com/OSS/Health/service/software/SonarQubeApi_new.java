package com.OSS.Health.service.software;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.util.*;

@Service
public class SonarQubeApi_new {
	@Autowired
    private MysqlDataMapper mysqlDataMapper;
	@Autowired
    private ObjectMapper objectMapper;
	
    private static final String SONAR_URL = "http://localhost:9000";
    private static final String PROJECT_KEY = "temp";
    // METRICS 覆盖三个方面：Quality(technical_debt[minutes]\bugs\code_smells\duplicated_lines_density[%]) Robustness(complexity\cognitive_complexity\vulnerabilities\comment_lines_density[%]) Productivity(lines\ncloc[no comment lines])
    private static final String METRICS = "sqale_index,bugs,code_smells,duplicated_lines_density,"
            + "complexity,cognitive_complexity,vulnerabilities,comment_lines_density,"
            + "lines,ncloc";
    // token必须是用户token，默认的不行
    // private static final String TOKEN = "sqp_c8419218d1d381be1163baa20f85eab335fccb31"; // 替换为你的 SonarQube Token
    private static final String TOKEN = "squ_a904ccc5cc3fb43d70d7d36c8f3d755370e286cc"; 
    private String REPO_OWNER;
    private String REPO_NAME;
    private String REPO_PATH;
    
    public void init(String repoOwner, String repoName, String repoPath) {
    	this.REPO_OWNER = repoOwner;
        this.REPO_NAME = repoName;
        this.REPO_PATH = repoPath;
    }
    
    public void analyzeProjectByMonth() throws Exception {
        if (REPO_OWNER == null || REPO_NAME == null || REPO_PATH == null) {
            throw new IllegalStateException("Repo info not initialized. Call init() first.");
        }

        String repoPath = REPO_PATH + "/.git";
        clearMysqlData();

        Git git = Git.open(new File(repoPath));
        checkoutToDefaultBranch(git);
        Iterable<RevCommit> commits = git.log().call();        

        // 按提交时间整理出“下个月第一天”->commit的映射
        TreeMap<LocalDate, RevCommit> monthFirstCommits = new TreeMap<>();
        for (RevCommit commit : commits) {
            Instant commitTime = Instant.ofEpochSecond(commit.getCommitTime());
            LocalDate commitDate = commitTime.atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate monthStart = commitDate.withDayOfMonth(1).plusMonths(1);
            monthFirstCommits.putIfAbsent(monthStart, commit);
        }

        if (monthFirstCommits.isEmpty()) {
            System.out.println("无提交数据，无法分析");
            git.close();
            return;
        }

        LocalDate firstMonth = monthFirstCommits.firstKey();
        LocalDate lastMonth = monthFirstCommits.lastKey();

        Map<String, Double> lastMetricMap = null;

        LocalDate currentMonth = firstMonth;
        while (!currentMonth.isAfter(lastMonth)) {
            if (monthFirstCommits.containsKey(currentMonth)) {
                // 有commit，切换commit，运行分析，获取新数据
                RevCommit commit = monthFirstCommits.get(currentMonth);
                System.out.println("分析月份：" + currentMonth);
                git.checkout().setName(commit.getName()).call();

                runSonarScanner();
                waitForAnalysisCompletion();

                String response = callApi(SONAR_URL + "/api/measures/component?component=" + PROJECT_KEY + "&metricKeys=" + METRICS);
                Map<String, Double> metricMap = parseMetricsFromResponse(response);

                writeResultsToDatabase(metricMap, currentMonth);

                lastMetricMap = metricMap;  // 更新最近数据快照
            } else if (lastMetricMap != null) {
                // 无commit，用最近一次数据填充
                System.out.println("无提交，使用上个月数据填充月份：" + currentMonth);
                writeResultsToDatabase(lastMetricMap, currentMonth);
            } else {
                // 无commit且无数据，跳过或写空
                System.out.println("无提交且无历史数据，跳过月份：" + currentMonth);
            }

            currentMonth = currentMonth.plusMonths(1);
        }

        try {
            checkoutToDefaultBranch(git);
        } catch (Exception e) {
            System.err.println("无法切换回默认分支: " + e.getMessage());
        } finally {
            git.close();
        }
    }
    
    private void checkoutToDefaultBranch(Git git) throws GitAPIException, IOException {
        // 获取所有分支引用
        List<Ref> refs = git.getRepository().getRefDatabase().getRefsByPrefix(Constants.R_HEADS);
        
        boolean hasMain = false;
        boolean hasMaster = false;
        
        // 检查存在哪些分支
        for (Ref ref : refs) {
            String branchName = Repository.shortenRefName(ref.getName());
            if ("main".equals(branchName)) {
                hasMain = true;
            } else if ("master".equals(branchName)) {
                hasMaster = true;
            }
        }
        
        // 根据存在的分支进行切换
        if (hasMain) {
            git.checkout().setName("main").call();
        } else if (hasMaster) {
            git.checkout().setName("master").call();
        } else {
            // 回退方案：切换回HEAD
            git.checkout().setName(git.getRepository().resolve("HEAD").getName()).call();
        }
    }
    
    private Map<String, Double> parseMetricsFromResponse(String response) throws Exception {
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode measuresNode = rootNode.path("component").path("measures");
        Map<String, Double> metricMap = new HashMap<>();

        for (JsonNode measure : measuresNode) {
            String metric = measure.path("metric").asText();
            double value = measure.path("value").asDouble();
            metricMap.put(metric, value);
        }

        return metricMap;
    }

    private void runSonarScanner() throws Exception {
    	ProcessBuilder builder = new ProcessBuilder(
    		    "cmd.exe", "/c", "docker", "run", "--rm",
    		    "-v", REPO_PATH + ":/usr/src",
    		    "-v", "E:\\GithubRep\\sonar-cache:/opt/sonar-scanner/.sonar",
    		    "-v", "E:\\GithubRep\\empty:/empty",                 // 挂载空目录到容器的 /empty
    		    "sonarsource/sonar-scanner-cli",
    		    "-Dsonar.projectKey=" + PROJECT_KEY,
    		    "-Dsonar.sources=/usr/src",
    		    "-Dsonar.host.url=http://host.docker.internal:9000",
    		    "-Dsonar.login=" + TOKEN,
    		    "-Dsonar.java.binaries=/empty",                      // 指向空目录
    		    "-Dsonar.exclusions=**/libs/**,**/build/**,**/node_modules/**,**/*.min.js,**/*.d.ts,**/Github_Api_Message/**"
    	);
        runCommand(builder);
    }

    private static void waitForAnalysisCompletion() throws Exception {
        String apiUrl = SONAR_URL + "/api/ce/component?component=" + PROJECT_KEY;

        while (true) {
            String response = callApi(apiUrl);
            String taskId = extractTaskId(response);
            if (taskId == null) {
                Thread.sleep(5000);
                continue;
            }

            String taskStatusUrl = SONAR_URL + "/api/ce/task?id=" + taskId;
            String taskResponse = callApi(taskStatusUrl);
            if (taskResponse.contains("\"status\":\"SUCCESS\"")) break;

            Thread.sleep(5000);
        }
    }

    private static String extractTaskId(String jsonResponse) {
        int index = jsonResponse.indexOf("\"id\":\"");
        if (index != -1) {
            int start = index + 6;
            int end = jsonResponse.indexOf("\"", start);
            return jsonResponse.substring(start, end);
        }
        return null;
    }

    private void writeResultsToDatabase(Map<String, Double> metricMap, LocalDate monthDate) {
        try {
            MysqlDataModel entity = new MysqlDataModel();
            entity.setTime(monthDate);
            entity.setS1("");

            insertMetrics(entity, metricMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertMetrics(MysqlDataModel entity, Map<String, Double> metricMap) {
    	insertIfPresent("1.1.1", metricMap.get("sqale_index"), entity);
    	insertIfPresent("1.1.2", metricMap.get("bugs"), entity);
    	insertIfPresent("1.1.3", metricMap.get("code_smells"), entity);
    	insertIfPresent("1.1.4", metricMap.get("duplicated_lines_density"), entity);
    	
    	insertIfPresent("1.2.1", metricMap.get("complexity"), entity);
    	insertIfPresent("1.2.2", metricMap.get("cognitive_complexity"), entity);
    	insertIfPresent("1.2.3", metricMap.get("vulnerabilities"), entity);
    	insertIfPresent("1.2.4", metricMap.get("comment_lines_density"), entity);
    }
    
    private void insertIfPresent(String id, Double value, MysqlDataModel entity) {
        if (value != null) {
            entity.setId(id);
            entity.setNumber(value);
            mysqlDataMapper.insertMysqlData_new(REPO_OWNER+'_'+REPO_NAME, entity);
        }else {
        	System.out.println("value is null: "+ id);
        }
        return;
    }

    private static String callApi(String apiUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + TOKEN);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private static void runCommand(ProcessBuilder builder) throws Exception {
        // 不合并输出，分别读取
        Process process = builder.start();

        // 读取标准输出，但不打印或丢弃
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // 这里不打印，丢弃或可选择打印到日志文件
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // 读取并打印错误输出
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
        }

        process.waitFor();
    }
    
    private void clearMysqlData() {
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.1.1");
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.1.2");
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.1.3");
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.1.4");
    	
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.2.1");
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.2.2");
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.2.3");
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.2.4");
    	
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.3.1");
    	mysqlDataMapper.clearMysqlDataById_new(REPO_OWNER+'_'+REPO_NAME, "1.3.2");
    }
}
