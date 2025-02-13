<template>
    <div>
      <div ref="radarChart" style="width: 100%; height: 400px;"></div>
    </div>
  </template>
  
  <script>
  import * as echarts from "echarts";
  
  export default {
    name: "RadarChart",
    mounted() {
      this.initChart();
    },
    methods: {
      initChart() {
        const chartDom = this.$refs.radarChart;
        const myChart = echarts.init(chartDom);
  
        const option = {
          title: {
            // text: "雷达图示例",
            // subtext: "各项指标得分",
            left: "center",
          },
          tooltip: {
            trigger: "item",
          },
          radar: {
            // 雷达图的设置
            indicator: [
              { name: "维度一", max: 100 },
              { name: "维度二", max: 100 },
              { name: "维度三", max: 100 },
              { name: "维度四", max: 100 },
              { name: "维度五", max: 100 },
            ],
            radius: "70%", // 雷达图的半径
            shape: "circle", // 雷达图的形状（可选 'circle' 或 'polygon'）
            splitNumber: 5, // 雷达图的分割数
            name: {
              textStyle: {
                color: "#fff",
                backgroundColor: "#999",
                borderRadius: 3,
                padding: [3, 5],
              },
            },
            splitLine: {
              lineStyle: {
                color: [
                "rgba(255, 99, 132, 0.8)",
                "rgba(54, 162, 235, 0.8)",
                "rgba(255, 206, 86, 0.8)",
                "rgba(75, 192, 192, 0.8)",
                "rgba(153, 102, 255, 0.8)",
                ],
              },
            },
            splitArea: {
              areaStyle: {
                color: "rgba(255, 255, 255, 0.2)",
              },
            },
          },
          series: [
            {
              name: "各项得分",
              type: "radar", // 类型为雷达图
              data: [
                {
                  value: [80, 70, 90, 60, 85], // 各项数据值
                  name: "得分",
                  areaStyle: {
                    color: "rgba(0, 255, 255, 0.5)", // 区域的颜色
                  },
                },
              ],
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
  /* 可以根据需要自定义样式 */
  </style>
  