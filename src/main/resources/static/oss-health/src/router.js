import { createRouter, createWebHistory } from 'vue-router'
//引入对应的page 
// import longTermContributors from './pages/longTermContributors.vue'
import HealthShow from './pages/HealthShow.vue';
import OverViewOSS from './pages/HealthComponents/OverViewOSS.vue';
import SoftwareOSS from './pages/HealthComponents/SoftwareOSS.vue';
import CommunityOSS from './pages/HealthComponents/CommunityOSS.vue';
import MarketOSS from './pages/HealthComponents/MarketOSS.vue';

const routes = [
  {
    path: "/",
    component: HealthShow,
    redirect: "overview",
    // 定义子路由
    children: [
      {
        path: "overview",
        name: "OverView",
        component: OverViewOSS,
      },
      {
        path: "software",
        name: "Software",
        component: SoftwareOSS,
      },
      {
        path: "community",
        name: "Community",
        component: CommunityOSS,
      },
      {
        path: "market",
        name: "Market",
        component: MarketOSS,
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(), // 使用 HTML5 History 模式
  routes, // 路由配置
});

export default router;