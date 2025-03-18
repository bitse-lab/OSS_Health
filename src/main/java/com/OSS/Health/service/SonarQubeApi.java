package com.OSS.Health.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SonarQubeApi {
    private static final String SONAR_URL = "http://localhost:9000";
    private static final String PROJECT_KEY = "temp";
    private static final String METRICS = "bugs,code_smells,vulnerabilities,coverage";
    private static final String TOKEN = "sqp_c8419218d1d381be1163baa20f85eab335fccb31"; // 替换为你的 SonarQube Token
    private static final String SOURCE_DIR = "D:\\Plateform\\Git\\repositories\\core"; // 你的 Java 代码路径

    public boolean GetSonarQubeApi() {
        try {
            System.out.println("开始代码分析...");
            runSonarScanner();

            System.out.println("等待 SonarQube 分析完成...");
            waitForAnalysisCompletion();

            System.out.println("获取最终分析报告...");
            fetchSonarAnalysisResults();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void runSonarScanner() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "docker", "run", "--rm",
            "-v", SOURCE_DIR + ":/usr/src",
            "sonarsource/sonar-scanner-cli",
            "-Dsonar.projectKey=" + PROJECT_KEY,
            "-Dsonar.sources=/usr/src",
            "-Dsonar.host.url=" + SONAR_URL,
            "-Dsonar.login=" + TOKEN
        );
        runCommand(builder);
    }

    private static void waitForAnalysisCompletion() throws Exception {
        String apiUrl = SONAR_URL + "/api/ce/component?component=" + PROJECT_KEY;

        while (true) {
            String response = callApi(apiUrl);

            String taskId = extractTaskId(response);
            if (taskId == null) {
                System.out.println("未找到任务 ID，等待 5 秒...");
                Thread.sleep(5000);
                continue;
            }

            String taskStatusUrl = SONAR_URL + "/api/ce/task?id=" + taskId;
            String taskResponse = callApi(taskStatusUrl);
            if (taskResponse.contains("\"status\":\"SUCCESS\"")) {
                System.out.println("代码分析完成！");
                break;
            }

            System.out.println("分析进行中，等待 5 秒...");
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

    private static void fetchSonarAnalysisResults() throws Exception {
        String apiUrl = SONAR_URL + "/api/measures/component?component=" + PROJECT_KEY + "&metricKeys=" + METRICS;
        String response = callApi(apiUrl);
        System.out.println("分析结果:");
        System.out.println(response);
    }

    private static String callApi(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + TOKEN);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private static void runCommand(ProcessBuilder builder) throws Exception {
        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        process.waitFor();
    }
}
