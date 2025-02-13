<template>
    <!-- 整体垂直布局 -->
    <div class="healthShow">
        <!-- 展示当前选择的库信息 -->
        <div class="repoShow">
            <TopRepo />
        </div>
        <div class="repoShowPosition"></div>
        <!-- 左侧展示侧边栏，右侧展示对应信息 -->
        <div class="sideBarAndContent">
            <!-- 侧边栏 -->
            <div class="sideBar">
                <SideBar />
            </div>
            <!-- 展示信息 -->
             <div class="content">
                <!-- 加载子路由 -->
                <router-view></router-view> 
             </div>
        </div>
    </div>
</template>

<script>
import TopRepo from './HealthComponents/TopRepo.vue';
import SideBar from './HealthComponents/SideBar.vue';
export default {
  name: 'HealthShow',
  components: {
    TopRepo,
    SideBar,
  },
};
</script>

<style scoped>
.healthShow{
    display: flex;
    flex-direction: column;
}

.repoShow{
    position: fixed; /* 固定定位 */
    top: 5px;
    left: 5px;
    right: 5px;
    z-index: 4;
}

.repoShowPosition{
    position: fixed;
    left: 5px;
    right: 5px;
    top: 0px;
    height: 60px;
    background-color: white;
    z-index: 3;
}

.sideBarAndContent{
    display: flex;
    flex-direction: row;
}

.sideBar{
    align-items: center; /* 垂直居中文本 */
    width: 200px;
    position: fixed; /* 固定定位 */
    left: 5px; /* 固定在左侧 */
    top:60px;
    bottom: 0; /* 占满高度 */
    padding: 10px;
    background-color: rgb(250, 240, 230);
    z-index: 2;
}

.content{
    position: absolute;
    top: 60px;
    left: 230px;
    right: 5px;
    background-color: rgb(240, 248, 255);
    z-index: 1;
}
</style>