<template>
    <div class="health-report">
      <h1 class="section-title">
        <el-icon class="title-icon"><Document /></el-icon>
        Health Report
      </h1>
  
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="50"><Loading /></el-icon>
        <p>Generating health report...</p>
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
  
      <!-- 报告内容 -->
      <div v-if="!loading && reportData" class="report-content">
        <!-- 概览卡片 - 修改为两个卡片占满整行 -->
        <el-row :gutter="20" class="overview-cards" justify="space-between">
        <el-col :span="11">  <!-- 改为11，留出2的间隔 -->
            <el-card class="status-card" :class="getStatusClass(reportData.status)">
            <div class="card-header">
                <el-icon :size="32"><TrendCharts /></el-icon>
                <span>Status</span>
            </div>
            <div class="card-value">{{ reportData.status || 'N/A' }}</div>
            </el-card>
        </el-col>
        <el-col :span="11">  <!-- 改为11，留出2的间隔 -->
            <el-card class="status-card info-card">
            <div class="card-header">
                <el-icon :size="32"><Clock /></el-icon>
                <span>Report Time</span>
            </div>
            <div class="card-value">{{ formatDate(reportData.timestamp) }}</div>
            </el-card>
        </el-col>
        </el-row>

        <!-- 文件信息 - 修改为居中显示 -->
        <div class="section-block">
        <h2 class="sub-title">
            <el-icon><Folder /></el-icon>
            File Information
        </h2>
        <el-row :gutter="40" justify="center">  <!-- 添加justify="center" -->
            <el-col :span="10">  <!-- 改为10，留出更多空间 -->
            <el-card class="info-detail-card">
                <h3>Input File</h3>
                <el-descriptions :column="1" border>
                <el-descriptions-item label="Lines">
                    {{ formatNumber(reportData.input_file_info?.file_lines) }}
                </el-descriptions-item>
                <el-descriptions-item label="Size">
                    {{ formatBytes(reportData.input_file_info?.file_size_bytes) }}
                </el-descriptions-item>
                </el-descriptions>
            </el-card>
            </el-col>
            <el-col :span="10">  <!-- 改为10，留出更多空间 -->
            <el-card class="info-detail-card">
                <h3>Output File</h3>
                <el-descriptions :column="1" border>
                <el-descriptions-item label="Exists">
                    <el-tag :type="reportData.output_file_info?.file_exists ? 'success' : 'danger'">
                    {{ reportData.output_file_info?.file_exists ? 'Yes' : 'No' }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="Size">
                    {{ formatBytes(reportData.output_file_info?.file_size_bytes) }}
                </el-descriptions-item>
                </el-descriptions>
            </el-card>
            </el-col>
        </el-row>
        </div>
  
        <!-- 报告内容详情 -->
        <div class="section-block">
          <h2 class="sub-title">
            <el-icon><Reading /></el-icon>
            Report Content
          </h2>
          <el-card class="content-card">
            <el-tabs v-model="activeTab" type="border-card">
              <el-tab-pane label="Formatted View" name="formatted">
                <div class="formatted-content">
                  <div v-html="formattedContent"></div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="Raw Text" name="raw">
                <el-input
                  v-model="reportData.output_content"
                  type="textarea"
                  :rows="20"
                  readonly
                  class="raw-content"
                />
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </div>
  
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" :icon="Refresh" @click="fetchReport">
            Refresh Report
          </el-button>
          <el-button type="success" :icon="Download" @click="downloadReport">
            Download Report
          </el-button>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, computed, onMounted } from 'vue'
  import axios from 'axios'
  import {
    Document, Loading, TrendCharts, Clock,
    Folder, Reading, Refresh, Download
  } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'
  
  const loading = ref(false)
  const error = ref(null)
  const reportData = ref(null)
  const activeTab = ref('formatted')
  
  // 获取报告数据
  const fetchReport = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.get('http://localhost:8080/api/report/test-data')
      reportData.value = response.data
      ElMessage.success('Report loaded successfully')
    } catch (err) {
      error.value = err.message || 'Failed to fetch report'
      ElMessage.error('Failed to load report')
    } finally {
      loading.value = false
    }
  }
  
  // 格式化报告内容
  const formattedContent = computed(() => {
    if (!reportData.value?.output_content) return ''
    
    let content = reportData.value.output_content
    // 将换行符转换为 <br>
    content = content.replace(/\r\n/g, '<br>')
    content = content.replace(/\n/g, '<br>')
    
    // 格式化标题
    content = content.replace(/^- (.+)$/gm, '<h3 class="content-title">$1</h3>')
    
    // 格式化指标名称
    content = content.replace(/Metric: (.+)$/gm, '<h4 class="metric-name">📊 Metric: $1</h4>')
    
    // 格式化 What/Why/How
    content = content.replace(/^(What|Why|How):/gm, '<strong class="section-label">$1:</strong>')
    
    return content
  })
  
  // 获取状态样式类
  const getStatusClass = (status) => {
    if (!status) return ''
    const statusLower = status.toLowerCase()
    if (statusLower.includes('success') || statusLower.includes('healthy')) return 'success-card'
    if (statusLower.includes('sub-healthy') || statusLower.includes('warning')) return 'warning-card'
    if (statusLower.includes('error') || statusLower.includes('unhealthy')) return 'error-card'
    return 'info-card'
  }
  
  // 格式化日期
  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A'
    return new Date(timestamp).toLocaleString()
  }
  
  // 格式化数字
  const formatNumber = (num) => {
    if (num === null || num === undefined) return 'N/A'
    return num.toLocaleString()
  }
  
  // 格式化字节大小
  const formatBytes = (bytes) => {
    if (!bytes) return 'N/A'
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(1024))
    return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
  }
  
  // 下载报告
  const downloadReport = () => {
    if (!reportData.value?.output_content) return
    
    const blob = new Blob([reportData.value.output_content], { type: 'text/plain' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `health-report-${Date.now()}.txt`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('Report downloaded')
  }
  
  onMounted(() => {
    fetchReport()
  })
  </script>
  
  <style scoped>
  .health-report {
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
  
  .overview-cards {
    margin-bottom: 30px;
  }
  
  .status-card {
    text-align: center;
    padding: 20px;
    border-radius: 12px;
    transition: transform 0.3s, box-shadow 0.3s;
  }
  
  .status-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  }
  
  .card-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    margin-bottom: 15px;
    color: #666;
    font-size: 14px;
    font-weight: 500;
  }
  
  .card-value {
    font-size: 24px;
    font-weight: bold;
    color: #333;
  }
  
  .card-value.small-text {
    font-size: 14px;
  }
  
  .success-card {
    background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
    border: 2px solid #4caf50;
  }
  
  .success-card .card-value {
    color: #2e7d32;
  }
  
  .warning-card {
    background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
    border: 2px solid #ff9800;
  }
  
  .warning-card .card-value {
    color: #e65100;
  }
  
  .error-card {
    background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
    border: 2px solid #f44336;
  }
  
  .error-card .card-value {
    color: #c62828;
  }
  
  .info-card {
    background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
    border: 2px solid #2196f3;
  }
  
  .info-card .card-value {
    color: #1565c0;
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
  
  .info-detail-card {
    border-radius: 8px;
    transition: box-shadow 0.3s;
  }
  
  .info-detail-card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  
  .info-detail-card h3 {
    margin-bottom: 15px;
    color: #444;
    font-size: 16px;
  }
  
  .content-card {
    border-radius: 8px;
  }
  
  .formatted-content {
    padding: 20px;
    line-height: 1.8;
    color: #333;
    max-height: 600px;
    overflow-y: auto;
  }
  
  .formatted-content :deep(.content-title) {
    font-size: 20px;
    font-weight: bold;
    color: #1e90ff;
    margin: 25px 0 15px 0;
    padding-bottom: 8px;
    border-bottom: 2px solid #e0e0e0;
  }
  
  .formatted-content :deep(.metric-name) {
    font-size: 16px;
    color: #ff6b6b;
    margin: 20px 0 10px 0;
    font-weight: 600;
  }
  
  .formatted-content :deep(.section-label) {
    color: #4caf50;
    font-weight: bold;
    font-size: 15px;
  }
  
  .raw-content {
    font-family: 'Courier New', monospace;
  }
  
  .action-buttons {
    margin-top: 30px;
    text-align: center;
    display: flex;
    justify-content: center;
    gap: 15px;
  }
  </style>
  