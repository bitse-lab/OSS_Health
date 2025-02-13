<template>
    <div class="container">
      <!-- 左侧 -->
      <div class="left">
        <!-- 左侧标题 -->
        <div class="title">Community</div>
        <!-- 左侧雷达图（用图片代替） -->
        <div class="radar-chart">
          <img src="../assets/image.png" alt="Community Radar Chart" class="radar-image" />
        </div>
      </div>
  
      <!-- 右侧 -->
      <div class="right">
        <!-- 右侧标题 -->
        <div class="title">Vigor</div>
        <!-- 右侧雷达图（用图片代替） -->
        <div class="radar-chart">
          <img src="../assets/image.png" alt="Vigor Radar Chart" class="radar-image" />
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'RadarPage',
  };
  </script>
  
  <style scoped>
  /* 容器布局 */
  .container {
    display: flex; /* 使用 flexbox 布局 */
    justify-content: space-between; /* 左右均匀分布 */
    height: 35vh; /* 高度为视口高度 */
  }
  
  /* 左侧 */
  .left,
  .right {
    width: 48%; /* 每侧占据 48% 的宽度，留出间隙 */
    display: flex;
    flex-direction: column; /* 垂直排列 */
    justify-content: space-between; /* 标题和雷达图之间均匀分配空间 */
  }
  
  .title {
    text-align: center;
    font-size: 24px;
    font-weight: bold;
    margin-bottom: 20px; /* 标题和图表之间的间距 */
  }
  
  .radar-chart {
    flex-grow: 1; /* 让雷达图占据剩余空间 */
    display: flex;
    justify-content: center; /* 雷达图居中 */
    align-items: center; /* 垂直居中 */
    background-color: #f0f0f0; /* 背景色，可根据需求调整 */
    border-radius: 8px;
  }
  
  .radar-image {
    max-width: 90%;
    object-fit: contain; /* 保持图片比例，避免变形 */
  }
  </style>
  