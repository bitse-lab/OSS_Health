<template>
    <div class="sideBarShow">
        <!-- 按钮组 -->
        <div class="buttons">
            <div :class="{'OSSHealth': true, 'OSSHealth1':selectedCategory==='OverView'}" @click="selectCategory('OverView')">
                    OSSHealth
            </div>
            <div style="font-size: 80%" class="threeButtons">
                <div :class="{'threeButtons1': selectedCategory==='Software'}" @click="selectCategory('Software')">
                    Software
                </div>
                <div :class="{'threeButtons1': selectedCategory==='Community'}" @click="selectCategory('Community')">
                    Community
                </div>
                <div :class="{'threeButtons1': selectedCategory==='Market'}" @click="selectCategory('Market')">
                    Market
                </div>                
            </div>
        </div>

        <!-- 分类内容 -->
        <div v-if="selectedCategory === 'OverView'" class="category-section">
            <button @click="goToExactRoute('OverView', 'overViewTop')">OverView</button>
        </div>
        <div v-if="selectedCategory === 'Software'" class="category-section">
            <button @click="goToExactRoute('Software', 'softwareTop')">OverView</button>    
            <button class="hover-element" @mouseover="qualityShowList = true" @mouseleave="qualityShowList = false" @click="goToExactRoute('Software', 'softwareQuality')">Quality</button>
            <div v-if="qualityShowList" class="dropdown-list" style="top: 110px;" @mouseenter="qualityShowList = true" @mouseleave="qualityShowList = false">
                <ul>
                    <li>Option 1</li>
                    <li>Option 2</li>
                    <li>Option 3</li>
                </ul>
            </div>
            <button class="hover-element" @mouseover="robustnessShowList = true" @mouseleave="robustnessShowList = false" @click="goToExactRoute('Software', 'softwareRoubstness')">Robustness</button>
            <div v-if="robustnessShowList" class="dropdown-list" style="top: 145px" @mouseenter="robustnessShowList = true" @mouseleave="robustnessShowList = false">
                <ul>
                    <li>Option 1</li>
                    <li>Option 2</li>
                    <li>Option 3</li>
                </ul>
            </div>
            <button class="hover-element" @mouseover="productivityShowList = true" @mouseleave="productivityShowList = false" @click="goToExactRoute('Software', 'softwareProductivity')">Productivity</button>
            <div v-if="productivityShowList" class="dropdown-list" style="top: 180px;" @mouseenter="productivityShowList = true" @mouseleave="productivityShowList = false">
                <ul>
                    <li>Option 1</li>
                    <li>Option 2</li>
                    <li>Option 3</li>
                </ul>
            </div>
        </div>
        <div v-if="selectedCategory === 'Community'" class="category-section">
            <button @click="goToExactRoute('Community', 'communityTop')">OverView</button>
            <button class="hover-element" @mouseover="organizationShowList = true" @mouseleave="organizationShowList = false" @click="goToExactRoute('Community', 'communityOrganization')">Organization</button>
            <div v-if="organizationShowList" class="dropdown-list" style="top: 110px;" @mouseenter="organizationShowList = true" @mouseleave="organizationShowList = false">
                <ul>
                    <li>Option 1</li>
                    <li>Option 2</li>
                    <li>Option 3</li>
                </ul>
            </div>
            <button class="hover-element" @mouseover="resilienceShowList = true" @mouseleave="resilienceShowList = false" @click="goToExactRoute('Community', 'communityResilience')">Resilience</button>
            <div v-if="resilienceShowList" class="dropdown-list" style="top: 145px;" @mouseenter="resilienceShowList = true" @mouseleave="resilienceShowList = false">
                <ul>
                    <li>Option 1</li>
                    <li>Option 2</li>
                    <li>Option 3</li>
                </ul>
            </div>
            <button class="hover-element" @mouseover="vigorShowList = true" @mouseleave="vigorShowList = false" @click="goToExactRoute('Community', 'communityVigor')">Vigor</button>
            <div v-if="vigorShowList" class="dropdown-list" style="top: 180px;" @mouseenter="vigorShowList = true" @mouseleave="vigorShowList = false">
                <ul>
                    <li>Option 1</li>
                    <li>Option 2</li>
                    <li>Option 3</li>
                </ul>
            </div>
        </div>
        <div v-if="selectedCategory === 'Market'" class="category-section">
            <button @click="goToExactRoute('Market', 'marketTop')">OverView</button>
            <button class="hover-element" @mouseover="competitivenessShowList = true" @mouseleave="competitivenessShowList = false" @click="goToExactRoute('Market', 'marketCompetitiveness')">Competitiveness</button>
            <div v-if="competitivenessShowList" class="dropdown-list" style="top: 110px;" @mouseenter="competitivenessShowList = true" @mouseleave="competitivenessShowList = false">
                <ul>
                    <li>Option 1</li>
                    <li>Option 2</li>
                    <li>Option 3</li>
                </ul>
            </div>
            <button class="hover-element" @mouseover="influenceShowList = true" @mouseleave="influenceShowList = false" @click="goToExactRoute('Market', 'marketInfluence')">Influence</button>
            <div v-if="influenceShowList" class="dropdown-list" style="top: 145px;" @mouseenter="influenceShowList = true" @mouseleave="influenceShowList = false">
                <ul>
                    <li>Option 1</li>
                    <li>Option 2</li>
                    <li>Option 3</li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    data() {
        return {
            selectedCategory: this.loadCategory() || 'OverView',
            qualityShowList: false,
            robustnessShowList: false,
            productivityShowList: false,
            organizationShowList: false,
            resilienceShowList: false,
            vigorShowList: false,
            competitivenessShowList: false,
            influenceShowList: false,
        };
    },
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
        // 进行分类内容选择
        selectCategory(category) {
            this.selectedCategory = category;
            sessionStorage.setItem('selectedCategory', category);
        },
        loadCategory() {
            return sessionStorage.getItem('selectedCategory');
        },
    },
};
</script>

<style> 
.sideBarShow{
    display: flex;
    flex-direction: column;
    align-items: center;
    border-radius: 5px;
    /* width: 200px; */
    background-color: rgb(250, 240, 230);
}

.buttons{
    display: flex;
    flex-direction: column;
    align-items: center;
    border-radius: 5px;
    width: 100%;
    background-color: rgb(220,220,220);
}

.OSSHealth{
    padding: 5px;
    margin-top: 5px;
    cursor: pointer;
}

.OSSHealth1{
    border-radius: 5px;
    background-color: white;
}

.threeButtons{
    display: flex;
    flex-direction: row;
    width: 100%;
    margin: 5px;
    cursor: pointer;
}

.threeButtons1{
    border-radius: 5px;
    background-color: white;
}

.threeButtons div{
    flex:1;
}

.category-section{
    display: flex;
    flex-direction: column;
    width: 100%;
}
.category-section button{
    font-size: 100%;
    margin: 5px;
}

.dropdown-list {
    position: absolute;
    left: 205px;
    background-color: white;
    width: 150px;
}
</style>