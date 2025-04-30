import { createRouter, createWebHistory } from 'vue-router'
//引入对应的page 
// import longTermContributors from './pages/longTermContributors.vue'
import HealthShow from './pages/HealthShow.vue';
import OverViewOSS from './pages/HealthComponents/OverViewOSS.vue';
import SoftwareOSS from './pages/HealthComponents/SoftwareOSS.vue';
import CommunityOSS from './pages/HealthComponents/CommunityOSS.vue';
import MarketOSS from './pages/HealthComponents/MarketOSS.vue';
import HomePage from './pages/HomePage.vue';
import DocsPage from './pages/DocsPage.vue';
import AboutPage from './pages/AboutPage.vue';

const routes = [
  {
    path: "/",
    name: "Start",
    redirect: "/healthshow",
  },
  {
    path: "/home",
    name: "Home",
    component: HomePage,
  },
  {
    path: "/healthshow",
    name: "Repos",
    component: HealthShow,
    redirect: "/healthshow/overview", // 默认跳转到 overview
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
  {
    path: "/docs",
    name: "Docs",
    component: DocsPage,
  },
  {
    path: "/about",
    name: "About",
    component: AboutPage,
  },
];

const router = createRouter({
  history: createWebHistory(), // 使用 HTML5 History 模式
  routes, // 路由配置
});

export default router;