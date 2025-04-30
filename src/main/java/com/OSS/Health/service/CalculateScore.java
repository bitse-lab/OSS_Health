//package com.OSS.Health.service;
//
//import java.io.File;
//import java.time.LocalDate;
//import java.time.YearMonth;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.StreamSupport;
//
//import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.revwalk.RevCommit;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import com.OSS.Health.mapper.MysqlDataMapper;
//import com.OSS.Health.model.MysqlDataModel;
//
//@Service
//public class CalculateScore{
//	@Autowired
//    private MysqlDataMapper mysqlDataMapper;
//	
//	private static final String REPO_PATH = "D:/Plateform/Git/repositories/core"; // 请替换为git仓库存储位置
//	
//	public void GetCalculatedScore() throws Exception{
//		// 设置Git仓库路径
//	    String repoPath = REPO_PATH + "/.git";
//
//	    try (Git git = Git.open(new File(repoPath))) {
//	    	// 获取所有提交记录
//	        Iterable<RevCommit> commitsTmp = git.log().call();
//	        List<RevCommit> commits = new ArrayList<>();
//	        commitsTmp.forEach(commits::add); // 将 Iterable 转换为 List
//
//	        // 获取最后一个提交的时间
//	        LocalDate lastCommitDate = getLastCommitDate(commits);
//	        YearMonth lastMonth = YearMonth.from(lastCommitDate).minusMonths(1);
//	        
//	        Map<String, Indicator> indicatorMap = loadIndicatorConfig();
//            double totalScore = calculateTotalScore(lastMonth, indicatorMap);
//            System.out.println("Total Score: " + totalScore);
//	    }
//	}
//	
//    private LocalDate getLastCommitDate(List<RevCommit> commits) {
//        return StreamSupport.stream(commits.spliterator(), false)
//                .map(commit -> commit.getAuthorIdent().getWhen()
//                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
//                .max(LocalDate::compareTo)
//                .orElseThrow(() -> new RuntimeException("No commits found"));
//    }
//    
//    private Map<String, Indicator> loadIndicatorConfig() {
//        // 实际情况应从 JSON 文件加载，这里硬编码示意
//        Map<String, Indicator> map = new HashMap<>();
//        map.put("2.1.1", new Indicator("monthorgcommits", 48.4, 100));
//        map.put("1.1.2", new Indicator("bugs", 80.6, 10));
//        // 添加其他指标...
//        return map;
//    }
//
//    private double calculateTotalScore(YearMonth lastMonth, Map<String, Indicator> indicatorMap) {
//        double totalScore = 0.0;
//
//        for (Map.Entry<String, Indicator> entry : indicatorMap.entrySet()) {
//            String id = entry.getKey();
//            Indicator indicator = entry.getValue();
//
//            MysqlDataModel model = mysqlDataMapper.getMysqlDataModelNoS1(id, lastMonth.toString());
//            if (model == null || model.getNumber() == null) continue;
//
//            double value = model.getNumber();
//            double threshold = indicator.getThreshold();
//            double weight = indicator.getWeight();
//
//            double score = Math.min(1.0, value / threshold);
//            totalScore += score * weight;
//        }
//
//        return totalScore;
//    }
//    
//    public class Indicator {
//        private String name;
//        private double weight;
//        private double threshold;
//
//        public Indicator(String name, double weight, double threshold) {
//            this.name = name;
//            this.weight = weight;
//            this.threshold = threshold;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public double getWeight() {
//            return weight;
//        }
//
//        public double getThreshold() {
//            return threshold;
//        }
//    } 
//}