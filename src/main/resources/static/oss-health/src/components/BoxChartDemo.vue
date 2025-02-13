<template>
    <div>
      <div ref="barChart" style="width: 100%; height: 300px;"></div>
    </div>
  </template>
  
  <script>
  import * as echarts from "echarts";
  
  export default {
    name: "BarChart",
    mounted() {
      this.initChart();
    },
    methods: {
      initChart() {
        const chartDom = this.$refs.barChart;
        const myChart = echarts.init(chartDom);
  
        const option = {
          title: {
            // text: "柱状图示例",
            // subtext: "每月销售额",
          },
          tooltip: {
            trigger: "axis",
          },
          // legend: {
          //   data: ["得分"],
          // },
          xAxis: {
            type: "category",
            data: ["一", "二", "三"], // 类别数据
          },
          yAxis: {
            type: "value",
            name: "score",
          },
          series: [
            {
              name: "得分",
              type: "bar", // 使用柱状图
              data: [30, 50, 20], // Y 轴数据
              itemStyle: {
                color: "#3398DB", // 设置柱子的颜色
              },
              barMaxWidth: 30,
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
  