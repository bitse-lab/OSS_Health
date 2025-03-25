package com.OSS.Health.service.software;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.model.MysqlDataModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SonarQubeApi {
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
    private static final String SOURCE_DIR = "D:\\Plateform\\Git\\repositories\\core"; // 代码路径
    
	public List<Map<String, Object>> getMysqlData(String mysqlId) {
		return mysqlDataMapper.getMysqlDataModelNoS1(mysqlId);
    }

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
            "-Dsonar.host.url=" + "http://host.docker.internal:"+ SONAR_URL.substring(SONAR_URL.lastIndexOf(":") + 1),
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

    private void fetchSonarAnalysisResults() throws Exception {
        String apiUrl = SONAR_URL + "/api/measures/component?component=" + PROJECT_KEY + "&metricKeys=" + METRICS;
        String response = callApi(apiUrl);
        System.out.println("分析结果:");
        System.out.println(response);
        System.out.println("分析结果写入数据库...");
        writeResultsToDatabase(response);
    }
    
    // response from SonarQube api
    private void writeResultsToDatabase(String response) {
    	// 获取当前日期
    	try {
	        LocalDate currentDate = LocalDate.now().withDayOfMonth(1);
	        MysqlDataModel entity = new MysqlDataModel();
	    	entity.setTime(currentDate);
	    	entity.setS1("");
	        JsonNode rootNode = objectMapper.readTree(response);
	        JsonNode measuresNode = rootNode.path("component").path("measures");
	        // 存储对应的指标和对应的值
	        Map<String, Double> metricMap = new HashMap<>();
	
	        for (JsonNode measure : measuresNode) {
	            String metric = measure.path("metric").asText();
	            double value = measure.path("value").asDouble();  // 解析为 double
	            metricMap.put(metric, value);
	        }
	    	// quality 部分
	    	entity.setId("1.1.1");
	    	entity.setNumber(metricMap.get("sqale_index"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	entity.setId("1.1.2");
	    	entity.setNumber(metricMap.get("bugs"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	entity.setId("1.1.3");
	    	entity.setNumber(metricMap.get("code_smells"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	entity.setId("1.1.4");
	    	entity.setNumber(metricMap.get("duplicated_lines_density"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	
	    	// robustness 部分
	    	entity.setId("1.2.1");
	    	entity.setNumber(metricMap.get("complexity"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	entity.setId("1.2.2");
	    	entity.setNumber(metricMap.get("cognitive_complexity"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	entity.setId("1.2.3");
	    	entity.setNumber(metricMap.get("vulnerabilities"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	entity.setId("1.2.4");
	    	entity.setNumber(metricMap.get("comment_lines_density"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	
	    	// productivity 部分
	    	entity.setId("1.3.1");
	    	entity.setNumber(metricMap.get("lines"));
	    	mysqlDataMapper.insertMysqlData(entity);
	    	entity.setId("1.3.2");
	    	entity.setNumber(metricMap.get("ncloc"));
	    	mysqlDataMapper.insertMysqlData(entity);
    	}catch (JsonMappingException e) {
            System.err.println(e.getMessage());
        }catch (JsonProcessingException e) {
        	System.err.println(e.getMessage());
        }
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
