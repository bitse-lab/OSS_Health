<template>
    <div class="sideBarShow">
        <h1>导航图</h1>
        <img src="@/assets/healthImage.png" usemap="#image-map" height="auto" width="200px">
        <p>点击图片对应位置可跳转到对应的维度进行展示</p>
    </div>
    <map name="image-map">
        <!-- 使用 Vue Router 的 to 属性 -->
        <!-- OSS Health -->
        <area shape="circle" coords="100,100,33" @click="goToExactRoute('OverView', 'overViewTop')">
        <!-- Software -->
        <area shape="poly" coords="40,130, 44,65, 96,34, 96,65, 70,83, 68,113" @click="goToExactRoute('Software', 'softwareTop')">
            <!-- quality -->
            <area shape="poly" coords="12,145, 2,86, 33,91, 40,129" @click="goToExactRoute('Software', 'softwareQuality')">
            <!-- robustness -->
            <area shape="poly" coords="3,81, 34,25, 54,50, 35,86" @click="goToExactRoute('Software', 'softwareRoubstness')">
            <!-- productivity -->
            <area shape="poly" coords="38,22, 96,1, 96,33, 59,46" @click="goToExactRoute('Software', 'softwareProductivity')">
        <!-- Community -->
        <area shape="poly" coords="103,34, 158,69, 158,129, 131,113, 129,82, 103,66" @click="goToExactRoute('Community', 'communityTop')">
            <!-- organization -->
            <area shape="poly" coords="102,1, 163,24, 142,48, 103,33" @click="goToExactRoute('Community', 'communityOrganization')">
            <!-- resilience -->
            <area shape="poly" coords="166,28, 197,84, 165,89, 146,52" @click="goToExactRoute('Community', 'communityResilience')">
            <!-- vigor -->
            <area shape="poly" coords="198,88, 187,146, 158,129, 166,93" @click="goToExactRoute('Community', 'communityVigor')">
        <!-- Market -->
        <area shape="poly" coords="155,135, 99,166, 44,135, 72,119, 99,134, 127.120" @click="goToExactRoute('Market', 'marketTop')">
            <!-- competitiveness -->
            <area shape="poly" coords="16,152, 97,198, 96,168, 43,136" @click="goToExactRoute('Market', 'marketCompetitiveness')">
            <!-- influence -->
            <area shape="poly" coords="102,198, 183,152, 156,136, 102,167" @click="goToExactRoute('Market', 'marketInfluence')">
    </map>
</template>

<script>
export default {
    methods: {
        goToRoute(routeName) {
        // 使用 Vue Router 进行跳转
        this.$router.push({ name: routeName });
        },
        goToExactRoute(routeName, hash = ''){
            if (hash && !hash.startsWith('#')) {
                hash = '#' + hash;
            }
            const currentRoute = this.$route;
            if (currentRoute.name == routeName){
                const element = document.querySelector(hash);
                if (element) {
                    // 使元素滚动到页面可见
                    element.scrollIntoView({ behavior: 'smooth'});
                }
                return;
            }
            this.$router.push({ name: routeName, hash }).then(() => {
                // 如果跳转成功，等待路由更新后自动滚动到对应的 hash
                const element = document.querySelector(hash);
                if (element) {
                    // 使元素滚动到页面可见
                    element.scrollIntoView({});
                    window.scrollBy(0, -60);
                }
            })
        },
    },
};
</script>

<style> 
.sideBarShow{
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 5px;
    border-radius: 5px;
    /* width: 200px; */
    background-color: rgb(250, 240, 230);
}
</style>