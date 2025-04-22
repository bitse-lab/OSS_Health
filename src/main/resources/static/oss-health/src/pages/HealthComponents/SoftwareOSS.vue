<template>
    <div class="software">
      <h1 id="softwareTop" class="section-title">Software</h1>
  
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
          <el-col :span="8">Quality</el-col>
          <el-col :span="8">Robustness</el-col>
          <el-col :span="8">Productivity</el-col>
        </el-row>
      </div>
  
      <!-- Quality 区域 -->
      <div class="section-block">
        <h2 id="softwareQuality" class="sub-title">Quality</h2>
        <el-row :gutter="20">
          <el-col :span="6"><el-card class="chart-card quality-robustness-card"><TechnicalDebtChart /></el-card></el-col>
          <el-col :span="6"><el-card class="chart-card quality-robustness-card"><BugsChart /></el-card></el-col>
          <el-col :span="6"><el-card class="chart-card quality-robustness-card"><CodeSmellsChart /></el-card></el-col>
          <el-col :span="6"><el-card class="chart-card quality-robustness-card"><DuplicatedLinesDensityChart /></el-card></el-col>
        </el-row>
      </div>
  
      <!-- Robustness 区域 -->
      <div class="section-block">
        <h2 id="softwareRoubstness" class="sub-title">Robustness</h2>
        <el-row :gutter="20">
          <el-col :span="6"><el-card class="chart-card quality-robustness-card"><ComplexityChart /></el-card></el-col>
          <el-col :span="6"><el-card class="chart-card quality-robustness-card"><CognitiveComplexityChart /></el-card></el-col>
          <el-col :span="6"><el-card class="chart-card quality-robustness-card"><VulnerabilitiesChart /></el-card></el-col>
          <el-col :span="6"><el-card class="chart-card quality-robustness-card"><CommentLinesDensityChart /></el-card></el-col>
        </el-row>
      </div>
  
      <!-- Productivity 区域 -->
      <div class="section-block">
        <h2 id="softwareProductivity" class="sub-title">Productivity</h2>
        <el-row :gutter="20">
          <el-col :span="12"><el-card class="chart-card"><MonthChangedCodesLineChart /></el-card></el-col>
          <el-col :span="12"><el-card class="chart-card"><MonthIssueLineChart /></el-card></el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px">
          <el-col :span="12"><el-card class="chart-card"><MonthCommitLineChart /></el-card></el-col>
          <el-col :span="12"><el-card class="chart-card"><MonthPRLineChart /></el-card></el-col>
        </el-row>
      </div>
  
    </div>
</template>

<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';
import TechnicalDebtChart from '@/components/oldDesign/TechnicalDebtChart.vue';
import BugsChart from '@/components/oldDesign/BugsChart.vue';
import CodeSmellsChart from '@/components/oldDesign/CodeSmellsChart.vue';
import DuplicatedLinesDensityChart from '@/components/oldDesign/DuplicatedLinesDensityChart.vue';
import ComplexityChart from '@/components/oldDesign/ComplexityChart.vue';
import CognitiveComplexityChart from '@/components/oldDesign/CognitiveComplexityChart.vue';
import VulnerabilitiesChart from '@/components/oldDesign/VulnerabilitiesChart.vue';
import CommentLinesDensityChart from '@/components/oldDesign/CommentLinesDensityChart.vue';
import MonthCommitLineChart from '@/components/oldDesign/MonthCommitLineChart.vue';
import MonthIssueLineChart from '@/components/oldDesign/MonthIssueLineChart.vue';
import MonthPRLineChart from '@/components/oldDesign/MonthPRLineChart.vue';
import MonthChangedCodesLineChart from '@/components/oldDesign/MonthChangedCodesLineChart.vue';

export default {
  name: 'SoftwareOSS',
  components: {
    BoxChartDemo,
    RadarChartDemo,
    TechnicalDebtChart,
    BugsChart,
    CodeSmellsChart,
    DuplicatedLinesDensityChart,
    ComplexityChart,
    CognitiveComplexityChart,
    VulnerabilitiesChart,
    CommentLinesDensityChart,
    MonthChangedCodesLineChart,
    MonthCommitLineChart,
    MonthIssueLineChart,
    MonthPRLineChart,
  },
};
</script>

<style>
.software {
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
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
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

/* 只修改 Quality 和 Robustness 区域中的卡片样式 */
.quality-robustness-card {
  height: 125px; /* 设置一致的高度 */
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
