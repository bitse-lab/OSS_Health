<template>
    <div>
      <div ref="lineChart" style="width: 100%; height: 300px;"></div>
    </div>
  </template>
  
  <script>
  import * as echarts from "echarts";
  
  export default {
    name: "LineChart",
    mounted() {
      this.initChart();
    },
    methods: {
      initChart() {
        const chartDom = this.$refs.lineChart;
        const myChart = echarts.init(chartDom);
  
        const option = {
          title: {
            // text: "时间与数量的关系",
          },
          tooltip: {
            trigger: "axis",
          },
          legend: {
            data: ['Software', 'Community', 'Market'], // 显示图例
          },
          xAxis: {
            type: "category",
            boundaryGap: false, // 不留间隙
            data: ["2025-01", "2025-02", "2025-03", "2025-04", "2025-05", "2025-06", "2025-07"],
            axisLabel: {
              formatter: (value) => {
                return value; // 格式化时间标签
              },
            },
          },
          yAxis: {
            type: "value",
            name: "num",
          },
          series: [
            {
              name: "Software",
              type: "line",
              data: [3, 10, 18, 8, 10, 20, 35], // 折线1的数据
              smooth: true, // 平滑曲线
              color: '#FF6347', // 自定义颜色
            },
            {
              name: "Community",
              type: "line",
              data: [10, 20, 35, 45, 50, 60, 75], // 折线2的数据
              smooth: true, // 平滑曲线
              color: '#4682B4', // 自定义颜色
            },
            {
              name: "Market",
              type: "line",
              data: [5, 15, 25, 40, 55, 65, 80], // 折线3的数据
              smooth: true, // 平滑曲线
              color: '#32CD32', // 自定义颜色
            },
          ],
        };
  
        myChart.setOption(option);
  
        // 监听窗口尺寸变化，自动调整图表大小
        window.addEventListener("resize", () => {
          myChart.resize();
        });
      },
    },
  };
  </script>
  
  <style scoped>
  /* 可自定义样式 */
  </style>
  