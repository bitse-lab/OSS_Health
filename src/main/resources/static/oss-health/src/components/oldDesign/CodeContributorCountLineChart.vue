<template>
    <div class="all">
      <h2>Code Contributor Count</h2>
      <!-- 按钮，点击时触发 fetchData 方法 -->
      <button @click="fetchData">加载数据</button>
  
      <!-- 显示提示消息 -->
      <div v-if="errorMessage" style="color: red; font-weight: bold;">
        {{ errorMessage }}
      </div>
  
      <!-- ECharts 容器 -->
      <div ref="chart" style="width: 100%; height: 400px;"></div>
    </div>
  </template>
  
  <script>
  // 导入 ECharts
  import * as echarts from 'echarts'
  import axios from 'axios'
  
  export default {
    data() {
      return {
        chartInstance: null,  // 用于存储 ECharts 实例
        chartData: {
          total: [],        // 存储总的长期贡献者数据
          codeCommitters: [], // 存储代码提交者数据
          prSubmitters: [], // 存储PR提交者数据
          reviewers: []     // 存储审核者数据
        },
        errorMessage: ''      // 存储错误消息
      }
    },
    methods: {
      // 请求数据并更新图表
      async fetchData() {
        try {
          // 发送并发请求获取数据
          const [totalResponse, codeCommittersResponse, prSubmittersResponse, reviewersResponse] = await Promise.all([
            axios.get('http://localhost:8080/api/codecontributorcount/total'),
            axios.get('http://localhost:8080/api/codecontributorcount/codecommiter'),
            axios.get('http://localhost:8080/api/codecontributorcount/prsubmitter'),
            axios.get('http://localhost:8080/api/codecontributorcount/reviewer')
          ])
          
          // 检查返回的数据格式并赋值
          this.chartData.total = totalResponse.data || []
          this.chartData.codeCommitters = codeCommittersResponse.data || []
          this.chartData.prSubmitters = prSubmittersResponse.data || []
          this.chartData.reviewers = reviewersResponse.data || []
          
          // 更新图表
          this.updateChart()
  
        } catch (error) {
          // 捕获错误并显示错误消息
          console.error('Error fetching data:', error)
          this.errorMessage = "Can't get JSON data from APIs"
        }
      },
  
      // 更新 ECharts 图表
      updateChart() {
        if (this.chartInstance) {
          // 格式化时间，保留到月份
          const times = this.chartData.total.map(item => {
              const date = new Date(item.time); // 假设 item.time 是日期字符串
              return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}`; // 格式化为 YYYY-MM
          });
  
          const total = this.chartData.total.map(item => item.number)
          const codeCommitters = this.chartData.codeCommitters.map(item => item.number)
          const prSubmitters = this.chartData.prSubmitters.map(item => item.number)
          const reviewers = this.chartData.reviewers.map(item => item.number)
  
          const option = {
            title: {
              text: ''
            },
            tooltip: {
              trigger: 'axis'
            },
            legend: {
              data: ['Total', 'Code Committers', 'PR Submitters', 'Reviewers']
            },
            xAxis: {
              type: 'category',
              data: times,  // X 轴数据为 time
              name: 'Time'
            },
            yAxis: {
              type: 'value',
              name: 'Number'
            },
            series: [
              {
                name: 'Total',
                data: total,  // Y 轴数据为 number
                type: 'line',
                smooth: true,
                color: 'red',
                lineStyle: {
                    opacity: 0.5  // 设置为半透明
                }
              },
              {
                name: 'Code Committers',
                data: codeCommitters,
                type: 'line',
                smooth: true,
                color: 'yellow',
                lineStyle: {
                    opacity: 0.5  // 设置为半透明
                }
              },
              {
                name: 'PR Submitters',
                data: prSubmitters,
                type: 'line',
                smooth: true,
                color: 'blue',
                lineStyle: {
                    opacity: 0.5  // 设置为半透明
                }
              },
              {
                name: 'Reviewers',
                data: reviewers,
                type: 'line',
                smooth: true,
                color: 'black',
                lineStyle: {
                    opacity: 0.5  // 设置为半透明
                }
              }
            ]
          }
  
          // 使用设置好的配置项更新图表
          this.chartInstance.setOption(option)
        }
      }
    },
    mounted() {
      // 初始化 ECharts 实例
      this.chartInstance = echarts.init(this.$refs.chart)
    },
    beforeUnmount() {
      // 在组件销毁之前清理 ECharts 实例
      if (this.chartInstance) {
        this.chartInstance.dispose()
      }
    }
  }
  </script>
  
  <style scoped>
  .all{
    background-color: rgba(173, 216, 230, 0.2); /* 浅蓝色背景，透明度20% */
  }
  
  button {
    margin-bottom: 20px;
    padding: 10px;
    background-color: #42A5F5;
    color: white;
    border: none;
    border-radius: 5px;
    cursor: pointer;
  }
  
  button:hover {
    background-color: #3498db;
  }
  </style>
  