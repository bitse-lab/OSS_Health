<template>
    <div class="sustain-predict">
      <h1 class="section-title">
        <el-icon class="title-icon"><DataAnalysis /></el-icon>
        Sustainability Prediction
      </h1>
  
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="50"><Loading /></el-icon>
        <p>Running prediction model...</p>
      </div>
  
      <!-- 错误提示 -->
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />
  
      <!-- 预测结果 -->
      <div v-if="!loading && predictionData" class="prediction-content">
        <!-- 预测结果概览 -->
        <div class="section-highlight">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-card class="result-card" :class="getPredictionClass()">
                <div class="card-icon">
                  <el-icon :size="48"><Trophy /></el-icon>
                </div>
                <div class="card-label">Prediction</div>
                <div class="card-value">{{ predictionData.prediction_result?.label || 'N/A' }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="result-card probability-card">
                <div class="card-icon">
                  <el-icon :size="48"><PieChart /></el-icon>
                </div>
                <div class="card-label">Probability</div>
                <div class="card-value">{{ formatPercent(predictionData.prediction_result?.probability) }}</div>
                <el-progress
                  :percentage="getPercentage(predictionData.prediction_result?.probability)"
                  :color="getProgressColor(predictionData.prediction_result?.probability)"
                  :stroke-width="8"
                />
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="result-card confidence-card">
                <div class="card-icon">
                  <el-icon :size="48"><Odometer /></el-icon>
                </div>
                <div class="card-label">Confidence</div>
                <div class="card-value">{{ formatPercent(predictionData.prediction_result?.confidence) }}</div>
                <el-progress
                  :percentage="getPercentage(predictionData.prediction_result?.confidence)"
                  :color="getProgressColor(predictionData.prediction_result?.confidence)"
                  :stroke-width="8"
                />
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="result-card status-card">
                <div class="card-icon">
                  <el-icon :size="48"><CircleCheck /></el-icon>
                </div>
                <div class="card-label">Status</div>
                <div class="card-value">{{ predictionData.status || 'N/A' }}</div>
              </el-card>
            </el-col>
          </el-row>
        </div>
  
        <!-- 模型信息 -->
        <div class="section-block">
          <h2 class="sub-title">
            <el-icon><Setting /></el-icon>
            Model Information
          </h2>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card class="info-card">
                <el-statistic title="Model AUC" :value="0.935" :precision="3">
                  <template #prefix>
                    <el-icon :color="predictionData.model_info?.model_loaded ? '#67C23A' : '#F56C6C'">
                      <CircleCheck v-if="predictionData.model_info?.model_loaded" />
                      <CircleClose v-else />
                    </el-icon>
                  </template>
                  <template #default>
                    {{ predictionData.model_info?.model_loaded ? 'Loaded' : 'Not Loaded' }}
                  </template>
                </el-statistic>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="info-card">
                <el-statistic title="Feature Count" :value="predictionData.model_info?.feature_count || 0">
                  <template #prefix>
                    <el-icon color="#409EFF"><DataLine /></el-icon>
                  </template>
                </el-statistic>
              </el-card>
            </el-col>
          </el-row>
        </div>
  
        <!-- 特征数据 -->
        <div class="section-block">
          <h2 class="sub-title">
            <el-icon><DataBoard /></el-icon>
            Feature Analysis
          </h2>
          <el-card class="features-card">
            <div class="features-grid">
              <div
                v-for="(value, key) in predictionData.test_data?.features"
                :key="key"
                class="feature-item"
              >
                <div class="feature-label">{{ formatFeatureName(key) }}</div>
                <div class="feature-value">{{ formatFeatureValue(value) }}</div>
                <el-progress
                  v-if="isNumericFeature(value)"
                  :percentage="normalizeFeature(key, value)"
                  :show-text="false"
                  :stroke-width="4"
                  color="#409EFF"
                />
              </div>
            </div>
          </el-card>
        </div>
  
        <!-- 可视化图表 -->
        <div class="section-block">
          <h2 class="sub-title">
            <el-icon><Histogram /></el-icon>
            Feature Distribution
          </h2>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card class="chart-card">
                <div ref="radarChart" style="width: 100%; height: 400px;"></div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="chart-card">
                <div ref="barChart" style="width: 100%; height: 400px;"></div>
              </el-card>
            </el-col>
          </el-row>
        </div>
  
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" :icon="Refresh" @click="fetchPrediction">
            Run New Prediction
          </el-button>
          <el-button type="success" :icon="Download" @click="exportResults">
            Export Results
          </el-button>
          <el-button type="info" :icon="Document" @click="showDetails">
            View Details
          </el-button>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted, nextTick } from 'vue'
  import axios from 'axios'
  import * as echarts from 'echarts'
  import {
    DataAnalysis, Loading, Trophy, PieChart, Odometer, CircleCheck, CircleClose,
    Setting, DataLine, DataBoard, Histogram, Refresh, Download, Document
  } from '@element-plus/icons-vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  
  const loading = ref(false)
  const error = ref(null)
  const predictionData = ref(null)
  const radarChart = ref(null)
  const barChart = ref(null)
  
  // 获取预测数据
  const fetchPrediction = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.get('http://localhost:8080/api/xgboost/test-prediction')
      predictionData.value = response.data
      ElMessage.success('Prediction completed successfully')
      await nextTick()
      initCharts()
    } catch (err) {
      error.value = err.message || 'Failed to fetch prediction'
      ElMessage.error('Failed to run prediction')
    } finally {
      loading.value = false
    }
  }
  
  // 初始化图表
  const initCharts = () => {
    if (!predictionData.value?.test_data?.features) return
    
    // 雷达图
    if (radarChart.value) {
      const chart = echarts.init(radarChart.value)
      const features = predictionData.value.test_data.features
      const indicator = Object.keys(features).slice(0, 8).map(key => ({
        name: formatFeatureName(key),
        max: getMaxValue(key, features[key])
      }))
      const data = Object.values(features).slice(0, 8)
      
      chart.setOption({
        title: { text: 'Top Features Radar', left: 'center' },
        tooltip: {},
        radar: { indicator },
        series: [{
          type: 'radar',
          data: [{ value: data, name: 'Features' }],
          areaStyle: { opacity: 0.3 }
        }]
      })
    }
    
    // 柱状图
    if (barChart.value) {
      const chart = echarts.init(barChart.value)
      const features = predictionData.value.test_data.features
      const keys = Object.keys(features).slice(0, 10)
      const values = keys.map(k => features[k])
      
      chart.setOption({
        title: { text: 'Feature Values', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: keys.map(formatFeatureName),
          axisLabel: { rotate: 45, interval: 0 }
        },
        yAxis: { type: 'value' },
        series: [{
          type: 'bar',
          data: values,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 1, color: '#188df0' }
            ])
          }
        }]
      })
    }
  }
  
  // 获取预测结果样式类
  const getPredictionClass = () => {
    const label = predictionData.value?.prediction_result?.label?.toLowerCase()
    if (label?.includes('survived') || label?.includes('success')) return 'success-result'
    return 'warning-result'
  }
  
  // 格式化百分比
  const formatPercent = (value) => {
    if (value === null || value === undefined) return 'N/A'
    return `${(value * 100).toFixed(2)}%`
  }
  
  // 获取百分比数值
  const getPercentage = (value) => {
    if (value === null || value === undefined) return 0
    return Math.round(value * 100)
  }
  
  // 获取进度条颜色
  const getProgressColor = (value) => {
    if (value >= 0.8) return '#67C23A'
    if (value >= 0.6) return '#E6A23C'
    return '#F56C6C'
  }
  
  // 格式化特征名称
  const formatFeatureName = (key) => {
    return key.split('_').map(word => 
      word.charAt(0).toUpperCase() + word.slice(1)
    ).join(' ')
  }
  
  // 格式化特征值
  const formatFeatureValue = (value) => {
    if (typeof value === 'number') {
      return value.toFixed(4)
    }
    return value
  }
  
  // 判断是否为数值特征
  const isNumericFeature = (value) => {
    return typeof value === 'number'
  }
  
  // 归一化特征值用于显示
  const normalizeFeature = (key, value) => {
    const max = getMaxValue(key, value)
    return Math.min(100, (value / max) * 100)
  }
  
  // 获取特征最大值
  const getMaxValue = (key, value) => {
    // 根据不同特征设置合理的最大值
    const maxValues = {
      fork_count: 100,
      issue_count: 50,
      star_count: 200,
      pr_count: 100,
      commit_count: 500,
      contributor_count: 50,
      code_changes: 500000
    }
    return maxValues[key] || Math.max(value * 2, 100)
  }
  
  // 导出结果
  const exportResults = () => {
    if (!predictionData.value) return
    
    const data = JSON.stringify(predictionData.value, null, 2)
    const blob = new Blob([data], { type: 'application/json' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `prediction-results-${Date.now()}.json`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('Results exported')
  }
  
  // 显示详细信息
  const showDetails = () => {
    ElMessageBox.alert(
      `<pre>${JSON.stringify(predictionData.value, null, 2)}</pre>`,
      'Prediction Details',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'Close'
      }
    )
  }
  
  onMounted(() => {
    fetchPrediction()
  })
  </script>
  
  <style scoped>
  .sustain-predict {
    padding: 30px;
    background-color: #f9f9f9;
    min-height: 100vh;
  }
  
  .section-title {
    font-size: 32px;
    font-weight: bold;
    margin-bottom: 30px;
    color: #222;
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .title-icon {
    font-size: 36px;
    color: #1e90ff;
  }
  
  .loading-container {
    text-align: center;
    padding: 60px;
    background: white;
    border-radius: 12px;
  }
  
  .loading-container p {
    margin-top: 20px;
    font-size: 16px;
    color: #666;
  }
  
  .section-highlight {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 30px;
    border-radius: 12px;
    margin-bottom: 30px;
  }
  
  .result-card {
    text-align: center;
    padding: 25px;
    border-radius: 12px;
    background: white;
    transition: transform 0.3s, box-shadow 0.3s;
  }
  
  .result-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  }
  
  .card-icon {
    margin-bottom: 15px;
    color: #409EFF;
  }
  
  .card-label {
    font-size: 14px;
    color: #666;
    margin-bottom: 10px;
    font-weight: 500;
  }
  
  .card-value {
    font-size: 28px;
    font-weight: bold;
    color: #333;
    margin-bottom: 10px;
  }
  
  .card-value.small {
    font-size: 16px;
  }
  
  .success-result {
    border: 3px solid #67C23A;
  }
  
  .success-result .card-value {
    color: #67C23A;
  }
  
  .warning-result {
    border: 3px solid #E6A23C;
  }
  
  .warning-result .card-value {
    color: #E6A23C;
  }
  
  .section-block {
    margin-top: 30px;
    padding: 25px;
    background-color: #ffffff;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
  
  .sub-title {
    font-size: 22px;
    font-weight: 600;
    color: #1e90ff;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  </style>