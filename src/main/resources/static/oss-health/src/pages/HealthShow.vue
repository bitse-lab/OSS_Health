<template>
  <el-layout class="healthShow">
    <!-- 顶部栏 -->
    <el-header class="repoShow">
      <TopBar />
    </el-header>

    <!-- 主体区域 -->
    <el-container class="sideBarAndContent">
      <!-- 侧边栏 -->
      <el-aside class="sideBar" width="220px">
        <SideBar />
      </el-aside>

      <!-- TopRepo 固定显示，带动画 -->
      <el-collapse-transition>
        <div v-show="!isHidden" class="fixed-toprepo">
          <TopRepo />
        </div>
      </el-collapse-transition>

      <!-- 主内容区域 -->
      <el-main class="content">
        <router-view ></router-view>
      </el-main>
    </el-container>
  </el-layout>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import SideBar from './HealthComponents/SideBar_new.vue';
import TopBar from './HealthComponents/TopBar.vue';
import TopRepo from './HealthComponents/TopRepo.vue';

export default {
  name: 'HealthShow',
  components: {
    SideBar,
    TopBar,
    TopRepo,
  },
  setup() {
    const isHidden = ref(false);
    let lastScroll = 0;

    const handleScroll = () => {
      const current = window.scrollY;
      if (current > lastScroll && current > 100) {
        setTimeout(() => {
        isHidden.value = true;
        }, 500); // 向下滚动隐藏
      } else if (current < lastScroll) {
        isHidden.value = false; // 向上滚动显示
      }
      lastScroll = current;
    };

    onMounted(() => {
      window.addEventListener('scroll', handleScroll);
    });

    onBeforeUnmount(() => {
      window.removeEventListener('scroll', handleScroll);
    });

    return { isHidden };
  },
};
</script>

<style scoped>
.healthShow {
  height: 100vh;
  background-color: rgb(240, 248, 255);
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
  top: 60px;
  left: 0;
  width: 220px;
  height: calc(100vh - 60px);
  overflow: hidden;
}

.fixed-toprepo {
  position: fixed;
  top: 60px; /* 紧贴 TopBar 下方 */
  left: 228px; /* 紧贴 SideBar 右边 */
  right: 8px;
  z-index: 90;
  background-color: rgb(250, 250, 240); /* 可自定义 */
  border-bottom: 1px solid #ddd;
  padding: 0px 20px;
}

.content {
  background-color: rgb(240, 248, 255);
  z-index: 1;
  padding: 20px;
  margin-left: 220px;
  margin-top: 120px; /* TopBar + TopRepo 高度 */
  min-height: calc(100vh - 60px);
}
</style>
