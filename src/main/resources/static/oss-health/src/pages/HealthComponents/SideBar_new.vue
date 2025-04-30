<template>
  <div class="sidebar">
    <div class="logo">
      <span>Navigation Bar </span>
      <el-icon><Compass /></el-icon>
    </div>

    <el-menu
      class="el-menu-vertical no-select"
      :default-active="selectedMenuItem"
      background-color="#2f4050"
      text-color="#fff"
      active-text-color="#1ab394"
      @select="handleSelect"
    >
      <!-- OverView -->
      <el-menu-item index="OverView" @click="goToExactRoute('OverView', 'overViewTop')">
        <el-icon><Grid /></el-icon>
        <span>OverView</span>
      </el-menu-item>

      <!-- Software -->
      <el-sub-menu index="Software">
        <template #title>
          <el-icon><Cpu /></el-icon>
          <span >Software</span>
        </template>
        <el-menu-item index="Software-overview" @click="goToExactRoute('Software', 'softwareTop')">
          <el-icon><Menu /></el-icon> OverView
        </el-menu-item>
        <el-menu-item index="Software-quality" @click="goToExactRoute('Software', 'softwareQuality')">
          <el-icon><CircleCheck /></el-icon> Quality
        </el-menu-item>
        <el-menu-item index="Software-robustness" @click="goToExactRoute('Software', 'softwareRoubstness')">
          <el-icon><SetUp /></el-icon> Robustness
        </el-menu-item>
        <el-menu-item index="Software-productivity" @click="goToExactRoute('Software', 'softwareProductivity')">
          <el-icon><Histogram /></el-icon> Productivity
        </el-menu-item>
      </el-sub-menu>

      <!-- Community -->
      <el-sub-menu index="Community">
        <template #title>
          <el-icon><Avatar /></el-icon>
          <span>Community</span>
        </template>
        <el-menu-item index="Community-overview" @click="goToExactRoute('Community', 'communityTop')">
          <el-icon><Menu /></el-icon> OverView
        </el-menu-item>
        <el-menu-item index="Community-organization" @click="goToExactRoute('Community', 'communityOrganization')">
          <el-icon><OfficeBuilding /></el-icon> Organization
        </el-menu-item>
        <el-menu-item index="Community-resilience" @click="goToExactRoute('Community', 'communityResilience')">
          <el-icon><BellFilled /></el-icon> Resilience
        </el-menu-item>
        <el-menu-item index="Community-vigor" @click="goToExactRoute('Community', 'communityVigor')">
          <el-icon><MagicStick /></el-icon> Vigor
        </el-menu-item>
      </el-sub-menu>

      <!-- Market -->
      <el-sub-menu index="Market">
        <template #title>
          <el-icon><Coin /></el-icon>
          <span>Market</span>
        </template>
        <el-menu-item index="Market-overview" @click="goToExactRoute('Market', 'marketTop')">
          <el-icon><Menu /></el-icon> OverView
        </el-menu-item>
        <el-menu-item index="Market-competitiveness" @click="goToExactRoute('Market', 'marketCompetitiveness')">
          <el-icon><Medal /></el-icon> Competitiveness
        </el-menu-item>
        <el-menu-item index="Market-influence" @click="goToExactRoute('Market', 'marketInfluence')">
          <el-icon><Share /></el-icon> Influence
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import {
  Compass,
  Grid, Cpu, Menu, CircleCheck, SetUp, Histogram,
  Avatar, OfficeBuilding, BellFilled, MagicStick,
  Coin, Medal, Share
} from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 当前选中的菜单项（带子项）
const selectedMenuItem = ref('OverView')

const goToExactRoute = (routeName, hash = '') => {
  if (hash && !hash.startsWith('#')) hash = '#' + hash
  if (route.name === routeName) {
    const el = document.querySelector(hash)
    if (el) el.scrollIntoView({ behavior: 'smooth' })
    return
  }
  router.push({ name: routeName, hash }).then(() => {
    const el = document.querySelector(hash)
    if (el) {
      el.scrollIntoView()
      window.scrollBy(0, -60)
    }
  })
}

// 菜单点击时的处理函数
const handleSelect = (index) => {
  selectedMenuItem.value = index
  const [category] = index.split('-')
  sessionStorage.setItem('selectedCategory', category)
  sessionStorage.setItem('selectedMenuItem', index)
}

// 页面加载时恢复状态
onMounted(() => {
  selectedMenuItem.value = sessionStorage.getItem('selectedMenuItem') || 'OverView'
})
</script>

<style scoped>
.sidebar {
  background-color: #2f4050;
  color: #fff;
  min-height: 100vh;
  width: 220px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  font-size: 16px;
  font-weight: bold;
}

.logo img {
  height: 24px;
  width: auto;
  margin-right: 8px;
}

.no-select {
  user-select: none;
}
</style>
