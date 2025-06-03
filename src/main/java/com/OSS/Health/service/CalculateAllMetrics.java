package com.OSS.Health.service;

import com.OSS.Health.mapper.MysqlDataMapper;
import com.OSS.Health.service.community.organization.*;
import com.OSS.Health.service.community.resilience.*;
import com.OSS.Health.service.community.vigor.*;
import com.OSS.Health.service.market.influence.*;
import com.OSS.Health.service.software.productivity.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Scope;

@Service
@Scope("prototype")
public class CalculateAllMetrics {
	@Autowired
    private MysqlDataMapper mysqlDataMapper;
	
    private String REPO_OWNER;
    private String REPO_NAME;
    private String REPO_PATH;
    
    @Autowired
    private MonthOrgCommitsService_new testMonthOrgCommits;
    @Autowired
    private MonthOrgEntropyService_new testMonthOrgEntropy;
    @Autowired
    private MonthVolunteerCommitsService_new testMonthVolunteerCommits;
    @Autowired
    private MonthVolunteerEntropyService_new testMonthVolunteerEntropy;
    @Autowired
    private PRLinkedIssueService_new testPRLinkedIssue;
    @Autowired
    private PRMergedRatioService_new testPRMergedRatio;
    @Autowired
    private ReviewRatioService_new testReviewRatio;
    @Autowired
    private CodeContributorCountService_new testCodeContributorCount;
    @Autowired
    private LongTermContributorService_new testLongTermContributor;
    @Autowired
    private MonthForkService_new testMonthFork;
    @Autowired
    private MonthStarService_new testMonthStar;
    @Autowired
    private MonthChangedCodesService_new testMonthChangedCodes;
    @Autowired
    private MonthCommitService_new testMonthCommit;
    @Autowired
    private MonthIssueService_new testMonthIssue;
    @Autowired
    private MonthPRService_new testMonthPR;
    
    public void init(String repoOwner, String repoName, String repoPath) {
    	this.REPO_OWNER = repoOwner;
        this.REPO_NAME = repoName;
        this.REPO_PATH = repoPath;
    }

    public boolean calculateAllMetrics() throws Exception {
    	if (REPO_OWNER == null || REPO_NAME == null || REPO_PATH == null) {
            throw new IllegalStateException("Repo info not initialized. Call init() first.");
        }
    	
    	// mysqlDataMapper.dropTable(REPO_OWNER+'_'+REPO_NAME);
    	
    	mysqlDataMapper.createTable(REPO_OWNER+'_'+REPO_NAME);
    	
    	testMonthOrgCommits.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthOrgCommits.generateMonthlyReport();
    	
    	testMonthOrgEntropy.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthOrgEntropy.generateMonthlyReport();
    	
    	testMonthVolunteerCommits.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthVolunteerCommits.generateMonthlyReport();
    	
    	testMonthVolunteerEntropy.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthVolunteerEntropy.generateMonthlyReport();
    	
    	testPRLinkedIssue.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testPRLinkedIssue.generateMonthlyReport();
    	
    	testPRMergedRatio.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testPRMergedRatio.generateMonthlyReport();
    	
    	testReviewRatio.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testReviewRatio.generateMonthlyReport();
    	
    	testCodeContributorCount.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testCodeContributorCount.generateMonthlyReport();
    	
    	testLongTermContributor.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testLongTermContributor.generateMonthlyReport();
    	
    	testMonthFork.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthFork.generateMonthlyReport();
    	
    	testMonthStar.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthStar.generateMonthlyReport();
    	
    	testMonthChangedCodes.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthChangedCodes.generateMonthlyReport();
    	
    	testMonthCommit.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthCommit.generateMonthlyReport();
    	
    	testMonthIssue.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthIssue.generateMonthlyReport();
    	
    	testMonthPR.init(REPO_OWNER, REPO_NAME, REPO_PATH);
    	testMonthPR.generateMonthlyReport();
    	
        return true;
    }
}

