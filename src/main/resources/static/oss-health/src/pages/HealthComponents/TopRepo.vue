<!-- 显示对应的仓库的信息 -->
<template>
    <!-- 限定在一行中 -->
    <div class="topRepoShow">
        <!-- 插入gitHub的图标 -->
        <img src="@/assets/gitHub.jpg" height="50%" width="auto" style="margin-left: 20px;">
         <!-- 显示对应的仓库名称 -->
          <p style="margin-left: 10px;">vuejs/core</p>
    </div>
</template>

<script>

</script>

<style scoped>
.topRepoShow{
    display: flex;
    flex-direction: row;
    align-items: center; /* 垂直居中文本 */
    justify-content: flex-start; /* 左对齐 */
    border-radius: 5px;
    height: 50px;
    background-color: rgb(255, 255, 240);
}
</style>
  