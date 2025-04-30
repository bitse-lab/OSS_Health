<template>
  <header class="navbar">
    <div class="navbar-left">
      <img src="@/assets/logo.png" alt="Logo" class="logo" />
      <span class="brand">OSS Health Monitor</span>
      <nav class="nav-links">
        <a
          @click="() => router.push('/home')"
          :class="{ active: route.path === '/home' }"
        >
          <el-icon><HomeFilled /></el-icon>
          Home
        </a>
        <a
          @click="() => router.push('/healthshow')"
          :class="{ active: route.path.startsWith('/healthshow') }"
        >
          <el-icon><Folder /></el-icon>
          Projects
        </a>
        <a
          @click="() => router.push('/docs')"
          :class="{ active: route.path === '/docs' }"
        >
          <el-icon><Document /></el-icon>
          Docs
        </a>
        <a
          @click="() => router.push('/about')"
          :class="{ active: route.path === '/about' }"
        >
          <el-icon><Link /></el-icon>
          About
        </a>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { Document, Folder, Link, HomeFilled } from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
</script>

<style scoped>
.navbar {
  background-color: #000;
  color: #fff;
  padding: 14px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1000;
}

.navbar-left {
  display: flex;
  align-items: center;
}

.logo {
  width: 32px;
  height: 32px;
  margin-right: 16px;
}

.brand {
  font-weight: bold;
  font-size: 18px;
  margin-right: 40px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-links a {
  color: #fff;
  text-decoration: none;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s, border-bottom 0.2s;
}

.nav-links a:hover {
  color: #409eff; /* 鼠标悬停时为蓝色 */
}

.nav-links a.active {
  color: orange; /* 当前路由高亮为橙色 */
  border-bottom: 2px solid orange;
  font-weight: bold;
}
</style>
