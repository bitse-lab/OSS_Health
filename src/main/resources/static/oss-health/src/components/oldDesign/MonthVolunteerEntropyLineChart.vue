<template>
    <div class="all">
        <h2>Volunteer Information Entropy</h2>
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
        chartData: [],        // 存储获取到的数据
        errorMessage: ''      // 存储错误消息
      }
    },
    methods: {
      // 请求数据并更新图表
      async fetchData() {
        try {
          // 发送请求获取数据
          const response = await axios.get('http://localhost:8080/api/monthvolunteerentropy')
          console.log(response.data)
  
          // 检查返回的是否是JSON格式
          if (Array.isArray(response.data)) {
            // 假设返回的数据是一个数组，包含 time 和 number 字段
            this.chartData = response.data
  
            // 更新图表
            this.updateChart()
  
          } else {
            throw new Error("Invalid JSON format")
          }
  
        } catch (error) {
          // 捕获错误并显示错误消息
          console.error('Error fetching data:', error)
          this.errorMessage = "Can't get JSON"
        }
      },
  
      // 更新 ECharts 图表
      updateChart() {
        if (this.chartInstance) {
          // 格式化时间，保留到月份
          const times = this.chartData.map(item => {
            const date = new Date(item.time); // 假设 item.time 是日期字符串
            return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}`; // 格式化为 YYYY-MM
          });
          const numbers = this.chartData.map(item => item.number)
  
          const option = {
            title: {
              text: ''
            },
            tooltip: {
              trigger: 'axis'
            },
            xAxis: {
              type: 'category',
              data: times,  // X 轴数据为 time
              name: 'Time'
            },
            yAxis: {
              type: 'value',
              name: 'Number',
            },
            series: [{
              data: numbers,  // Y 轴数据为 number
              type: 'line',  // 折线图
              smooth: true  // 平滑曲线
            }]
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
  