<template>
  <div>
    <div class="overview-button">
      <button :class="{ active: selectedCategory === 'Overview' }" @click="selectCategory('Overview')">
        OVERVIEW
      </button>
    </div>
    <!-- 按钮组 -->
    <div class="buttons">
      <button :class="{ active: selectedCategory === 'Software' }" @click="selectCategory('Software')">
        Software
      </button>
      <button :class="{ active: selectedCategory === 'Community' }" @click="selectCategory('Community')">
        Community
      </button>
      <button :class="{ active: selectedCategory === 'Market' }" @click="selectCategory('Market')">
        Market
      </button>
    </div>

    <!-- 分类内容 -->
    <div v-if="selectedCategory === 'Software'" class="category-section">
      <button>Quality</button>
      <button>Robustness</button>
      <button>Productivity</button>
    </div>
    <div v-if="selectedCategory === 'Community'" class="category-section">
      <button>Organization</button>
      <button>Resilience</button>
      <button>Vigor</button>
      <div class="long-term-contributors">LongTermContributors</div>
    </div>
    <div v-if="selectedCategory === 'Market'" class="category-section">
      <button>Competitiveness</button>
      <button>Influence</button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      selectedCategory: null,
    };
  },
  methods: {
    selectCategory(category) {
      this.selectedCategory = category;
    },
  },
};
</script>

<style scoped>
/* OVERVIEW 按钮样式 */
.overview-button {
  margin-bottom: 10px; /* 与其他按钮组之间的间距 */
  text-align: center; /* 居中对齐 */
}

.overview-button button {
  padding: 10px 100px;
  background-color: #f0f0f0;
  border: 1px solid #ccc;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  text-align: center;
}

/* 按钮组样式 */
.buttons {
  display: flex;
  justify-content: space-between; /* 均匀分布按钮 */
  background-color: rgba(173, 216, 230, 0.1); /* 浅蓝色背景，透明度10% */
  padding: 10px;
  border-radius: 5px;
}

.buttons button {
  flex: 1; /* 按钮等宽 */
  margin: 0 5px; /* 按钮间距 */
  padding: 10px;
  background-color: white; /* 白色背景 */
  border: 1px solid #ddd; /* 浅灰色边框 */
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  text-align: center; /* 按钮文字居中 */
}

.buttons button.active {
  background-color: #e6f7ff; /* 激活按钮背景色 */
  border-color: #1890ff; /* 激活按钮边框色 */
  color: #1890ff; /* 激活按钮文字色 */
}

/* 分类内容样式 */
.category-section {
  display: flex;
  flex-direction: column; /* 垂直排列 */
  margin-top: 20px;
}

.category-section button {
  display: block; /* 独占一行 */
  width: 100%; /* 填充宽度 */
  margin: 10px 0; /* 移除外边距 */
  padding: 10px;
  font-size: 14px;
  color: #333;
  background-color: white; /* 白色背景 */
  border: 1px solid #ddd; /* 灰色边框 */
  border-bottom: 1px solid #eee; /* 分隔线 */
  cursor: pointer;
  text-align: left; /* 左对齐文字 */
}

.category-section button:last-child {
  border-bottom: 1px solid #ddd; /* 去掉最后一个按钮的底部分隔线 */
}

/* LongTermContributors 样式 */
.long-term-contributors {
  font-size: 0.9rem; /* 小一号字体 */
  color: #666; /* 深灰色文字 */
  margin-top: 10px; /* 与上方按钮间距 */
  padding: 10px;
  border: 1px dashed #1890ff; /* 蓝色虚线边框 */
  background-color: #f9f9f9; /* 浅灰背景 */
}
</style>
