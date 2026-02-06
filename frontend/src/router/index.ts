import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import AuthLayout from '../layout/AuthLayout.vue';
import TopLayout from '../layout/TopLayout.vue';
import Login from '../pages/auth/Login.vue';
import Register from '../pages/auth/Register.vue';

const routes: Array<RouteRecordRaw> = [
    // 根路径重定向到资产管理（登录后默认页面）
    {
        path: '/',
        redirect: '/dashboard',
    },
    // 认证相关路由（登录、注册）
    {
        path: '/auth',
        component: AuthLayout,
        children: [
            {
                path: '/login',
                name: 'Login',
                component: Login,
                meta: { title: '登录 - FinCoach' }
            },
            {
                path: '/register',
                name: 'Register',
                component: Register,
                meta: { title: '注册 - FinCoach' }
            }
        ]
    },
    // 主应用路由 - 使用 TopLayout 顶部导航布局
    {
        path: '/',
        component: TopLayout,
        meta: { requiresAuth: true },
        children: [
            {
                path: '/dashboard',
                name: 'Dashboard',
                component: () => import('../views/dashboard/Index.vue'),
                meta: { requiresAuth: true, title: '资产管理 - FinCoach' }
            },
            {
                path: '/market',
                name: 'Market',
                component: () => import('../views/market/Index.vue'),
                meta: { requiresAuth: true, title: '市场中心 - FinCoach' }
            },
            {
                path: '/market/security/:id',
                name: 'SecurityDetail',
                component: () => import('../views/market/SecurityDetail.vue'),
                meta: { requiresAuth: true, title: '证券详情 - FinCoach' }
            },
            {
                path: '/diagnosis',
                name: 'Diagnosis',
                component: () => import('../views/diagnosis/Index.vue'),
                meta: { requiresAuth: true, title: '资产体检 - FinCoach' }
            },
            {
                path: '/plan',
                name: 'Plan',
                component: () => import('../views/plan/Index.vue'),
                meta: { requiresAuth: true, title: '投资计划 - FinCoach' }
            }
        ]
    },
    // 风险测评路由（保持独立，不使用 TopLayout）
    {
        path: '/risk/assessment',
        name: 'RiskAssessment',
        component: () => import('../views/risk/AssessmentWizard.vue'),
        meta: { requiresAuth: true, title: '风险测评 - FinCoach' }
    },
    {
        path: '/risk/result',
        name: 'RiskResult',
        component: () => import('../views/risk/AssessmentResult.vue'),
        meta: { requiresAuth: true, title: '测评结果 - FinCoach' }
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

// 全局路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token');

    if (to.meta.title) {
        document.title = to.meta.title as string;
    }

    if (to.meta.requiresAuth && !token) {
        next('/login');
    } else {
        next();
    }
});

export default router;
