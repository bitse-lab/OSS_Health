<template>
  <div class="overview">
    <h1 id="overViewTop" class="section-title">Health Overview</h1>

    <!-- 总览折线图 -->
    <el-card class="chart-card"><Line3ChartDemo /></el-card>

    <!-- 一级维度 Box 图 -->
    <h2 class="sub-title">First-level Dimension Health Overview</h2>
    <div class="section-highlight">
      <el-row :gutter="20">
        <el-col :span="8"><el-card class="chart-card"><BoxChartDemo /></el-card></el-col>
        <el-col :span="8"><el-card class="chart-card"><BoxChartDemo /></el-card></el-col>
        <el-col :span="8"><el-card class="chart-card"><BoxChartDemo /></el-card></el-col>
      </el-row>
      <el-row :gutter="20" class="center-text label-row">
        <el-col :span="8">Software</el-col>
        <el-col :span="8">Community</el-col>
        <el-col :span="8">Market</el-col>
      </el-row>
    </div>

    <!-- 二级维度 -->
    <h2 class="sub-title">Second-level Dimension Health Overview</h2>

    <!-- Software 区域 -->
    <div class="section-block">
      <h3 class="small-sub-title">Software</h3>
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

    <!-- Community 区域 -->
    <div class="section-block">
      <h3 class="small-sub-title">Community</h3>
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

    <!-- Market 区域 -->
    <div class="section-block">
      <h3 class="small-sub-title">Market</h3>
      <el-row :gutter="20">
        <el-col :span="12"><el-card class="chart-card"><RadarChartDemo /></el-card></el-col>
        <el-col :span="12"><el-card class="chart-card"><RadarChartDemo /></el-card></el-col>
      </el-row>
      <el-row :gutter="20" class="center-text label-row">
        <el-col :span="12">Competitiveness</el-col>
        <el-col :span="12">Influence</el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import BoxChartDemo from '@/components/BoxChartDemo.vue';
import Line3ChartDemo from '@/components/Line3ChartDemo.vue';
import RadarChartDemo from '@/components/RadarChartDemo.vue';

export default {
  name: 'OverViewOSS',
  components: {
    BoxChartDemo,
    RadarChartDemo,
    Line3ChartDemo,
  },
};
</script>

<style>
.overview {
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

.small-sub-title {
  font-size: 20px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #444;
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
