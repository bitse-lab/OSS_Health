<template>
    <div class="community">
      <h1 id="communityTop" class="section-title">Community</h1>
  
      <!-- Box 图 -->
      <el-card class="chart-card"><BoxChartDemo /></el-card>
  
      <!-- 雷达图 + label -->
      <div class="section-highlight">
        <el-row :gutter="20">
          <el-col :span="8"><el-card class="chart-card"><RadarChartDemo /></el-card></el-col>
          <el-col :span="8"><el-card class="chart-card"><RadarChartDemo /></el-card></el-col>
          <el-col :span="8"><el-card class="chart-card"><RadarChartDemo /></el-card></el-col>
        </el-row>
        <el-row :gutter="20" class="center-text label-row">
          <el-col :span="8">Organization</el-col>
          <el-col :span="8">Resilience</el-col>
          <el-col :span="8">Vigor</el-col>
        </el-row>
      </div>
  
      <!-- Organization 区域 -->
      <div class="section-block">
        <h2 id="communityOrganization" class="sub-title">Organization</h2>
        <el-row :gutter="20">
          <el-col :span="12"><el-card class="chart-card"><MonthOrgCommitsLineChart /></el-card></el-col>
          <el-col :span="12"><el-card class="chart-card"><MonthVolunteerCommitsLineChart /></el-card></el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px">
          <el-col :span="12"><el-card class="chart-card"><MonthOrgEntropyLineChart /></el-card></el-col>
        </el-row>
      </div>
  
      <!-- Resilience 区域 -->
      <div class="section-block">
        <h2 id="communityResilience" class="sub-title">Resilience</h2>
        <el-row :gutter="20">
          <el-col :span="12"><el-card class="chart-card"><ReviewRatioLineChart /></el-card></el-col>
          <el-col :span="12"><el-card class="chart-card"><PRMergedRatioLineChart /></el-card></el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px">
          <el-col :span="12"><el-card class="chart-card"><PRLinkedIssueLineChart /></el-card></el-col>
        </el-row>
      </div>
  
      <!-- Vigor 区域 -->
      <div class="section-block">
        <h2 id="communityVigor" class="sub-title">Vigor</h2>
        <el-row :gutter="20">
          <el-col :span="12"><el-card class="chart-card"><CodeContributorCountLineChart /></el-card></el-col>
        </el-row>
      </div>
    </div>
</template>
  
  
<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';
import CodeContributorCountLineChart from '@/components/oldDesign/CodeContributorCountLineChart.vue';
import ReviewRatioLineChart from '@/components/oldDesign/ReviewRatioLineChart.vue';
import PRMergedRatioLineChart from '@/components/oldDesign/PRMergedRatioLineChart.vue';
import PRLinkedIssueLineChart from '@/components/oldDesign/PRLinkedIssueLineChart.vue';
import MonthOrgCommitsLineChart from '@/components/oldDesign/MonthOrgCommitsLineChart.vue';
import MonthOrgEntropyLineChart from '@/components/oldDesign/MonthOrgEntropyLineChart.vue';
import MonthVolunteerCommitsLineChart from '@/components/oldDesign/MonthVolunteerCommitsLineChart.vue';

export default {
    name: 'CommunityOSS',
    components: {
      BoxChartDemo,
      RadarChartDemo,
      CodeContributorCountLineChart,
      ReviewRatioLineChart,
      PRMergedRatioLineChart,
      PRLinkedIssueLineChart,
      MonthOrgCommitsLineChart,
      MonthOrgEntropyLineChart,
      MonthVolunteerCommitsLineChart,
    },
};
</script>
  
<style>
.community {
    padding: 30px;
    background-color: #f9f9f9;
}
  
.section-title {
    font-size: 32px;
    font-weight: bold;
    margin-bottom: 30px;
    color: #222;
}
  
.sub-title {
    font-size: 24px;
    font-weight: 600;
    color: #1e90ff;
    margin-bottom: 20px;
}
  
.chart-card {
    transition: box-shadow 0.3s;
    border-radius: 12px;
}
  
.chart-card:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
  
.section-block {
    margin-top: 40px;
    padding: 20px;
    background-color: #ffffff;
    border-radius: 12px;
    box-shadow: 0 1px 6px rgba(0, 0, 0, 0.05);
}
  
.section-highlight {
    background: #f0f8ff;
    padding: 20px;
    border-radius: 12px;
    margin-top: 20px;
    margin-bottom: 40px;
}
  
.center-text {
    text-align: center;
    color: #666;
} 
  
.label-row {
    margin-top: 10px;
    font-weight: 500;
}
</style>
  