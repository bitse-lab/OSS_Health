<template>
  <el-layout class="healthShow">
    <!-- 展示当前选择的库信息 -->
    <el-header class="repoShow">
      <TopRepo />
    </el-header>

    <!-- 左侧展示侧边栏，右侧展示对应信息 -->
    <el-container class="sideBarAndContent">
      <!-- 侧边栏 -->
      <el-aside class="sideBar" width="220px">
        <SideBar />
      </el-aside>

      <!-- 展示信息 -->
      <el-main class="content">
        <!-- 加载子路由 -->
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-layout>
</template>

<script>
import TopRepo from './HealthComponents/TopRepo.vue';
import SideBar from './HealthComponents/SideBar_new.vue';

export default {
  name: 'HealthShow',
  components: {
    TopRepo,
    SideBar,
  },
};
</script>

<style scoped>
.healthShow {
  height: 100vh; /* 全屏高度 */
  background-color: rgb(240, 248, 255); /* 背景颜色 */
}

.repoShow {
  height: 60px;
  background-color: rgb(255, 255, 240);
  z-index: 100;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  border-bottom: 1px solid #ddd;
}

.sideBarAndContent {
  display: flex;
  flex-direction: row;
}

.sideBar {
  background-color: white;
  padding: 7px;
  z-index: 99;
  position: fixed;
  top: 60px; /* 下面顶栏 */
  left: 0;
  width: 220px;
  height: calc(100vh - 60px); /* 减去顶部栏 */
  overflow: hidden; /* 禁止滚动条 */
}

.content {
  background-color: rgb(240, 248, 255);
  z-index: 1;
  padding: 20px;
  margin-left: 220px; /* 与侧边栏宽度一致 */
  margin-top: 60px;   /* 与顶部栏高度一致 */
  min-height: calc(100vh - 60px); /* 保证至少占满可视区域 */
}
</style>
