<template>
  <div class="market">
    <h1 id="marketTop" class="section-title">Market</h1>

    <!-- Box 图 -->
    <el-card class="chart-card"><BoxChartDemo /></el-card>

    <!-- 雷达图 + label -->
    <div class="section-highlight">
      <el-row :gutter="20">
        <el-col :span="12"><el-card class="chart-card"><RadarChartDemo /></el-card></el-col>
        <el-col :span="12"><el-card class="chart-card"><RadarChartDemo /></el-card></el-col>
      </el-row>
      <el-row :gutter="20" class="center-text label-row">
        <el-col :span="12">Competitiveness</el-col>
        <el-col :span="12">Influence</el-col>
      </el-row>
    </div>

    <!-- Competitiveness 区域 -->
    <div class="section-block">
      <h2 id="marketCompetitiveness" class="sub-title">Competitiveness</h2>
      <el-row :gutter="20">
        <el-col :span="12"><el-card class="chart-card"><LineChartDemo /></el-card></el-col>
        <el-col :span="12"><el-card class="chart-card"><LineChartDemo /></el-card></el-col>
      </el-row>
    </div>

    <!-- Influence 区域 -->
    <div class="section-block">
      <h2 id="marketInfluence" class="sub-title">Influence</h2>
      <el-row :gutter="20">
        <el-col :span="12"><el-card class="chart-card"><MonthForkLineChart /></el-card></el-col>
        <el-col :span="12"><el-card class="chart-card"><MonthStarLineChart /></el-card></el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';
import LineChartDemo from '@/components/LineChartDemo.vue';
import MonthForkLineChart from '@/components/oldDesign/MonthForkLineChart.vue';
import MonthStarLineChart from '@/components/oldDesign/MonthStarLineChart.vue';

export default {
  name: 'MarketOSS',
  components: {
    BoxChartDemo,
    RadarChartDemo,
    LineChartDemo,
    MonthForkLineChart,
    MonthStarLineChart,
  },
};
</script>

<style>
.market {
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
