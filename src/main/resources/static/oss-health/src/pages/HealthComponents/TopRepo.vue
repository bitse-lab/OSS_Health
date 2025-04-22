<!-- 显示对应的仓库的信息 -->
<template>
    <el-row class="topRepoShow">
      <!-- GitHub 图标 -->
      <el-avatar
        :src="require('@/assets/gitHub.jpg')"
        shape="square"
        size="medium"
        class="repo-icon"
      />
  
      <!-- 仓库名称 -->
      <el-text class="repo-name" size="large">
        vuejs/core
      </el-text>
    </el-row>
</template>
  
<script setup>

</script>
  
<style scoped>
.topRepoShow {
    background-color: rgb(255, 255, 240);
    height: 50px;
    padding-left: 20px;
    border-radius: 5px;
    display: flex;
    align-items: center;
    margin-top: 5px;
}
  
.repo-icon {
    margin-right: 10px;
}
  
.repo-name {
    font-weight: 500;
}
</style>
  

  