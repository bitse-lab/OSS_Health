<template>
  <div class="containerUp">
    <div class="Up1">
      <Up1 />
    </div>
    <div class="container">
      <div class="sidebar">
        <Left1 />
      </div>
      <div class="chart-container">
        <div>
          <LineChart2 />
        </div>
        <div>
          <LineChart1 />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import LineChart1 from '../components/LineChart.vue';
import Left1 from '../components/LineLeft.vue';
import Up1 from '../components/LineUp.vue';
import LineChart2 from '../components/ComponentChart.vue';

export default {
  name: 'App',
  components: {
    LineChart1,
    Left1,
    Up1,
    LineChart2,
  },
};
</script>

<style>
.containerUp {
  display: flex;
  flex-direction: column; /* 改为垂直布局 */
  height: 100vh; /* 设置整个页面的高度 */
}

.Up1 {
  padding: 10px; /* 可调整根据实际需求 */
  background-color: #f0f0f0; /* 可选，设置背景颜色 */
}

.container {
  display: flex;
  flex-grow: 1; /* 让这个容器占据剩余空间 */
}

.sidebar {
  width: 20%;
  padding: 20px;
  border-right: 1px solid #ccc;
  overflow-y: auto; /* 超出部分滚动 */
}

.chart-container {
  width: 75%;
  padding: 20px;
  flex-direction: column; /* 垂直排列 */
  overflow-y: auto; /* 超出部分滚动 */
}
</style>
